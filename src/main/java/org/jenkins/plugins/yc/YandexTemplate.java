package org.jenkins.plugins.yc;

import com.google.protobuf.TextFormat;
import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Saveable;
import hudson.model.labels.LabelAtom;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jenkins.model.Jenkins;
import jenkins.slaves.iterators.api.NodeIterator;
import org.jenkins.plugins.yc.exception.LoginFailed;
import org.jenkins.plugins.yc.util.YCAgentConfig;
import org.jenkins.plugins.yc.util.YCAgentFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import yandex.cloud.api.compute.v1.InstanceOuterClass;
import yandex.cloud.api.compute.v1.InstanceServiceGrpc;
import yandex.cloud.api.compute.v1.InstanceServiceOuterClass;
import yandex.cloud.api.operation.OperationOuterClass;
import yandex.cloud.sdk.ChannelFactory;
import yandex.cloud.sdk.ServiceFactory;
import yandex.cloud.sdk.auth.Auth;
import yandex.cloud.sdk.auth.jwt.ServiceAccountKey;
import yandex.cloud.sdk.auth.provider.CredentialProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jenkins.plugins.yc.AbstractCloud.DescriptorImpl.getCredentials;

public class YandexTemplate implements Describable<YandexTemplate> {

    private static final Logger LOGGER = Logger.getLogger(YandexTemplate.class.getName());

    protected transient AbstractCloud parent;

    private final String description;

    private final Node.Mode mode;
    private final String labels;
    private final String initScript;
    private final String remoteAdmin;

    private final String idleTerminationMinutes;

    private final int numExecutors;

    private final boolean stopOnTerminate;

    private final String remoteFS;

    private final String tmpDir;

    private static final String userData = "#cloud-config%nusers:%n  - name: %s%n    sudo: ['ALL=(ALL) NOPASSWD:ALL']%n    ssh-authorized-keys:%n      - %s";

    private final List<YCTag> tags;

    private DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties;

    private transient Set<LabelAtom> labelSet;

    public enum ProvisionOptions { ALLOW_CREATE, FORCE_CREATE }

    private transient CredentialProvider provider;
    private transient InstanceServiceGrpc.InstanceServiceBlockingStub instanceServiceBlockingStub;

    @DataBoundConstructor
    public YandexTemplate(String description, Node.Mode mode,
                          String labels, String initScript, String remoteFS, String tmpDir,
                          String remoteAdmin, String idleTerminationMinutes,
                          boolean stopOnTerminate, List<YCTag> tags,
                          int numExecutors) {
        this.labels = Util.fixNull(labels);
        this.description = description;
        this.mode = mode;
        this.initScript = initScript;
        this.remoteAdmin = remoteAdmin;
        this.remoteFS = remoteFS == null || remoteFS.isEmpty() ? "/tmp/hudson" : remoteFS;
        this.tmpDir = tmpDir == null || tmpDir.isEmpty() ? "/tmp" : tmpDir;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.stopOnTerminate = stopOnTerminate;
        this.tags = tags;
        this.numExecutors = numExecutors;
        readResolve();
    }

    @Override
    public Descriptor<YandexTemplate> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());
    }


    protected Object readResolve() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        labelSet = Label.parse(labels);

        if (nodeProperties == null) {
            nodeProperties = new DescribableList<>(Saveable.NOOP);
        }

        return this;
    }

    public Node.Mode getMode() {
        return mode;
    }

    public Set<LabelAtom> getLabelSet() {
        return labelSet;
    }

    public List<YCTag> getTags() {
        if (null == tags)
            return null;
        return Collections.unmodifiableList(tags);
    }

    public AbstractCloud getParent() {
        return parent;
    }

    public String getDescription() {
        return description;
    }

    public String getLabels() {
        return labels;
    }

    public String getInitScript() {
        return initScript;
    }

    public String getRemoteAdmin() {
        return remoteAdmin;
    }

    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }

    public int getNumExecutors() {
        return numExecutors;
    }

    public boolean isStopOnTerminate() {
        return stopOnTerminate;
    }

    public DescribableList<NodeProperty<?>, NodePropertyDescriptor> getNodeProperties() {
        return nodeProperties;
    }

    public CredentialProvider getProvider() {
        return provider;
    }

    public String getRemoteFS() {
        return remoteFS;
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public List<YCAbstractSlave> provision(int number, EnumSet<ProvisionOptions> provisionOptions) throws Exception {
        return provisionOnDemand(number, provisionOptions);
    }

    private List<YCAbstractSlave> provisionOnDemand(int number, EnumSet<ProvisionOptions> provisionOptions) throws Exception {
        YCPrivateKey ycPrivateKey = this.parent.resolvePrivateKey();
        if(ycPrivateKey == null){
            throw new Exception("Failed to get ssh");
        }
        List<InstanceOuterClass.Instance> orphans = findOrphansOrStopInstance(tplInstance(), number);
        if (orphans.isEmpty() && !provisionOptions.contains(ProvisionOptions.FORCE_CREATE) &&
                !provisionOptions.contains(ProvisionOptions.ALLOW_CREATE)) {
            logProvisionInfo("No existing instance found - but cannot create new instance");
            return null;
        }
        wakeUpInstance(orphans);
        if (orphans.size() == number) {
            return toSlaves(orphans.get(0));
        }
        int needCreateCount = number - orphans.size();
        InstanceServiceOuterClass.ListInstancesResponse listInstancesResponse = Api.getFilterInstanceResponse(this);
        if(needCreateCount > 0 && listInstancesResponse.getInstancesList().isEmpty()) {
            doCreateVM();
        }
        return toSlaves(tplInstance().get(0));
    }

    private void logProvisionInfo(String message) {
        LOGGER.log(Level.INFO, this + ". " + message);
    }

    private List<YCAbstractSlave> toSlaves(InstanceOuterClass.Instance instance) throws IOException {
        try {
            logProvisionInfo("Return instance: " + instance.toString());
            List<YCAbstractSlave> slaves = new ArrayList<>();
            slaves.add(newOnDemandSlave(instance));
            return slaves;
        } catch (Descriptor.FormException e) {
            throw new AssertionError(e); // we should have discovered all configuration issues upfront
        }
    }


    private YCAbstractSlave newOnDemandSlave(InstanceOuterClass.Instance instance) throws Descriptor.FormException, IOException {
        YCAgentConfig.OnDemand config = new YCAgentConfig.OnDemandBuilder()
                .withName(instance.getName())
                .withInstanceId(instance.getId())
                .withDescription(description)
                .withMode(mode)
                .withCloudName(parent.name)
                .withLabelString(labels)
                .withInitScript(initScript)
                .withRemoteAdmin(remoteAdmin)
                .withStopOnTerminate(stopOnTerminate)
                .withIdleTerminationMinutes(idleTerminationMinutes)
                .withLaunchTimeout(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .withNodeProperties(nodeProperties.toList())
                .withNumExecutors(numExecutors)
                .withRemoteFS(remoteFS)
                .withTmpDir(tmpDir)
                .build();
        return YCAgentFactory.getInstance().createOnDemandAgent(config);
    }

    private List<InstanceOuterClass.Instance> findOrphansOrStopInstance(List<InstanceOuterClass.Instance> tplAllInstance, int number) {
        List<InstanceOuterClass.Instance> orphans = new ArrayList<>();
        if (tplAllInstance == null) {
            return orphans;
        }
        int count = 0;
        for (InstanceOuterClass.Instance instance : tplAllInstance) {
            if (checkInstance(instance)) {
                // instance is not connected to jenkins
                orphans.add(instance);
                count++;
            }
            if (count == number) {
                return orphans;
            }
        }
        return orphans;
    }

    private boolean checkInstance(InstanceOuterClass.Instance instance) {
        for (YCAbstractSlave node : NodeIterator.nodes(YCAbstractSlave.class)) {
            if (node.getInstanceId().equals(instance.getId()) && !"STOPPED".equals(instance.getStatus().name())) {
                return false;
            }
        }
        return true;
    }

    private List<InstanceOuterClass.Instance> tplInstance() throws Exception {
        InstanceServiceOuterClass.ListInstancesResponse listInstancesResponse = Api.getFilterInstanceResponse(this);
        return listInstancesResponse.getInstancesList();
    }

    private void wakeUpInstance(List<InstanceOuterClass.Instance> orphans) {
        List<String> instances = new ArrayList<>();
        for (InstanceOuterClass.Instance sd : orphans) {
            if ("STOPPED".equals(sd.getStatus().name())) {
                instances.add(sd.getId());
            }
        }
        try {
            if (!instances.isEmpty()) {
                for(String instanceId : instances) {
                    Api.startInstance(this, instanceId);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage());
        }
    }

    public void doCreateVM() throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        String remote = remoteAdmin.isEmpty() ? "root" : remoteAdmin;
        YCPrivateKey privateKey =  this.parent.resolvePrivateKey();
        if(privateKey == null){
            throw new Exception("Failed get ssh key");
        }
        InstanceServiceOuterClass.CreateInstanceRequest createInstanceRequest;
        InstanceServiceOuterClass.CreateInstanceRequest.Builder builder = InstanceServiceOuterClass.CreateInstanceRequest.newBuilder();
        TextFormat.merge(this.parent.getInitVMTemplate(), builder);
        createInstanceRequest = builder
                .setName(parent.name)
                .setZoneId("ru-central1-b")
                .setFolderId(parent.getFolderId())
                .putMetadata("user-data", String.format(userData, remote, privateKey.getPublicFingerprint() + "= " + remote))
                .build();
        OperationOuterClass.Operation response = Api.createInstanceResponse(this, createInstanceRequest);
        if(!response.getError().getMessage().isEmpty()){
            throw new Exception("Error for create: " + response.getError().getMessage());
        }
    }

    public InstanceServiceGrpc.InstanceServiceBlockingStub getInstanceServiceBlockingStub() throws Exception {
        ServiceAccount serviceAccount = getCredentials(parent.getCredentialsId());
        if(serviceAccount == null){
            throw new LoginFailed("Failed find serviceAccount");
        }
        if(provider == null || provider.get().getExpiresAt().isBefore(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())) {
            LOGGER.log(Level.WARNING, "Token null or expired. Generate new");
            ManagedChannel channel = ManagedChannelBuilder.forTarget("iam".concat(ChannelFactory.DEFAULT_ENDPOINT)).usePlaintext().build();
            channel.shutdownNow();
            channel.awaitTermination(1, TimeUnit.MINUTES);
            provider = Auth.apiKeyBuilder()
                    .serviceAccountKey(new ServiceAccountKey(serviceAccount.getId(),
                            serviceAccount.getServiceAccountId(),
                            serviceAccount.getCreatedAt(),
                            serviceAccount.getKeyAlgorithm(),
                            serviceAccount.getPublicKey(),
                            serviceAccount.getPrivateKey()))
                    .build();
            if (provider.get().getToken() == null) {
                throw new LoginFailed("Failed to login!");
            }
            channel = ManagedChannelBuilder.forTarget("compute".concat(ChannelFactory.DEFAULT_ENDPOINT)).usePlaintext().build();
            channel.shutdownNow();
            channel.awaitTermination(1, TimeUnit.MINUTES);
            instanceServiceBlockingStub = ServiceFactory.builder()
                    .credentialProvider(provider)
                    .build().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub);
        }
        return instanceServiceBlockingStub;
    }


    @Extension
    public static final class DescriptorImpl extends Descriptor<YandexTemplate> {

        @Override
        public String getDisplayName() {
            return "";
        }
    }
}

package org.jenkins.plugins.yc;

import com.google.protobuf.TextFormat;
import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Saveable;
import hudson.model.labels.LabelAtom;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jenkins.model.Jenkins;
import jenkins.slaves.iterators.api.NodeIterator;
import org.jenkins.plugins.yc.exception.LoginFailed;
import org.jenkins.plugins.yc.exception.YandexClientException;
import org.jenkins.plugins.yc.util.YCAgentConfig;
import org.jenkins.plugins.yc.util.YCAgentFactory;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
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
import java.util.regex.Pattern;

import static org.jenkins.plugins.yc.AbstractCloud.DescriptorImpl.getCredentials;

public class YandexTemplate implements Describable<YandexTemplate> {

    private static final Logger LOGGER = Logger.getLogger(YandexTemplate.class.getName());

    protected transient AbstractCloud parent;

    private final String vmName;

    private final String initVMTemplate;

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
    public YandexTemplate(String vmName, String initVMTemplate, String description, Node.Mode mode,
                          String labels, String initScript, String remoteFS, String tmpDir,
                          String remoteAdmin, String idleTerminationMinutes,
                          boolean stopOnTerminate, List<YCTag> tags,
                          int numExecutors) {
        this.vmName = vmName;
        this.initVMTemplate = initVMTemplate;
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
        Jenkins j = Jenkins.getInstanceOrNull();
        if (j != null) {
            j.checkPermission(Jenkins.ADMINISTER);
        }

        if (nodeProperties == null) {
            nodeProperties = new DescribableList<>(Saveable.NOOP);
        }

        return this;
    }

    public String getVmName() {
        return vmName;
    }

    public String getInitVMTemplate() {
        return initVMTemplate;
    }

    public Node.Mode getMode() {
        return mode;
    }

    public Set<LabelAtom> getLabelSet() {
        if (labelSet == null) {
            labelSet = Label.parse(labels);
        }
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

    public YCAbstractSlave provision(int number, EnumSet<ProvisionOptions> provisionOptions) throws Exception {
        return provisionOnDemand(number, provisionOptions);
    }

    private YCAbstractSlave provisionOnDemand(int number, EnumSet<ProvisionOptions> provisionOptions) throws Exception {
        List<InstanceOuterClass.Instance> orphans = findOrphansOrStopInstance(tplInstance(), number);
        if (orphans.isEmpty() && !provisionOptions.contains(ProvisionOptions.FORCE_CREATE) &&
                !provisionOptions.contains(ProvisionOptions.ALLOW_CREATE)) {
            logProvisionInfo("No existing instance found - but cannot create new instance");
            return null;
        }
        wakeUpInstance(orphans);
        if (orphans.size() == number) {
            return toSlave(orphans.get(0));
        }
        int needCreateCount = number - orphans.size();
        InstanceServiceOuterClass.ListInstancesResponse listInstancesResponse = Api.getFilterInstanceResponse(this);
        if(needCreateCount > 0 && listInstancesResponse.getInstancesList().isEmpty()) {
            OperationOuterClass.Operation response = Api.createInstanceResponse(this, createVm());
            if(!response.getError().getMessage().isEmpty()){
                throw new YandexClientException("Error for create: " + response.getError().getMessage());
            }
        }
        return toSlave(tplInstance().get(0));
    }

    private void logProvisionInfo(String message) {
        LOGGER.log(Level.INFO, this + ". " + message);
    }

    private YCAbstractSlave toSlave(InstanceOuterClass.Instance instance) throws IOException {
        try {
            logProvisionInfo("Return instance: " + instance.toString());
            return newOnDemandSlave(instance);
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

    public InstanceServiceOuterClass.CreateInstanceRequest createVm() throws IOException {
        Jenkins j = Jenkins.getInstanceOrNull();
        if (j != null) {
            j.checkPermission(Jenkins.ADMINISTER);
        }
        String remote = remoteAdmin == null || remoteAdmin.isEmpty() ? "root" : remoteAdmin;
        YCPrivateKey privateKey =  this.getParent().resolvePrivateKey();
        if(privateKey == null){
            throw new YandexClientException("Failed get ssh key");
        }
        InstanceServiceOuterClass.CreateInstanceRequest.Builder builder = InstanceServiceOuterClass.CreateInstanceRequest.newBuilder();
        TextFormat.merge(this.getInitVMTemplate(), builder);
        return builder
                .setName(this.getVmName())
                .setZoneId("ru-central1-b")
                .setFolderId(parent.getFolderId())
                .putMetadata("user-data", String.format(userData, remote, privateKey.getPublicFingerprint() + "= " + remote))
                .build();
    }

    public InstanceServiceGrpc.InstanceServiceBlockingStub getInstanceServiceBlockingStub() throws Exception {
        ServiceAccount serviceAccount = getCredentials(parent.getCredentialsId());
        if(serviceAccount == null){
            throw new LoginFailed("Failed find serviceAccount");
        }
        if(provider == null || provider.get().getExpiresAt().isBefore(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())) {
            LOGGER.log(Level.WARNING, "Token null or expired. Generate new");
            closeChannel("iam".concat(ChannelFactory.DEFAULT_ENDPOINT));
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
            closeChannel("compute".concat(ChannelFactory.DEFAULT_ENDPOINT));
            instanceServiceBlockingStub = ServiceFactory.builder()
                    .credentialProvider(provider)
                    .build().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub);
        }
        return instanceServiceBlockingStub;
    }

    public void closeChannel(String target) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        channel.shutdownNow();
        channel.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<YandexTemplate> {

        @Override
        public String getDisplayName() {
            return "";
        }

        @RequirePOST
        public FormValidation doCheckVmName(@AncestorInPath ItemGroup context, @QueryParameter String value) throws IOException {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.isEmpty()){
                return FormValidation.error("Empty value");
            }
            if(Pattern.matches("|[a-z]([-a-z0-9]{0,61}[a-z0-9])?", value)){
                return FormValidation.ok();
            }
            return FormValidation.error("Not valid");
        }

        @RequirePOST
        public FormValidation doCheckInitVMTemplate(@AncestorInPath ItemGroup context, @QueryParameter String value) throws IOException {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.isEmpty()){
                return FormValidation.error("Init VM script empty");
            }
            return FormValidation.ok();
        }
    }
}

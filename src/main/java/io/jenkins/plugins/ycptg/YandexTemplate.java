package io.jenkins.plugins.ycptg;

import com.google.protobuf.TextFormat;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.jenkins.plugins.ycptg.exception.LoginFailed;
import io.jenkins.plugins.ycptg.exception.YandexClientException;
import io.jenkins.plugins.ycptg.util.YCAgentConfig;
import io.jenkins.plugins.ycptg.util.YCAgentFactory;
import jenkins.model.Jenkins;
import jenkins.slaves.iterators.api.NodeIterator;
import lombok.Getter;
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
import yandex.cloud.sdk.auth.provider.CredentialProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class YandexTemplate implements Describable<YandexTemplate> {

    private static final Logger LOGGER = Logger.getLogger(YandexTemplate.class.getName());

    @Getter
    protected transient AbstractCloud parent;

    @Getter
    private final String vmName;

    @Getter
    private final String initVMTemplate;

    @Getter
    private final String description;

    @Getter
    private final Node.Mode mode;
    @Getter
    private final String labels;
    @Getter
    private final String initScript;

    @Getter
    private final String idleTerminationMinutes;

    @Getter
    private final int numExecutors;

    @Getter
    private final boolean stopOnTerminate;

    @Getter
    private final String remoteFS;

    @Getter
    private final String tmpDir;

    private static final String userDataTemplate = 
        "#cloud-config\n" +
        "users:\n" +
        "  - name: %s\n" +
        "    sudo: ['ALL=(ALL) NOPASSWD:ALL']\n" +
        "    ssh-authorized-keys:\n" +
        "      - %s\n";

    private final List<YCTag> tags;

    @Getter
    private DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties;

    private transient Set<LabelAtom> labelSet;

    public enum ProvisionOptions {ALLOW_CREATE, FORCE_CREATE}

    @Getter
    private transient CredentialProvider provider;
    private transient InstanceServiceGrpc.InstanceServiceBlockingStub instanceServiceBlockingStub;

    @DataBoundConstructor
    public YandexTemplate(String vmName, String initVMTemplate, String description, Node.Mode mode,
                          String labels, String initScript, String remoteFS, String tmpDir,
                          String idleTerminationMinutes, boolean stopOnTerminate,
                          List<YCTag> tags, int numExecutors) {
        this.vmName = vmName;
        this.initVMTemplate = initVMTemplate;
        this.labels = Util.fixNull(labels);
        this.description = description;
        this.mode = mode;
        this.initScript = initScript;
        this.remoteFS = remoteFS == null || remoteFS.isEmpty() ? "/tmp/hudson" : remoteFS;
        this.tmpDir = tmpDir == null || tmpDir.isEmpty() ? "/tmp" : tmpDir;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.stopOnTerminate = stopOnTerminate;
        this.tags = tags;
        this.numExecutors = numExecutors;
        readResolve();
    }

    @Override
    @SuppressWarnings({"unchecked"})
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

    public Set<LabelAtom> getLabelSet() {
        if (labelSet == null) {
            labelSet = Label.parse(labels);
        }
        return labelSet;
    }

    @SuppressWarnings({"unused"})
    public List<YCTag> getTags() {
        if (null == tags) {
            return null;
        }
        return Collections.unmodifiableList(tags);
    }

    public YCAbstractSlave provision(int number, EnumSet<ProvisionOptions> provisionOptions) throws Exception {
        return provisionOnDemand(number, provisionOptions);
    }

    private YCAbstractSlave provisionOnDemand(int number, EnumSet<ProvisionOptions> provisionOptions) throws Exception {
        InstanceServiceOuterClass.CreateInstanceRequest createInstanceRequest = createVm();
        List<InstanceOuterClass.Instance> orphans = findOrphansOrStopInstance(tplInstance(createInstanceRequest), number);
        if (orphans.isEmpty() && !provisionOptions.contains(ProvisionOptions.FORCE_CREATE)
                && !provisionOptions.contains(ProvisionOptions.ALLOW_CREATE)) {
            logProvisionInfo("No existing instance found - but cannot create new instance");
            return null;
        }
        wakeUpInstance(orphans);
        if (orphans.size() == number) {
            return toSlave(orphans.get(0));
        }
        int needCreateCount = number - orphans.size();
        InstanceServiceOuterClass.ListInstancesResponse listInstancesResponse = getFilterInstanceResponse(createInstanceRequest.getFolderId());
        if (needCreateCount > 0 && listInstancesResponse.getInstancesList().isEmpty()) {
            OperationOuterClass.Operation response = createInstanceResponse(createInstanceRequest);
            if (!response.getError().getMessage().isEmpty()) {
                throw new YandexClientException("Error for create: " + response.getError().getMessage());
            }
        }
        return toSlave(tplInstance(createInstanceRequest).get(0));
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

    private List<InstanceOuterClass.Instance> tplInstance(InstanceServiceOuterClass.CreateInstanceRequest createInstanceRequest) throws Exception {
        InstanceServiceOuterClass.ListInstancesResponse listInstancesResponse = getFilterInstanceResponse(createInstanceRequest.getFolderId());
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
                for (String instanceId : instances) {
                    startInstance(instanceId);
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
        YCPrivateKey privateKey = this.getParent().resolvePrivateKey();
        if (privateKey == null) {
            throw new YandexClientException("Failed to get SSH key");
        }

        String userName = privateKey.getUserName();
        String publicKey = privateKey.getPublic();

        // Логирование имени пользователя и SSH ключа
        LOGGER.info("Creating VM with user: " + userName);
        LOGGER.info("Using SSH public key: " + publicKey);

        InstanceServiceOuterClass.CreateInstanceRequest.Builder builder = InstanceServiceOuterClass.CreateInstanceRequest.newBuilder();
        TextFormat.merge(this.getInitVMTemplate(), builder);
        String userData = String.format(userDataTemplate, userName, publicKey);
        LOGGER.info("userData : " + userData);
        return builder
                .setName(this.getVmName())
                .putMetadata("user-data", userData)
                .build();
    }

    public InstanceServiceGrpc.InstanceServiceBlockingStub getInstanceServiceBlockingStub() throws Exception {
        ServiceAccount serviceAccount = AbstractCloud.DescriptorImpl.getCredentials(parent.getCredentialsId());
        if (serviceAccount == null) {
            throw new LoginFailed("Failed find serviceAccount");
        }
        if (provider == null || provider.get().getExpiresAt().isBefore(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())) {
            LOGGER.log(Level.WARNING, "Token null or expired. Generate new");
            closeChannel("iam".concat(ChannelFactory.DEFAULT_ENDPOINT));
            provider = serviceAccount.buildCredentialProvider();
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

    public OperationOuterClass.Operation createInstanceResponse(InstanceServiceOuterClass.CreateInstanceRequest instanceRequest) throws Exception {
        return this.getInstanceServiceBlockingStub().create(instanceRequest);
    }

    public void startInstance(String instanceId) throws Exception {
        //noinspection ResultOfMethodCallIgnored
        this.getInstanceServiceBlockingStub().start(InstanceServiceOuterClass.StartInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }


    public void stopInstance(String instanceId) throws Exception {
        //noinspection ResultOfMethodCallIgnored
        this.getInstanceServiceBlockingStub().stop(InstanceServiceOuterClass.StopInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

    public InstanceOuterClass.Instance getInstanceResponse(String instanceId) throws Exception {
        return this.getInstanceServiceBlockingStub().get(InstanceServiceOuterClass.GetInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

    public InstanceServiceOuterClass.ListInstancesResponse getFilterInstanceResponse(String folderId) throws Exception {
        return this.getInstanceServiceBlockingStub().list(InstanceServiceOuterClass.ListInstancesRequest.newBuilder()
                .setFolderId(folderId)
                .setFilter("name=\"".concat(this.getVmName()).concat("\""))
                .build());
    }


    public void deleteInstanceResponse(String instanceId) throws Exception {
        //noinspection ResultOfMethodCallIgnored
        this.getInstanceServiceBlockingStub().delete(InstanceServiceOuterClass.DeleteInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

    @SuppressWarnings("unused")
    @Extension
    public static final class DescriptorImpl extends Descriptor<YandexTemplate> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "";
        }

        @RequirePOST
        public FormValidation doCheckVmName(@AncestorInPath ItemGroup context, @QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Empty value");
            }
            if (Pattern.matches("|[a-z]([-a-z0-9]{0,61}[a-z0-9])?", value)) {
                return FormValidation.ok();
            }
            return FormValidation.error("Not valid");
        }

        @RequirePOST
        public FormValidation doCheckInitVMTemplate(@AncestorInPath ItemGroup context, @QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Init VM script empty");
            }
            return FormValidation.ok();
        }
    }
}

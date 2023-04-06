package org.jenkins.plugins.yc;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.json.JSONObject;
import yandex.cloud.api.compute.v1.InstanceOuterClass;
import yandex.cloud.sdk.ServiceFactory;
import yandex.cloud.sdk.auth.Auth;
import yandex.cloud.sdk.auth.jwt.ServiceAccountKey;
import yandex.cloud.sdk.auth.provider.CredentialProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jenkins.plugins.yc.AbstractCloud.DescriptorImpl.getCredentials;

public abstract class AbstractCloud extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(AbstractCloud.class.getName());
    private static final long VM_WAITER = 100000;

    private final List<? extends YandexTemplate> templates;

    public final String initVMTemplate;

    private transient ReentrantLock slaveCountingLock = new ReentrantLock();

    public final String credentialsId;
    public final String folderId;
    @CheckForNull
    public final String sshKeysCredentialsId;


    protected AbstractCloud(String name,
                            List<? extends YandexTemplate> templates, String credentialsId,
                            String folderId, String sshKeysCredentialsId,
                            String initVMTemplate) {
        super(name);
        this.templates = Objects.requireNonNullElse(templates, Collections.emptyList());
        this.credentialsId = credentialsId;
        this.folderId = folderId;
        this.sshKeysCredentialsId = sshKeysCredentialsId;
        this.initVMTemplate = initVMTemplate;
        readResolve();
    }

    @Override
    public boolean canProvision(Cloud.CloudState state) {
        return !getTemplates(state.getLabel()).isEmpty();
    }

    /**
     * Obtains a agent whose AMI matches the AMI of the given template, and that also has requiredLabel (if requiredLabel is non-null)
     * forceCreateNew specifies that the creation of a new agent is required. Otherwise, an existing matching agent may be re-used
     */
    public List<YCAbstractSlave> getNewOrExistingAvailableSlave(YandexTemplate t, int number, boolean forceCreateNew) {
        try {
            slaveCountingLock.lock();
            int possibleSlavesCount = 1;
            try {
                EnumSet<YandexTemplate.ProvisionOptions> provisionOptions;
                if (forceCreateNew)
                    provisionOptions = EnumSet.of(YandexTemplate.ProvisionOptions.FORCE_CREATE);
                else
                    provisionOptions = EnumSet.of(YandexTemplate.ProvisionOptions.ALLOW_CREATE);

                if (number > possibleSlavesCount) {
                    LOGGER.log(Level.INFO, String.format("%d nodes were requested for the template %s, " +
                            "but because of instance cap only %d can be provisioned", number, t, possibleSlavesCount));
                    number = possibleSlavesCount;
                }

                return t.provision(number, provisionOptions);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, t + ". Exception during provisioning", e);
                return null;
            }
        } finally { slaveCountingLock.unlock(); }
    }

    public static abstract class DescriptorImpl extends Descriptor<Cloud> {

        @CheckForNull
        public static ServiceAccount getCredentials(@CheckForNull String credentialsId) throws Exception {
            if (StringUtils.isBlank(credentialsId)) {
                return null;
            }
            FileCredentialsImpl fileCredentials = (FileCredentialsImpl)CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(FileCredentialsImpl.class, Jenkins.get(), ACL.SYSTEM, Collections.EMPTY_LIST),
                    CredentialsMatchers.withId(credentialsId));
            if(fileCredentials != null){
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(fileCredentials.getContent(), "UTF-8"))) {
                    StringBuilder responseStrBuilder = new StringBuilder();
                    String inputStr;
                    while ((inputStr = reader.readLine()) != null)
                        responseStrBuilder.append(inputStr);
                    JSONObject result = new JSONObject(responseStrBuilder.toString());
                    return new ServiceAccount(fileCredentials.getScope(),
                            result.getString("id"),
                            fileCredentials.getDescription(),
                            result.getString("created_at"),
                            result.getString("key_algorithm"),
                            result.getString("service_account_id"),
                            result.getString("private_key"),
                            result.getString("public_key"));
                }
            }
            throw new Exception("File not found");
        }
    }

    protected Object readResolve() {
        this.slaveCountingLock = new ReentrantLock();
        for (YandexTemplate t : templates)
            t.parent = this;
        return this;
    }

    @CheckForNull
    public YCPrivateKey resolvePrivateKey(){
        if (sshKeysCredentialsId != null) {
            SSHUserPrivateKey privateKeyCredential = getSshCredential(sshKeysCredentialsId);
            if (privateKeyCredential != null) {
                return new YCPrivateKey(privateKeyCredential.getPrivateKey());
            }
        }
        return null;
    }

    @CheckForNull
    private static SSHUserPrivateKey getSshCredential(String id){
        SSHUserPrivateKey credential = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        SSHUserPrivateKey.class,
                        Jenkins.get(),
                        null,
                        Collections.emptyList()),
                CredentialsMatchers.withId(id));

        if (credential == null){
            LOGGER.log(Level.WARNING, "YC Plugin could not find the specified credentials ({0}) in the Jenkins Global Credentials Store, YC Plugin for cloud must be manually reconfigured", new String[]{id});
        }

        return credential;
    }

    public List<YandexTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    @javax.annotation.CheckForNull
    public YandexTemplate getTemplate(String template) {
        for (YandexTemplate t : templates) {
            if (t.description.equals(template)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Gets list of {@link YandexTemplate} that matches {@link Label}.
     */
    public Collection<YandexTemplate> getTemplates(Label label) {
        List<YandexTemplate> matchingTemplates = new ArrayList<>();
        for (YandexTemplate t : templates) {
            if (t.getMode() == Node.Mode.NORMAL) {
                if (label == null || label.matches(t.getLabelSet())) {
                    matchingTemplates.add(t);
                }
            } else if (t.getMode() == Node.Mode.EXCLUSIVE) {
                if (label != null && label.matches(t.getLabelSet())) {
                    matchingTemplates.add(t);
                }
            }
        }
        return matchingTemplates;
    }

    public ServiceFactory getServiceFactory() throws Exception {
        ServiceAccount serviceAccount = getCredentials(this.credentialsId);
        if(serviceAccount == null){
            throw new Exception("Failed find serviceAccount");
        }
        CredentialProvider provider = Auth.apiKeyBuilder()
                .serviceAccountKey(new ServiceAccountKey(serviceAccount.getId(),
                        serviceAccount.getServiceAccountId(),
                        serviceAccount.getCreatedAt(),
                        serviceAccount.getKeyAlgorithm(),
                        serviceAccount.getPublicKey(),
                        serviceAccount.getPrivateKey()))
                .build();
        if (provider.get().getToken() == null) {
            throw new Exception("Failed to login!");
        }
        return ServiceFactory.builder()
                .credentialProvider(provider)
                .build();
        }

    public NodeProvisioner.PlannedNode createPlannedNode(YandexTemplate t, YCAbstractSlave slave) {
        return new NodeProvisioner.PlannedNode(t.getParent().getDisplayName(),
                Computer.threadPoolForRemoting.submit(new Callable<Node>() {
                    private static final int DESCRIBE_LIMIT = 1;
                    int retryCount = 0;

                    public Node call() throws Exception {
                        while (true) {
                            Thread.sleep(VM_WAITER);
                            String instanceId = slave.getInstanceId();
                            InstanceOuterClass.Instance instance = Api.getInstanceResponse(instanceId, slave.getCloud());
                            if (instance == null) {
                                LOGGER.log(Level.WARNING, "{0} Can't find instance with instance id `{1}` in cloud {2}. Terminate provisioning ",
                                        new Object[]{t, instanceId, slave.cloudName});
                                return null;
                            }
                            String state = instance.getStatus().name();
                            if (state.equals("RUNNING")) {
                                Computer c = slave.toComputer();
                                if (slave.getStopOnTerminate() && (c != null)) {
                                    c.connect(false);
                                }

                                long startTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                                LOGGER.log(Level.INFO, "{0} Node {1} moved to RUNNING state in {2} seconds and is ready to be connected by Jenkins",
                                        new Object[]{t, slave.getNodeName(), startTime});
                                return slave;
                            }

                            if (!state.equals("PROVISIONING")) {
                                if (retryCount >= DESCRIBE_LIMIT) {
                                    LOGGER.log(Level.WARNING, "Instance {0} did not move to running after 1 attempts, terminating provisioning",
                                            new Object[]{instanceId/*, retryCount*/});
                                    return null;
                                }

                                LOGGER.log(Level.INFO, "Attempt {0}: {1}. Node {2} is neither pending, neither running, it''s {3}. Will try again after 5s",
                                        new Object[]{retryCount, t, slave.getNodeName(), state});
                                retryCount++;
                            }
                            Thread.sleep(5000);
                        }
                    }
                })
                , 1);
    }
}

package io.jenkins.plugins.yc;


import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Computer;
import hudson.model.Descriptor.FormException;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import io.jenkins.plugins.yc.util.ResettableCountDownLatch;
import org.kohsuke.stapler.StaplerRequest;
import yandex.cloud.api.compute.v1.InstanceOuterClass;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


@SuppressWarnings("serial")
public abstract class YCAbstractSlave extends Slave {

    private static final Logger LOGGER = Logger.getLogger(YCAbstractSlave.class.getName());

    private String instanceId;

    private final String initScript;
    private final String tmpDir;
    private final String templateDescription;

    private final boolean stopOnTerminate;
    private final String idleTerminationMinutes;

    private boolean isConnected = false;
    private final List<YCTag> tags;
    private final String cloudName;

    /* The last instance data to be fetched for the agent. */
    protected transient InstanceOuterClass.Instance lastFetchInstance = null;

    /* The time at which we fetched the last instance data. */
    protected transient long lastFetchTime;

    /** Terminate was scheduled. */
    protected transient ResettableCountDownLatch terminateScheduled = new ResettableCountDownLatch(1, false);

    protected static final long MIN_FETCH_TIME = Long.getLong("hudson.plugins.ec2.EC2AbstractSlave.MIN_FETCH_TIME",
            TimeUnit.SECONDS.toMillis(20));

    protected final long launchTimeout;

    public YCAbstractSlave(String name, String instanceId, String templateDescription, String remoteFS, int numExecutors,
                           Mode mode, String labelString, ComputerLauncher launcher, RetentionStrategy<YCComputer> retentionStrategy,
                           String initScript, String tmpDir, List<? extends NodeProperty<?>> nodeProperties,
                           boolean stopOnTerminate, String idleTerminationMinutes, List<YCTag> tags, String cloudName,
                           long launchTimeout)
            throws FormException, IOException {
        super(name, remoteFS, launcher);
        setNumExecutors(numExecutors);
        setMode(mode);
        setLabelString(labelString);
        setRetentionStrategy(retentionStrategy);
        setNodeProperties(nodeProperties);

        this.instanceId = instanceId;
        this.templateDescription = templateDescription;
        this.initScript = initScript;
        this.tmpDir = tmpDir;
        this.stopOnTerminate = stopOnTerminate;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.tags = tags;
        this.cloudName = cloudName;
        this.launchTimeout = launchTimeout;
        readResolve();
    }


    @Override
    protected Object readResolve() {
        /*
         * If instanceId is null, this object was deserialized from an old version of the plugin, where this field did
         * not exist (prior to version 1.18). In those versions, the node name *was* the instance ID, so we can get it
         * from there.
         */
        if (instanceId == null) {
            instanceId = getNodeName();
        }

        /*
         * If this field is null (as it would be if this object is deserialized and not constructed normally) then
         * we need to explicitly initialize it, otherwise we will cause major blocker issues such as this one which
         * made Jenkins entirely unusable for some in the 1.50 release:
         * https://issues.jenkins-ci.org/browse/JENKINS-62043
         */
        if (terminateScheduled == null) {
            terminateScheduled = new ResettableCountDownLatch(1, false);
        }

        return this;
    }

    public YandexCloud getCloud() {
        return (YandexCloud) Jenkins.get().getCloud(cloudName);
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public Computer createComputer() {
        return new YCComputer(this);
    }

    public void terminate() {
        terminateScheduled = new ResettableCountDownLatch(1, false);
    }

    void stop() {
        try {
            YandexTemplate template = getCloud().getTemplate(templateDescription);
            if (template != null) {
                template.stopInstance(instanceId);
            }
            Computer computer = toComputer();
            if (computer != null) {
                computer.disconnect(null);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to stop YC instance: " + getInstanceId(), e);
        }

    }

    @Override
    public Node reconfigure(@NonNull final StaplerRequest req, JSONObject form) throws FormException {
        if (form == null) {
            return null;
        }

        return super.reconfigure(req, form);
    }

    @Override
    public boolean isAcceptingTasks() {
        return terminateScheduled.getCount() == 0;
    }

    void idleTimeout() {
        LOGGER.log(Level.INFO,"YC instance idle time expired: " + getInstanceId());
        if (!stopOnTerminate) {
            terminate();
        } else {
            stop();
        }
    }

    void launchTimeout(){
        LOGGER.log(Level.INFO,"YC instance failed to launch: " + getInstanceId());
        terminate();
    }

    public long getLaunchTimeoutInMillis() {
        return launchTimeout * 1000L;
    }

    public int getSshPort() {
        return 22;
    }

    public boolean getStopOnTerminate() {
        return stopOnTerminate;
    }

    public Secret getAdminPassword() {
        return Secret.fromString("");
    }

    public boolean isSpecifyPassword() {
        return false;
    }

    public String getInitScript() {
        return initScript;
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public boolean isStopOnTerminate() {
        return stopOnTerminate;
    }

    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public List<YCTag> getTags() {
        return tags;
    }

    public String getCloudName() {
        return cloudName;
    }

    /**
     * Called when the agent is connected to Jenkins.
     */
    public void onConnected() {
        isConnected = true;
    }

    protected boolean isAlive(boolean force) throws Exception {
        fetchLiveInstanceData(force);
        if (lastFetchInstance == null) {
            return false;
        }
        if (lastFetchInstance.getStatus().name().equals("STOPPED")) {
            return false;
        }
        return true;
    }

    /*
     * Much of the YC data is beyond our direct control, therefore we need to refresh it from time to time to ensure we
     * reflect the reality of the instances.
     */
    private void fetchLiveInstanceData(final boolean force) throws Exception {
        /*
         * If we've grabbed the data recently, don't bother getting it again unless we are forced.
         */
        long now = System.currentTimeMillis();
        if ((lastFetchTime > 0) && (now - lastFetchTime < MIN_FETCH_TIME) && !force) {
            return;
        }

        if (StringUtils.isEmpty(getInstanceId())) {
            return;
        }

        InstanceOuterClass.Instance i;
        i = null;
        try {
            YandexTemplate template = getCloud().getTemplate(templateDescription);

            if (template != null) {
                i = template.getInstanceResponse(getInstanceId());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "YandexTemplate problem catched!", e);
        }
        lastFetchTime = now;
        lastFetchInstance = i;
        if (i == null) {
            return;
        }
        /*
         * Only fetch tags from live instance if tags are set. This check is required to mitigate a race condition
         * when fetchLiveInstanceData() is called before pushLiveInstancedata().
         */
        /*if(!i.getTags().isEmpty()) {
            tags = new LinkedList<YCTag>();
            for (YCTag t : i.getTags()) {
                tags.add(new YCTag(t.getKey(), t.getValue()));
            }
        }*/
    }

    public static abstract class DescriptorImpl extends SlaveDescriptor {

        @NonNull
        @Override
        public abstract String getDisplayName();

        @Override
        public boolean isInstantiable() {
            return false;
        }
    }

}

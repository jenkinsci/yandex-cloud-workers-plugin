package io.jenkins.plugins.ycptg;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor.FormException;
import hudson.model.Node;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YCOndemandSlave extends YCAbstractSlave {
    private static final Logger LOGGER = Logger.getLogger(YCOndemandSlave.class.getName());

    @DataBoundConstructor
    public YCOndemandSlave(String name, String instanceId, String templateDescription,
                           String remoteFS, int numExecutors, String labelString, ComputerLauncher launcher,
                           Node.Mode mode, String initScript,
                           String tmpDir, List<? extends NodeProperty<?>> nodeProperties,
                           boolean stopOnTerminate, String idleTerminationMinutes,
                           List<YCTag> tags, String cloudName, long launchTimeout)
            throws FormException, IOException {

        super(name, instanceId, templateDescription, remoteFS, numExecutors, mode, labelString, launcher, new YandexRetentionStrategy(idleTerminationMinutes), initScript, tmpDir, nodeProperties, stopOnTerminate, idleTerminationMinutes, tags, cloudName, launchTimeout);
    }

    /**
     * Constructor for debugging
     * @param instanceId - vm instance id
     */
    public YCOndemandSlave(String instanceId) throws FormException, IOException {
        this(instanceId, instanceId, "debug", "/tmp/hudson", 1, "debug", new YCUnixComputerLauncher(), Mode.NORMAL, "", "/tmp", Collections.emptyList(), false, null, null, "debug", 0);
    }

    public YCOndemandSlave(String name, String instanceId,
                           String description, String remoteFS,
                           String labelString, String cloudName,
                           String idleTerminationMinutes, String initScript,
                           String tmpDir, List<? extends NodeProperty<?>> nodeProperties,
                           long launchTimeOut, boolean stopOnTerminate,
                           int numExecutors) throws FormException, IOException {
        this(name, instanceId, description, remoteFS, numExecutors, labelString, new YCUnixComputerLauncher(), Mode.NORMAL, initScript, tmpDir, nodeProperties, stopOnTerminate, idleTerminationMinutes, null, cloudName,  launchTimeOut);
    }

    @Override
    public void terminate() {
        if (terminateScheduled.getCount() == 0) {
            synchronized (terminateScheduled) {
                if (terminateScheduled.getCount() == 0) {
                    Computer.threadPoolForRemoting.submit(() -> {
                        try {
                            if (!isAlive(true)) {
                                LOGGER.log(Level.INFO, "YC instance already terminated: " + getInstanceId());
                            } else {
                                YandexTemplate template = getCloud().getTemplate(getTemplateDescription());
                                if (template != null) {
                                    template.deleteInstanceResponse(getInstanceId());
                                }
                            }
                            Jenkins.get().removeNode(this);
                            LOGGER.log(Level.INFO, "Removed YC instance from jenkins master: " + getInstanceId());
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to terminate YC instance: " + getInstanceId(), e);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            synchronized (terminateScheduled) {
                                terminateScheduled.countDown();
                            }
                        }
                    });
                    terminateScheduled.reset();
                }
            }
        }
    }

    @Override
    public Node reconfigure(@NonNull final StaplerRequest req, JSONObject form) throws FormException {
        if (form == null) {
            return null;
        }

        try {
            if (!isAlive(true)) {
                LOGGER.log(Level.INFO, "YC instance terminated externally: " + getInstanceId());
                try {
                    Jenkins.get().removeNode(this);
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, "Attempt to reconfigure YC instance which has been externally terminated: "
                            + getInstanceId(), ioe);
                }

                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return super.reconfigure(req, form);
    }

    @Extension
    public static final class DescriptorImpl extends YCAbstractSlave.DescriptorImpl {
        @NonNull
        @Override
        public String getDisplayName() {
            return "OnDemandSlaveYC";
        }
    }
}

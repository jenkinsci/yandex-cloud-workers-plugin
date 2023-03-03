package org.jenkins.plugins.yc;

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

/**
 * Agent running on YC.
 *
 * @author Kohsuke Kawaguchi
 */
public class YCOndemandSlave extends YCAbstractSlave {
    private static final Logger LOGGER = Logger.getLogger(YCOndemandSlave.class.getName());

    @DataBoundConstructor
    public YCOndemandSlave(String name, String instanceId, String templateDescription,
                           String remoteFS, int numExecutors, String labelString, ComputerLauncher launcher,
                           Node.Mode mode, String initScript,
                           String tmpDir, List<? extends NodeProperty<?>> nodeProperties, String remoteAdmin,
                           boolean stopOnTerminate, String idleTerminationMinutes,
                           List<YCTag> tags, String cloudName, int launchTimeout,
                           int maxTotalUses, Tenancy tenancy)
            throws FormException, IOException {

        super(name, instanceId, templateDescription, remoteFS, numExecutors, mode, labelString, launcher, new YandexRetentionStrategy(idleTerminationMinutes), initScript, tmpDir, nodeProperties, remoteAdmin, stopOnTerminate, idleTerminationMinutes, tags, cloudName, launchTimeout, maxTotalUses, tenancy);
    }

    /**
     * Constructor for debugging.
     */

    @Deprecated
    public YCOndemandSlave(String name, String instanceId, String templateDescription,
                           String remoteFS, int numExecutors, String labelString, ComputerLauncher launcher,
                           Mode mode, String initScript, String tmpDir,
                           List<? extends NodeProperty<?>> nodeProperties,
                           String remoteAdmin, String jvmopts, boolean stopOnTerminate,
                           String idleTerminationMinutes, String publicDNS, String privateDNS,
                           List<YCTag> tags, String cloudName,
                           boolean useDedicatedTenancy, int launchTimeout,
                           String connectionStrategy,
                           int maxTotalUses)
            throws FormException, IOException {

        this(name, instanceId, templateDescription, remoteFS, numExecutors, labelString, launcher, mode, initScript, tmpDir, nodeProperties, remoteAdmin, stopOnTerminate, idleTerminationMinutes, tags, cloudName, launchTimeout, maxTotalUses, Tenancy.backwardsCompatible(useDedicatedTenancy));
    }

    public YCOndemandSlave(String name, String instanceId, String description, String labelString, String cloudName, String idleTerminationMinutes, String initScript, String remoteAdmin) throws FormException, IOException {
        this(name, instanceId, description, "/tmp/hudson", 1, labelString, new YCUnixComputerLauncher(), Mode.NORMAL, initScript, "/tmp", Collections.emptyList(), remoteAdmin, null, false, idleTerminationMinutes, "Fake public", "Fake private", null, cloudName, false, 0,  "PRIVATE_IP", -1);
    }

    @Override
    public void terminate() {
        if (terminateScheduled.getCount() == 0) {
            synchronized (terminateScheduled) {
                if (terminateScheduled.getCount() == 0) {
                    Computer.threadPoolForRemoting.submit(() -> {
                        try {
                            if (!isAlive(true)) {
                                LOGGER.log(Level.INFO,"YC instance already terminated: " + getInstanceId());
                            } else {
                                Api.deleteInstanceResponse(getInstanceId(), getCloud());
                            }
                            Jenkins.get().removeNode(this);
                            LOGGER.log(Level.INFO,"Removed YC instance from jenkins master: " + getInstanceId());
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
    public Node reconfigure(final StaplerRequest req, JSONObject form) throws FormException {
        if (form == null) {
            return null;
        }

        try {
            if (!isAlive(true)) {
                LOGGER.log(Level.INFO,"YC instance terminated externally: " + getInstanceId());
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
        @Override
        public String getDisplayName() {
            return "OnDemandSlaveYC";
        }
    }

    @Override
    public String getYCType() {
        return "OnDemandSlaveOnDemand";
    }
}

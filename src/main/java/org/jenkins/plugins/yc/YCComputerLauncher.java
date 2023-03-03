package org.jenkins.plugins.yc;

import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ComputerLauncher} for YC that wraps the real user-specified {@link ComputerLauncher}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class YCComputerLauncher extends ComputerLauncher {
    private static final Logger LOGGER = Logger.getLogger(YCComputerLauncher.class.getName());

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener listener) {
        try {
            YCComputer computer = (YCComputer) slaveComputer;
            launchScript(computer, listener);
        } catch (IOException e) {
            e.printStackTrace(listener.error(e.getMessage()));
            if (slaveComputer.getNode() != null) {
                LOGGER.log(Level.FINE, String.format("Terminating the yc agent %s due a problem launching or connecting to it", slaveComputer.getName()), e);
                YCAbstractSlave ycAbstractSlave = (YCAbstractSlave) slaveComputer.getNode();
                if (ycAbstractSlave != null) {
                    ycAbstractSlave.terminate();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace(listener.error(e.getMessage()));
            if (slaveComputer.getNode() != null) {
                LOGGER.log(Level.FINE, String.format("Terminating the yc agent %s due a problem launching or connecting to it", slaveComputer.getName()), e);
                YCAbstractSlave ycAbstractSlave = (YCAbstractSlave) slaveComputer.getNode();
                if (ycAbstractSlave != null) {
                    ycAbstractSlave.terminate();
                }
            }
        }

    }

    /**
     * Stage 2 of the launch. Called after the YC instance comes up.
     */
    protected abstract boolean launchScript(YCComputer computer, TaskListener listener)
            throws IOException, InterruptedException;

}

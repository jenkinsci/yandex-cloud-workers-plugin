package io.jenkins.plugins.ycptg;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.init.InitMilestone;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YandexRetentionStrategy extends RetentionStrategy<YCComputer> implements ExecutorListener {

    private static final Logger LOGGER = Logger.getLogger(YandexRetentionStrategy.class.getName());
    private transient ReentrantLock checkLock;
    private final int idleTerminationMinutes;

    private static final int STARTUP_TIME_DEFAULT_VALUE = 30;

    private transient Clock clock;
    private long nextCheckAfter = -1;
    public static final boolean DISABLED = Boolean.getBoolean(YandexRetentionStrategy.class.getName() + ".disabled");

    @DataBoundConstructor
    public YandexRetentionStrategy(String idleTerminationMinutes){
        readResolve();
        if (idleTerminationMinutes == null || idleTerminationMinutes.trim().isEmpty()) {
            this.idleTerminationMinutes = 0;
        } else {
            int value = STARTUP_TIME_DEFAULT_VALUE;
            try {
                value = Integer.parseInt(idleTerminationMinutes);
            } catch (NumberFormatException nfe) {
                LOGGER.log(Level.INFO, "Malformed default idleTermination value: " + idleTerminationMinutes);
            }

            this.idleTerminationMinutes = value;
        }
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long l) {
        postJobAction(executor);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long l, Throwable throwable) {
        postJobAction(executor);
    }

    private void postJobAction(Executor executor) {
        YCComputer computer = (YCComputer) executor.getOwner();
        YCAbstractSlave slaveNode = computer.getNode();
        if (slaveNode != null) {
            // At this point, if agent is in suspended state and has 1 last executer running, it is safe to terminate.
            if (computer.countBusy() <= 1 && !computer.isAcceptingTasks()) {
                LOGGER.log(Level.INFO, "Agent " + slaveNode.getInstanceId() + " is terminated due to maxTotalUses ");
                slaveNode.terminate();
            }
        }
    }

    /**
     * Called when a new {@link YCComputer} object is introduced (such as when Hudson started, or when
     * a new agent is added.)
     * <p>
     * When Jenkins has just started, we don't want to spin up all the instances, so we only start if
     * the YC instance is already running
     */
    @Override
    public void start(@NonNull YCComputer c) {
        //Jenkins is in the process of starting up
        if (Jenkins.get().getInitLevel() != InitMilestone.COMPLETED) {
            String state = null;
            try {
                state = c.getStatus();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error getting YC instance state for " + c.getName(), e);
            }
            if (!"RUNNING".equals(state)) {
                LOGGER.info("Ignoring start request for " + c.getName()
                        + " during Jenkins startup due to YC instance state of " + state);
                return;
            }
        }
        LOGGER.info("Start requested for " + c.getName());
        c.connect(false);
    }

    @Override
    public long check(@NonNull YCComputer c) {
        if (!checkLock.tryLock()) {
            return 1;
        } else {
            try {
                long currentTime = this.clock.millis();
                if (currentTime > nextCheckAfter) {
                    long intervalMinutes = internalCheck(c);
                    nextCheckAfter = currentTime + TimeUnit.MINUTES.toMillis(intervalMinutes);
                    return intervalMinutes;
                } else {
                    return 1;
                }
            } finally {
                checkLock.unlock();
            }
        }
    }


    private long internalCheck(YCComputer computer) {
        // If we've been told never to terminate, or node is null(deleted), no checks to perform
        if (idleTerminationMinutes == 0 || computer.getNode() == null) {
            return 1;
        }

        if (computer.isIdle() && !DISABLED) {
            final long uptime;
            String state;
            try {
                state = computer.getStatus();
                uptime = computer.getUptime();
            } catch (Exception e) {
                LOGGER.fine("Exception while checking host uptime for " + computer.getName()
                        + ", will retry next check. Exception: " + e);
                return 1;
            }

            //on rare occasions, YC may return fault instance which shows running in Yandex cloud console but can not be connected.
            //need terminate such fault instance.
            //an instance may also fail running user data scripts and need to be cleaned up.
            if (computer.isOffline()) {
                if (computer.isConnecting()) {
                    LOGGER.log(Level.INFO, "Computer {0} connecting and still offline, will check if the launch timeout has expired", computer.getInstanceId());

                    YCAbstractSlave node = computer.getNode();
                    if (Objects.isNull(node)) {
                        return 1;
                    }
                    long launchTimeout = node.getLaunchTimeoutInMillis();
                    if (launchTimeout > 0 && uptime > launchTimeout) {
                        // Computer is offline and startup time has expired
                        LOGGER.log(Level.INFO, "Startup timeout of " + computer.getName() + " after "
                                + uptime +
                                " milliseconds (timeout: " + launchTimeout + " milliseconds), instance status: " + state);
                        node.launchTimeout();
                    }
                    return 1;
                }
                else {
                    LOGGER.log(Level.FINE, "Computer {0} offline but not connecting, will check if it should be terminated because of the idle time configured", computer.getInstanceId());
                }
            }
            final long idleMilliseconds = this.clock.millis() - computer.getIdleStartMilliseconds();
            if (idleTerminationMinutes > 0) {
                if (idleMilliseconds > TimeUnit.MINUTES.toMillis(idleTerminationMinutes)) {
                    LOGGER.log(Level.INFO, "Idle timeout of " + computer.getName() + " after "
                            + TimeUnit.MILLISECONDS.toMinutes(idleMilliseconds) +
                            " idle minutes, instance status: " + state);
                    YCAbstractSlave slaveNode = computer.getNode();
                    if (slaveNode != null) {
                        slaveNode.idleTimeout();
                    }
                }
            }else {
                final int oneHourSeconds = (int) TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);
                final int freeSecondsLeft = oneHourSeconds
                        - (int) (TimeUnit.SECONDS.convert(uptime, TimeUnit.MILLISECONDS) % oneHourSeconds);
                if (freeSecondsLeft <= TimeUnit.MINUTES.toSeconds(Math.abs(idleTerminationMinutes))) {
                    LOGGER.log(Level.INFO, "Idle timeout of " + computer.getName() + " after "
                            + TimeUnit.MILLISECONDS.toMinutes(idleMilliseconds) + " idle minutes, with "
                            + TimeUnit.SECONDS.toMinutes(freeSecondsLeft)
                            + " minutes remaining in billing period");
                    YCAbstractSlave slaveNode = computer.getNode();
                    if (slaveNode != null) {
                        slaveNode.idleTimeout();
                    }
                }
            }
        }

        return 1;
    }

    protected Object readResolve() {
        checkLock = new ReentrantLock(false);
        clock = Clock.systemUTC();
        return this;
    }
}

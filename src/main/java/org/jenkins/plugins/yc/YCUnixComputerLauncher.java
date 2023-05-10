package org.jenkins.plugins.yc;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.HTTPProxyData;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.ServerHostKeyVerifier;
import com.trilead.ssh2.Session;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.yc.exception.YandexClientException;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jenkins.plugins.yc.YCHostAddressProvider.getPrivateIpAddress;


public class YCUnixComputerLauncher extends YCComputerLauncher{

    private static final Logger LOGGER = Logger.getLogger(YCUnixComputerLauncher.class.getName());

    private static final int bootstrapAuthSleepMs = 30000;

    @Override
    protected boolean launchScript(YCComputer computer, TaskListener listener) throws IOException {
        final Connection conn;
        Connection cleanupConn = null;
        boolean successful = false;
        PrintStream logger = listener.getLogger();
        YCAbstractSlave node = computer.getNode();
        YandexTemplate template = computer.getSlaveTemplate();

        if (node == null) {
            throw new IllegalStateException();
        }

        if (template == null) {
            throw new IOException("Could not find corresponding slave template for " + computer.getDisplayName());
        }

        LOGGER.log(Level.INFO, String.format("Launch instance with id: %s", node.getInstanceId()));
        YCPrivateKey ycPrivateKey = computer.getCloud().resolvePrivateKey();
        try{
            boolean isBootstrapped = bootstrap(computer, listener, template);
            if (isBootstrapped) {
                if(ycPrivateKey != null) {
                    LOGGER.log(Level.INFO, "connect fresh as " + ycPrivateKey.getUserName());
                    cleanupConn = connectToSsh(computer, listener);
                    if (!cleanupConn.authenticateWithPublicKey(ycPrivateKey.getUserName(),
                            ycPrivateKey.getPrivateKey().toCharArray(), "")) {
                        LOGGER.log(Level.WARNING, "Authentication failed");
                        return false;
                    }
                }else{
                    LOGGER.log(Level.WARNING, "Authentication failed");
                    return false;
                }
            } else {
                LOGGER.log(Level.WARNING, "Bootstrap result failed");
                throw new YandexClientException("Ssh connection error");
            }
            conn = cleanupConn;
            SCPClient scp = conn.createSCPClient();
            String initScript = node.getInitScript();
            String tmpDir = (Util.fixEmptyAndTrim(node.getTmpDir()) != null ? node.getTmpDir() : "/tmp");
            LOGGER.log(Level.INFO,  "Creating tmp directory (" + tmpDir + ") if it does not exist");
            conn.exec("mkdir -p " + tmpDir, logger);
            if (initScript != null && initScript.trim().length() > 0
                    && conn.exec("test -e ~/.hudson-run-init", logger) != 0) {
                LOGGER.log(Level.INFO, "Executing init script");
                scp.put(initScript.getBytes("UTF-8"), "init.sh", tmpDir, "0700");
                Session sess = conn.openSession();
                sess.requestDumbPTY();
                sess.execCommand(buildUpCommand(tmpDir + "/init.sh"));
                sess.getStdin().close();
                sess.getStderr().close();
                IOUtils.copy(sess.getStdout(), logger);
                int exitStatus = waitCompletion(sess);
                if (exitStatus != 0) {
                    LOGGER.log(Level.WARNING, "init script failed: exit code=" + exitStatus);
                    return false;
                }
                sess.close();
                LOGGER.log(Level.INFO, "Creating ~/.hudson-run-init");
                sess = conn.openSession();
                sess.requestDumbPTY();
                sess.execCommand(buildUpCommand("touch ~/.hudson-run-init"));
                sess.getStdin().close();
                sess.getStderr().close();
                IOUtils.copy(sess.getStdout(), logger);
                exitStatus = waitCompletion(sess);
                if (exitStatus != 0) {
                    LOGGER.log(Level.WARNING, "init script failed: exit code=" + exitStatus);
                    return false;
                }
                sess.close();
            }
            executeRemote(conn, "java -fullversion", "sudo apt update; sudo apt install default-jdk -y", logger);
            executeRemote(conn, "which scp", "sudo apt install openssh-server -y", logger);
            LOGGER.log(Level.INFO,  "Copying remoting.jar to: " + tmpDir);
            scp.put(Jenkins.get().getJnlpJars("remoting.jar").readFully(), "remoting.jar", tmpDir);
            final Session javaOpens = conn.openSession();
            String javaAddOpens = "java --add-opens java.base/java.util=ALL-UNNAMED";
            javaOpens.execCommand(javaAddOpens);
            javaOpens.close();
            final String remoteFS = node.getRemoteFS();
            final String workDir = Util.fixEmptyAndTrim(remoteFS) != null ? remoteFS : tmpDir;
            String launchString = "java -jar " + tmpDir + "/remoting.jar -workDir " + workDir;
            LOGGER.log(Level.INFO, "Launching remoting agent (via Trilead SSH2 Connection): " + launchString);
            final Session sess = conn.openSession();
            sess.execCommand(launchString);
            computer.setChannel(sess.getStdout(), sess.getStdin(), logger, new Channel.Listener() {
                @Override
                public void onClosed(Channel channel, IOException cause) {
                    sess.close();
                    conn.close();
                }
            });
            successful = true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,  "Error via launch agent " + e.getMessage());
            throw new YandexClientException(e.getMessage());
        } finally {
            if (cleanupConn != null && !successful)
                cleanupConn.close();
        }
        return true;
    }



    private boolean bootstrap(YCComputer computer, TaskListener listener, YandexTemplate template) throws Exception{
        LOGGER.log(Level.INFO, "bootstrap()");
        Connection bootstrapConn = null;
        try {
            int tries = template.getParent().getAuthSleepMs() / bootstrapAuthSleepMs;
            tries = tries == 0 ? 1 : tries;
            boolean isAuthenticated = false;
            LOGGER.log(Level.INFO, "Getting keypair...");
            YCPrivateKey ycPrivateKey = computer.getCloud().resolvePrivateKey();
            if (ycPrivateKey == null){
                LOGGER.log(Level.WARNING, "Could not retrieve a valid key pair.");
                return false;
            }
            while (tries-- > 0) {
                LOGGER.log(Level.INFO, String.format("Authenticating as " + ycPrivateKey.getUserName()));
                try {
                    bootstrapConn = connectToSsh(computer, listener);
                    isAuthenticated = bootstrapConn.authenticateWithPublicKey(ycPrivateKey.getUserName(),
                            ycPrivateKey.getPrivateKey().toCharArray(),
                            "");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Exception trying to authenticate", e);
                    bootstrapConn.close();
                }
                if (isAuthenticated) {
                    break;
                }
                LOGGER.log(Level.WARNING, "Authentication failed. Trying again...");
                Thread.sleep(bootstrapAuthSleepMs);
            }
            if (!isAuthenticated) {
                LOGGER.log(Level.WARNING, "Authentication failed, timed out after" + (tries * bootstrapAuthSleepMs / 1000) + "s with status " + computer.getStatus());
                return false;
            }
        } finally {
            if (bootstrapConn != null) {
                bootstrapConn.close();
            }
        }
        return true;
    }


    private Connection connectToSsh(YCComputer computer, TaskListener listener) throws Exception {
        final YCAbstractSlave node = computer.getNode();
        final long timeout = node == null ? 0L : node.getLaunchTimeoutInMillis();
        final long startTime = System.currentTimeMillis();
        while (true) {
            try {
                long waitTime = System.currentTimeMillis() - startTime;
                if (timeout > 0 && waitTime > timeout) {
                    throw new Exception("Timed out after " + (waitTime / 1000)
                            + " seconds of waiting for ssh to become available. (maximum timeout configured is "
                            + (timeout / 1000) + ")");
                }
                if (computer.getNode() == null || computer.getInstanceId() == null) {
                    // getInstanceId() on EC2SpotSlave can return null if the spot request doesn't yet know
                    // the instance id that it is starting. Continue to wait until the instanceId is set.
                    LOGGER.log(Level.INFO,  "empty instanceId for Spot Slave.");
                    throw new IOException("goto sleep");
                }

                String host = getPrivateIpAddress(computer);
                if (StringUtils.isBlank(host)) {
                    LOGGER.log(Level.WARNING,  "Empty host, your host is most likely waiting for an ip address.");
                    throw new IOException("goto sleep");
                }

                if ("0.0.0.0".equals(host)) {
                    LOGGER.log(Level.WARNING,  "Invalid host 0.0.0.0, your host is most likely waiting for an ip address.");
                    throw new IOException("goto sleep");
                }

                int port = computer.getSshPort();
                int slaveConnectTimeout = 10000;
                LOGGER.log(Level.INFO,  "Connecting to " + host + " on port " + port + ", with timeout " + slaveConnectTimeout
                        + ".");
                Connection conn = new Connection(host, port);
                ProxyConfiguration proxyConfig = Jenkins.get().proxy;
                Proxy proxy = proxyConfig == null ? Proxy.NO_PROXY : proxyConfig.createProxy(host);
                if (!proxy.equals(Proxy.NO_PROXY) && proxy.address() instanceof InetSocketAddress) {
                    InetSocketAddress address = (InetSocketAddress) proxy.address();
                    HTTPProxyData proxyData = null;
                    if (null != proxyConfig.getUserName()) {
                        proxyData = new HTTPProxyData(address.getHostName(), address.getPort(), proxyConfig.getUserName(), proxyConfig.getPassword());
                    } else {
                        proxyData = new HTTPProxyData(address.getHostName(), address.getPort());
                    }
                    conn.setProxyData(proxyData);
                    LOGGER.log(Level.INFO, "Using HTTP Proxy Configuration");
                }

                conn.connect(new ServerHostKeyVerifierImpl(computer, listener), slaveConnectTimeout, slaveConnectTimeout);
                LOGGER.log(Level.INFO,  "Connected via SSH.");
                return conn; // successfully connected
            } catch (IOException e) {
                // keep retrying until SSH comes up
                LOGGER.log(Level.INFO, "Failed to connect via ssh: " + e.getMessage());

                // If the computer was set offline because it's not trusted, we avoid persisting in connecting to it.
                // The computer is offline for a long period
                if (computer.isOffline() && StringUtils.isNotBlank(computer.getOfflineCauseReason())) {
                    throw new Exception("The connection couldn't be established and the computer is now offline", e);
                } else {
                    LOGGER.log(Level.INFO, "Waiting for SSH to come up. Sleeping 5.");
                    Thread.sleep(5000);
                }
            }
        }
    }

    protected String buildUpCommand(String command) {
        return "sudo " + command;
    }

    private int waitCompletion(Session session) throws InterruptedException {
        // I noticed that the exit status delivery often gets delayed. Wait up
        // to 1 sec.
        for (int i = 0; i < 10; i++) {
            Integer r = session.getExitStatus();
            if (r != null)
                return r;
            Thread.sleep(100);
        }
        return -1;
    }

    private boolean executeRemote(Connection conn, String checkCommand, String command, PrintStream logger)
            throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Verifying: " + checkCommand);
        if (conn.exec(checkCommand, logger) != 0) {
            LOGGER.log(Level.INFO, "Installing: " + command);
            if (conn.exec(command, logger) != 0) {
                LOGGER.log(Level.WARNING,  "Failed to install: " + command);
                return false;
            }
        }
        return true;
    }

    private static class ServerHostKeyVerifierImpl implements ServerHostKeyVerifier {
        private final YCComputer computer;
        private final TaskListener listener;

        public ServerHostKeyVerifierImpl(final YCComputer computer, final TaskListener listener) {
            this.computer = computer;
            this.listener = listener;
        }

        @Override
        public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
            LOGGER.log(Level.INFO,  String.format("No SSH key verification (%s) for connections to %s", serverHostKeyAlgorithm, computer.getName()));
            return true;
        }
    }

}

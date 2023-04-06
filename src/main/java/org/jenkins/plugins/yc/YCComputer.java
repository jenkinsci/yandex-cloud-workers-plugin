package org.jenkins.plugins.yc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Node;
import hudson.slaves.SlaveComputer;
import org.jenkins.plugins.yc.util.TimeUtils;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import yandex.cloud.api.compute.v1.InstanceOuterClass;

public class YCComputer extends SlaveComputer {

    /**
     * Cached description of this YC instance. Lazily fetched.
     */
    private volatile InstanceOuterClass.Instance ycInstanceDescription;

    public YCComputer(YCAbstractSlave slave) {
        super(slave);
    }

    @Override
    public YCAbstractSlave getNode() {
        return (YCAbstractSlave) super.getNode();
    }

    @CheckForNull
    public String getInstanceId() {
        YCAbstractSlave node = getNode();
        return node == null ? null : node.getInstanceId();
    }

    public AbstractCloud getCloud() {
        YCAbstractSlave node = getNode();
        return node == null ? null : node.getCloud();
    }

    @CheckForNull
    public YandexTemplate getSlaveTemplate() {
        YCAbstractSlave node = getNode();
        if (node != null) {
            return node.getCloud().getTemplate(node.templateDescription);
        }
        return null;
    }


    public InstanceOuterClass.Instance describeInstance() throws Exception {
        if (ycInstanceDescription == null)
            ycInstanceDescription = Api.getInstanceResponse(getInstanceId(), getCloud());
        return ycInstanceDescription;
    }

    public String getStatus() throws Exception {
        ycInstanceDescription = Api.getInstanceResponse(getInstanceId(), getCloud());
        return ycInstanceDescription.getStatus().name();
    }

    /**
     * When the slave is deleted, terminate the instance.
     */
    @Override
    public HttpResponse doDoDelete() {
        checkPermission(DELETE);
        YCAbstractSlave node = getNode();
        if (node != null)
            node.terminate();
        return new HttpRedirect("..");
    }

    /**
     * What username to use to run root-like commands
     *
     * @return remote admin or {@code null} if the associated {@link Node} is {@code null}
     */
    @CheckForNull
    public String getRemoteAdmin() {
        YCAbstractSlave node = getNode();
        return node == null ? null : node.getRemoteAdmin();
    }

    public int getSshPort() {
        YCAbstractSlave node = getNode();
        return node == null ? 22 : node.getSshPort();
    }

    public String getRootCommandPrefix() {
        YCAbstractSlave node = getNode();
        return node == null ? "" : node.getRootCommandPrefix();
    }

    public String getSlaveCommandPrefix() {
        YCAbstractSlave node = getNode();
        return node == null ? "" : node.getSlaveCommandPrefix();
    }

    public String getSlaveCommandSuffix() {
        YCAbstractSlave node = getNode();
        return node == null ? "" : node.getSlaveCommandSuffix();
    }

    public void onConnected() {
        YCAbstractSlave node = getNode();
        if (node != null) {
            node.onConnected();
        }
    }

    public long getUptime() throws Exception {
        return System.currentTimeMillis() - TimeUtils.dateStrToLong(String.valueOf(describeInstance().getCreatedAt()));
    }
}

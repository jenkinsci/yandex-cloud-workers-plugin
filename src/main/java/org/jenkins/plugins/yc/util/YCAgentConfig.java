package org.jenkins.plugins.yc.util;

import hudson.model.Node;
import hudson.slaves.NodeProperty;
import org.jenkins.plugins.yc.YCTag;

import java.util.List;

public abstract class YCAgentConfig {
    final String name;
    final String description;
    final String remoteFS;
    final int numExecutors;
    final String labelString;
    final Node.Mode mode;
    final String initScript;
    final String tmpDir;
    final List<? extends NodeProperty<?>> nodeProperties;
    final String remoteAdmin;
    final List<YCTag> tags;
    final String cloudName;
    final String idleTerminationMinutes;
    final long launchTimeout;

    private YCAgentConfig(Builder<? extends Builder, ? extends YCAgentConfig> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.remoteFS = builder.remoteFS;
        this.numExecutors = builder.numExecutors;
        this.labelString = builder.labelString;
        this.mode = builder.mode;
        this.initScript = builder.initScript;
        this.tmpDir = builder.tmpDir;
        this.nodeProperties = builder.nodeProperties;
        this.remoteAdmin = builder.remoteAdmin;
        this.idleTerminationMinutes = builder.idleTerminationMinutes;
        this.tags = builder.tags;
        this.cloudName = builder.cloudName;
        this.launchTimeout = builder.launchTimeout;
    }

    public static class OnDemand extends YCAgentConfig {

        final String instanceId;
        final boolean stopOnTerminate;

        private OnDemand(OnDemandBuilder builder) {
            super(builder);
            this.instanceId = builder.getInstanceId();
            this.stopOnTerminate = builder.isStopOnTerminate();
        }
    }

    private static abstract class Builder<B extends Builder<B, C>, C extends YCAgentConfig> {
        private String name;
        private String description;
        private String remoteFS;
        private int numExecutors;
        private String labelString;
        private Node.Mode mode;
        private String initScript;
        private String tmpDir;
        private List<? extends NodeProperty<?>> nodeProperties;
        private String remoteAdmin;
        private String idleTerminationMinutes;
        private List<YCTag> tags;
        private String cloudName;
        private long launchTimeout;


        public B withName(String name) {
            this.name = name;
            return self();
        }

        public B withDescription(String description) {
            this.description = description;
            return self();
        }

        public B withRemoteFS(String remoteFS) {
            this.remoteFS = remoteFS;
            return self();
        }

        public B withNumExecutors(int numExecutors) {
            this.numExecutors = numExecutors;
            return self();
        }

        public B withLabelString(String labelString) {
            this.labelString = labelString;
            return self();
        }

        public B withMode(Node.Mode mode) {
            this.mode = mode;
            return self();
        }

        public B withNodeProperties(List<? extends NodeProperty<?>> nodeProperties) {
            this.nodeProperties = nodeProperties;
            return self();
        }

        public List<? extends NodeProperty<?>> getNodeProperties() {
            return nodeProperties;
        }

        public B withRemoteAdmin(String remoteAdmin) {
            this.remoteAdmin = remoteAdmin;
            return self();
        }

        public B withTags(List<YCTag> tags) {
            this.tags = tags;
            return self();
        }

        public B withLaunchTimeout(long launchTimeout) {
            this.launchTimeout = launchTimeout;
            return self();
        }

        public B withIdleTerminationMinutes(String idleTerminationMinutes) {
            this.idleTerminationMinutes = idleTerminationMinutes;
            return self();
        }

        public B withCloudName(String cloudName) {
            this.cloudName = cloudName;
            return self();
        }

        public B withInitScript(String initScript) {
            this.initScript = initScript;
            return self();
        }

        public B withTmpDir(String tmpDir) {
            this.tmpDir = tmpDir;
            return self();
        }

        protected abstract B self();

        public abstract C build();
    }

    public static class OnDemandBuilder extends Builder<OnDemandBuilder, OnDemand> {

        private String instanceId;
        private boolean stopOnTerminate;

        public String getInstanceId() {
            return instanceId;
        }

        public OnDemandBuilder withInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public OnDemandBuilder withStopOnTerminate(boolean stopOnTerminate) {
            this.stopOnTerminate = stopOnTerminate;
            return this;
        }

        public boolean isStopOnTerminate() {
            return stopOnTerminate;
        }

        @Override
        protected OnDemandBuilder self() {
            return this;
        }

        @Override
        public OnDemand build() {
            return new OnDemand(this);
        }
    }
}

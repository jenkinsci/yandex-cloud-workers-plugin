package io.jenkins.plugins.ycptg.util;

import hudson.model.Node;
import hudson.slaves.NodeProperty;
import io.jenkins.plugins.ycptg.YCTag;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class YCAgentConfig {
    private final String name;
    private final String description;

    private final String remoteFS;

    private final String tmpDir;
    private final String labelString;
    private final Node.Mode mode;
    private final String initScript;
    private final List<? extends NodeProperty<?>> nodeProperties;
    private final List<YCTag> tags;
    private final String cloudName;
    private final String idleTerminationMinutes;
    private final long launchTimeout;

    private final int numExecutors;

    private YCAgentConfig(Builder<? extends Builder, ? extends YCAgentConfig> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.labelString = builder.labelString;
        this.mode = builder.mode;
        this.initScript = builder.initScript;
        this.nodeProperties = builder.nodeProperties;
        this.idleTerminationMinutes = builder.idleTerminationMinutes;
        this.tags = builder.tags;
        this.cloudName = builder.cloudName;
        this.launchTimeout = builder.launchTimeout;
        this.numExecutors = builder.numExecutors;
        this.remoteFS = builder.remoteFS;
        this.tmpDir = builder.tmpDir;
    }

    @Getter
    public static class OnDemand extends YCAgentConfig {

        private final String instanceId;
        private final boolean stopOnTerminate;

        private OnDemand(OnDemandBuilder builder) {
            super(builder);
            this.instanceId = builder.getInstanceId();
            this.stopOnTerminate = builder.isStopOnTerminate();
        }
    }

    private static abstract class Builder<B extends Builder<B, C>, C extends YCAgentConfig> {
        private String name;
        private String description;
        private String labelString;
        private Node.Mode mode;
        private String initScript;
        private List<? extends NodeProperty<?>> nodeProperties;
        private String idleTerminationMinutes;
        private List<YCTag> tags;
        private String cloudName;
        private long launchTimeout;

        private int numExecutors;

        private String remoteFS;

        private String tmpDir;


        public B withName(String name) {
            this.name = name;
            return self();
        }

        public B withDescription(String description) {
            this.description = description;
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

        public B withNumExecutors(int numExecutors) {
            this.numExecutors = numExecutors;
            return self();
        }

        public B withRemoteFS(String remoteFS) {
            this.remoteFS = remoteFS;
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

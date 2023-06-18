package io.jenkins.plugins.yc.util;

import hudson.model.Descriptor;
import io.jenkins.plugins.yc.YCAbstractSlave;
import jenkins.model.Jenkins;

import java.io.IOException;

public interface YCAgentFactory {
    static YCAgentFactory getInstance() {
        YCAgentFactory instance = null;
        for (YCAgentFactory implementation : Jenkins.get().getExtensionList(YCAgentFactory.class)) {
            if (instance != null) {
                throw new IllegalStateException("Multiple implementations of " + YCAgentFactory.class.getName()
                        + " found. If overriding, please consider using ExtensionFilter");
            }
            instance = implementation;
        }
        return instance;
    }

    YCAbstractSlave createOnDemandAgent(YCAgentConfig.OnDemand config) throws Descriptor.FormException, IOException;
}

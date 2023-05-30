package org.jenkins.plugins.yc.util;

import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkins.plugins.yc.YCAbstractSlave;
import org.jenkins.plugins.yc.YCOndemandSlave;

import java.io.IOException;

@Extension
public class YCAgentFactoryImpl implements YCAgentFactory {

    @Override
    public YCAbstractSlave createOnDemandAgent(YCAgentConfig.OnDemand config) throws Descriptor.FormException, IOException {
        return new YCOndemandSlave(
                config.getName(), config.getInstanceId(),
                config.getDescription(), config.getRemoteFS(), config.getLabelString(),
                config.getCloudName(), config.getIdleTerminationMinutes(),
                config.getInitScript(), config.getTmpDir(),
                config.getNodeProperties(), config.getLaunchTimeout(),
                config.isStopOnTerminate(), config.getNumExecutors());
    }
}

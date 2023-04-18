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
                config.name, config.instanceId,
                config.description, config.remoteFS, config.labelString,
                config.cloudName, config.idleTerminationMinutes,
                config.initScript, config.tmpDir,
                config.remoteAdmin, config.nodeProperties,
                config.launchTimeout, config.stopOnTerminate,
                config.numExecutors);
    }
}

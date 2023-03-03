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
        return new YCOndemandSlave(config.name, config.instanceId, config.description, config.labelString, config.cloudName, config.idleTerminationMinutes, config.initScript, config.remoteAdmin/*,
                config.remoteFS, config.numExecutors,
                config.mode, config.initScript, config.tmpDir,
                config.nodeProperties, config.stopOnTerminate,
                config.idleTerminationMinutes, config.tags,
                config.launchTimeout*/);
    }
}

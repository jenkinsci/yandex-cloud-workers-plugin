package org.jenkins.plugins.yc;

import hudson.model.Node;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class YCOnDemandSlaveTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testSpecifyMode() throws Exception {
        YCOndemandSlave slaveNormal = new YCOndemandSlave("name", "instanceId",
                "description", "remoteFS",
                1, "labelString", new YCUnixComputerLauncher(), Node.Mode.NORMAL,
                "initScript", "tmpDir",
                Collections.emptyList(), "remoteAdmin",
                false, "30", Collections.emptyList(),
                "cloudName",  0);
        assertEquals(Node.Mode.NORMAL, slaveNormal.getMode());

        YCOndemandSlave slaveExclusive = new YCOndemandSlave("name", "instanceId",
                "description", "remoteFS",
                1, "labelString", new YCUnixComputerLauncher(), Node.Mode.EXCLUSIVE,
                "initScript", "tmpDir", Collections.emptyList(),
                "remoteAdmin", false, "30",
                Collections.emptyList(), "cloudName",
                0);

        assertEquals(Node.Mode.EXCLUSIVE, slaveExclusive.getMode());
    }
}

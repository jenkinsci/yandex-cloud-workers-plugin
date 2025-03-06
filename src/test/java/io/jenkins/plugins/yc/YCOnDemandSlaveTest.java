package io.jenkins.plugins.ycptg;

import hudson.model.Node;
import io.jenkins.plugins.ycptg.YCOndemandSlave;
import io.jenkins.plugins.ycptg.YCUnixComputerLauncher;

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
                Collections.emptyList(),
                false, "30", Collections.emptyList(),
                "cloudName",  0);
        assertEquals(Node.Mode.NORMAL, slaveNormal.getMode());

        YCOndemandSlave slaveExclusive = new YCOndemandSlave("name", "instanceId",
                "description", "remoteFS",
                1, "labelString", new YCUnixComputerLauncher(), Node.Mode.EXCLUSIVE,
                "initScript", "tmpDir", Collections.emptyList(),
                false, "30",
                Collections.emptyList(), "cloudName",
                0);

        assertEquals(Node.Mode.EXCLUSIVE, slaveExclusive.getMode());
    }
}

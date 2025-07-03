package io.jenkins.plugins.yc;

import hudson.model.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class YCOnDemandSlaveTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testSpecifyMode() throws Exception {
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

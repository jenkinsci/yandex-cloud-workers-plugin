package io.jenkins.plugins.ycptg;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.slaves.ComputerListener;

@Extension
public class YCComputerListener extends ComputerListener {

    @Override
    public void onOnline(Computer c, TaskListener listener) {
        if (c instanceof YCComputer) {
            ((YCComputer) c).onConnected();
        }
    }
}

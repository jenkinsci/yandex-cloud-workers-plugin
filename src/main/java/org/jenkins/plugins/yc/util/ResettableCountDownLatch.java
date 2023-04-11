package org.jenkins.plugins.yc.util;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Restricted(NoExternalUse.class)
public class ResettableCountDownLatch {
    private final int count;
    private final AtomicReference<CountDownLatch> latchHolder = new AtomicReference<>();

    public ResettableCountDownLatch(int count, boolean setInitialState) {
        this.count = count;
        if (setInitialState) {
            latchHolder.set(new CountDownLatch(count));
        } else {
            latchHolder.set(new CountDownLatch(0));
        }
    }

    public void countDown() {
        latchHolder.get().countDown();
    }

    public void reset() {
        latchHolder.set(new CountDownLatch(count));
    }

    public long getCount() {
        return latchHolder.get().getCount();
    }
}
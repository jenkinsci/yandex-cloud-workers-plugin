package io.jenkins.plugins.ycptg.util;

import hudson.model.Job;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudUtil {

    private CloudUtil(){
        throw new IllegalStateException("Utility class");
    }

    private static final Logger LOGGER = Logger.getLogger(CloudUtil.class.getName());

    public static Queue.Item getItem(final String label) {
        try {
            Job job = null;

            if (label != null) {
                job = (Job) Jenkins.get().getItem(label);
            }

            if (job != null) {
                Queue.Item item = job.getQueueItem();
                if (item != null) {
                    return item;
                }
            } else {
                Queue queue = Jenkins.get().getQueue();
                Queue.Item[] items = queue.getItems();
                if (items != null && items.length > 0) {
                    return items[0];
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Exception caught trying to terminate slave", e);
        }
        return null;
    }


    public static void cancelItem(Queue.Item item, String label) {
        LOGGER.info("Cancelling Item ");
        try {

            if (item != null) {
                Queue queue = Queue.getInstance();
                queue.cancel(item);
                LOGGER.warning("Build " + label + " "
                        + " has been canceled");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Exception caught trying to terminate slave", e);
        }
    }

}

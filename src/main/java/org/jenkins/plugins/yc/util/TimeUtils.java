package org.jenkins.plugins.yc.util;

public class TimeUtils {
    public static long dateStrToLong(String dateStr) {
        return Long.parseLong(dateStr.replace("seconds: ", "").replace("\n", "")) * 1000;
    }
}

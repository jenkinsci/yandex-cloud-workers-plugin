package org.jenkins.plugins.yc.util;

public class TimeUtils {

    private TimeUtils(){
        throw new IllegalStateException("Utility class");
    }

    public static long dateStrToLong(String dateStr) {
        return Long.parseLong(dateStr.replace("seconds: ", "").replace("\n", "")) * 1000;
    }
}

package com.logmein.aws.utils;

public class DateTimeUtils {

    /**
     * @param milliSeconds
     */
    public static void sleep(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

}

package com.logmein.aws;

/**
 * @author ashwink
 */
public class DeviceFarmException extends RuntimeException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1856556560799516224L;

    /**
     * @param message
     */
    public DeviceFarmException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param th
     */
    public DeviceFarmException(String message, Throwable th) {
        super(message, th);
    }
}

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
     * @param message error message.
     */
    public DeviceFarmException(String message) {
        super(message);
    }

    /**
     * @param message error message.
     * @param th {@link Throwable}.
     */
    public DeviceFarmException(String message, Throwable th) {
        super(message, th);
    }
}

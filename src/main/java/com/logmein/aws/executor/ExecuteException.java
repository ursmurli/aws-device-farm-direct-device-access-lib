
package com.logmein.aws.executor;

/**
 * Exception thrown by executor when command execution fails.
 * @author ashwink
 */
public class ExecuteException extends RuntimeException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 2914001195415055302L;

    /**
     * Constructor.
     * @param message error message.
     * @param th {@link Throwable}.
     */
    public ExecuteException(final String message, final Throwable th) {
        super(message, th);
    }

    /**
     * Constructor.
     * @param th {@link Throwable}.
     */
    public ExecuteException(final Throwable th) {
        super(th);
    }

}

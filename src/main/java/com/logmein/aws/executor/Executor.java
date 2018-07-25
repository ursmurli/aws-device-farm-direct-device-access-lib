
package com.logmein.aws.executor;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logmein.aws.utils.DateTimeUtils;

/**
 * Builds on apache's {@link org.apache.commons.exec.Executor} to provide some custom functionality
 * like result collection etc.
 * @author ashwink
 */
public class Executor {

    /**
     * @author Ashwin.Kusabhadran
     */
    public class StreamCollector extends LogOutputStream {
        /**
         *
         */
        private final List<String> lines = new LinkedList<>();

        /**
         * @return the stream as a list of String.
         */
        public List<String> getLines() {
            return lines;
        }

        @Override
        protected void processLine(final String line, final int level) {
            lines.add(line);
        }
    }

    /**
     * @param collection the Collection to check
     * @return true if given collection is null or empty, else false.
     */
    private static boolean isCollectionEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * {@link StreamCollector}.
     */
    private StreamCollector errorStream;

    /**
     * Executor.
     */
    private final org.apache.commons.exec.Executor executor;

    /**
     * Process exit value.
     */
    private int exitValue = -1;

    /**
     * Instance of a {@link Logger}.
     */
    private final Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * {@link ResultHandler}.
     */
    private DefaultExecuteResultHandler resultHandler;

    /**
     * {@link StreamCollector}.
     */
    private StreamCollector stdOutStream;

    /**
     * {@link ExecuteWatchdog}.
     */
    private ExecuteWatchdog watchdog = null;

    /**
     * timeout in milliseconds.
     */
    private Long timeOutInMilliSeconds;
    /**
     * {@link CommandLine}.
     */
    private CommandLine commandLine;

    /**
     * Boolean flag to indicate if the command should be run async.
     */
    private Boolean runInBackground;

    /**
     * Constructor.
     */
    public Executor() {
        this(null);
    }

    /**
     * Constructor.
     * @param cmdline {@link CommandLine}.
     */
    public Executor(final CommandLine cmdline) {
        executor = new DefaultExecutor();
        commandLine = cmdline;
        runInBackground = false;
    }

    /**
     * Set the commandline.
     * @param cmdline the {@link CommandLine}.
     * @return {@link Executor}
     */
    public Executor commandLine(final CommandLine cmdline) {
        commandLine = cmdline;
        return this;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        try {
            logger.debug("Command line: {}", commandLine);
            // create a watchdog
            if (timeOutInMilliSeconds > 0) {
                watchdog = new ExecuteWatchdog(timeOutInMilliSeconds);
            } else {
                watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
            }
            executor.setWatchdog(watchdog);

            ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
            executor.setProcessDestroyer(processDestroyer);

            // adding output and error streams.
            stdOutStream = new StreamCollector();
            errorStream = new StreamCollector();

            PumpStreamHandler streamHandler = new PumpStreamHandler(stdOutStream, errorStream);
            executor.setStreamHandler(streamHandler);

            if (runInBackground) {
                logger.debug("Executing command in background as non-blocking task");
                resultHandler = new DefaultExecuteResultHandler();
                executor.execute(commandLine, resultHandler);
            } else {
                logger.debug("Executing command as a blocking task.");
                exitValue = executor.execute(commandLine);
            }
        } catch (Exception | AssertionError e) {
            StringBuilder str = new StringBuilder();
            str.append("Command execution failed. ");
            str.append(getCommandAndResultForLogging());
            throw new ExecuteException(str.toString(), e);
        }
    }

    /**
     * @return the CommandLine along with output stream, error stream and exit code for logging.
     *         This should be called only after the execution is complete.
     */
    public String getCommandAndResultForLogging() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("\nCommand Line: ");
        strBuilder.append(commandLine.toString().replaceAll(",", " "));
        ExecutorResult result = getResult();
        if (result != null) {
            strBuilder.append("\nOutputStream: ");
            strBuilder.append(result.getOutputStream());
            strBuilder.append("\nErrorStream: ");
            strBuilder.append(result.getErrorStream());
            strBuilder.append("\nExitCode: ");
            strBuilder.append(result.getExitCode());
        }
        return strBuilder.toString();
    }

    /**
     * @return {@link org.apache.commons.exec.Executor}.
     */
    public org.apache.commons.exec.Executor getExecutor() {
        return executor;
    }

    /**
     * Get the result.This should be called after the process execution is complete or you will get
     * an incomplete result object. In the case of async execution first call the {@link #waitFor()}
     * or {@link #waitFor(long)} for execution to complete.
     * @return {@link ExecutorResult}.
     */
    public ExecutorResult getResult() {
        ExecutorResult result = new ExecutorResult().outputStream(stdOutStream).errorStream(
                errorStream);
        // if process has not finished execution, there will be no exit code.
        if (isRunning()) {
            return result.exitCode(-1);
        }
        // for asynchronous execution.
        if (resultHandler != null) {
            return result.exitCode(resultHandler.getExitValue());
        }
        // for synchronous execution.
        return result.exitCode(exitValue);

    }

    /**
     * Indicates whether process is running..
     * @return true if the process is still running, otherwise false.
     */
    public boolean isRunning() {
        // watch dog will be created for both asynchronous and synchronous execution.
        if (watchdog == null) {
            return false;
        }
        // The null check is required as the resultHandler is only there for async calls.
        // Need to check resultHandler before the watchdog. In case of process startup failures
        // we can get into deadlock if we call watchdog.isWatching(); The watch dog implementation
        // will first wait and ensure that the process is started. In case of process start up
        // failures this will never happen.
        if (resultHandler != null && resultHandler.hasResult()) {
            return false;
        }
        return watchdog.isWatching();
    }

    /**
     * @param background set to true to run as a background task. By default this is false.
     * @return {@link Executor}.
     */
    public Executor runInBackground(final boolean background) {
        runInBackground = background;
        return this;

    }

    /**
     * Shutdown the process that was started by the executor.
     */
    public void stop() {
        if (isRunning()) { // do not call destroyProcess() without this check, will deadlock if the
                           // process startup has failed.
            watchdog.destroyProcess();
        }
    }

    /**
     * Shutdown the process that was started by the executor ignoring all errors and warnings.
     */
    public void stopSilently() {
        if (isRunning()) { // do not call destroyProcess() without this check, will deadlock if the
                           // process startup has failed.
            try {
                watchdog.destroyProcess();
            } catch (Exception e) {
                logger.debug("Failed to stop the executor.", e);
            }
        }
    }

    /**
     * @param timeOut max wait time in milliseconds for the executable to complete. If this is set
     *            to a value greater than 0, then the executable process will be shutdown as soon as
     *            the timeout expires.
     * @return {@link Executor}.
     */
    public Executor timeout(final long timeOut) {
        timeOutInMilliSeconds = timeOut;
        return this;
    }

    /**
     * Causes the current thread to wait, if necessary, until the process has terminated. This
     * method returns immediately if the process has already terminated. If the process has not yet
     * terminated, the calling thread will be blocked until the process exits.<br>
     * <b>Note:</b> This is only applicable for asynchronous execution.
     * @throws UnsupportedOperationException if call made for non async execution
     * @throws ExecuteException if process execution has failed.
     */
    public void waitFor() {
        if (resultHandler == null) {
            throw new java.lang.UnsupportedOperationException("Wait for is applicable for "
                    + "Async/run in background calls Only");
        }
        try {
            resultHandler.waitFor();
        } catch (InterruptedException e) {
            logger.warn("Interrupted", e);
            Thread.currentThread().interrupt();
        }

        if (resultHandler.getException() != null) {
            throw new ExecuteException(resultHandler.getException());
        }
    }

    /**
     * Causes the current thread to wait for the given time out period for the process to terminate.
     * This method returns immediately if the process has already terminated. If the process has not
     * yet terminated, the calling thread will be blocked until timeout has expired.<br>
     * <b>Note:</b> This is only applicable for asynchronous execution.
     * @param timeoutInMillisec time out value.
     * @throws TimeoutException if the execution is not complete within given timeout.
     * @throws UnsupportedOperationException if call made for non-async execution.
     * @throws ExecuteException if process execution has failed.
     */
    public void waitFor(final long timeoutInMillisec) throws TimeoutException {
        if (resultHandler == null) {
            throw new java.lang.UnsupportedOperationException("Wait for is applicable for "
                    + "Async/run in background calls Only");
        }
        try {
            resultHandler.waitFor(timeoutInMillisec);
        } catch (InterruptedException e) {
            logger.warn("Interrupted", e);
            Thread.currentThread().interrupt();
        }
        if (!resultHandler.hasResult()) {
            throw new TimeoutException("Execution is not complete within given time out of "
                    + timeoutInMillisec + " milliseconds.");
        }
        if (resultHandler.getException() != null) {
            throw new ExecuteException(resultHandler.getException());
        }
    }

    /**
     * Wait until the output or error stream is not empty.
     * @param waitTimeInMilliseconds max time in milliseconds to wait for either of the streams to
     *            get populated.
     * @return true if either of the streams got populated within the given timeout, else false.
     * @throws IllegalStateException if the execution is not yet started.
     */
    public boolean waitUntilOutputOrErrorStreamIsNotEmpty(final long waitTimeInMilliseconds) {
        Instant waitUntil = Instant.now().plusMillis(waitTimeInMilliseconds);
        while (waitUntil.isAfter(Instant.now())) {

            if (stdOutStream == null || errorStream == null) {
                throw new IllegalStateException(
                        "Execution is not yet started. Call excute() method first to start the process.");
            }

            if (!isCollectionEmpty(stdOutStream.getLines()) || !isCollectionEmpty(errorStream
                    .getLines())) {
                return true;
            }
            DateTimeUtils.sleep(500);
        }
        return false;
    }

}

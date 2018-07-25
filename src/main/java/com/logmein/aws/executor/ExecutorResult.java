
package com.logmein.aws.executor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.logmein.aws.executor.Executor.StreamCollector;

/**
 * Result for Executor that warps the output stream, error stream and exit code.
 * @author ashwink
 */
public class ExecutorResult {
    /**
     * Output stream.
     */
    private StreamCollector outputStream;

    /**
     * Error stream.
     */
    private StreamCollector errorStream;

    /**
     * Exit code.
     */
    private int exitCode;

    /**
     *
     */
    public ExecutorResult() {
    }

    /**
     * @param stream the error stream
     * @return {@link ExecutorResult}.
     */
    public ExecutorResult errorStream(final StreamCollector stream) {
        errorStream = stream;
        return this;
    }

    /**
     * @param code the exit code.
     * @return {@link ExecutorResult}.
     */
    public ExecutorResult exitCode(final int code) {
        exitCode = code;
        return this;
    }

    /**
     * @return error stream.
     */
    public List<String> getErrorStream() {
        if (errorStream != null) {
            return errorStream.getLines();
        }
        return new ArrayList<>(0);
    }

    /**
     * @return exit code.
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * @return output stream.
     */
    public List<String> getOutputStream() {
        if (outputStream != null) {
            return outputStream.getLines();
        }
        return new ArrayList<>(0);
    }

    /**
     * Check if error stream is empty or null.
     * @return boolean
     */
    public boolean isErrorStreamEmpty() {
        String errorStreamString = getErrorStream().toString();
        if (StringUtils.isBlank(errorStreamString)) {
            return true;
        }
        // remove the starting '[' and the end ']'
        errorStreamString = errorStreamString.substring(1, errorStreamString.length() - 1);
        return StringUtils.isBlank(errorStreamString);
    }

    /**
     * Check if output stream is empty or null.
     * @return boolean
     */
    public boolean isOutputStreamEmpty() {
        String outputStreamString = getOutputStream().toString();
        if (StringUtils.isBlank(outputStreamString)) {
            return true;
        }
        // remove the starting '[' and the end ']'
        outputStreamString = outputStreamString.substring(1, outputStreamString.length() - 1);
        return StringUtils.isBlank(outputStreamString);
    }

    /**
     * @param stream the output stream
     * @return {@link ExecutorResult}.
     */
    public ExecutorResult outputStream(final StreamCollector stream) {
        outputStream = stream;
        return this;
    }

}

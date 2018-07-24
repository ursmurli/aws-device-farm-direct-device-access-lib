package com.logmein.aws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logmein.aws.executor.Executor;
import com.logmein.aws.executor.ExecutorResult;
import com.logmein.aws.utils.DateTimeUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * Class for starting and stopping tunnel required for setting up a direct device access session.
 * @author ashwink
 */
public class DeviceFarmTunnel {

    private static final String MAC_TUNNEL_FILE_NAME = "aws-device-farm-tunnel";

    private static final int BYTES = 1024;

    private static final String ERROR_MSG = "tunnel cannot be created";

    private static final String SUCCESS_MSG = "Use `ctrl + c` to stop the daemon";

    /**
     * @return
     */
    private static synchronized File getRootTunnelDir() {
        String userHome = System.getProperty("user.home");
        File root = new File(userHome, "DirectDeviceAccessTunnels");
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new DeviceFarmException("Failed to create dir: " + root.getAbsolutePath());
            }
        }
        return root;

    }

    /**
     * logger.
     */
    private Logger logger = LoggerFactory.getLogger(DeviceFarmTunnel.class);

    /**
     * IP address of the host that you want to start the tunnel connection to.
     */
    private String ipAddress;

    /**
     * {@link Executor}.
     */
    private Executor executor;

    /**
     * Constructor.
     * @param hostIp
     */
    public DeviceFarmTunnel(final String hostIp) {
        ipAddress = hostIp;
    }

    public void start(AWSDirectory dir) {
        File tunnleZip = getTunnel();
        logger.info("Tunnel file: {}", tunnleZip);

        // unzip the file
        unzip(tunnleZip.getAbsolutePath(), tunnleZip.getParentFile().getAbsolutePath());

        File tunnelFile = new File(tunnleZip.getParentFile(), MAC_TUNNEL_FILE_NAME);
        tunnelFile.setExecutable(true);

        // start the tunnel.
        CommandLine command = new CommandLine(tunnelFile);
        command.addArgument("start");
        command.addArgument(dir.getPrivateKey().getAbsolutePath());
        command.addArgument(ipAddress);

        executor = new Executor(command).runInBackground(true).timeout(0);
        executor.execute();
        executor.waitUntilOutputOrErrorStreamIsNotEmpty(10000);

        ExecutorResult result = null;
        Instant timeout = Instant.now().plusSeconds(30);
        while (timeout.isAfter(Instant.now())) {
            result = executor.getResult();
            logger.debug("Output stream: " + result.getOutputStream());
            logger.debug("Error stream: " + result.getErrorStream());

            if (result.getOutputStream().toString().contains(SUCCESS_MSG)) {
                break;
            }
            if (result.getErrorStream().toString().contains(ERROR_MSG) || result.getOutputStream()
                    .toString().contains(ERROR_MSG)) {
                throw new DeviceFarmException("Failed to create tunnel to AWS.\n" + executor
                        .getCommandAndResultForLogging());
            }
            DateTimeUtils.sleep(500);
        }
    }

    /**
     * Stop the tunnel.
     */
    public void stop() {
        if (executor == null) {
            logger.warn("Tunnel was not started, so nothing to stop.");
        }
        executor.stopSilently();
    }

    /**
     * @return
     */
    private File createDestination() {
        File tunnelDir = new File(getRootTunnelDir(), ipAddress.trim());
        if (tunnelDir.exists()) {
            return tunnelDir;
        }
        logger.info("Creating tunnel dir at: {}", tunnelDir.getAbsolutePath());
        if (!tunnelDir.mkdir()) {
            throw new DeviceFarmException("Failed to create dir for tunnel file: " + tunnelDir
                    .getAbsolutePath());
        }
        return tunnelDir;
    }

    /**
     * @return
     */
    private File getTunnel() {
        String resourceFilePath = "";

        if (SystemUtils.IS_OS_MAC) {
            resourceFilePath = "aws-device-farm-tunnel-macos.zip";
        } else if (SystemUtils.IS_OS_LINUX) {
            resourceFilePath = "aws-device-farm-tunnel-linux.zip";
        } else {
            throw new DeviceFarmException("Unsupported OS for direct device access.");
        }

        File destinationDir = createDestination();
        File destination = new File(destinationDir, resourceFilePath);

        InputStream is = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        is = loader.getResourceAsStream(resourceFilePath);
        if (is == null) {
            is = loader.getResourceAsStream("/" + resourceFilePath);
        }

        if (is == null) {
            // Using Global Properties
            is = DeviceFarmTunnel.class.getResourceAsStream(resourceFilePath);
            if (is == null) {
                is = DeviceFarmTunnel.class.getResourceAsStream("/" + resourceFilePath);
            }
        }

        if (is == null) {
            throw new DeviceFarmException("Unable to get the tunnel file.");
        }

        OutputStream out;
        try {
            out = new FileOutputStream(destination);
        } catch (FileNotFoundException e) {
            throw new DeviceFarmException("Unable to get the tunnel file.", e);
        }

        byte[] buf = new byte[BYTES];
        int len = -1;
        try {
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new DeviceFarmException("Unable to get the tunnel file.", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                throw new DeviceFarmException("Unable to get the tunnel file.", e);
            }
            try {
                is.close();
            } catch (IOException e) {
                throw new DeviceFarmException("Unable to get the tunnel file.", e);
            }
        }
        return destination;
    }

    /**
     * Unzips the file to the given directory.
     * @param zipFilePath absolute path to the zip file.
     * @param directoryToExtractTo the directory where the file is to be extracted to.
     */
    private void unzip(final String zipFilePath, final String directoryToExtractTo) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            zipFile.extractAll(directoryToExtractTo);
        } catch (ZipException e) {
            StringBuilder str = new StringBuilder();
            str.append("Failed to unzip the file: ");
            str.append(zipFilePath);
            str.append(" to: ");
            str.append(directoryToExtractTo);
            throw new DeviceFarmException(str.toString(), e);
        }
    }

}

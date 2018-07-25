package com.logmein.aws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
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

    public static final String TUNNEL_FILE_NAME = "aws-device-farm-tunnel";

    private static final int BYTES = 1024;

    private static final String ERROR_MSG = "tunnel cannot be created";

    private static final String SUCCESS_MSG = "Use `ctrl + c` to stop the daemon";

    public static final String TUNNEL_ROOT_DIR = "DirectDeviceAccessTunnels";

    /**
     * @return the root directory where the tunnel files are saved.
     */
    private static synchronized File getRootTunnelDir() {
        String userHome = System.getProperty("user.home");
        File root = new File(userHome, TUNNEL_ROOT_DIR);
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new DeviceFarmException("Failed to create dir: " + root.getAbsolutePath());
            }
        }
        return root;

    }

    /**
     * Logger.
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

    private File location;

    /**
     * Constructor.
     * @param hostIp the rmeote host to which the tunnel is being created.
     */
    public DeviceFarmTunnel(final String hostIp) {
        ipAddress = hostIp;
        location = createTunnelDirectory();
        addShutDownHookToStopTunnelAndDeleteFile();
    }

    /**
     * @return {@link File} - location of tunnel file.
     */
    public File getLocation() {
        return location;
    }

    /**
     * Start the tunnel.
     * @param dir {@link AWSDirectory}.
     */
    public void start(AWSDirectory dir) {
        File tunnelFile = getTunnel();
        logger.debug("Tunnel file: {}", tunnelFile.getAbsolutePath());

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
    public void stopTunnel() {
        if (executor == null) {
            logger.warn("Tunnel was not started, so nothing to stop.");
        } else {
            executor.stopSilently();
        }
        FileUtils.deleteQuietly(location);
    }

    /**
     * Shutdown hook to stop the tunnel and delete the folder.
     */
    private void addShutDownHookToStopTunnelAndDeleteFile() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            /**
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                stopTunnel();

            }
        });
    }

    /**
     * @return {@link File} folder where the tunnel file is saved.
     */
    private File createTunnelDirectory() {
        File tunnelDir = new File(getRootTunnelDir(), ipAddress.trim());
        if (tunnelDir.exists()) {
            return tunnelDir;
        }
        logger.debug("Creating tunnel dir at: {}", tunnelDir.getAbsolutePath());
        if (!tunnelDir.mkdir()) {
            throw new DeviceFarmException("Failed to create dir for tunnel file: " + tunnelDir
                    .getAbsolutePath());
        }
        return tunnelDir;
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

    /**
     * @return The tunnel file.
     */
    protected File getTunnel() {
        String resourceFilePath = "";

        if (SystemUtils.IS_OS_MAC) {
            resourceFilePath = "aws-device-farm-tunnel-macos.zip";
        } else if (SystemUtils.IS_OS_LINUX) {
            resourceFilePath = "aws-device-farm-tunnel-linux.zip";
        } else {
            throw new DeviceFarmException("Unsupported OS for direct device access.");
        }

        File tunnelZipFile = new File(location, resourceFilePath);

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
            out = new FileOutputStream(tunnelZipFile);
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

        // unzip the file
        unzip(tunnelZipFile.getAbsolutePath(), tunnelZipFile.getParentFile().getAbsolutePath());

        File tunnelFile = new File(tunnelZipFile.getParentFile(), TUNNEL_FILE_NAME);
        tunnelFile.setExecutable(true);
        return tunnelFile;
    }

}

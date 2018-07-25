package com.logmein.aws;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Class to find and parse contents of the '.aws' dir.
 */
public class AWSDirectory {

    public static final String PRI_KEY_PEM = "prikey.pem";

    public static final String PUB_KEY_PUB = "pubkey.pub";

    /**
     * @return the default .aws directory.
     */
    private static File getDefaultAWSDir() {
        String userHome = System.getProperty("user.home");
        File dir = new File(userHome, ".aws");
        if (!dir.exists()) {
            throw new DeviceFarmException("Could not find .aws directory under default location "
                    + dir.getAbsolutePath());
        }
        validateAwsDirectory(dir);
        return dir;
    }

    /**
     * @param awsDirectory directory to validate.
     */
    private static void validateAwsDirectory(File awsDirectory) {
        if (!awsDirectory.exists() || !awsDirectory.isDirectory()) {
            throw new DeviceFarmException(".aws dir '" + awsDirectory.getAbsolutePath()
                    + "' does not exists or is not a directory.");
        }

        File deviceFarm = new File(awsDirectory, "devicefarm");
        if (!deviceFarm.exists()) {
            throw new DeviceFarmException(
                    "Could not find 'devicefarm' folder under .aws directory: " + awsDirectory
                            .getAbsolutePath());
        }

        if (!new File(deviceFarm, PRI_KEY_PEM).exists()) {
            throw new DeviceFarmException("Could not find '" + PRI_KEY_PEM
                    + "' file under devicefarm directory: " + deviceFarm.getAbsolutePath());
        }

        if (!new File(deviceFarm, PUB_KEY_PUB).exists()) {
            throw new DeviceFarmException("Could not find '" + PUB_KEY_PUB
                    + "' file under devicefarm directory: " + deviceFarm.getAbsolutePath());
        }
    }

    /**
     * .aws directory.
     */
    private File awsDir = null;

    /**
     * Default constructor.
     */
    public AWSDirectory() {
        this(getDefaultAWSDir());
    }

    /**
     * @param dir The '.aws' directory.
     */
    public AWSDirectory(File dir) {
        validateAwsDirectory(dir);
        awsDir = dir;
    }

    /**
     * @return the location of the .aws directory.
     */
    public File getLocation() {
        return awsDir;
    }

    /**
     * @return the private key File.
     */
    public File getPrivateKey() {
        File priKey = new File(getDeviceFarm(), PRI_KEY_PEM);
        if (!priKey.exists()) {
            throw new DeviceFarmException(String.format("Could not find %s at %", PUB_KEY_PUB,
                    priKey.getAbsolutePath()));
        }
        return priKey;
    }

    /**
     * @return the public key file.
     */
    public File getPublicKey() {
        File pubKey = new File(getDeviceFarm(), PUB_KEY_PUB);
        if (!pubKey.exists()) {
            throw new DeviceFarmException(String.format("Could not find %s at %", PUB_KEY_PUB,
                    pubKey.getAbsolutePath()));

        }
        return pubKey;
    }

    /**
     * @return the public key as String.
     */
    public String getPublicKeyContents() {
        File publicKey = getPublicKey();
        String contents = "";
        try {
            contents = new String(Files.readAllBytes(publicKey.toPath()));
        } catch (IOException e) {
            throw new DeviceFarmException("Error reading public key file:" + publicKey
                    .getAbsolutePath(), e);
        }
        return contents;
    }

    /**
     * @return 'devicefarm' folder.
     */
    private File getDeviceFarm() {
        File deviceFarm = new File(getLocation(), "devicefarm");
        if (!deviceFarm.exists()) {
            throw new DeviceFarmException(
                    "Could not find 'devicefarm' directory under '.aws' directory at: "
                            + getLocation());
        }
        return deviceFarm;
    }
}

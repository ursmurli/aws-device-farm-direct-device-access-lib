package com.logmein.aws;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.devicefarm.model.DeviceInstance;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Entry point for running as a jar.
 * @author ashwink
 */
public class Main {

    /**
     * @param args arguments
     * @throws IOException exception
     */
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        JCommander jcommander = new JCommander(main);
        jcommander.parse(args);
        if (main.help) {
            jcommander.usage();
            return;
        }
        main.startDirectDeviceSession();
        System.exit(0);
    }

    @Parameter(names = {"--projectArn"}, description = "AWS project ARN", required = true)
    String projectArn;

    @Parameter(names = {"--profile"}, description = "AWS profile to use", required = true)
    String profile;

    @Parameter(names = {"--name"}, description = "Session name", required = false)
    String name = "Test " + ZonedDateTime.now(ZoneId.systemDefault());

    @Parameter(names = {"--timeOut"}, description = "Time out in seconds", required = false)
    long timeOutInSeconds = 10;

    @Parameter(names = "--help", help = true)
    private boolean help = false;

    /**
     * Start the direct device access session.
     * @throws IOException exception.
     */
    public void startDirectDeviceSession() throws IOException {
        DeviceFarmClient client = new DeviceFarmClient(new ProfileCredentialsProvider(profile));

        System.out.println("\n==> Getting an Android private device from your device pool.");
        DeviceInstance androidDevice = client.getAndroidPhone(timeOutInSeconds);
        System.out.println("\n==> Device obtained: " + androidDevice);

        //@formatter:off
        DirectDeviceAccessCapabilities caps = new DirectDeviceAccessCapabilities()
                .deviceInstance(androidDevice)
                .name(name)
                .projectArn(projectArn)
                .timeOutInSeconds(600);
        //@formatter:on

        DirectDeviceSession session = client.createDirectDeviceAccessSession(caps);
        System.out.println("\n==> Session Created: " + session.toString());

        System.out.println("\n==> Hit enter to close session...");
        System.in.read();

        System.out.println("\n==> Stopping session...");
        client.stopDirectDeviceAccessSession(session);
        System.out.println("\n==> Session stopped");

    }

}

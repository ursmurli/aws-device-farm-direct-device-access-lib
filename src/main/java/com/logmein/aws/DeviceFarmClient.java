package com.logmein.aws;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.devicefarm.AWSDeviceFarm;
import com.amazonaws.services.devicefarm.AWSDeviceFarmClientBuilder;
import com.amazonaws.services.devicefarm.model.CreateRemoteAccessSessionRequest;
import com.amazonaws.services.devicefarm.model.CreateRemoteAccessSessionResult;
import com.amazonaws.services.devicefarm.model.Device;
import com.amazonaws.services.devicefarm.model.DeviceInstance;
import com.amazonaws.services.devicefarm.model.GetRemoteAccessSessionRequest;
import com.amazonaws.services.devicefarm.model.GetRemoteAccessSessionResult;
import com.amazonaws.services.devicefarm.model.ListDevicesRequest;
import com.amazonaws.services.devicefarm.model.ListDevicesResult;
import com.amazonaws.services.devicefarm.model.RemoteAccessSession;
import com.amazonaws.services.devicefarm.model.StopRemoteAccessSessionRequest;
import com.amazonaws.util.CollectionUtils;
import com.logmein.aws.utils.DateTimeUtils;

/**
 * AWS Device farm client for starting and stopping Direct Device Access sessions.
 * @author ashwink
 */
public class DeviceFarmClient {

    private Logger logger = LoggerFactory.getLogger(DeviceFarmClient.class);

    private AWSDeviceFarm farm;

    private AWSDirectory awsDirectory;

    /**
     * @param credentialsProvider {@link AWSCredentialsProvider}.
     */
    public DeviceFarmClient(AWSCredentialsProvider credentialsProvider) {
        this(credentialsProvider, new AWSDirectory());
    }

    /**
     * @param credentialsProvider {@link AWSCredentialsProvider}.
     * @param directory {@link AWSDirectory}.
     */
    public DeviceFarmClient(AWSCredentialsProvider credentialsProvider, AWSDirectory directory) {
        farm = AWSDeviceFarmClientBuilder.standard().withCredentials(credentialsProvider).build();
        awsDirectory = new AWSDirectory();
    }

    /**
     * @param capabilities {@link DirectDeviceAccessCapabilities}.
     * @return {@link DirectDeviceSession}.
     */
    public DirectDeviceSession createDirectDeviceAccessSession(
            DirectDeviceAccessCapabilities capabilities) {
        RemoteAccessSession remoteAccessSession = createRemoteAccessSession(capabilities);
        DeviceFarmTunnel tunnel = new DeviceFarmTunnel(remoteAccessSession.getHostAddress());
        tunnel.start(new AWSDirectory());

        DirectDeviceSession session = new DirectDeviceSession(remoteAccessSession, tunnel);
        return session;
    }

    /**
     * @param waitInSeconds Max time to wait for a device to be available in seconds.
     * @return {@link DeviceInstance}.
     */
    public DeviceInstance getAndroidPhone(long waitInSeconds) {
        ListDevicesResult listDeviceResult = listDevices();

        //@formatter:off
        List<Device> devices = listDeviceResult.getDevices().stream().filter(device -> (
                device.getFleetType().equalsIgnoreCase("PRIVATE") &&
                        device.getFormFactor().equalsIgnoreCase("PHONE") &&
                        device.getPlatform().equalsIgnoreCase("ANDROID"))
        ).collect(Collectors.toList());
        // @formatter:on

        Instant timeOut = Instant.now().plusSeconds(waitInSeconds);

        while (Instant.now().isBefore(timeOut)) {
            for (Device device : devices) {
                List<DeviceInstance> instances = device.getInstances();
                if (CollectionUtils.isNullOrEmpty(instances)) {
                    continue;
                }
                for (DeviceInstance instance : instances) {
                    if (instance.getStatus().equalsIgnoreCase("AVAILABLE")) {
                        return instance;
                    }
                }
            }
            DateTimeUtils.sleep(5000);
        }
        throw new DeviceFarmException("Unable to get an Android device within given timeout of "
                + waitInSeconds + " seconds. Devices returned: \n" + devices.toString());
    }

    /**
     * @param session {@link DirectDeviceSession}.
     * @return {@link RemoteAccessSession}
     */
    public RemoteAccessSession stopDirectDeviceAccessSession(DirectDeviceSession session) {
        session.getTunnel().stopTunnel();
        return stopRemoteAccessSession(session.getRemoteAccessSession());
    }

    /**
     * @param capabilities {@link DirectDeviceAccessCapabilities}.
     * @return {@link RemoteAccessSession}.
     */
    private RemoteAccessSession createRemoteAccessSession(
            final DirectDeviceAccessCapabilities capabilities) {

        CreateRemoteAccessSessionRequest request = new CreateRemoteAccessSessionRequest();
        request.setProjectArn(capabilities.getProjectArn());
        request.setDeviceArn(capabilities.getDeviceInstance().getDeviceArn());
        request.setSshPublicKey(awsDirectory.getPublicKeyContents());
        request.setRemoteDebugEnabled(true);
        request.setName(capabilities.getName());

        CreateRemoteAccessSessionResult result = farm.createRemoteAccessSession(request);
        RemoteAccessSession session = result.getRemoteAccessSession();

        GetRemoteAccessSessionRequest remoteAccessSessionReq = new GetRemoteAccessSessionRequest();
        remoteAccessSessionReq.setArn(session.getArn());

        GetRemoteAccessSessionResult remoteAccessSessionResult = null;
        Instant sessionTimeout = Instant.now().plusSeconds(capabilities.getTimeOutInSeconds());

        while (sessionTimeout.isAfter(Instant.now())) {
            remoteAccessSessionResult = farm.getRemoteAccessSession(remoteAccessSessionReq);

            session = remoteAccessSessionResult.getRemoteAccessSession();
            logger.debug("Status: {}, Session arn: {}, ", session.getStatus(),
                    remoteAccessSessionReq);

            if ("RUNNING".equalsIgnoreCase(session.getStatus())) {
                return session;
            }
            DateTimeUtils.sleep(2000);
        }
        // one more check
        if ("RUNNING".equalsIgnoreCase(session.getStatus())) {
            return session;
        }
        throw new DeviceFarmException(
                "RemoteAccessSession did not start within the given timeout of " + capabilities
                        .getTimeOutInSeconds() + " seconds.");

    }

    /**
     * @return {@link ListDevicesResult}.
     */
    private ListDevicesResult listDevices() {
        ListDevicesRequest request = new ListDevicesRequest();
        return farm.listDevices(request);
    }

    /**
     * @param session {@link RemoteAccessSession}
     * @return {@link RemoteAccessSession}
     */
    private RemoteAccessSession stopRemoteAccessSession(RemoteAccessSession session) {
        StopRemoteAccessSessionRequest request = new StopRemoteAccessSessionRequest();
        request.setArn(session.getArn());

        return farm.stopRemoteAccessSession(request).getRemoteAccessSession();
    }

}

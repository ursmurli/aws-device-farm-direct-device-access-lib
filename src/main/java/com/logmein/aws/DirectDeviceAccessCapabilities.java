package com.logmein.aws;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

import com.amazonaws.services.devicefarm.model.DeviceInstance;

/**
 * Capabilities to Start a Direct device access session.
 * @author ashwink
 */
public class DirectDeviceAccessCapabilities {

    /**
     * Capabilities map.
     */
    private HashMap<String, Object> capabilities = new HashMap<>();

    /**
     *
     */
    public DirectDeviceAccessCapabilities() {
        capabilities = new HashMap<>();
    }

    public DirectDeviceAccessCapabilities deviceInstance(DeviceInstance instance) {
        capabilities.put(CapabilityType.DEVICE_INSTANCE, instance);
        return this;
    }

    public DeviceInstance getDeviceInstance() {
        return (DeviceInstance) capabilities.get(CapabilityType.DEVICE_INSTANCE);
    }

    public String getName() {
        Object obj = capabilities.get(CapabilityType.NAME);
        if (obj == null) {
            return "Session " + ZonedDateTime.now(ZoneId.systemDefault());
        }
        return (String) obj;
    }

    public String getProjectArn() {
        return (String) capabilities.get(CapabilityType.PROJECT_ARN);
    }

    public long getTimeOutInSeconds() {
        Object obj = capabilities.get(CapabilityType.TIME_OUT_IN_SECONDS);
        if (obj == null) {
            return 600; // 10 minutes;
        }
        return Long.parseLong(String.valueOf(obj));
    }

    public DirectDeviceAccessCapabilities name(String name) {
        capabilities.put(CapabilityType.NAME, name);
        return this;
    }

    public DirectDeviceAccessCapabilities projectArn(String arg) {
        capabilities.put(CapabilityType.PROJECT_ARN, arg);
        return this;
    }

    public DirectDeviceAccessCapabilities timeOutInSeconds(long timeOutInSeconds) {
        capabilities.put(CapabilityType.TIME_OUT_IN_SECONDS, timeOutInSeconds);
        return this;
    }

}

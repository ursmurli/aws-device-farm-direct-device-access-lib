package com.logmein.aws;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.amazonaws.services.devicefarm.model.DeviceInstance;

/**
 * Test for {@link DirectDeviceAccessCapabilities}.
 * @author ashwink
 */
public class DirectDeviceAccessCapabilitiesTest {

    private static DeviceInstance instance = new DeviceInstance();

    /**
     * Verify defailt values set.
     */
    @Test
    public void verifuyDefaultValues() {
        //@formatter:off
        DirectDeviceAccessCapabilities caps = new DirectDeviceAccessCapabilities()
                .deviceInstance(instance)
                .name("123")
                .projectArn("123456")
                .timeOutInSeconds(10);
        //@formatter:on

        assertThat(caps.getDeviceInstance(), sameInstance(instance));

        assertThat(caps.getName(), is("123"));
        assertThat(caps.getProjectArn(), is("123456"));
        assertThat(caps.getTimeOutInSeconds(), is(10L));

    }

    /**
     * Verify defaults set by capabilities.
     */
    @Test
    public void verifyCapabilityCreation() {
        DirectDeviceAccessCapabilities caps = new DirectDeviceAccessCapabilities();
        assertThat(caps.getDeviceInstance(), nullValue());
        assertThat(caps.getName(), containsString("Session"));
        assertThat(caps.getProjectArn(), nullValue());
        assertThat(caps.getTimeOutInSeconds(), is(600L));
    }

}

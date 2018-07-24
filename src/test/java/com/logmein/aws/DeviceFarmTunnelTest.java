package com.logmein.aws;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

/**
 * Test for {@link DeviceFarmTunnel} class.
 * @author ashwink
 */
public class DeviceFarmTunnelTest {

    /**
     * Test {@link DeviceFarmTunnel#getTunnel()}.
     */
    @Test
    public void getTunnelTest() {
        DeviceFarmTunnel tunnel = new DeviceFarmTunnel("1.1.1.1");
        File f = tunnel.getTunnel();
        assertThat(f.getAbsolutePath(), containsString(DeviceFarmTunnel.TUNNEL_ROOT_DIR
                + File.separator + "1.1.1.1" + File.separator + DeviceFarmTunnel.TUNNEL_FILE_NAME));

        DeviceFarmTunnel tunnel2 = new DeviceFarmTunnel("2.2.2.2");
        File f2 = tunnel2.getTunnel();
        assertThat(f2.getAbsolutePath(), containsString(DeviceFarmTunnel.TUNNEL_ROOT_DIR
                + File.separator + "2.2.2.2" + File.separator + DeviceFarmTunnel.TUNNEL_FILE_NAME));
    }

}

package com.logmein.aws;

import com.amazonaws.services.devicefarm.model.RemoteAccessSession;

/**
 * Class to represent a direct device session that is started in DeviceFarm.
 */
public class DirectDeviceSession {

    private RemoteAccessSession remoteAccessSession;

    private DeviceFarmTunnel deviceFarmTunnel;

    public DirectDeviceSession(RemoteAccessSession session, DeviceFarmTunnel tunnel) {
        remoteAccessSession = session;
        deviceFarmTunnel = tunnel;
    }

    /**
     * @return {@link RemoteAccessSession}
     */
    public RemoteAccessSession getRemoteAccessSession() {
        return remoteAccessSession;
    }

    /**
     * @return {@link DeviceFarmTunnel} used for the session.
     */
    public DeviceFarmTunnel getTunnel() {
        return deviceFarmTunnel;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("{");
        str.append("RemoteAccessSession: ");
        if (remoteAccessSession != null) {
            str.append(remoteAccessSession.toString());
        }
        str.append("}");
        return str.toString();
    }

}

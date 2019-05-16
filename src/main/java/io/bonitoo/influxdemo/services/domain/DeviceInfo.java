package io.bonitoo.influxdemo.services.domain;

import java.util.Date;

public class DeviceInfo {

    private String deviceNumber;
    private String name;
    private String deviceType;
    private boolean authorized;
    private String authId;
    private String authToken;

    private Date lastSeen;

    public String getDeviceNumber() {
        return deviceNumber;
    }

    public void setDeviceNumber(final String deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(final boolean authorized) {
        this.authorized = authorized;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(final String authId) {
        this.authId = authId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(final String authToken) {
        this.authToken = authToken;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(final Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
    }
}

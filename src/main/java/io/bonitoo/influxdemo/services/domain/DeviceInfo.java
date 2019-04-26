package io.bonitoo.influxdemo.services.domain;

public class DeviceInfo {

    public String deviceNumber;
    public String name;
    public boolean authorized;
    public String authId;
    public String authToken;

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
    }
}

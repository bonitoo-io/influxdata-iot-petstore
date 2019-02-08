package io.bonitoo.influxdemo.entities;

import java.time.Instant;

import org.influxdata.client.annotations.Column;
import org.influxdata.client.annotations.Measurement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Measurement(name = "sensor")
public class Sensor {

    @Column(timestamp = true)
    private Instant time;

    @Column(tag = true)
    private
    String sid;

    @Column(tag = true)
    private
    String location;

    @Column
    private
    double temperature;

    @Column
    private
    double humidity;

    @Column(name = "battery_capacity")
    private
    double batteryCapacity;

    public String getSid() {
        return sid;
    }

    public void setSid(final String sid) {
        this.sid = sid;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(final double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(final double humidity) {
        this.humidity = humidity;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(final double battery_capacity) {
        this.batteryCapacity = battery_capacity;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(final Instant time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

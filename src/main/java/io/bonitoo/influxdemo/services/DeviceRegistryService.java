package io.bonitoo.influxdemo.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.influxdata.client.AuthorizationsApi;
import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.domain.Authorization;
import org.influxdata.client.domain.AuthorizationUpdateRequest;
import org.influxdata.client.domain.Permission;
import org.influxdata.client.domain.PermissionResource;
import org.influxdata.spring.influx.InfluxDB2Properties;
import io.bonitoo.influxdemo.services.domain.DeviceInfo;

import org.springframework.stereotype.Service;

/**
 * Registry holding information about connected devices. In memory example implementation.
 */
@Service
public class DeviceRegistryService {

    private final InfluxDBClient influxDBClient;
    private final InfluxDB2Properties properties;

    //map holding device number and authorizations id
    private List<DeviceInfo> list = new ArrayList<>();

    public DeviceRegistryService(final InfluxDBClient influxDBClient, InfluxDB2Properties properties) {
        this.influxDBClient = influxDBClient;
        this.properties = properties;
    }

    public Optional<DeviceInfo> getDeviceInfo(String deviceId) {
        return list.stream().filter(d -> d.deviceNumber.equals(deviceId)).findFirst();
    }

    public void registerDevice(String deviceNumber) {
        updateDeviceInfo(deviceNumber, false, null);
    }

    private void updateDeviceInfo(String deviceNumber, boolean authorized, String authId) {

        Optional<DeviceInfo> deviceInfo = getDeviceInfo(deviceNumber);

        DeviceInfo dev = deviceInfo.orElse(new DeviceInfo());
        dev.deviceNumber = deviceNumber;
        dev.authorized = authorized;
        dev.authId = authId;

        if (!deviceInfo.isPresent()) {
            list.add(dev);
        }
    }

    public void removeDeviceInfo(String deviceId) {
        Optional<DeviceInfo> deviceInfo = getDeviceInfo(deviceId);

        if (!deviceInfo.isPresent()) {
            throw new IllegalStateException("Unable to remove. Device does not exist.");
        }

        deviceInfo.ifPresent(d -> {
            if (d.authId != null) {
                AuthorizationsApi authorizationsApi = influxDBClient.getAuthorizationsApi();
                authorizationsApi.deleteAuthorization(d.authId);
            }
            list.remove(d);
        });

    }

    public List<DeviceInfo> getDeviceInfos() {
        return Collections.unmodifiableList((list));
    }

    public List<DeviceInfo> getDeviceInfos(final int offset, final int limit) {
        return list.subList(offset, offset + limit);
    }

    public void authorizeDevice(String deviceId) {
        AuthorizationsApi authorizationsApi = influxDBClient.getAuthorizationsApi();

        Authorization a = new Authorization();
        a.setOrgID(properties.getOrg());
        a.setStatus(AuthorizationUpdateRequest.StatusEnum.ACTIVE);
        a.setDescription(deviceId);

        a.setPermissions(createWritePermissions());

        Authorization authorization = authorizationsApi.createAuthorization(a);

        Optional<DeviceInfo> infoOptional = getDeviceInfo(deviceId);
        infoOptional.ifPresent(device -> {
            device.authToken = authorization.getToken();
            device.authId = authorization.getId();
        });

        if (!infoOptional.isPresent()) {
            throw new IllegalStateException("Unable to authorize device " + deviceId);
        }

    }

    private List<Permission> createWritePermissions() {

        PermissionResource resource = new PermissionResource();
        resource.setOrgID(properties.getOrg());
        resource.setType(PermissionResource.TypeEnum.BUCKETS);

        Permission permission = new Permission();
        permission.setAction(Permission.ActionEnum.WRITE);
        permission.setResource(resource);

        return Collections.singletonList(permission);
    }


    /**
     * Validate device number
     */
    public boolean isValidDeviceNumber(final String id) {

        return id != null && id.matches("[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}");

    }

}

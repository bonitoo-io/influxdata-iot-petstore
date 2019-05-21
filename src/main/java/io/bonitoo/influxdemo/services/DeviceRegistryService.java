package io.bonitoo.influxdemo.services;

import java.time.Instant;
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
import org.influxdata.query.FluxTable;
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
        return list.stream().filter(d -> d.getDeviceNumber().equals(deviceId)).findFirst();
    }

    public void registerDevice(String deviceNumber, String remoteAddress) {
        Optional<DeviceInfo> deviceInfo = getDeviceInfo(deviceNumber);

        DeviceInfo dev = deviceInfo.orElse(new DeviceInfo());
        dev.setRemoteAddress(remoteAddress);
        dev.setDeviceNumber(deviceNumber);
        dev.setAuthorized(false);
        dev.setAuthId(null);

        if (!deviceInfo.isPresent()) {
            list.add(dev);
        }
    }
    public String getDeviceName(String id) {
        Optional<DeviceInfo> deviceInfo = getDeviceInfo(id);
        return deviceInfo.map(DeviceInfo::getName).orElseThrow(() -> new IllegalArgumentException("Device with "+id + " does not exist"));
    }

    public void setDeviceName (String id, String name) {

        Optional<DeviceInfo> deviceInfo = getDeviceInfo(id);

        deviceInfo.ifPresent(d -> d.setName(name));

        if (!deviceInfo.isPresent()) {
            throw new IllegalArgumentException("Device with "+id + " does not exist");
        }
    }

    public void removeDeviceInfo(String deviceId) {
        Optional<DeviceInfo> deviceInfo = getDeviceInfo(deviceId);

        if (!deviceInfo.isPresent()) {
            throw new IllegalStateException("Unable to remove. Device does not exist.");
        }

        deviceInfo.ifPresent(d -> {
            if (d.getAuthId() != null) {
                AuthorizationsApi authorizationsApi = influxDBClient.getAuthorizationsApi();
                authorizationsApi.deleteAuthorization(d.getAuthId());
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
            device.setAuthToken(authorization.getToken());
            device.setAuthId(authorization.getId());
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
        return id != null;
//        return id != null && id.matches("[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}");

    }

    public Instant getLastSeen(String deviceId) {

        Optional<DeviceInfo> deviceInfo = getDeviceInfo(deviceId);

        if (deviceInfo.isPresent()) {


            String s = "from(bucket: \"my-bucket\")\n" +
                "  |> range(start: -1w)\n" +
                "  |> filter(fn: (r) => r._measurement == \"sensor\")\n" +
                "  |> filter(fn: (r) => r.sid == \"" + deviceId + "\")\n" +
                "  |> keep(columns: [\"_time\",\"_value\"])\n" +
                "  |> last()";

            List<FluxTable> query = influxDBClient.getQueryApi().query(s);

            return query.stream().flatMap(fluxTable -> fluxTable.getRecords().stream()).findFirst().map(r -> r.getTime()).orElse(null);

        }

        return null;
    }

}

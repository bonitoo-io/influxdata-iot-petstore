package io.bonitoo.influxdemo.services;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.influxdb.client.AuthorizationsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.Authorization;
import com.influxdb.client.domain.AuthorizationUpdateRequest;
import com.influxdb.client.domain.Permission;
import com.influxdb.client.domain.PermissionResource;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.influxdb.spring.influx.InfluxDB2Properties;

import io.bonitoo.influxdemo.domain.DeviceInfo;
import io.bonitoo.influxdemo.services.data.DeviceInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Registry holding information about connected devices. In memory example implementation.
 */
@Service
public class DeviceRegistryService {

    private static Logger log = LoggerFactory.getLogger(DeviceRegistryService.class);

    private final InfluxDBClient influxDBClient;
    private final InfluxDB2Properties properties;
    private final DeviceInfoRepository repository;

    public DeviceRegistryService(final InfluxDBClient influxDBClient,
                                 final InfluxDB2Properties properties,
                                 final DeviceInfoRepository repository) {

        this.influxDBClient = influxDBClient;
        this.properties = properties;
        this.repository = repository;
    }

    public Optional<DeviceInfo> getDeviceInfo(String deviceId) {
        return repository.findById(deviceId);
    }

    public void registerDevice(String deviceNumber, String remoteAddress) {
        Optional<DeviceInfo> deviceInfo = getDeviceInfo(deviceNumber);

        DeviceInfo dev = deviceInfo.orElse(new DeviceInfo());
        dev.setRemoteAddress(remoteAddress);
        dev.setDeviceId(deviceNumber);
        dev.setCreatedAt(new Date());
        dev.setAuthorized(false);
        dev.setAuthId(null);

        if (!deviceInfo.isPresent()) {
            repository.save(dev);
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
            repository.deleteById(deviceId);
        });

    }

    public List<DeviceInfo> getDeviceInfos() {
        return repository.findAllByOrderByCreatedAt();
    }

    public List<DeviceInfo> getDeviceInfos(final int offset, final int limit) {
        return getDeviceInfos().subList(offset, offset + limit);
    }

    public void authorizeDevice(String deviceId, final String deviceName) {
        Optional<DeviceInfo> infoOptional = getDeviceInfo(deviceId);
        infoOptional.ifPresent(device -> {

            AuthorizationsApi authorizationsApi = influxDBClient.getAuthorizationsApi();

            Authorization a = new Authorization();
            a.setOrgID(properties.getOrg());
            a.setStatus(AuthorizationUpdateRequest.StatusEnum.ACTIVE);
            a.setDescription(deviceId);

            a.setPermissions(createWritePermissions());

            Authorization authorization = authorizationsApi.createAuthorization(a);

            device.setName(deviceName);
            device.setAuthToken(authorization.getToken());
            device.setAuthId(authorization.getId());

            repository.save(device);
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
                "  |> filter(fn: (r) => r.device_id == \"" + deviceId + "\")\n" +
                "  |> keep(columns: [\"_time\",\"_value\"])\n" +
                "  |> last()";

            List<FluxTable> query;
            try {
                query = influxDBClient.getQueryApi().query(s);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }

            return query.stream().flatMap(fluxTable -> fluxTable.getRecords().stream()).findFirst().map(FluxRecord::getTime).orElse(null);

        }

        return null;
    }

}

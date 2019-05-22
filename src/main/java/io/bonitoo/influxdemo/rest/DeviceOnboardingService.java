package io.bonitoo.influxdemo.rest;


import java.util.Date;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import org.influxdata.spring.influx.InfluxDB2Properties;

import io.bonitoo.influxdemo.rest.models.OnboardingResponse;
import io.bonitoo.influxdemo.services.DeviceRegistryService;
import io.bonitoo.influxdemo.domain.DeviceInfo;
import io.bonitoo.influxdemo.services.data.DeviceInfoRepository;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DeviceOnboardingService {

    private final InfluxDB2Properties properties;

    private static Logger log = LoggerFactory.getLogger(DeviceOnboardingService.class);

    private final DeviceRegistryService deviceRegistry;
    private final DeviceInfoRepository repository;

    @Autowired
    public DeviceOnboardingService(final InfluxDB2Properties properties,
                                   final DeviceRegistryService deviceRegistryService,
                                   final DeviceInfoRepository repository) {

        this.properties = properties;
        this.deviceRegistry = deviceRegistryService;
        this.repository = repository;

    }

    @RequestMapping(value = "/register/{id}", method = RequestMethod.GET)
    @ApiOperation(
        value = "Register the new device into IoT Hub",
        notes = "Returns configuration for device, including influxdb authToken, bucket name, orgId.",
        response = OnboardingResponse.class)

    @ApiResponses(
        value = {
            @ApiResponse(code = 200, message = "Successful retrieval of device registration", response = OnboardingResponse.class),
            @ApiResponse(code = 201, message = "Registration request was send, waiting for authorization."),
            @ApiResponse(code = 204, message = "Device was already registered and authorized."),
            @ApiResponse(code = 406, message = "Invalid device number"),
            @ApiResponse(code = 500, message = "Internal server error")
        }
    )
    @Timed(value = "registerDevice", percentiles = {0.5, 0.95, 0.999}, histogram = true)
    public ResponseEntity registerDevice(@PathVariable String id, HttpServletRequest request) {

        log.info("register device request for {}", id);

        if (!deviceRegistry.isValidDeviceNumber(id)) {
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
        }

        Optional<DeviceInfo> deviceInfo = deviceRegistry.getDeviceInfo(id);

        //first registration
        if (!deviceInfo.isPresent()) {
            deviceRegistry.registerDevice(id, request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        DeviceInfo info = deviceInfo.get();

        //pending authorization
        if (info.getAuthToken() == null) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        //device is already authorized
        if (info.isAuthorized()) {
            info.setLastSeen(new Date());
            repository.save(info);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        //return onboarding request with authtoken
        OnboardingResponse resp = new OnboardingResponse();

        resp.setAuthToken(info.getAuthToken());
        resp.setOrgId(properties.getOrg());
        resp.setBucket(properties.getBucket());
        resp.setUrl(properties.getUrl());

        resp.setDeviceId(id);

        info.setAuthorized(true);

        repository.save(info);

        return ResponseEntity.ok(resp);

    }


}

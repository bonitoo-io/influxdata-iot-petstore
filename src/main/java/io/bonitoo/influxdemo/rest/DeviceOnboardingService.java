package io.bonitoo.influxdemo.rest;


import java.util.Optional;

import io.bonitoo.influxdemo.rest.models.OnboardingResponse;
import io.bonitoo.influxdemo.services.DeviceRegistryService;
import io.bonitoo.influxdemo.services.InfluxDBService;
import io.bonitoo.influxdemo.services.domain.DeviceInfo;
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

    private final InfluxDBService influxDBService;

    private static Logger log = LoggerFactory.getLogger(DeviceOnboardingService.class);

    private final DeviceRegistryService deviceRegistry;

    @Autowired
    public DeviceOnboardingService(final InfluxDBService influxDBService, final DeviceRegistryService deviceRegistryService) {
        this.influxDBService = influxDBService;
        this.deviceRegistry = deviceRegistryService;
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
    public ResponseEntity registerDevice(@PathVariable String id) {

        log.info("register device request for {}", id);

        if (!deviceRegistry.isValidDeviceNumber(id)) {
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
        }

        Optional<DeviceInfo> deviceInfo = deviceRegistry.getDeviceInfo(id);

        //first registration
        if (!deviceInfo.isPresent()) {
            deviceRegistry.registerDevice(id);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        DeviceInfo info = deviceInfo.get();

        //pending authorization
        if (info.authToken == null) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        //device is already authorized
        if (info.authorized) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        //return onboarding request with authtoken
        OnboardingResponse resp = new OnboardingResponse();

        resp.setAuthToken(info.authToken);
        resp.setOrgId(influxDBService.getOrgId());
        resp.setBucket(influxDBService.getBucket());
        resp.setUrl(influxDBService.getUrl());

        resp.setDeviceId(id);

        info.authorized = true;

        return  ResponseEntity.ok(resp);

    }


}

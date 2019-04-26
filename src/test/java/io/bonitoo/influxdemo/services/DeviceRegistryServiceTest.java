package io.bonitoo.influxdemo.services;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {InfluxDBService.class,DeviceRegistryService.class})
@EnableConfigurationProperties

public class DeviceRegistryServiceTest {

    @Autowired
    DeviceRegistryService deviceRegistryService;

    @Test
    public void isValidDeviceNumber() {
        Assert.assertTrue(deviceRegistryService.isValidDeviceNumber("AS12-BV34-1234-ZXCY"));
        Assert.assertFalse(deviceRegistryService.isValidDeviceNumber("XXXAS12-BV34-1234-ZXCY"));
    }
}
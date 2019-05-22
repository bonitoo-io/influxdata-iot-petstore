package io.bonitoo.influxdemo.services;

import io.bonitoo.influxdemo.services.data.RepositoryConfiguration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RepositoryConfiguration.class, InfluxDBService.class,DeviceRegistryService.class})
@EnableConfigurationProperties
@EnableAutoConfiguration
public class DeviceRegistryServiceTest {

    @Autowired
    DeviceRegistryService deviceRegistryService;

    @Test
    public void isValidDeviceNumber() {
        Assert.assertTrue(deviceRegistryService.isValidDeviceNumber("AS12-BV34-1234-ZXCY"));
        Assert.assertTrue(deviceRegistryService.isValidDeviceNumber("XXXAS12-BV34-1234-ZXCY"));
        Assert.assertFalse(deviceRegistryService.isValidDeviceNumber(null));
    }
}
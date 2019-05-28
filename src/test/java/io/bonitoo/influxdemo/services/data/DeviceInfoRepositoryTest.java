package io.bonitoo.influxdemo.services.data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import io.bonitoo.influxdemo.domain.DeviceInfo;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Jakub Bednar (bednar@github) (22/05/2019 09:54)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RepositoryConfiguration.class}, properties = {"petstore.db=./db.test.json"})
@EnableConfigurationProperties
@EnableAutoConfiguration
public class DeviceInfoRepositoryTest {

    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    @Before
    public void clear() {
        deviceInfoRepository.deleteAll();
    }

    @Test
    public void put() {

        DeviceInfo device = new DeviceInfo();
        device.setDeviceNumber(UUID.randomUUID().toString());
        device.setName("my-device");
        device.setDeviceType("my-device");
        device.setAuthorized(true);
        device.setAuthId("auth-id-1");
        device.setAuthToken("auth-token-4");
        device.setLastSeen(new Date());
        device.setRemoteAddress("192.168.1.1");

        deviceInfoRepository.save(device);
    }

    @Test
    public void findById() {

        String deviceNumber = UUID.randomUUID().toString();

        Optional<DeviceInfo> optional = deviceInfoRepository.findById(deviceNumber);

        Assertions.assertThat(optional.isPresent()).isFalse();

        DeviceInfo device = new DeviceInfo();
        device.setDeviceNumber(deviceNumber);
        device.setName("Testing name");

        deviceInfoRepository.save(device);

        optional = deviceInfoRepository.findById(deviceNumber);

        Assertions.assertThat(optional.isPresent()).isTrue();
    }

    @Test
    public void deleteById() {

        DeviceInfo device = new DeviceInfo();
        device.setName("Device without number");

        device = deviceInfoRepository.save(device);

        Assertions.assertThat(device.getDeviceNumber()).isNotBlank();

        String id = device.getDeviceNumber();

        device = deviceInfoRepository.findById(id).get();
        Assertions.assertThat(device.getDeviceNumber()).isEqualTo(id);
        Assertions.assertThat(device.getName()).isEqualTo("Device without number");

        deviceInfoRepository.deleteById(id);

        Assertions.assertThat(deviceInfoRepository.findById(id).isPresent()).isFalse();
    }

    @Test
    public void findAll() {

        Instant now = Instant.now();

        List<DeviceInfo> devices = deviceInfoRepository.findAllByOrderByCreatedAt();

        Assertions.assertThat(devices).isEmpty();

        DeviceInfo device1 = new DeviceInfo();
        device1.setName("device1");
        device1.setCreatedAt(Date.from(now));

        deviceInfoRepository.save(device1);

        DeviceInfo device2 = new DeviceInfo();
        device2.setName("device2");
        device2.setCreatedAt(Date.from(now.minus(1 , ChronoUnit.HOURS)));

        deviceInfoRepository.save(device2);

        devices = deviceInfoRepository.findAllByOrderByCreatedAt();
        Assertions.assertThat(devices).hasSize(2);
        Assertions.assertThat(devices.get(0).getName()).isEqualTo("device2");
        Assertions.assertThat(devices.get(1).getName()).isEqualTo("device1");
    }

    @Test
    public void disk() {

        Instant now = Instant.now();

        IntStream.range(0, 50).forEach(index ->
        {
            DeviceInfo device = new DeviceInfo();
            device.setName("device" + index);
            device.setCreatedAt(Date.from(now.plus(1, ChronoUnit.HOURS)));

            deviceInfoRepository.save(device);
        });

        KeyValueOperations operations = new RepositoryConfiguration("./db.test.json").persistKeyValue();
        Iterable<DeviceInfo> iterable = operations.findAll(DeviceInfo.class);

        List<DeviceInfo> devices = ImmutableList.copyOf(iterable);

        Assertions.assertThat(devices).hasSize(50);
    }
}

package io.bonitoo.demo.device;

import org.junit.Test;

public class DeviceTest {

    @Test
    public void testDevice() throws Exception {

        Device device = new Device();
        Thread.sleep(10000L);

        device.shutdown();

    }
}
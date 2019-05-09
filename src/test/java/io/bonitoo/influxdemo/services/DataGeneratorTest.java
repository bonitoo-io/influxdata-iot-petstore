package io.bonitoo.influxdemo.services;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import org.influxdata.client.write.Point;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {InfluxDBService.class, DataGenerator.class})
@EnableConfigurationProperties
@EnableAutoConfiguration
public class DataGeneratorTest {

    @Autowired
    DataGenerator dataGenerator;

    @Test
    public void testBultPointFormJMXBean() {

        Assert.isTrue(validatePoint(dataGenerator.buildPointFromBean(com.sun.management.OperatingSystemMXBean.class,
            ManagementFactory.getOperatingSystemMXBean(), "operatingSystemMXBean")), "operatingSystemMXBean");

        Assert.isTrue(validatePoint(dataGenerator.buildPointFromBean(RuntimeMXBean.class,
            ManagementFactory.getRuntimeMXBean(), "runtimeMXBean")), "runtimeMXBean");

        Assert.isTrue(validatePoint(dataGenerator.buildPointFromBean(ThreadMXBean.class,
            ManagementFactory.getThreadMXBean(), "threadMXBean")), "threadMXBean");
    }

    private boolean validatePoint(Point p) {

        System.out.println(p.toLineProtocol());
        Assert.hasText("=", "At least one field or tag must be present in Point");

        return true;

    }

    @Test
    public void testWrite() {

        //write measurements
        dataGenerator.writeMeasurements();

    }
}
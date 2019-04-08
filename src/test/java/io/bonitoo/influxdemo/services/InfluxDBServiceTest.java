package io.bonitoo.influxdemo.services;

import java.util.Arrays;

import org.influxdata.LogLevel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class InfluxDBServiceTest {


    InfluxDBService influxDBService;

    @Before
    public void setUp() throws Exception {
        influxDBService = InfluxDBService.getInstance(false);
        influxDBService.getPlatformClient().setLogLevel(LogLevel.BODY);
    }

    @BeforeClass
    public static void beforeClass() {


    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getMeasurementTagKeys() {

        String[] sensors = influxDBService.getMeasurementTagValues(influxDBService.getBucket(),"sensor", "sid");

        Assert.assertTrue(Arrays.asList(sensors).contains("sensor1"));
        Assert.assertTrue(Arrays.asList(sensors).contains("sensor2"));
        Assert.assertTrue(Arrays.asList(sensors).contains("sensor3"));


    }
}
package io.bonitoo.demo.device;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.influxdata.client.domain.WritePrecision;
import org.influxdata.client.write.Point;

import com.pi4j.io.i2c.I2CFactory;
import i2c.sensor.BME280;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BME280Device extends Device {

    Logger log = LoggerFactory.getLogger(BME280Device.class);

    private BME280 bme280;

    BME280Device() {
        super();

        try {
            bme280 = new BME280();
        } catch (I2CFactory.UnsupportedBusNumberException e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Device device = new BME280Device();
        do {
            Thread.sleep(10000);
        } while (!device.isRunning());


    }


    @Override
    List<Point> getMetrics() {

        if (bme280 == null) {
            return new ArrayList<>();
        }

        Point p = Point.measurement("sensor");
        p.time(Instant.now(), WritePrecision.S);
        p.addTag("sid", getDeviceNumber());
        try {
            p.addField("pressure", bme280.readPressure());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            p.addField("temperature", bme280.readTemperature());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            p.addField("humidity", bme280.readHumidity());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Collections.singletonList(p);

    }
}

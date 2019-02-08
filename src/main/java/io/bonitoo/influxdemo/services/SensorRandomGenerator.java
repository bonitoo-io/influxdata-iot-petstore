package io.bonitoo.influxdemo.services;

import java.time.Instant;
import java.util.Random;

import io.bonitoo.influxdemo.entities.Sensor;

public class SensorRandomGenerator {

    static String[] sids = new String[]{"sensor1", "sensor2", "sensor3"};
    static String[] locations = new String[]{"Prague", "Přerov", "San Francisco", "London", "Brusel"};

    static Sensor getRandomData(Instant time, String sid, String location) {

        Sensor s = new Sensor();

        s.setTime(time);
        s.setSid(sid);
        s.setLocation(location);

        if ("Prague".equals(location)) {
            s.setTemperature(random(5, 30));
        } else if ("San Francisco".equals(location)) {
            s.setTemperature(random(10, 40));
        } else  {
            s.setTemperature(random(10,35));
        }


        s.setBatteryCapacity(random(0, 100));
        s.setHumidity(random(10, 70));


        return s;
    }

    private static double random(double rangeMin, double rangeMax) {
        Random r = new Random();
        return rangeMin + (rangeMax - rangeMin) * r.nextDouble();

    }

}

package io.bonitoo.influxdemo.services;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.WriteApi;
import org.influxdata.client.domain.WritePrecision;
import org.influxdata.client.write.Point;
import org.influxdata.client.write.events.WriteErrorEvent;
import org.influxdata.client.write.events.WriteRetriableErrorEvent;
import org.influxdata.client.write.events.WriteSuccessEvent;
import io.bonitoo.influxdemo.entities.Sensor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * This class demonstrates how to use InfluxDB WriteApi to periodically store JMX values.
 */
@Service
@EnableScheduling
public class DataGenerator {

    private static Logger log = LoggerFactory.getLogger(DataGenerator.class);
    private InfluxDBClient influxDBClient;
    private boolean running;
    Counter counter;

    @Autowired
    public DataGenerator(final InfluxDBClient influxDBClient, MeterRegistry meterRegistry) {

        this.influxDBClient = influxDBClient;
        counter = meterRegistry.counter("write.count");
        running = true;
    }

    public void startGenerator() {
        log.info("Starting data generator");
        running = true;
    }

    public void stopGenerator() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }


    @Scheduled(cron = "${jobs.dataGenerator.cronSchedule:-}")
    void writeMeasurements() {

        if (!running) {
            return;
        }

        try (WriteApi writeClient = influxDBClient.getWriteApi()) {

            counter.increment();

            writeClient.listenEvents(WriteErrorEvent.class, e -> {
                log.error("WriteErrorEvent error", e.getThrowable());
            });

            writeClient.listenEvents(WriteRetriableErrorEvent.class, e -> {
                log.error("WriteRetriableErrorEvent error", e.getThrowable());
            });
            writeClient.listenEvents(WriteSuccessEvent.class, e -> {
                log.debug("WriteSuccessEvent success {}", e.getLineProtocol());
            });

            log.debug("Write random data");
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Instant now = Instant.now();
            Arrays.stream(SensorRandomGenerator.sids).forEach(sid -> {
                Arrays.stream(SensorRandomGenerator.locations).forEach(loc -> {

                    Sensor randomData = SensorRandomGenerator.getRandomData(now, sid, loc);
                    //write data using sensor POJO class
                    writeClient.writeMeasurement(WritePrecision.MS, randomData);
                    log.debug("Writing: " + randomData);
                });
            });

            //write localhost JMX data using Point structure
            String hostName = null;
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ignore) {
            }

            log.info("Write JMX data");
            Point operatingSystemMXBeanPoint = buildPointFromBean(com.sun.management.OperatingSystemMXBean.class, ManagementFactory.getOperatingSystemMXBean(), "operatingSystemMXBean");
            operatingSystemMXBeanPoint.addTag("host", hostName);
            writeClient.writePoint(operatingSystemMXBeanPoint);

            Point runtimeMXBeanPoint = buildPointFromBean(RuntimeMXBean.class, ManagementFactory.getRuntimeMXBean(), "runtimeMXBean");
            runtimeMXBeanPoint.addTag("host", hostName);
            writeClient.writePoint(runtimeMXBeanPoint);

            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            Point p = Point.measurement("memoryMXBean")
                .addField("HeapMemoryUsage.used", memoryMXBean.getHeapMemoryUsage().getUsed())
                .addField("HeapMemoryUsage.max", memoryMXBean.getHeapMemoryUsage().getMax());
            writeClient.writePoint(p);
            stopWatch.stop();
            log.info("Write job finished in {}", stopWatch.getTime());
        }

    }

    /**
     * Creates Point structure for generic java bean using reflections. All numeric properties are added as fields.
     *
     * @param interfaceClass interface of bean
     * @param bean           bean
     * @param name           measurement name
     * @return new instance of {@link Point}
     */
    Point buildPointFromBean(Class interfaceClass, final Object bean, String name) {
        Point point = Point.measurement(name);

        Method[] declaredMethods = interfaceClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && method.getParameterCount() == 0
                && (method.getReturnType().isInstance(Number.class) || method.getReturnType().isPrimitive())) {
                try {
                    Object value = method.invoke(bean);
                    String fieldName = methodName.substring(3);
                    if (value instanceof Number && !Double.isNaN(((Number) value).doubleValue())) {
                        point.addField(fieldName, (Number) value);
                    }
                } catch (Exception e) {
                    log.error("error accessing bean: {}:{}", bean, method);
                    e.printStackTrace();
                }
            }
        }
        return point;
    }
}

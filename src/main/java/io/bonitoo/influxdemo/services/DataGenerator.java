package io.bonitoo.influxdemo.services;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.influxdata.client.WriteApi;
import org.influxdata.client.domain.WritePrecision;
import org.influxdata.client.write.Point;
import org.influxdata.client.write.events.WriteErrorEvent;
import org.influxdata.client.write.events.WriteRetriableErrorEvent;
import org.influxdata.client.write.events.WriteSuccessEvent;

import io.bonitoo.influxdemo.entities.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class demonstrates how to use InfluxDB WriteApi to periodically store JMX values.
 */
class DataGenerator {

    private static Logger log = LoggerFactory.getLogger(DataGenerator.class);
    private InfluxDBService influxDBService;
    private static boolean running;
    private static ScheduledExecutorService executor;

    DataGenerator(final InfluxDBService influxDBService) {

        this.influxDBService = influxDBService;
    }

    void startGenerator() {
        if (!running) {
            WriteApi writeClient = influxDBService.getPlatformClient().getWriteApi();
            writeClient.listenEvents(WriteErrorEvent.class, e -> {
                log.error("WriteErrorEvent error", e.getThrowable());
            });

            writeClient.listenEvents(WriteRetriableErrorEvent.class, e -> {
                log.error("WriteRetriableErrorEvent error", e.getThrowable());
            });
            writeClient.listenEvents(WriteSuccessEvent.class, e -> {
                log.debug("WriteSuccessEvent success {}", e.getLineProtocol());
            });

            String orgId = influxDBService.getOrgId();
            String bucket = influxDBService.getBucket();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Instant now = Instant.now();
                    Arrays.stream(SensorRandomGenerator.sids).forEach(sid -> {
                        Arrays.stream(SensorRandomGenerator.locations).forEach(loc -> {

                            Sensor randomData = SensorRandomGenerator.getRandomData(now, sid, loc);
                            //write data using sensor POJO class
                            writeClient.writeMeasurement(bucket, orgId, WritePrecision.MS, randomData);
                            log.debug("Writing: " + randomData);
                        });
                    });

                    //write localhost JMX data using Point structure
                    String hostName = null;
                    try {
                        hostName = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException ignore) {
                    }

                    Point operatingSystemMXBeanPoint = buildPointFromBean(ManagementFactory.getOperatingSystemMXBean(), "operatingSystemMXBean");
                    operatingSystemMXBeanPoint.addTag("host", hostName);
                    writeClient.writePoint(bucket, orgId, operatingSystemMXBeanPoint);

                    Point runtimeMXBeanPoint = buildPointFromBean(ManagementFactory.getRuntimeMXBean(), "runtimeMXBean");
                    runtimeMXBeanPoint.addTag("host", hostName);
                    writeClient.writePoint(bucket, orgId, runtimeMXBeanPoint);

                    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                    Point p = Point.measurement("memoryMXBean")
                        .addField("HeapMemoryUsage.used", memoryMXBean.getHeapMemoryUsage().getUsed())
                        .addField("HeapMemoryUsage.max", memoryMXBean.getHeapMemoryUsage().getMax());
                    writeClient.writePoint(bucket, orgId, p);
                }
            };

            executor = Executors.newScheduledThreadPool(10);

            long delay = 1000L;
            long period = 1000L;
            executor.scheduleAtFixedRate(timerTask, delay, period, TimeUnit.MILLISECONDS);
            running = true;
        }

    }

    /**
     * Creates Point structure for generic java bean using reflections. All numeric properties are added as fields.
     *
     * @param bean bean
     * @param name measurement name
     * @return new instance of {@link Point}
     */
    private Point buildPointFromBean(final Object bean, String name) {

        Point point = Point.measurement(name);

        Method[] declaredMethods = bean.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            method.setAccessible(true);
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

    void stopGenerator() {

        executor.shutdown();
        running = false;

    }

    boolean isRunning() {
        return running;
    }
}

package io.bonitoo.influxdemo.services;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.influxdata.java.client.InfluxDBClient;
import org.influxdata.java.client.InfluxDBClientFactory;
import org.influxdata.java.client.WriteApi;
import org.influxdata.java.client.writes.Point;
import org.influxdata.java.client.writes.events.BackpressureEvent;
import org.influxdata.java.client.writes.events.WriteErrorEvent;
import org.influxdata.java.client.writes.events.WriteSuccessEvent;

import io.bonitoo.influxdemo.entities.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfluxDBService {

    private static Logger log = LoggerFactory.getLogger(InfluxDBService.class);
    private static String DEMO_CONFIG_FILE = "demo-config.properties";

    private static InfluxDBService instance;
    private InfluxDBClient platformClient;
    private static ScheduledExecutorService executor;
    static private boolean running;

    private String orgId;
    private String bucket;

    public static synchronized InfluxDBService getInstance() {
        if (instance == null) {
            instance = new InfluxDBService();
        }
        return instance;
    }

    Properties p = new Properties();

    private InfluxDBService() {
        try {
            log.info("Loading {}", DEMO_CONFIG_FILE);
            p.load(InfluxDBService.class.getClassLoader().getResourceAsStream(DEMO_CONFIG_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + DEMO_CONFIG_FILE, e);
        }

        final char[] authToken = p.getProperty("influxdb.token").toCharArray();
        String url = p.getProperty("influxdb.baseUrl", "http://localhost:9999");
        orgId = p.getProperty("influxdb.org");
        bucket = p.getProperty("influxdb.bucket");
        platformClient = InfluxDBClientFactory.create(url, authToken);

        startWriteMetricsJob();
    }

    public InfluxDBClient getPlatformClient() {
        return platformClient;
    }

    public void startWriteMetricsJob() {
        log.info("Starting scheduler");

        if (!running) {
            WriteApi writeClient = platformClient.getWriteApi();
            writeClient.listenEvents(WriteErrorEvent.class, e -> {
                log.error("Write error", e);
            });

            writeClient.listenEvents(WriteSuccessEvent.class, e -> {
                log.debug("Write success", e);
            });

            writeClient.listenEvents(BackpressureEvent.class, e -> {
                log.warn("Backpressure event", e);
            });

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Instant now = Instant.now();
                    Arrays.stream(SensorRandomGenerator.sids).forEach(sid -> {
                        Arrays.stream(SensorRandomGenerator.locations).forEach(loc -> {

                            Sensor randomData = SensorRandomGenerator.getRandomData(now, sid, loc);
                            //write data using sensor POJO class
                            writeClient.writeMeasurement(bucket, getOrgId(), ChronoUnit.MILLIS,
                                randomData);
                            log.debug("Writing: " + randomData);
                        });
                    });

                    //write localhost JMX data using Point structure
                    writeClient.writePoint(bucket, getOrgId(), buildPointFromBean(ManagementFactory.getOperatingSystemMXBean(), "operatingSystemMXBean"));
                    writeClient.writePoint(bucket, getOrgId(), buildPointFromBean(ManagementFactory.getRuntimeMXBean(), "runtimeMXBean"));

                    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                    Point p = Point.measurement("memoryMXBean")
                        .addField("HeapMemoryUsage.used", memoryMXBean.getHeapMemoryUsage().getUsed())
                        .addField("HeapMemoryUsage.max", memoryMXBean.getHeapMemoryUsage().getMax());
                    writeClient.writePoint(bucket, getOrgId(), p);
                }
            };

            executor = Executors.newScheduledThreadPool(10);

            long delay = 1000L;
            long period = 1000L;
            executor.scheduleAtFixedRate(timerTask, delay, period, TimeUnit.MILLISECONDS);
            running = true;
        }
    }

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

    public void stopWriteMetricsJob() {
        running = false;
        log.info("Stopping scheduler.");
        executor.shutdown();
    }

    public boolean isRunningWrite() {
        return running;
    }

    public String getOrgId() {
        return orgId;
    }
}

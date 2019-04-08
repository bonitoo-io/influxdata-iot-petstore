package io.bonitoo.influxdemo.services;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.InfluxDBClientFactory;
import org.influxdata.client.WriteApi;
import org.influxdata.client.domain.WritePrecision;
import org.influxdata.client.write.Point;
import org.influxdata.client.write.events.BackpressureEvent;
import org.influxdata.client.write.events.WriteErrorEvent;
import org.influxdata.client.write.events.WriteRetriableErrorEvent;
import org.influxdata.client.write.events.WriteSuccessEvent;
import org.influxdata.query.FluxTable;

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

    public InfluxDBService() {
        this(true);
    }

    public static synchronized InfluxDBService getInstance() {
        if (instance == null) {
            instance = new InfluxDBService();
        }
        return instance;
    }

    public static synchronized InfluxDBService getInstance(boolean startJob) {
        if (instance == null) {
            instance = new InfluxDBService(startJob);
        }
        return instance;
    }

    Properties p = new Properties();

    private InfluxDBService(final boolean startJob) {
        try {
            log.info("Loading {}", DEMO_CONFIG_FILE);
            p.load(InfluxDBService.class.getClassLoader().getResourceAsStream(DEMO_CONFIG_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + DEMO_CONFIG_FILE, e);
        }

        final char[] authToken = p.getProperty("influxdb.token").toCharArray();
        String url = p.getProperty("influxdb.baseUrl", "http://localhost:9999");
        orgId = p.getProperty("influxdb.orgId");
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
                log.error("WriteErrorEvent error", e);
            });


            writeClient.listenEvents(WriteRetriableErrorEvent.class, e -> {
                log.error("WriteRetriableErrorEvent error", e);
            });
            writeClient.listenEvents(WriteSuccessEvent.class, e -> {
                log.debug("WriteSuccessEvent success", e);
            });

            writeClient.listenEvents(BackpressureEvent.class, e -> {
                log.warn("BackpressureEvent event", e);
            });

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Instant now = Instant.now();
                    Arrays.stream(SensorRandomGenerator.sids).forEach(sid -> {
                        Arrays.stream(SensorRandomGenerator.locations).forEach(loc -> {

                            Sensor randomData = SensorRandomGenerator.getRandomData(now, sid, loc);
                            //write data using sensor POJO class
                            writeClient.writeMeasurement(bucket, getOrgId(), WritePrecision.MS, randomData);
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

    public String getBucket() {
        return bucket;
    }

    /**
     * Gets all existing tags values for given measurement
     *
     * @param measurement measurement name
     * @param tag         tag name
     * @return tag values
     */
    public String[] getMeasurementTagValues(String bucket, String measurement, String tag) {

        String q = "import \"influxdata/influxdb/v1\"\n" +
            "\n" +
            "v1.measurementTagValues(\n" +
            "  bucket: \"" + bucket + "\",\n" +
            "  measurement: \"" + measurement + "\",\n" +
            "  tag: \"" + tag + "\"\n" +
            ")";

        System.out.println(q);

        return queryStringValues(q);

    }

    /**
     * Gets all existing tags values for given tags
     *
     * @param tags
     * @param start     start
     * @param predicate predicate flux function
     * @return tag values
     */
    public String[] getTagValues(String bucket, Set<String> tags, String start, String predicate) {

        if (tags == null) {
            return null;
        }


        ArrayList<String> ret = new ArrayList<>();

        for (String tag : tags) {
            String q = "import \"influxdata/influxdb/v1\"\n" +
                "\n" +
                "v1.tagValues(\n" +
                "  bucket: \"" + bucket + "\",\n" +
                "  tag: \"" + tag + "\",\n";

            if (predicate != null) {
                q += "  predicate: " + predicate + ",\n";
            }

            q += "  start: " + start + "\n" + ")";

            System.out.println(q);

            ret.addAll(Arrays.asList(queryStringValues(q)));

        }

        return ret.toArray(new String[0]);


    }

    /**
     * Gets all existing tags
     *
     * @param start     start
     * @param predicate predicate flux function
     * @return tag values
     */
    public String[] getTagKeys(String bucket, String start, String predicate) {

        String q = "import \"influxdata/influxdb/v1\"\n" +
            "\n" +
            "v1.tagKeys(\n" +
            "  bucket: \"" + bucket + "\",\n";

        if (predicate != null) {
            q += "  predicate: " + predicate + ",\n";
        }
        q += "  start: " + start + "\n" + ")";

        System.out.println(q);
        return queryStringValues(q);

    }


    /**
     * Gets the array of available measurements
     *
     * @return all available measurement names in the bucket
     */
    public String[] getMeasurements(String bucket) {

        String q =
            "import \"influxdata/influxdb/v1\"\n" +

                "v1.measurements(bucket: \"" + bucket + "\")";

        return queryStringValues(q);

    }

    private String[] queryStringValues(final String q) {
        List<FluxTable> result = InfluxDBService.getInstance().getPlatformClient().getQueryApi().query(q, orgId);

        if (result.isEmpty()) {
            return new String[0];
        }

        return result.get(0).getRecords().stream().map(rec -> (String) rec.getValue()).toArray(String[]::new);
    }

    public String[] getFields(final String bucket, final Set<String> selectedMeasurements) {

        final StringBuffer q = new StringBuffer();
        q.append("from(bucket: \"").append(bucket).append("\")\n")
            .append(
                "  |> range(start: -5m, stop: now())\n");

        //filter by measurement
        q.append(createOrFilter("_measurement", selectedMeasurements));
        q.append("  |> filter(fn: (r) => true)\n")
            .append(
                "  |> group(columns: [\"_field\"])\n")
            .append(
                "  |> distinct(column: \"_field\")\n")
            .append(
                "  |> keep(columns: [\"_value\"])\n")
            .append(
                "  |> limit(n: 200)\n").append(
            "  |> sort()");

        System.out.println(q);

        return queryStringValues(q.toString());
    }

    public String createOrFilter(final String measurement_, final Set<String> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("  |> filter(fn: (r) => ");

        for (Iterator<String> iterator = selectedItems.iterator(); iterator.hasNext(); ) {
            String item = iterator.next();
            sb.append("r.").append(measurement_).append(" == ").append("\"").append(item).append("\"");
            if (iterator.hasNext()) {
                sb.append(" or ");
            }
        }

        sb.append(" ) \n");
        return sb.toString();
    }


    public String[] getBuckets() {
        String q = "buckets()\n" +
            "  |> rename(columns: {\"name\": \"_value\"})\n" +
            "  |> keep(columns: [\"_value\"])";
        return queryStringValues(q);
    }
}

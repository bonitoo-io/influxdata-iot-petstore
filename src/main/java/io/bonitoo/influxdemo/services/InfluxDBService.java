package io.bonitoo.influxdemo.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.InfluxDBClientFactory;
import org.influxdata.query.FluxTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfluxDBService {

    private static Logger log = LoggerFactory.getLogger(InfluxDBService.class);
    private static String DEMO_CONFIG_FILE = "demo-config.properties";

    private static InfluxDBService instance;
    private InfluxDBClient platformClient;
    static private boolean running;

    private String orgId;
    private String bucket;
    private DataGenerator dataGenerator;

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

    private InfluxDBService() {
        this(true);
    }

    Properties p = new Properties();

    public InfluxDBService(final boolean startJob) {

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
        this.dataGenerator = new DataGenerator(this);

        startGenerator();
    }

    public InfluxDBClient getPlatformClient() {
        return platformClient;
    }

    public void startGenerator() {
        dataGenerator.startGenerator();
    }

    public void stopGenerator() {
        dataGenerator.stopGenerator();
    }

    public boolean isGeneratorRunning() {
        return dataGenerator.isRunning();
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

        String q = "from(bucket: \"" + bucket + "\")\n" +
            "  |> range(start: -5m, stop: now())\n" +
            createOrFilter("_measurement", selectedMeasurements) +
            "  |> filter(fn: (r) => true)\n"
            + "  |> group(columns: [\"_field\"])\n"
            + "  |> distinct(column: \"_field\")\n"
            + "  |> keep(columns: [\"_value\"])\n"
            + "  |> limit(n: 200)\n"
            + "  |> sort()";

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

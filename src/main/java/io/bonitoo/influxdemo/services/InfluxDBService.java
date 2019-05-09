package io.bonitoo.influxdemo.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.query.FluxTable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties ("spring.influx2")
public class InfluxDBService {

    private InfluxDBClient influxDBClient;

    @Autowired
    public InfluxDBService(final InfluxDBClient influxDBClient) {

        this.influxDBClient = influxDBClient;
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
        List<FluxTable> result = influxDBClient.getQueryApi().query(q);

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

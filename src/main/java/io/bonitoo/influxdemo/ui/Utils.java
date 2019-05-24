package io.bonitoo.influxdemo.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.influxdata.query.FluxRecord;

import com.vaadin.flow.component.grid.Grid;

public class Utils {

    static String rangeValue(String rangeLabel) {
        return rangeLabel.replace("Past ", "-");
    }

    static Stream<String> getTimeRangeList() {
        String[] ret = new String[]{
            "Past 5m", "Past 30m", "Past 1h", "Past 6h", "Past 12h", "Past 24h", "Past 2d", "Past 7d", "Past 30d"};
        return Arrays.stream(ret);
    }

    public static Grid<FluxRecord> createGrid(final List<FluxRecord> records) {
        Grid<FluxRecord> grid = new Grid<>(FluxRecord.class);
        grid.setSizeFull();
        grid.getColumnByKey("values").setVisible(false);
        grid.getColumns().forEach(c -> c.setResizable(true));

        grid.setItems(records);
        if (records.size() > 0) {
            FluxRecord fluxRecord = records.get(0);
            Map<String, Object> values = fluxRecord.getValues();

            values.keySet().forEach(key -> {

                if (grid.getColumnByKey(key) == null) {
                    if (!key.startsWith("_") && !"result".equals(key)) {
                        //add columns with tags
                        grid.addColumn(record -> record.getValueByKey(key)).setKey(key).setHeader(key);
                    }
                }
            });
        }

        return grid;
    }

    public static String agregateWindow(final String rangeStart) {
        String unit = rangeStart.substring(rangeStart.length()-1);

        if (rangeStart.endsWith("m")) {
            return null;
        }

        if (rangeStart.endsWith("h")) {
            return "30s";
        }

        if (rangeStart.endsWith("d")) {
            return "30m";
        }

        return "1h";
        //return (val/100f)+unit;

    }
}

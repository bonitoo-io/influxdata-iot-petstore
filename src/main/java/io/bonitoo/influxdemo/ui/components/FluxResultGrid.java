package io.bonitoo.influxdemo.ui.components;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxRecord;
import org.influxdata.query.FluxTable;
import io.bonitoo.influxdemo.ui.DashboardView;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

//@com.vaadin.flow.component.Tag (value = "flux-result-grid")

public class FluxResultGrid extends Composite<Div> {

    private Grid<FluxRecord> grid;
    private Chart chart;

    private Checkbox displaySwitchCheckbox;
    private int selectedTab = 0;
    private VerticalLayout layout;

    private InfluxDBClient influxDBClient;

    public FluxResultGrid(InfluxDBClient influxDBClient) {


        layout = new VerticalLayout();

        this.influxDBClient = influxDBClient;

        displaySwitchCheckbox = new Checkbox();

        displaySwitchCheckbox.setLabel("Show as table: ");
        layout.add(displaySwitchCheckbox);
        layout.setWidthFull();
        getContent().setWidthFull();
        getContent().add(layout);
    }

    public FluxResultGrid query(String fluxQuery) {

        QueryApi queryApi = influxDBClient.getQueryApi();

        List<FluxTable> fluxResult = queryApi.query(fluxQuery);

        Tabs tabs = new Tabs();
        layout.add(tabs);

        for (int i = 0; i < fluxResult.size(); i++) {
            Tab tab = new Tab();
            tab.setLabel("" + i);

            if (i == 0) {
                addResultComponent(fluxResult);
            }
            tabs.add(tab);
        }

        tabs.addSelectedChangeListener(event -> {
            selectedTab = tabs.getSelectedIndex();
            addResultComponent(Collections.singletonList(fluxResult.get(selectedTab)));

        });

        displaySwitchCheckbox.addValueChangeListener(e -> {
            addResultComponent(Collections.singletonList(fluxResult.get(selectedTab)));
        });

        return this;

    }

    private void addResultComponent(final List<FluxTable> fluxResult) {

        if (grid != null) {
            layout.remove(grid);
        }
        if (chart != null) {
            layout.remove(chart);
        }

        if (displaySwitchCheckbox.getValue()) {
            grid = createGrid(fluxResult.get(0));
            layout.add(grid);
        } else {
            chart = createChart(fluxResult.get(0));
            layout.add(chart);
        }
    }


    private static Grid<FluxRecord> createGrid(FluxTable... fluxTables) {
        List<FluxRecord> records = Arrays.stream(fluxTables)
            .flatMap(fluxTable -> fluxTable.getRecords().stream()).collect(Collectors.toList());

        return createGrid(records);
    }

    private static Grid<FluxRecord> createGrid(List<FluxRecord> records) {
        Grid<FluxRecord> grid = new Grid<>(FluxRecord.class);
        grid.setHeight("800px");
        grid.setItems(records);

        if (records.size() > 0) {
            FluxRecord fluxRecord = records.get(0);
            Map<String, Object> values = fluxRecord.getValues();

            values.keySet().forEach(key -> {

                if (grid.getColumnByKey(key) == null) {
                    grid.addColumn(record -> record.getValueByKey(key)).setKey(key).setHeader(key);
                }
            });
        }

        grid.getColumnByKey("values").setVisible(false);
        grid.getColumns().forEach(c -> c.setResizable(true));

        return grid;
    }

    private static Chart createChart(FluxTable... tables) {

        Chart chart = new Chart();
        chart.setHeight("800px");
        final Configuration configuration = chart.getConfiguration();
        chart.setTimeline(true);
        configuration.getChart().setType(ChartType.AREA);

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);

        configuration.getTooltip().setEnabled(true);
        configuration.getLegend().setEnabled(true);

        for (FluxTable fluxTable : tables) {
            DataSeries dataSeries = new DataSeries();
            dataSeries.setName(DashboardView.getSeriesNameShort(fluxTable));
            configuration.addSeries(dataSeries);
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                if (fluxRecord.getTime() != null) {
                    Object value = fluxRecord.getValue();
                    if (value instanceof Number) {
                        DataSeriesItem item = new DataSeriesItem(fluxRecord.getTime(), (Number) value);
                        dataSeries.add(item);
                    }
                }
            }
        }
        return chart;
    }

}

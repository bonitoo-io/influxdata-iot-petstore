package io.bonitoo.influxdemo.ui.components;

import java.util.List;
import java.util.Map;

import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxRecord;
import org.influxdata.query.FluxTable;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.springframework.beans.factory.annotation.Autowired;

public class FluxResultGrid extends VerticalLayout {


    private Grid<FluxRecord> grid = new Grid<>();
    private Chart chart;

    private InfluxDBService influxDBService;


    @Autowired
    public FluxResultGrid(InfluxDBService influxDBService, String query) {
        this.influxDBService = influxDBService;
        setSizeFull();
        fetchData(query);
    }

    public void fetchData (String fluxQuery) {

        QueryApi queryApi = influxDBService.getPlatformClient().getQueryApi();

        List<FluxTable> fluxResult = queryApi.query(fluxQuery, influxDBService.getOrgId());

        Tabs tabs = new Tabs();
        add(tabs);

//        Div pages = new Div();

        for (int i = 0; i < fluxResult.size(); i++) {
            Tab tab = new Tab();
            tab.setLabel("" + i);

            if (i == 0) {
                grid = createGrid(fluxResult.get(0).getRecords());
                add(grid);

            }
            tabs.add(tab);
        }

        add(grid);

        tabs.addSelectedChangeListener(event -> {
            int selectedIndex = tabs.getSelectedIndex();

            if (grid != null) {
                remove(grid);
            }

            grid = createGrid(fluxResult.get(selectedIndex).getRecords());
            add(grid);

        });

    }


    public static Grid<FluxRecord> createGrid(final List<FluxRecord> records) {
        Grid<FluxRecord> grid = new Grid<>(FluxRecord.class);
        grid.setSizeFull();
//        grid.setHeight("800px");
//        grid.getColumnByKey("values").setVisible(false);
//        grid.getColumns().forEach(c -> c.setResizable(true));

        grid.setItems(records);

        if (records.size() > 0) {
            FluxRecord fluxRecord = records.get(0);
            Map<String, Object> values = fluxRecord.getValues();

            values.keySet().forEach(key -> {

                if (grid.getColumnByKey(key) == null) {
//                    if (!key.startsWith("_") && !"result".equals(key)) {
                    //add columns with tags
                    grid.addColumn(record -> record.getValueByKey(key)).setKey(key).setHeader(key);
//                    }
                }
            });
        }

        return grid;
    }

}

package io.bonitoo.influxdemo.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.influxdb.spring.influx.InfluxDB2Properties;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.PlotOptionsArea;
import com.vaadin.flow.component.charts.model.Stacking;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.gatanaso.MultiselectComboBox;


@Route(value = "Browse", layout = MainLayout.class)
@PageTitle(value = "BrowseData")
public class BrowseDataView extends HorizontalLayout {

    public static final String VIEW_NAME = "Browse";

    private static Logger log = LoggerFactory.getLogger(ExecuteFluxView.class);

    public enum DisplayType {
        GRID,
        CHART,
        CHART_STACKED
    }

    //display data as GRID by default
    private DisplayType displayType = DisplayType.CHART;

    private TextArea fluxTextArea;
    private TextField statusLabel;
    private ComboBox<String> bucketBox;
    private ComboBox<String> timeRangeBox;
    private MultiselectComboBox<String> measurementsCombo;
    private MultiselectComboBox<String> fieldsBox;
    private MultiselectComboBox<String> tagsKeysBox;
    private MultiselectComboBox<String> tagsValuesBox;

    private ComboBox<DisplayType> displayDataType;

    private Grid<FluxRecord> grid;
    private ProgressBar progressBar;

    private String selectedBucket;
    private Set<String> selectedMeasurements = new TreeSet<>();
    private Set<String> selectedTags = new TreeSet<>();
    private String query = "";

    private final InfluxDBService influxDBService;
    private final InfluxDBClient influxDBClient;
    private final InfluxDB2Properties properties;

    @Autowired
    public BrowseDataView(InfluxDBService influxDBService,
                          InfluxDBClient influxDBClient,
                          InfluxDB2Properties properties) {

        this.influxDBService = influxDBService;
        this.influxDBClient = influxDBClient;
        this.properties = properties;

        setSizeFull();

        SplitLayout splitLayout = new SplitLayout();
        add(splitLayout);
        splitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        splitLayout.setWidth("100%");

        VerticalLayout contentLayout = new VerticalLayout();
        splitLayout.addToPrimary(contentLayout);
        contentLayout.add(new H3("Browse data"));

        VerticalLayout filterLayout = new VerticalLayout();
        filterLayout.add(new Label("Filter"));
        splitLayout.addToSecondary(filterLayout);
        filterLayout.setSizeFull();

        splitLayout.setSecondaryStyle("minWidth", "200px");
        splitLayout.setSecondaryStyle("maxWidth", "350px");

        selectedBucket = this.properties.getBucket();

        bucketBox = new ComboBox<>();
        bucketBox.setWidth("100%");
        bucketBox.setItems(this.influxDBService.getBuckets());
        bucketBox.setLabel("Bucket");
        bucketBox.setValue(this.properties.getBucket());
        bucketBox.addValueChangeListener(e -> {
            selectedBucket = e.getValue();
            measurementsCombo.setItems(this.influxDBService.getMeasurements(selectedBucket));
        });

        filterLayout.add(bucketBox);

        timeRangeBox = new ComboBox<>();
        timeRangeBox.setLabel("Time range");
        timeRangeBox.setWidth("100%");
        timeRangeBox.setItems(Utils.getTimeRangeList());
        Utils.getTimeRangeList().findFirst().ifPresent(timeRangeBox::setValue);
        filterLayout.add(timeRangeBox);

        measurementsCombo = new MultiselectComboBox<>();
        measurementsCombo.setLabel("Measurements");
        measurementsCombo.setItems(this.influxDBService.getMeasurements(selectedBucket));

        filterLayout.add(measurementsCombo);

        fieldsBox = new MultiselectComboBox<>();
        fieldsBox.setLabel("Fields");
        fieldsBox.setItems(influxDBService.getFields(selectedBucket, selectedMeasurements));
        filterLayout.add(fieldsBox);

        measurementsCombo.addValueChangeListener(event -> {
            selectedMeasurements = event.getValue();
            fieldsBox.setItems(this.influxDBService.getFields(selectedBucket, selectedMeasurements));
            tagsKeysBox.setItems(this.influxDBService.getTagKeys(selectedBucket, Utils.rangeValue(timeRangeBox.getValue()), null));

        });


        tagsKeysBox = new MultiselectComboBox<>();
        tagsValuesBox = new MultiselectComboBox<>();

        tagsKeysBox.setLabel("Tags");
        tagsKeysBox.setItems(this.influxDBService.getTagKeys(selectedBucket, Utils.rangeValue(timeRangeBox.getValue()), null));
        tagsKeysBox.addValueChangeListener(event -> {
            selectedTags = event.getValue();
            tagsValuesBox.setItems(this.influxDBService.getTagValues(selectedBucket, tagsKeysBox.getSelectedItems(),
                Utils.rangeValue(timeRangeBox.getValue()), null));
        });

        filterLayout.add(tagsKeysBox);

        tagsValuesBox.setLabel("Tags values");
        filterLayout.add(tagsValuesBox);

        fluxTextArea = new TextArea();
        fluxTextArea.setValue(query);
        fluxTextArea.setWidth("100%");
        fluxTextArea.getStyle().set("overflow", "auto");
        fluxTextArea.setHeight("25%");

        statusLabel = new TextField();
        statusLabel.setWidth("100%");
        statusLabel.setEnabled(false);
        statusLabel.addThemeVariants(TextFieldVariant.LUMO_SMALL);

//        contentLayout.add(statusLabel);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setWidth("100%");
        contentLayout.add(hl);

        Button executeFlux = new Button("Execute Flux");
        executeFlux.setWidth("200px");
        executeFlux.addThemeVariants(ButtonVariant.LUMO_SMALL);

        VerticalLayout resultLayout = new VerticalLayout();

        executeFlux.addClickListener(event ->
        {
            query = createFluxQuery();
            fluxTextArea.setValue(query);
            executeFlux(resultLayout);
        });

        displayDataType = new ComboBox<>("Display as", DisplayType.values());
        displayDataType.setValue(displayType);
        displayDataType.setWidth("100%");
        filterLayout.add(displayDataType);
        displayDataType.addValueChangeListener(event -> {
            displayType = event.getValue();
            executeFlux(resultLayout);
        });

        filterLayout.add(executeFlux);
        filterLayout.add(fluxTextArea);

        progressBar = new ProgressBar();
//        contentLayout.add(progressBar);
        resultLayout.setSizeFull();
        contentLayout.add(resultLayout);

        executeFlux(resultLayout);
    }

    private void executeFlux(VerticalLayout contentLayout) {

        contentLayout.removeAll();

        query = createFluxQuery();

        UI.getCurrent().setPollInterval(1000);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<FluxRecord> records = new ArrayList<>();

        QueryApi queryClient = influxDBClient.getQueryApi();

        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        statusLabel.setValue("Executing...");

        log.info("Executing queryInfluxDB: " + query);

        UI current = UI.getCurrent();

        if (displayType == DisplayType.GRID) {
            queryClient.query(query,

                (cancellable, record) -> {
                    records.add(record);
                },
                (error) -> {
                    handleError(current, error, statusLabel, progressBar);
                },
                () -> {
                    //on complete
                    log.info("Query completed.");
                    current.accessSynchronously(() -> {
                        contentLayout.add(Utils.createGrid(records));
                        notifyComplete(stopWatch, current, statusLabel, progressBar);
                    });
                });
        }
        if (displayType == DisplayType.CHART || displayType == DisplayType.CHART_STACKED) {
            List<FluxTable> result = queryClient.query(this.query);
            addToChart(contentLayout, result, displayType);
            notifyComplete(stopWatch, current, statusLabel, progressBar);
        }
    }

    static void handleError(final UI current, final Throwable error, TextField statusLabel, ProgressBar progressBar) {
        log.error(error.getMessage(), error);
        current.accessSynchronously(() -> {
            Notification notification = new Notification(
                error.getMessage(), 3000);
            notification.setPosition(Notification.Position.TOP_CENTER);
            notification.open();
            statusLabel.setValue(error.getMessage());
            statusLabel.setInvalid(true);
            progressBar.setIndeterminate(false);
            current.setPollInterval(-1);
        });
    }

    static void notifyComplete(final StopWatch stopWatch, final UI current, TextField statusLabel, ProgressBar progressBar) {
        stopWatch.stop();
        statusLabel.setValue("Query completed. " + stopWatch.getTime());
        statusLabel.setInvalid(false);
        progressBar.setIndeterminate(false);
        current.setPollInterval(-1);
    }

    private void addToChart(final VerticalLayout contentLayout, final List<FluxTable> tables, DisplayType displayType) {
        final Chart chart = new Chart();
        chart.setHeightFull();
        final Configuration configuration = chart.getConfiguration();
        chart.setTimeline(true);
        configuration.getChart().setType(ChartType.AREA);

        if (displayType == DisplayType.CHART_STACKED) {
            PlotOptionsArea plotOptions = new PlotOptionsArea();
            plotOptions.setStacking(Stacking.NORMAL);
            configuration.setPlotOptions(plotOptions);
        }

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);

        configuration.getTooltip().setEnabled(true);
        configuration.getLegend().setEnabled(true);

        for (FluxTable fluxTable : tables) {
            DataSeries dataSeries = new DataSeries();
            if (displayType == DisplayType.CHART_STACKED) {
                PlotOptionsArea plotOptions = new PlotOptionsArea();
                plotOptions.setStacking(Stacking.NORMAL);
                dataSeries.setConfiguration(new Configuration());
                dataSeries.getConfiguration().setPlotOptions(plotOptions);
            }

            dataSeries.setName(DashboardView.getSeriesName(fluxTable));

            configuration.addSeries(dataSeries);
            List<FluxRecord> records = fluxTable.getRecords();
            records.stream()
                .filter(record -> record.getTime() != null && record.getValue() instanceof Number)
                .forEach(r -> dataSeries.add(new DataSeriesItem(r.getTime(), (Number) r.getValue())));
        }

        contentLayout.add(chart);
    }

    private String createFluxQuery() {

        StringBuilder q = new StringBuilder("from(bucket: \"" + selectedBucket + "\") \n");

        timeRangeBox.getOptionalValue().ifPresent(timeRange -> {
            q.append("  |> range (start: ").append(Utils.rangeValue(timeRange)).append(",stop: now() )\n");
        });

        q.append(influxDBService.createOrFilter("_measurement ", measurementsCombo.getSelectedItems()));
        q.append(influxDBService.createOrFilter("_field ", fieldsBox.getSelectedItems()));

        Set<String> selectedKeys = tagsKeysBox.getSelectedItems();

        for (String next : selectedKeys) {
            q.append(influxDBService.createOrFilter(next, tagsValuesBox.getSelectedItems()));
        }

        return q.toString();
    }

}
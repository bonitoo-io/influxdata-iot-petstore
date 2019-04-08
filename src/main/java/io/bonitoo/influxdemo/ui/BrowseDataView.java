package io.bonitoo.influxdemo.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxRecord;
import org.influxdata.query.FluxTable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.router.Route;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.gatanaso.MultiselectComboBox;


@Route(value = "Browse", layout = MainLayout.class)
public class BrowseDataView extends HorizontalLayout {

    public static final String VIEW_NAME = "Browse";

    private static Logger log = LoggerFactory.getLogger(ExecuteFluxView.class);

    enum DisplayTypeEnum {
        //        raw,
        grid,
        chart
    }

    //display data as grid by default
    private DisplayTypeEnum displayType = DisplayTypeEnum.chart;

    private TextArea fluxTextArea;
    private TextField statusLabel;
    private ComboBox<String> bucketBox;
    private ComboBox<String> timeRangeBox;
    private MultiselectComboBox<String> measurementsCombo;
    private MultiselectComboBox<String> fieldsBox;
    MultiselectComboBox<String> tagsKeysBox;
    MultiselectComboBox<String> tagsValuesBox;

    private ComboBox<DisplayTypeEnum> displayDataType;

    private Grid<FluxRecord> grid;
    private ProgressBar progressBar;

    private String selectedBucket;
    private Set<String> selectedMeasurements = new TreeSet<>();
    private Set<String> selectedTags = new TreeSet<>();
    private String query = "";

    private InfluxDBService influxDBService = InfluxDBService.getInstance();

    public BrowseDataView() {

        setSizeFull();

        SplitLayout splitLayout = new SplitLayout();
        add(splitLayout);
        splitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        splitLayout.setWidth("100%");

        VerticalLayout contentLayout = new VerticalLayout();
        splitLayout.addToPrimary(contentLayout);

        VerticalLayout filterLayout = new VerticalLayout();
        filterLayout.add(new Label("Filter"));
        splitLayout.addToSecondary(filterLayout);
        filterLayout.setSizeFull();

        splitLayout.setSecondaryStyle("minWidth", "200px");
        splitLayout.setSecondaryStyle("maxWidth", "350px");

        selectedBucket = influxDBService.getBucket();

        bucketBox = new ComboBox<>();
        bucketBox.setItems(influxDBService.getBuckets());
        bucketBox.setLabel("Bucket");
        bucketBox.setValue(influxDBService.getBucket());
        bucketBox.addValueChangeListener(e -> {
            selectedBucket = e.getValue();
            measurementsCombo.setItems(influxDBService.getMeasurements(selectedBucket));
        });

        filterLayout.add(bucketBox);

        timeRangeBox = new ComboBox<>();
        timeRangeBox.setLabel("Time range");
        timeRangeBox.setItems(getTimeRangeList());
        getTimeRangeList().findFirst().ifPresent(timeRangeBox::setValue);
        filterLayout.add(timeRangeBox);

        measurementsCombo = new MultiselectComboBox<>();
        measurementsCombo.setLabel("Measurements");
        measurementsCombo.setItems(influxDBService.getMeasurements(selectedBucket));

        filterLayout.add(measurementsCombo);

        fieldsBox = new MultiselectComboBox<>();
        fieldsBox.setLabel("Fields");
        fieldsBox.setItems(InfluxDBService.getInstance().getFields(selectedBucket, selectedMeasurements));
        filterLayout.add(fieldsBox);

        measurementsCombo.addValueChangeListener(event -> {
            selectedMeasurements = event.getValue();
            fieldsBox.setItems(influxDBService.getFields(selectedBucket, selectedMeasurements));
            tagsKeysBox.setItems(influxDBService.getTagKeys(selectedBucket, rangeValue(timeRangeBox.getValue()), null));

        });


        tagsKeysBox = new MultiselectComboBox<>();
        tagsValuesBox = new MultiselectComboBox<>();

        tagsKeysBox.setLabel("Tags");
        tagsKeysBox.setItems(influxDBService.getTagKeys(selectedBucket, rangeValue(timeRangeBox.getValue()), null));
        tagsKeysBox.addValueChangeListener(event -> {
            selectedTags = event.getValue();
            tagsValuesBox.setItems(influxDBService.getTagValues(selectedBucket, tagsKeysBox.getSelectedItems(),
                rangeValue(timeRangeBox.getValue()), null));
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

        contentLayout.add(statusLabel);

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

        displayDataType = new ComboBox<>("Display as", DisplayTypeEnum.values());
        displayDataType.setValue(DisplayTypeEnum.grid);
        filterLayout.add(displayDataType);
        displayDataType.addValueChangeListener(event -> {
            displayType = event.getValue();
            executeFlux(resultLayout);
        });

        filterLayout.add(executeFlux);
        filterLayout.add(fluxTextArea);

        progressBar = new ProgressBar();

        contentLayout.add(progressBar);
        resultLayout.setSizeFull();
        contentLayout.add(resultLayout);

        executeFlux(resultLayout);
    }

    private Stream<String> getTimeRangeList() {

        String[] ret = new String[]{
            "Past 5m", "Past 30m", "Past 1h", "Past 6h", "Past 12h", "Past 24h", "Past 2d", "Past 7d", "Past 30d"};

        return Arrays.stream(ret);

    }

    private void executeFlux(VerticalLayout contentLayout) {

        contentLayout.removeAll();

        if (displayType == DisplayTypeEnum.grid) {
            grid = new Grid<>(FluxRecord.class);
            grid.setSizeFull();
            grid.getColumnByKey("values").setVisible(false);
            grid.getColumns().forEach(c -> c.setResizable(true));
            contentLayout.add(grid);
        }

        query = createFluxQuery();


        UI.getCurrent().setPollInterval(1000);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<FluxRecord> records = new ArrayList<>();

        QueryApi queryClient =
            InfluxDBService.getInstance().getPlatformClient().getQueryApi();

        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        statusLabel.setValue("Executing...");

        log.info("Executing queryInfluxDB: " + query);

        UI current = UI.getCurrent();

        if (displayType == DisplayTypeEnum.grid) {
            queryClient.query(query, InfluxDBService.getInstance().getOrgId(),

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
                        addToGrid(grid, records);
                        notifyComplete(stopWatch, current);
                    });
                });
        }
        if (displayType == DisplayTypeEnum.chart) {
            List<FluxTable> result = queryClient.query(this.query, InfluxDBService.getInstance().getOrgId());
            addToChart(contentLayout, result);
            notifyComplete(stopWatch, current);

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
//                    progressBar.setVisible(false);
            current.setPollInterval(-1);
        });
    }

    private void notifyComplete(final StopWatch stopWatch, final UI current) {
        stopWatch.stop();
        statusLabel.setValue("Query completed. " + stopWatch.getTime());
        statusLabel.setInvalid(false);
        progressBar.setIndeterminate(false);
        current.setPollInterval(-1);
    }

    private void addToChart(final VerticalLayout contentLayout, final List<FluxTable> tables) {
        final Chart chart = new Chart();
        final Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);
        configuration.getTitle().setText("chart");

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);

        configuration.getTooltip().setEnabled(true);
        configuration.getLegend().setEnabled(true);

        for (FluxTable fluxTable : tables) {
            DataSeries dataSeries = new DataSeries();
            dataSeries.setName(DashboardView.getSeriesName(fluxTable));

            configuration.addSeries(dataSeries);
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                if (fluxRecord.getTime() != null) {
                    dataSeries.add(new DataSeriesItem(fluxRecord.getTime(), (Number) fluxRecord.getValue()));
                }
            }
        }

        contentLayout.add(chart);
    }

    static void addToGrid(final Grid<FluxRecord> grid, final List<FluxRecord> records) {
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
    }

    private String createFluxQuery() {

        StringBuilder q = new StringBuilder("from(bucket: \"" + selectedBucket + "\") \n");

        timeRangeBox.getOptionalValue().ifPresent(timeRange -> {
            q.append("  |> range (start: ").append(rangeValue(timeRange)).append(",stop: now() )\n");
        });

        q.append(influxDBService.createOrFilter("_measurement ", measurementsCombo.getSelectedItems()));
        q.append(influxDBService.createOrFilter("_field ", fieldsBox.getSelectedItems()));

        Set<String> selectedKeys = tagsKeysBox.getSelectedItems();

        for (String next : selectedKeys) {
            q.append(influxDBService.createOrFilter(next, tagsValuesBox.getSelectedItems()));
        }

        return q.toString();
    }

    private String rangeValue(String rangeLabel) {
        return rangeLabel.replace("Past ", "-");
    }

}
package io.bonitoo.influxdemo.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxColumn;
import org.influxdata.query.FluxRecord;
import org.influxdata.query.FluxTable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisTitle;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Labels;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.shared.Registration;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = DashboardView.VIEW_NAME, layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle(value = "Dashboard")

public class DashboardView extends VerticalLayout {

    private static Logger log = LoggerFactory.getLogger(DashboardView.class);

    public static final String VIEW_NAME = "Dashboard";
    private static String[] rangeStartValues = new String[]{"-1m", "-3m", "-5m", "-15m", "-30m", "-1h", "-5h"};
    private static String[] windowAggregates = new String[]{"4s", "3s", "20s", "30s", "1m", "2m", "10m"};

    private final Chart chartTemperature;
    private final FluxChartSettings chartHumiditySettings;
    private final Chart chartHumidity;
    private final FluxChartSettings osBeanChartSettings;
    private final FluxChartSettings memBeanChartSettings;
    private final Chart osBeanChart;
    private final Chart memBeanChart;
    private TextField statusLabel;
    private ComboBox<String> rangeCombo;
    private FluxChartSettings chartTemperatureSettings;


    public DashboardView() {

        setClassName("dashboard");
        setSizeFull();

        InfluxDBService influxDBService = InfluxDBService.getInstance();

        statusLabel = new TextField();
        statusLabel.setWidth("90%");
        statusLabel.setEnabled(false);
        statusLabel.setValue(influxDBService.isGeneratorRunning() ? "Running" : "Not running.");
        statusLabel.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        add(statusLabel);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setWidth("100%");
        add(hl);

        Button writeButton = new Button(influxDBService.isGeneratorRunning() ? "Stop write" : "Start write");
        writeButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        writeButton.addClickListener(event ->
        {
            if (influxDBService.isGeneratorRunning()) {
                influxDBService.stopGenerator();
            } else {
                influxDBService.startGenerator();
            }
            statusLabel.setValue(influxDBService.isGeneratorRunning() ? "Running" : "Not running.");
            writeButton.setText(influxDBService.isGeneratorRunning() ? "Stop write" : "Start write");
        });

        rangeCombo = new ComboBox<>("Range start", rangeStartValues);
        rangeCombo.setRequired(true);
        final String rangeStart = "-5m";
        rangeCombo.setValue(rangeStart);


        add(writeButton);
        add(rangeCombo);

        String bucketName = InfluxDBService.getInstance().getBucket();
        chartTemperatureSettings = new FluxChartSettings("Temperature", bucketName, "sensor",
            new String[]{"temperature"},
            new TagStructure[]{new TagStructure("location", "Prague", "San Francisco"),
                new TagStructure("sid", "sensor1", "sensor2")},
            null, 40, ChartType.SPLINE);

        chartTemperature = createChart(chartTemperatureSettings);


        chartTemperature.setWidth("80%");
        chartTemperature.setHeight("400px");

        chartHumiditySettings = new FluxChartSettings("Humidity", bucketName, "sensor",
            new String[]{"humidity"},
            new TagStructure[]{
                new TagStructure("location", "Prague", "San Francisco"),
                new TagStructure("sid", "sensor1", "sensor2")},
            null, 80, ChartType.SPLINE);

        chartHumidity = createChart(chartHumiditySettings);


        chartHumidity.setWidth("80%");
        chartHumidity.setHeight("400px");


        osBeanChartSettings = new FluxChartSettings("CPU", bucketName, "operatingSystemMXBean",
            new String[]{"SystemCpuLoad", "ProcessCpuLoad"}, null, null, 0, ChartType.SPLINE);
        osBeanChart = createChart(osBeanChartSettings);
        osBeanChart.setWidth("80%");
        osBeanChart.setHeight("400px");

        memBeanChartSettings = new FluxChartSettings("Memory", bucketName, "memoryMXBean",
            new String[]{"HeapMemoryUsage.max", "HeapMemoryUsage.used"}, null, null, 0, ChartType.AREA);
        memBeanChart = createChart(memBeanChartSettings);
        memBeanChart.setHeight("400px");
        memBeanChart.setWidth("80%");


        add(osBeanChart, memBeanChart, chartTemperature, chartHumidity);


    }

    private Registration registration;

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getUI().ifPresent(ui -> {
            ui.setPollInterval(2000);

            registration = ui.addPollListener(l -> {
                log.debug("pooling....");
                refreshChart(chartHumidity, chartHumiditySettings);
                refreshChart(chartTemperature, chartTemperatureSettings);
                refreshChart(osBeanChart, osBeanChartSettings);
                refreshChart(memBeanChart, memBeanChartSettings);
            });

            ui.addDetachListener(l -> {
                ui.setPollInterval(-1);
                registration.remove();
            });
        });
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        UI ui = getUI().get();
        ui.setPollInterval(-1);
        registration.remove();
    }

    private Chart createChart(FluxChartSettings fs) {

        QueryApi queryClient = InfluxDBService.getInstance().getPlatformClient().getQueryApi();

        final Chart chart = new Chart();

        final Configuration configuration = chart.getConfiguration();
        if (fs.chartType == null) {
            fs.chartType = ChartType.SPLINE;

        }
        configuration.getChart().setType(fs.chartType);

        configuration.getTitle().setText(fs.label);

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle(fs.label));
        yAxis.setMin(0);
        if (fs.yMax != null && fs.yMax.doubleValue() > 0) {
            yAxis.setMax(fs.yMax);
        }
        if (fs.format != null) {
            Labels labels = new Labels();
            labels.setFormatter(fs.format);
            yAxis.setLabels(labels);
        }

        configuration.getTooltip().setEnabled(true);
        configuration.getLegend().setEnabled(true);
        final String fluxQueryBase = constructQuery(fs.bucket, rangeCombo.getValue(), fs.measurement, fs.filterFields, fs.filterTags);
        List<FluxTable> tables = queryInfluxDB(fluxQueryBase);

        for (FluxTable fluxTable : tables) {
            DataSeries dataSeries = new DataSeries();
            dataSeries.setName(getSeriesName(fluxTable));
            configuration.addSeries(dataSeries);
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                if (fluxRecord.getTime() != null) {
                    dataSeries.add(new DataSeriesItem(fluxRecord.getTime(), (Number) fluxRecord.getValue()));
                }
            }
        }
        return chart;
    }

    private List<FluxTable> queryInfluxDB(String query) {

        QueryApi queryClient = InfluxDBService.getInstance().getPlatformClient().getQueryApi();

        try {
            return queryClient.query(query, InfluxDBService.getInstance().getOrgId());
        } catch (Exception e) {

            Notification notification = new Notification(
                e.getMessage(), 3000);
            notification.setPosition(Notification.Position.TOP_CENTER);
            notification.open();
            return new ArrayList<>();
        }

    }

    private void refreshChart(Chart chart, FluxChartSettings fs) {
        String query = constructQuery(fs.bucket, rangeCombo.getValue(),
            fs.measurement, fs.filterFields, fs.filterTags);

        List<FluxTable> fluxTables = queryInfluxDB(query);

        for (int i = 0; i < fluxTables.size(); i++) {
            FluxTable fluxTable = fluxTables.get(i);

            if (chart.getConfiguration().getSeries().size() == 0) {
                break;
            }

            DataSeries series = (DataSeries) chart.getConfiguration().getSeries().get(i);
            series.clear();

            fluxTable.getRecords().forEach(record -> {
                DataSeriesItem item = new DataSeriesItem(record.getTime(), (Number) record.getValue());
                series.add(item, false, false);
            });
        }
        chart.drawChart();
    }

    /**
     * Construct series name from flux table
     *
     * @param fluxTable flux table result
     * @return series display name
     */
    public static String getSeriesName(final FluxTable fluxTable) {

        List<FluxColumn> groupKey = fluxTable.getGroupKey();
        StringBuilder sb = new StringBuilder();
        for (FluxColumn fluxColumn : groupKey) {
            if (!fluxColumn.isGroup()
                || "_start".equals(fluxColumn.getLabel())
                || "_stop".equals(fluxColumn.getLabel())) {
                continue;
            }
            if (fluxTable.getRecords().size() > 0) {

                FluxRecord first = fluxTable.getRecords().get(0);
                if (fluxColumn.getLabel().startsWith("_")) {
                    sb.append(first.getValueByIndex(fluxColumn.getIndex())).append(" ");
                } else {
                    sb.append(fluxColumn.getLabel());
                    sb.append(": ")
                        .append(first.getValueByIndex(fluxColumn.getIndex()))
                        .append(" ");
                }
            }
        }
        return sb.toString();
    }


    public static String constructQuery(final String bucket, String rangeStart,
                                        final String measurement, final String[] filterFields, TagStructure[] filterTags) {
        StringBuffer fluxQueryBase =
            new StringBuffer("from(bucket: \"" + bucket + "\")\n").append(
                "  |> range(start: " + rangeStart + ")\n").append(
                "  |> filter(fn: (r) => r._measurement == \"" + measurement + "\")\n");

        if (filterFields != null && filterFields.length > 0) {
            fluxQueryBase.append("  |> filter(fn: (r) => ");

            for (int i = 0; i < filterFields.length; i++) {
                fluxQueryBase.append("r._field == \"" + filterFields[i] + "\"");
                if (i < filterFields.length - 1) {
                    fluxQueryBase.append(" or ");
                }
            }
            fluxQueryBase.append(")\n");
        }

        if (filterTags != null && filterTags.length > 0) {
            Arrays.stream(filterTags).forEach(tagStructure -> {
                fluxQueryBase.append("  |> filter(fn: (r) => ");
                String[] values = tagStructure.values;
                for (int i = 0; i < values.length; i++) {
                    fluxQueryBase.append("r." + tagStructure.tag + " == \"" + values[i] + "\"");
                    if (i < values.length - 1) {
                        fluxQueryBase.append(" or ");
                    }
                }
                fluxQueryBase.append(")");
            });
        }

        int index = Arrays.asList(rangeStartValues).indexOf(rangeStart);
        String windowAggregate = windowAggregates[index];
        fluxQueryBase.append("   |> aggregateWindow(every: " + windowAggregate + ", fn:mean)");
        log.debug(fluxQueryBase.toString());

        return fluxQueryBase.toString();
    }

}

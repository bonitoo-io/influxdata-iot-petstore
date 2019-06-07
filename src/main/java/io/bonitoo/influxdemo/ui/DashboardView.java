package io.bonitoo.influxdemo.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxColumn;
import org.influxdata.query.FluxRecord;
import org.influxdata.query.FluxTable;
import org.influxdata.spring.influx.InfluxDB2Properties;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.shared.Registration;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.domain.DeviceInfo;
import io.bonitoo.influxdemo.services.DeviceRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = DashboardView.VIEW_NAME, layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle(value = "Dashboard")
public class DashboardView extends VerticalLayout {

    private static Logger log = LoggerFactory.getLogger(DashboardView.class);

    public static final String VIEW_NAME = "Dashboard";

    private FluxChartSettings chartTemperatureSettings;
    private FluxChartSettings chartHumiditySettings;
    private FluxChartSettings chartPressureSettings;

    private Chart chartTemperature;
    private Chart chartHumidity;
    private Chart chartPressure;

    private ComboBox<String> rangeCombo;

    private final InfluxDBClient influxDBClient;
    private DeviceRegistryService deviceRegistryService;

    @Autowired
    public DashboardView(final InfluxDBClient influxDBClient,
                         final InfluxDB2Properties properties,
                         final DeviceRegistryService registryService) {
        this.deviceRegistryService = registryService;
        this.influxDBClient = influxDBClient;


        List<String> deviceNumbers = registryService.getDeviceInfos().stream()
            .filter(DeviceInfo::isAuthorized)
            .map(DeviceInfo::getDeviceId)
            .collect(Collectors.toList());

        String bucketName = properties.getBucket();

        if (deviceNumbers.isEmpty()) {

            add(new Label("No authorized devices found. "));
            return;
        }

        setClassName("dashboard");
        setSizeFull();

        add(new H3(VIEW_NAME));

        rangeCombo = new ComboBox<String>("Range start");
        rangeCombo.setItems(Utils.getTimeRangeList());
        rangeCombo.setRequired(true);
        rangeCombo.setAllowCustomValue(false);
        Utils.getTimeRangeList().findFirst().ifPresent(rangeCombo::setValue);

        add(rangeCombo);


        chartTemperatureSettings = new FluxChartSettings("Temperature °C", bucketName, "air",
            new String[]{"temperature"},
            new TagStructure[]{
                new TagStructure("device_id", deviceNumbers.toArray(new String[0]))},
            null, 40, ChartType.SPLINE);

        chartTemperature = createChart(chartTemperatureSettings);


//        chartTemperature.setWidth("80%");
        chartTemperature.setHeight("300px");
        chartTemperature.setWidthFull();

        chartHumiditySettings = new FluxChartSettings("Humidity %", bucketName, "air",
            new String[]{"humidity"},
            new TagStructure[]{
                new TagStructure("device_id", deviceNumbers.toArray(new String[0]))},
            null, 0, ChartType.SPLINE);

        chartHumidity = createChart(chartHumiditySettings);
        chartHumidity.setHeight("300px");
        chartHumidity.setWidthFull();


        chartPressureSettings = new FluxChartSettings("Pressure hPa", bucketName, "air",
            new String[]{"pressure"},
            new TagStructure[]{
                new TagStructure("device_id", deviceNumbers.toArray(new String[0]))},
            null, 0, ChartType.SPLINE);
        chartPressure = createChart(chartPressureSettings);

        chartPressure.setHeight("300px");
        chartPressure.setWidthFull();

        add(chartTemperature, chartHumidity, chartPressure);

    }

    private Registration registration;

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getUI().ifPresent(ui -> {
            ui.setPollInterval(2000);


            registration = ui.addPollListener(l -> {
                log.debug("pooling....");
                if (chartHumiditySettings != null) {
                    refreshChart(chartHumidity, chartHumiditySettings);
                    refreshChart(chartTemperature, chartTemperatureSettings);
                    refreshChart(chartPressure, chartPressureSettings);
                }
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
        getUI().ifPresent(ui -> ui.setPollInterval(-1));
        registration.remove();
    }

    private Chart createChart(FluxChartSettings fs) {

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
//        yAxis.setMin(0);
//        if (fs.yMax != null && fs.yMax.doubleValue() > 0) {
//            yAxis.setMax(fs.yMax);
//        }
        if (fs.format != null) {
            Labels labels = new Labels();
            labels.setFormatter(fs.format);
            yAxis.setLabels(labels);
        }

        configuration.getTooltip().setEnabled(true);
        configuration.getLegend().setEnabled(true);
        final String fluxQueryBase = constructQuery(fs.bucket, Utils.rangeValue(rangeCombo.getValue()), fs.measurement, fs.filterFields, fs.filterTags);
        List<FluxTable> tables = queryInfluxDB(fluxQueryBase);

        for (FluxTable fluxTable : tables) {
            DataSeries dataSeries = new DataSeries();
            dataSeries.setName(getSeriesNameDevice(fluxTable));
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

        QueryApi queryClient = influxDBClient.getQueryApi();

        try {
            return queryClient.query(query);
        } catch (Exception e) {

            Notification notification = new Notification(
                e.getMessage(), 3000);
            notification.setPosition(Notification.Position.TOP_CENTER);
            notification.open();
            return new ArrayList<>();
        }

    }

    private void refreshChart(Chart chart, FluxChartSettings fs) {
        String query = constructQuery(fs.bucket, Utils.rangeValue(rangeCombo.getValue()),
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

    public String getSeriesNameDevice(final FluxTable fluxTable) {
        FluxRecord fluxRecord = fluxTable.getRecords().get(0);
        return deviceRegistryService.getDeviceName(Objects.requireNonNull(fluxRecord.getValueByKey("device_id")).toString());
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

    /**
     * Construct series name from flux table
     *
     * @param fluxTable flux table result
     * @return series display name
     */
    public static String getSeriesNameShort(final FluxTable fluxTable) {
        return fluxTable.getGroupKey().stream().filter(
            fluxColumn -> fluxColumn.isGroup()
                && !"_start".equals(fluxColumn.getLabel())
                && !"_stop".equals(fluxColumn.getLabel()))
            .map(fluxColumn -> fluxColumn.getLabel() + "=" + fluxTable.getRecords().get(0).getValueByIndex(fluxColumn.getIndex()))
            .collect(Collectors.joining(", "));
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


        String windowAggregate = Utils.agregateWindow(rangeStart);
        if (windowAggregate != null) {
            fluxQueryBase.append("   |> aggregateWindow(every: " + windowAggregate + ", fn:mean)");
        }
        log.debug(fluxQueryBase.toString());

        return fluxQueryBase.toString();
    }
}

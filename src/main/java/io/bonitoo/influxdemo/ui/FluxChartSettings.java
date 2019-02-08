package io.bonitoo.influxdemo.ui;

import com.vaadin.flow.component.charts.model.ChartType;
import io.bonitoo.influxdemo.ui.TagStructure;

public class FluxChartSettings {

    public String label;
    public String bucket;
    public String measurement;
    public String[] filterFields;
    public TagStructure[] filterTags;
    public String format;
    public Number yMax;
    public ChartType chartType;

    public FluxChartSettings(final String label, final String bucket, final String measurement, final String[] filterFields, final TagStructure[] filterTags, final String format, final Number yMax, final ChartType chartType) {
        this.label = label;
        this.bucket = bucket;
        this.measurement = measurement;
        this.filterFields = filterFields;
        this.filterTags = filterTags;
        this.format = format;
        this.yMax = yMax;
        this.chartType = chartType;
    }


}

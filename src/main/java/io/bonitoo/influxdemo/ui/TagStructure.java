package io.bonitoo.influxdemo.ui;

public class TagStructure {
    String tag;
    String[] values;
    public TagStructure(final String tag, final String... values) {
        this.tag = tag;
        this.values = values;
    }
}
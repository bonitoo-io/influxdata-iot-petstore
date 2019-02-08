package io.bonitoo.influxdemo.customer;

import io.bonitoo.influxdemo.ui.DashboardView;
import io.bonitoo.influxdemo.ui.TagStructure;
import org.junit.jupiter.api.Test;

class DashboardViewTest {

    @Test
    void constructQuery() {

        String q1 = DashboardView.constructQuery("my-bucket", "-1m", "sensor",
            new String[]{"temperature"},
            new TagStructure[]{
                new TagStructure("location", new String[]{"Prague", "San Francisco"})}
        );

        System.out.println(q1);


    }
}
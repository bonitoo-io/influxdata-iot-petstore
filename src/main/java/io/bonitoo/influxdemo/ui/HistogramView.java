package io.bonitoo.influxdemo.ui;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.bonitoo.influxdemo.MainLayout;

@Route(value = "Histogram", layout = MainLayout.class)
@PageTitle(value = "Histogram")
public class HistogramView extends VerticalLayout {

    public HistogramView() {

        add(new H3("TODO"));
    }
}

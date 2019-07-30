package io.bonitoo.influxdemo.ui;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.Check;
import io.bonitoo.influxdemo.MainLayout;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Version;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "About", layout = MainLayout.class)
@PageTitle("About")
public class AboutView extends VerticalLayout {

    public static final String VIEW_NAME = "About";

    public AboutView(@Autowired InfluxDBClient influxDBClient) {
        add(VaadinIcon.INFO_CIRCLE.create());
        add(new Span(" This application is using Vaadin Flow "
            + Version.getFullVersion() + "."));

        setSizeFull();

        Check health = influxDBClient.health();

        add(new Span("InfluxDB health: " + health.getMessage()));

        Span healthStatus = new Span("InfluxDB health status: ");
        add(healthStatus);

        Span status = new Span(health.getStatus().getValue());
        status.addClassName("influxdb-status-" + health.getStatus());
        healthStatus.add(status);

        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
    }
}

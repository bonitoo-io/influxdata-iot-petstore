package io.bonitoo.influxdemo.ui;

import org.influxdata.client.domain.Check;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Version;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "About", layout = MainLayout.class)
@PageTitle("About")
public class AboutView extends VerticalLayout {

    public static final String VIEW_NAME = "About";

    public AboutView(@Autowired InfluxDBService influxDBService) {
        add(VaadinIcon.INFO_CIRCLE.create());
        add(new Span(" This application is using Vaadin Flow "
            + Version.getFullVersion() + "."));

        setSizeFull();

        Check health = influxDBService.getPlatformClient().health();

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

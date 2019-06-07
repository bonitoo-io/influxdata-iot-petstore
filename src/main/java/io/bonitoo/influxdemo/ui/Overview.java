package io.bonitoo.influxdemo.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.authentication.LoginScreen;

@Route(value = "Overview", layout = MainLayout.class)

public class Overview extends VerticalLayout {

    public static final String VIEW_NAME = "Docs";

    public Overview() {

        setClassName("mainMenuContent");

        add(new H1("IoT Petstore demo use cases"));


        add(new H3("A. Collection of Data"));
        {
            UnorderedList ul = new UnorderedList();
            add(ul);
            ul.add(addMenuItemLink("Data generator","DataGenerator.java","https://github.com/rhajek/influx-demo/blob/master/src/main/java/io/bonitoo/influxdemo/services/DataGenerator.java"));
            ul.add(addMenuItem("Import of data from external sources such as csv"));
            ul.add(addMenuItem("~Aggregator from devices~"));
        }

        add(new H3("B. IoT OnboardingResponse"));
        {
            UnorderedList ul = new UnorderedList();
            add(ul);
            ul.add(addMenuItemLink("How to write metrics using Java","influxdb-client-java", "https://github.com/bonitoo-io/influxdb-client-java#writes-and-queries-in-influxdb-20"));
            ul.add(addMenuItemLink("How to write metrics using Python", "influxdb-python", "https://github.com/influxdata/influxdb-python"));
            ul.add(addMenuItem("telegraf on IoT device with special output plugin"));
            ul.add(addMenuItem("How to onboard/register new IoT device"));
        }


        add(new H3("C. Dashboard for IoT customers"));
        {
            UnorderedList ul = new UnorderedList();
            add(ul);

            ul.add(addMenuItem("User Authentication -- organization and bucket.", LoginScreen.class));
            ul.add(addMenuItem("Query / list of my devices."));
            ul.add(addMenuItem("Map query to chart objects in order to render chart. Per device.", DashboardView.class));
            ul.add(addMenuItem("Display historical data charts / paging", BrowseDataView.class));
            ul.add(addMenuItem("Register object for troggering the alerts (max temperature)."));
            ul.add(addMenuItem("My data export (as csv)"));

        }

        add(new H3("D. Analytic Queries"));
        {
            UnorderedList ul = new UnorderedList();
            add(ul);

            ul.add(addMenuItem("Histogram", HistogramView.class));
            ul.add(addMenuItem("Cohort analysis", CohortView.class));
            ul.add(addMenuItem("Pivot example in order to see correlation of one or more datasets"));
        }

    }

    private Component addMenuItemLink(final String s, final String text, final String href) {
        ListItem li = new ListItem(s);

        if (href != null) {
            li.add(new Span(" | "));
            Anchor routerLink = new Anchor(href, text);
            routerLink.setTarget("_blank");
            li.add(routerLink);
        }

        return li;
    }

    private ListItem addMenuItem(final String s) {

        return addMenuItem(s, null);
    }

    private ListItem addMenuItem(final String s, final Class component) {

        ListItem li = new ListItem(s);

        if (component != null) {
            Route annotation = (Route) component.getAnnotation(Route.class);
            PageTitle pageTitle = (PageTitle) component.getAnnotation(PageTitle.class);
            if (pageTitle != null && annotation != null) {
                li.add(new Span(" | "));
                RouterLink routerLink = new RouterLink(pageTitle.value(), component);
                li.add(routerLink);
            }
        }

        return li;
    }
}

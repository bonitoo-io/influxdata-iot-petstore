package io.bonitoo.influxdemo;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import io.bonitoo.influxdemo.ui.AboutView;
import io.bonitoo.influxdemo.ui.BrowseDataView;
import io.bonitoo.influxdemo.ui.DashboardView;
import io.bonitoo.influxdemo.ui.ExecuteFluxView;

/**
 * The layout of the pages e.g. About and Inventory.
 */
@HtmlImport("css/shared-styles.html")
@Theme(value = Lumo.class)
@PWA(name = "InfluxDB PetStore", shortName = "InfluxDB PetStore")
public class MainLayout extends FlexLayout implements RouterLayout {
    private Menu menu;

    public MainLayout() {
        setSizeFull();
        setClassName("main-layout");
        menu = new Menu();
        menu.addView(DashboardView.class, DashboardView.VIEW_NAME, VaadinIcon.DASHBOARD.create());
        menu.addView(ExecuteFluxView.class, ExecuteFluxView.VIEW_NAME, VaadinIcon.EDIT.create());
        menu.addView(BrowseDataView.class, BrowseDataView.VIEW_NAME, VaadinIcon.SEARCH.create());
        menu.addView(AboutView.class, AboutView.VIEW_NAME, VaadinIcon.INFO_CIRCLE.create());
//        menu.addView(TestView.class, TestView.VIEW_NAME, VaadinIcon.AIRPLANE.create());
        add(menu);
    }
}

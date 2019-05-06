package io.bonitoo.influxdemo.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.services.InfluxDBService;
import io.bonitoo.influxdemo.ui.components.FluxResultGrid;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "FluxResultView", layout = MainLayout.class)
@PageTitle(value = "FluxResultView")
public class FluxResultView extends HorizontalLayout {

    private static Logger log = LoggerFactory.getLogger(FluxResultView.class);


    public static final String VIEW_NAME = "FluxResultView";

    private String appendLimit(String query, int limit, int offset) {
        String execQuery = query +
            "\n  |> limit(n: " + limit + ", offset: " + offset + ")";

        return execQuery;
    }

    private String appendLimit(String query, Query dataProviderQuery) {
        return appendLimit(query, dataProviderQuery.getLimit(), dataProviderQuery.getOffset());
    }

    private InfluxDBService influxDBService;

    private TextArea fluxTextArea;
    private TextField statusLabel;


    private FluxResultGrid grid;
    private ProgressBar progressBar;
    private String query;


    public FluxResultView(@Autowired InfluxDBService influxDBService) {

        this.influxDBService = influxDBService;
        query = "from(bucket: \"" + influxDBService.getBucket() + "\")\n" +
            "  |> range(start: -15m)\n" +
            "  |> filter(fn: (r) => r._measurement == \"sensor\")\n" +
            "  |> filter(fn: (r) => r._field == \"temperature\")\n";

        setSizeFull();
        VerticalLayout verticalLayout = new VerticalLayout();


        Button fluxButton = new Button("Execute Flux");
        fluxButton.setWidth("150px");
        fluxButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        fluxButton.addClickListener(event ->
        {
            //todo replace parameters
            query = fluxTextArea.getValue()
                .replace("timeRangeStart", "-30d");

            executeFlux(query, verticalLayout);
        });

//        verticalLayout.add(fluxButton);

        fluxTextArea = new TextArea();
        fluxTextArea.setValue(query);
        fluxTextArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        fluxTextArea.setWidth("100%");
        fluxTextArea.getStyle().set("overflow", "auto");
        fluxTextArea.addClassName("monospaced");
        fluxTextArea.addClassName("tiny");
        fluxTextArea.setHeight("100px");
        verticalLayout.add(fluxTextArea);

        statusLabel = new TextField();
        statusLabel.setWidth("100%");
        statusLabel.setEnabled(false);
        statusLabel.addThemeVariants(TextFieldVariant.LUMO_SMALL);

//        verticalLayout.add(statusLabel);

//        HorizontalLayout hl = new HorizontalLayout();
//        hl.setWidth("100%");
//        verticalLayout.add(hl);


        progressBar = new ProgressBar();
        verticalLayout.add(progressBar);
        add(verticalLayout);

        HorizontalLayout hl = new HorizontalLayout(fluxButton, statusLabel);
        hl.setWidthFull();
        verticalLayout.add(hl);

        grid = new FluxResultGrid(influxDBService, query);
        verticalLayout.add();

        executeFlux(query, verticalLayout);
    }

    private void executeFlux(String query, VerticalLayout verticalLayout) {

        UI.getCurrent().setPollInterval(1000);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();


        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        statusLabel.setValue("Executing...");

        log.info("Executing queryInfluxDB: " + query);

        UI current = UI.getCurrent();

        current.access( () -> {

            if (grid != null) {
                verticalLayout.remove(grid);
            }

            grid = new FluxResultGrid(influxDBService, query);
            verticalLayout.add(grid);
            BrowseDataView.notifyComplete(stopWatch, current, statusLabel, progressBar);
        });

    }

}

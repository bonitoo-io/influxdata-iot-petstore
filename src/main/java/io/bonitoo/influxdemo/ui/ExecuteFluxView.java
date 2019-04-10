package io.bonitoo.influxdemo.ui;

import java.util.ArrayList;
import java.util.List;

import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxRecord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "Execute", layout = MainLayout.class)
@PageTitle(value = "Execute Flux")
public class ExecuteFluxView extends HorizontalLayout {

    public static final String VIEW_NAME = "Execute Flux";

    private static Logger log = LoggerFactory.getLogger(ExecuteFluxView.class);

    private TextArea fluxTextArea;
    private TextField statusLabel;

    private String query = "from(bucket: \"" + InfluxDBService.getInstance().getBucket() + "\")\n" +
        "  |> range(start: -1d)\n" +
        "  |> filter(fn: (r) => r._measurement == \"sensor\")\n" +
        "  |> filter(fn: (r) => r._field == \"temperature\")\n" +
        "  |> limit(n: 10, offset: 1)";


    private Grid<FluxRecord> grid;
    private ProgressBar progressBar;


    public ExecuteFluxView() {
        setSizeFull();
        VerticalLayout verticalLayout = new VerticalLayout();
        fluxTextArea = new TextArea();
        fluxTextArea.setValue(query);
        fluxTextArea.setWidth("100%");
        fluxTextArea.getStyle().set("overflow", "auto");
        fluxTextArea.setHeight("25%");
        verticalLayout.add(fluxTextArea);

        statusLabel = new TextField();
        statusLabel.setWidth("100%");
        statusLabel.setEnabled(false);
        statusLabel.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        verticalLayout.add(statusLabel);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setWidth("100%");
        verticalLayout.add(hl);

        Button fluxButton = new Button("Execute Flux");
        fluxButton.setWidth("200px");
        fluxButton.addThemeVariants(ButtonVariant.LUMO_LARGE);
        fluxButton.addClickListener(event ->
        {
            //todo replace parameters
            query = fluxTextArea.getValue()
                .replace("timeRangeStart", "-30d");
            executeFlux(query, verticalLayout);
        });

        hl.add(fluxButton);

        progressBar = new ProgressBar();
        verticalLayout.add(progressBar);
        add(verticalLayout);

        executeFlux(query, verticalLayout);
    }

    private void executeFlux(String query, VerticalLayout verticalLayout) {

        UI.getCurrent().setPollInterval(1000);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<FluxRecord> records = new ArrayList<>();

        QueryApi queryClient =
            InfluxDBService.getInstance().getPlatformClient().getQueryApi();

        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        statusLabel.setValue("Executing...");

        log.info("Executing queryInfluxDB: " + query);

        UI current = UI.getCurrent();

        queryClient.query(query, InfluxDBService.getInstance().getOrgId(),

            (cancellable, record) -> {
                records.add(record);
            },

            (error) -> {
                BrowseDataView.handleError(current, error, statusLabel, progressBar);
            },

            () -> {
                //on complete
                log.info("Query completed.");
                current.accessSynchronously(() -> {

                    if (grid != null) {
                        verticalLayout.remove(grid);
                    }

                    grid = BrowseDataView.createGrid(records);
                    verticalLayout.add(grid);
                    BrowseDataView.notifyComplete(stopWatch, current, statusLabel, progressBar);
                });

            });
    }
}

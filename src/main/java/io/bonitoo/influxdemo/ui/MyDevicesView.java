package io.bonitoo.influxdemo.ui;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import io.bonitoo.influxdemo.MainLayout;
import io.bonitoo.influxdemo.services.DeviceRegistryService;
import io.bonitoo.influxdemo.services.domain.DeviceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "my-devices", layout = MainLayout.class)
@PageTitle(value = "My Devices")

public class MyDevicesView extends VerticalLayout {

    public static final String VIEW_NAME = "My Devices";

    private static Logger log = LoggerFactory.getLogger(MyDevicesView.class);

    private Registration registration;
    private DeviceRegistryService deviceRegistryService;

    Grid<DeviceInfo> deviceGrid;


    @Autowired
    public MyDevicesView(DeviceRegistryService deviceRegistryService) {
        this.deviceRegistryService = deviceRegistryService;
        setSizeFull();

        VerticalLayout verticalLayout = new VerticalLayout();

        H3 h3 = new H3("My Devices");
        verticalLayout.add(h3);


        ComponentRenderer<Span, DeviceInfo> cr = new ComponentRenderer<>(deviceInfo -> {
            Span span = new Span();
            span.setText(deviceInfo.deviceNumber);
            span.addClassName("monospace");
            return span;
        });

        ComponentRenderer<Icon, DeviceInfo> authIconColumn = new ComponentRenderer<>(deviceInfo -> {

            if (deviceInfo.authorized) {
                Icon icon = new Icon(VaadinIcon.CHECK);
                icon.setColor("00FF00");
                return icon;
            } else {
                return new Icon(VaadinIcon.QUESTION_CIRCLE_O);
            }
        });

        ComponentRenderer<Div, DeviceInfo> authRenderer = new ComponentRenderer<>(deviceInfo -> {

            Div div = new Div();
            {
                Div span = new Div();
                span.setText(deviceInfo.authId);
                span.addClassName("monospace");
                div.add(span);

            }
            {
                Div span = new Div();
                span.setText(deviceInfo.authToken);
                span.addClassName("tiny");
                span.addClassName("monospace");
                div.add(span);
            }
            return div;
        });


        deviceGrid = new Grid<>();
        deviceGrid.addColumn(cr).setHeader("Device Number").setWidth("20%");
        deviceGrid.addColumn(authIconColumn).setHeader("Authorized").setWidth("10%");
        deviceGrid.addColumn(authRenderer).setHeader("Auth Info").setWidth("50%");
        deviceGrid.addColumn(new ComponentRenderer<>(d -> {
            HorizontalLayout buttons = new HorizontalLayout();
            if (!d.authorized) {
                // button for saving the name to backend
                Button authorize = new Button("Authorize", event -> {

                    deviceRegistryService.authorizeDevice(d.deviceNumber);
                    Notification.show("Device "+d.deviceNumber + " was authorized. ");
                    deviceGrid.getDataProvider().refreshItem(d);
                });
                authorize.addThemeVariants(ButtonVariant.LUMO_SMALL);
                buttons.add(authorize);
            }

            if (d.authorized) {
                // button for saving the name to backend
                Button remove = new Button("Remove", event -> {
                    d.authorized = true;
                    deviceRegistryService.removeDeviceInfo(d.deviceNumber);
                    deviceGrid.getDataProvider().refreshItem(d);

                    Notification.show("Device "+d.deviceNumber + " was removed. ");
                });
                remove.addThemeVariants(ButtonVariant.LUMO_SMALL);
                remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
                buttons.add(remove);
            }
            // layouts for placing the text field on top of the buttons
            return new VerticalLayout(buttons);

        })).setHeader("Actions");


        verticalLayout.add(deviceGrid);

        add(verticalLayout);

    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getUI().ifPresent(ui -> {
            ui.setPollInterval(2000);

            registration = ui.addPollListener(l -> {
                log.debug("pooling....");
                deviceGrid.setItems(deviceRegistryService.getDeviceInfos());
            });

            ui.addDetachListener(l -> {
                ui.setPollInterval(-1);
                registration.remove();
            });
        });
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        getUI().ifPresent(ui -> ui.setPollInterval(-1));
        registration.remove();
    }

}

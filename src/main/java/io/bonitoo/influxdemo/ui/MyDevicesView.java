package io.bonitoo.influxdemo.ui;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
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


        ComponentRenderer<Span, DeviceInfo> deviceNumberColumn = new ComponentRenderer<>(deviceInfo -> {
            Span span = new Span();
            span.setText(deviceInfo.getDeviceNumber());
            span.addClassName("monospace");
            span.addClassName("tiny");
            return span;
        });

        ComponentRenderer<Icon, DeviceInfo> authIconColumn = new ComponentRenderer<>(deviceInfo -> {

            if (deviceInfo.isAuthorized()) {
                Icon icon = new Icon(VaadinIcon.CHECK);
                icon.setColor("00FF00");
                return icon;
            } else {
                return new Icon(VaadinIcon.QUESTION_CIRCLE_O);
            }
        });

        ComponentRenderer<Component, DeviceInfo> lastSeen = new ComponentRenderer<>(deviceInfo -> {

            Instant lastSeen1 = deviceRegistryService.getLastSeen(deviceInfo.getDeviceNumber());

            DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime( FormatStyle.MEDIUM )
                    .withLocale( Locale.UK )
                    .withZone( ZoneId.systemDefault() );


                Span span = new Span();
                if (lastSeen1 != null) {
                    span.setText(formatter.format(lastSeen1));
                } else {
                    span.setText(" - ");
                }
                span.addClassName("tiny");
                return span;
        });

        ComponentRenderer<Div, DeviceInfo> authRenderer = new ComponentRenderer<>(deviceInfo -> {

            Div div = new Div();
            {
                Span span = new Span();
                span.setText(deviceInfo.getAuthId());
                span.addClassName("monospace");
                span.addClassName("tiny");
                div.add(span);

            }
            /*
            {
                Div span = new Div();
                span.setText(deviceInfo.getAuthToken());
                span.addClassName("tiny");
                span.addClassName("monospace");
                div.add(span);
            }
             */
            return div;
        });


        deviceGrid = new Grid<>();

        DataProvider<DeviceInfo, Void> dataProvider = DataProvider.fromCallbacks(
            // First callback fetches items based on a query
            query -> {
                // The index of the first item to load
                int offset = query.getOffset();

                // The number of items to load
                int limit = query.getLimit();

                List<DeviceInfo> persons = deviceRegistryService
                    .getDeviceInfos(offset, limit);

                return persons.stream();
            },
            // Second callback fetches the number of items for a query
            query -> deviceRegistryService.getDeviceInfos().size());

        deviceGrid.setDataProvider(dataProvider);


        deviceGrid.addColumn(deviceNumberColumn).setHeader("Device Number").setWidth("15%");
        deviceGrid.addColumn(DeviceInfo::getName).setHeader("Device Name").setWidth("25%");
        deviceGrid.addColumn(authIconColumn).setHeader("Authorized").setWidth("10%");
        deviceGrid.addColumn(authRenderer).setHeader("Auth Info").setWidth("10%");
        deviceGrid.addColumn(lastSeen).setHeader("Last seen").setWidth("20%");
        deviceGrid.addColumn(new ComponentRenderer<>(d -> {
            HorizontalLayout buttons = new HorizontalLayout();
            if (!d.isAuthorized()) {
                // button for saving the name to backend
                Button authorize = new Button("Authorize", event -> {

                    FormLayout formLayout = new FormLayout();

                    Dialog dialog = new Dialog();
                    dialog.add(formLayout);
                    dialog.setCloseOnOutsideClick(true);
                    dialog.setCloseOnEsc(true);


                    TextField deviceNameField = new TextField("Enter device name: ");
                    deviceNameField.setRequired(true);
                    formLayout.addFormItem(deviceNameField, "");

                    HorizontalLayout actions = new HorizontalLayout();
                    dialog.add(actions);
                    Button cancel = new Button("Cancel", event1 -> {
                        dialog.close();
                    });
                    actions.add(cancel);
                    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);


                    Button confirm = new Button("Authorize Device", event1 -> {
                        if (deviceNameField.isEmpty()) {
                            deviceNameField.setInvalid(true);
                            deviceNameField.setErrorMessage("Device name is required!");
                            return;
                        }
                        deviceRegistryService.authorizeDevice(d.getDeviceNumber());
                        Notification.show("Device " + d.getDeviceNumber() + " was authorized. ");
                        d.setName(deviceNameField.getValue());
                        deviceGrid.getDataProvider().refreshItem(d);
                        dialog.close();
                    });
                    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

                    actions.add(confirm);

                    dialog.open();
                });
                authorize.addThemeVariants(ButtonVariant.LUMO_SMALL);
                buttons.add(authorize);
            }

//            if (d.isAuthorized()) {
                // button for saving the name to backend
                Button remove = new Button("Remove", event -> {
                    d.setAuthorized(true);
                    deviceRegistryService.removeDeviceInfo(d.getDeviceNumber());
                    deviceGrid.getDataProvider().refreshItem(d);

                    Notification.show("Device " + d.getDeviceNumber() + " was removed. ");
                });
                remove.addThemeVariants(ButtonVariant.LUMO_SMALL);
                remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
                buttons.add(remove);
//            }
            // layouts for placing the text field on top of the buttons
            return new VerticalLayout(buttons);

        })).setHeader("Actions").setWidth("20%");


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
                deviceGrid.getDataProvider().refreshAll();
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

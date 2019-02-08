package io.bonitoo.influxdemo;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.bonitoo.influxdemo.authentication.AccessControl;
import io.bonitoo.influxdemo.authentication.AccessControlFactory;
import io.bonitoo.influxdemo.authentication.LoginScreen;
import io.bonitoo.influxdemo.services.InfluxDBService;

/**
 * This class is used to listen to BeforeEnter event of all UIs in order to
 * check whether a user is signed in or not before allowing entering any page.
 * It is registered in a file named
 * com.vaadin.flow.server.VaadinServiceInitListener in META-INF/services.
 */
public class InitListener implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent initEvent) {

        final AccessControl accessControl = AccessControlFactory.getInstance()
            .createAccessControl();

        initEvent.getSource().addServiceDestroyListener(event -> {
            InfluxDBService.getInstance().stopWriteMetricsJob();

        });

        initEvent.getSource().addUIInitListener(uiInitEvent -> {
            uiInitEvent.getUI().addBeforeEnterListener(enterEvent -> {
                //navigate to login
                if (!accessControl.isUserSignedIn() && !LoginScreen.class
                    .equals(enterEvent.getNavigationTarget()))
                    enterEvent.rerouteTo(LoginScreen.class);
            });
        });
    }
}

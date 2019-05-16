package io.bonitoo.influxdemo;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import io.bonitoo.influxdemo.authentication.AccessControl;
import io.bonitoo.influxdemo.authentication.AccessControlFactory;
import io.bonitoo.influxdemo.authentication.LoginScreen;
import io.bonitoo.influxdemo.services.DataGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is used to listen to BeforeEnter event of all UIs in order to
 * check whether a user is signed in or not before allowing entering any page.
 * It is registered in a file named
 * com.vaadin.flow.server.VaadinServiceInitListener in META-INF/services.
 */
@SpringComponent
public class InitListener implements VaadinServiceInitListener {

    @Autowired
    DataGenerator dataGenerator;

    public void serviceInit(ServiceInitEvent initEvent) {

        final AccessControl accessControl = AccessControlFactory.getInstance()
            .createAccessControl();

        initEvent.getSource().addServiceDestroyListener(event -> {
            dataGenerator.stopGenerator();
        });

        if (!System.getProperty("disableLogin", "false").equals("true")) {
            initEvent.getSource().addUIInitListener(uiInitEvent -> {
                uiInitEvent.getUI().addBeforeEnterListener(enterEvent -> {
                    //navigate to login

                    if (!accessControl.isUserSignedIn() && !LoginScreen.class.equals(enterEvent.getNavigationTarget())) {
                        enterEvent.rerouteTo(LoginScreen.class);
                    }

                });
            });
        }

    }
}

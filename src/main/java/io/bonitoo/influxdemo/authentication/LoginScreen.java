package io.bonitoo.influxdemo.authentication;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

/**
 * UI content when the user is not logged in yet.
 */
@Route("Login")
@PageTitle("Login")
@HtmlImport("css/shared-styles.html")
public class LoginScreen extends FlexLayout {

    private TextField username;
    private PasswordField password;
    private Button login;
    private AccessControl accessControl;

    public LoginScreen() {
        accessControl = AccessControlFactory.getInstance().createAccessControl();
        buildUI();
        username.focus();
    }

    private void buildUI() {
        setSizeFull();
        setClassName("login-screen");

        // login form, centered in the available part of the screen
        Component loginForm = buildLoginForm();

        // layout to center login form when there is sufficient screen space
        FlexLayout centeringLayout = new FlexLayout();
        centeringLayout.setClassName("centeringLayout");
        centeringLayout.setSizeFull();
        centeringLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        centeringLayout.setAlignItems(Alignment.CENTER);
        centeringLayout.add(loginForm);

        // information text about logging in
        Component loginInformation = buildLoginInformation();

        add(loginInformation);
        add(centeringLayout);
    }

    private Component buildLoginForm() {
        FormLayout loginForm = new FormLayout();

        loginForm.setWidth("310px");

        loginForm.addFormItem(username = new TextField(), "Username");
        username.setWidth("15em");
        username.setValue("admin");
        loginForm.add(new Html("<br/>"));
        loginForm.addFormItem(password = new PasswordField(), "Password");
        password.setWidth("15em");

        HorizontalLayout buttons = new HorizontalLayout();
        loginForm.add(new Html("<br/>"));
        loginForm.add(buttons);

        buttons.add(login = new Button("Login"));
        login.setId("login-button");
        login.addClickListener(event -> login());
        loginForm.getElement().addEventListener("keypress", event -> login()).setFilter("event.key == 'Enter'");
        login.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        Button forgotPassword = new Button("Forgot password?");
        buttons.add(forgotPassword );
        forgotPassword.addClickListener(event -> showNotification(new Notification("Hint: try anything")));
        forgotPassword.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        return loginForm;
    }

    private Component buildLoginInformation() {
        VerticalLayout loginInformation = new VerticalLayout();
        loginInformation.setClassName("login-information");

        String resolvedImage = VaadinServletService.getCurrent()
            .resolveResource("frontend://img/influxdb.svg",
                VaadinSession.getCurrent().getBrowser());


        Image image = new Image(resolvedImage, "");
        loginInformation.add(image);

        loginInformation.add(new H2("InfluxData"));

        H4 loginInfoHeader = new H4("`PetStore` Demo");
        Span loginInfoText = new Span(
            "Log in as \"admin\" to have full access. Log in with any other username to have read-only access. For all users, any password is fine.");
        loginInformation.add(loginInfoHeader);
        loginInformation.add(loginInfoText);

        return loginInformation;
    }

    private void login() {
        login.setEnabled(false);
        try {
            if (accessControl.signIn(username.getValue(), password.getValue())) {
                getUI().get().navigate("");
            } else {
                showNotification(new Notification("Login failed. " +
                    "Please check your username and password and try again."));
                username.focus();
            }
        } finally {
            login.setEnabled(true);
        }
    }

    private void showNotification(Notification notification) {
        // keep the notification visible a little while after moving the
        // mouse, or until clicked
        notification.setDuration(2000);
        notification.open();
    }
}

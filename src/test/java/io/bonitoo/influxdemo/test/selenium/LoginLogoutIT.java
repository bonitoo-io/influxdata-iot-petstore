package io.bonitoo.influxdemo.test.selenium;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginLogoutIT  {

    private WebDriver driver;
    private long clickTimeout = 10;

    @Before
    public void  before() {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--proxy-server='direct://'");
        options.addArguments("--proxy-bypass-list=*");
        options.addArguments("--start-maximized");
        options.addArguments("--headless");

        driver = new ChromeDriver(options);

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    @Test
    public void loginLogoutTest() {
        driver.get("http://localhost:8080/");
        Assertions.assertThat(driver.getPageSource().contains("Log in with any other username to have read-only access")).isTrue();

        click(By.id("login-button"));
        click(By.id("Dashboard"));
        click(By.id("logout-button"));

        Assertions.assertThat(driver.getPageSource().contains("Log in with any other username to have read-only access")).isTrue();
    }

    @After
    public void after() {
        driver.quit();
    }

    private void click(By element) {
        WebDriverWait wait = new WebDriverWait(driver, clickTimeout);
        wait.until(ExpectedConditions.elementToBeClickable(element));
        driver.findElement(element).click();
    }
}
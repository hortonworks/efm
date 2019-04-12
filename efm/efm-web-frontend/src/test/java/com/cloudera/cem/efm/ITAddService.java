package com.cloudera.cem.efm;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ITAddService {
    private WebDriver driver;
    private String baseUrl;
    private boolean acceptNextAlert = true;
    private WebDriverWait wait;
    private StringBuffer verificationErrors = new StringBuffer();

    @Before
    public void setUp() throws Exception {
        WebDriverManager.chromedriver().setup();

        driver = new ChromeDriver();

        baseUrl = "http://localhost:10080/efm/ui";

        wait = new WebDriverWait(driver, 30);
    }

    @Test
    public void testAddService() throws Exception {
        // go directly to Flow Designer by URL
        driver.get(baseUrl + "/#/flow-designer/open");

        // confirm Open Flow dialog visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"efm-perspectives-container\"]/ng-component/div/div[1]")));

        // confirm Class B flow exists
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"efm-perspectives-container\"]/ng-component/div/div[1]/efm-flow-listing/div/div/div[2]/div[2]")));

        // select Class B flow
        Actions action = new Actions(driver);
        WebElement selectFlow = driver.findElement(By.xpath("//*[@id=\"efm-perspectives-container\"]/ng-component/div/div[1]/efm-flow-listing/div/div/div[2]/div[2]"));
        action.doubleClick(selectFlow).perform();

        // confirm Open Flow dialog closes
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//*[@id=\"efm-perspectives-container\"]/ng-component/div/div[1]")));

        // confirm canvas exists
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"efm-flow-designer-app-container\"]")));

        // right-click canvas to show context menu
        WebElement processor2 = driver.findElement(By.xpath("//*[@id=\"efm-flow-designer-app-container\"]"));
        action.contextClick(processor2).perform();

        // wait for context menu
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("show-component-listing-menu-item")));

        // select services from context menu
        WebElement servicesButton = driver.findElement(By.id("show-component-listing-menu-item"));
        servicesButton.click();

        // wait for Add Service button to be visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-automation-id='add-service-button']")));

        // select Add Service button
        WebElement addServiceButton = driver.findElement(By.cssSelector("[data-automation-id='add-service-button']"));
        addServiceButton.click();

        // wait for confirm dialog
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.cdk-overlay-pane")));

        // wait for service to select
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"flow-designer-extension-creation-list\"]/div[3]")));

        // select service from Add Service dialog
        WebElement selectService = driver.findElement(By.xpath("//*[@id=\"flow-designer-extension-creation-list\"]/div[3]"));
        action.doubleClick(selectService).perform();

        // verify service added
        List<WebElement> serviceCount = driver.findElements(By.cssSelector("#efm-flow-designer-component-listing > flow-designer-component-listing > div"));
        assertEquals(1, serviceCount.size());
    }

    @After
    public void tearDown() throws Exception {
        // confirm trash button existence
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("fa-trash")));

        // select trash icon for service
        WebElement deleteServiceButton = driver.findElement(By.className("fa-trash"));
        deleteServiceButton.click();

        // wait for confirm dialog
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.cdk-overlay-pane")));

        // confirm existence of delete button
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.cdk-overlay-pane button.mat-fds-warn")));

        // select delete button
        WebElement deleteButton = driver.findElement(By.cssSelector("div.cdk-overlay-pane button.mat-fds-warn"));
        deleteButton.click();

        // wait for the confirm dialog to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.cdk-overlay-pane")));

        // verify service deleted
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-automation-id='no-services-added-message']")));

        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean isAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    private String closeAlertAndGetItsText() {
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            if (acceptNextAlert) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            return alertText;
        } finally {
            acceptNextAlert = true;
        }
    }
}

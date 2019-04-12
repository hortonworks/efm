package com.cloudera.cem.efm;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.Assert.fail;

public class ITAddServiceCancel {
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
    public void testAddServiceCancel() throws Exception {
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

        // wait for cancel button
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.cdk-overlay-pane button.mat-fds-regular")));

        // select cancel button
        WebElement cancelButton = driver.findElement(By.cssSelector("div.cdk-overlay-pane button.mat-fds-regular"));
        JavascriptExecutor executor = (JavascriptExecutor)driver;
        executor.executeScript("arguments[0].click();", cancelButton);

        // wait for the confirm dialog to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.cdk-overlay-pane")));

        // verify service not added
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-automation-id='no-services-added-message']")));

    }


    @After
    public void tearDown() throws Exception {

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

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

public class ITOpenFlow {
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
    public void testOpenFlow() throws Exception {
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

        // confirm existence of Actions button on canvas
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"efm-flow-designer-app-container\"]/div/div[3]/button")));

        // select Actions button
        WebElement selectActionsButton = driver.findElement(By.xpath("//*[@id=\"efm-flow-designer-app-container\"]/div/div[3]/button"));
        selectActionsButton.click();

        // confirm existence of Open in drop-down menu
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"cdk-overlay-1\"]/div/div/button[1]")));

        // select Open from drop-down menu
        WebElement selectOpen = driver.findElement(By.xpath("//*[@id=\"cdk-overlay-1\"]/div/div/button[1]"));
        JavascriptExecutor executor = (JavascriptExecutor)driver;
        executor.executeScript("arguments[0].click();", selectOpen);

        // confirm drop-down menu closes
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//*[@id=\"cdk-overlay-1\"]/div/div/button[1]")));

        // confirm second flow in list exists
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"efm-perspectives-container\"]/ng-component/div/div[1]/efm-flow-listing/div/div/div[2]/div[2]/div[2]")));

        // select flow
        WebElement selectFlow2 = driver.findElement(By.xpath("//*[@id=\"efm-perspectives-container\"]/ng-component/div/div[1]/efm-flow-listing/div/div/div[2]/div[2]/div[2]"));
        action.doubleClick(selectFlow2).perform();

        // confirm existence of Actions button on canvas
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"efm-flow-designer-app-container\"]/div/div[3]/button")));

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

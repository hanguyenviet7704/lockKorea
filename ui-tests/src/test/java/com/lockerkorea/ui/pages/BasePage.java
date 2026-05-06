package com.lockerkorea.ui.pages;

import com.lockerkorea.ui.utils.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Base page class with common page functionality.
 * All page-level waits and utilities are centralized here.
 */
public abstract class BasePage {
    protected WebDriver driver;

    /** Default explicit wait (seconds) read from config */
    protected static final int EXPLICIT_WAIT = ConfigReader.getInt("explicit.wait", 20);

    /** Shorter wait for transient elements (toasts, animations) */
    protected static final int SHORT_WAIT = 5;

    public BasePage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Navigate to page URL
     */
    public abstract void navigateTo();

    /**
     * Check if page is loaded
     */
    public abstract boolean isLoaded();

    /**
     * Get page title
     */
    public String getPageTitle() {
        return driver.getTitle();
    }
    
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Wait for page to load completely
     */
    protected void waitForPageLoad() {
        new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT))
            .until(webDriver -> ((org.openqa.selenium.JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").toString().equals("complete"));
    }

    /**
     * Wait for element to be clickable
     */
    protected void waitForElementClickable(By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT))
            .until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait for element to be visible
     */
    protected void waitForElementVisible(By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT))
            .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait for element to disappear (e.g. loading spinner)
     */
    protected void waitForElementInvisible(By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT))
            .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Wait for a toast message matching the given CSS locator, then return its text.
     * Returns empty string if not found within SHORT_WAIT seconds.
     */
    public String waitForToast(By toastLocator) {
        try {
            WebElement toast = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT))
                    .until(ExpectedConditions.visibilityOfElementLocated(toastLocator));
            return toast.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Click element safely
     */
    protected void click(By locator) {
        waitForElementClickable(locator);
        driver.findElement(locator).click();
    }

    /**
     * Click WebElement directly
     */
    protected void click(WebElement element) {
        element.click();
    }

    /**
     * Click element using JavaScript (fallback for overlapping elements)
     */
    protected void jsClick(By locator) {
        WebElement element = driver.findElement(locator);
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Type text into input — clears first, then sends keys.
     */
    public void type(By locator, String text) {
        WebElement element = waitForElement(locator);
        element.clear();
        // Use CTRL+A + DELETE to ensure full clear for Angular reactive forms
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        element.sendKeys(text);
    }

    /**
     * Wait for element to be present and visible, then return it.
     */
    protected WebElement waitForElement(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Get text from element
     */
    protected String getText(By locator) {
        return driver.findElement(locator).getText();
    }

    /**
     * Check if element is displayed
     */
    public boolean isElementDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Get toast message
     */
    public String getToastMessage() {
        try {
            return driver.findElement(By.cssSelector(".p-toast .p-toast-message")).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get element
     */
    protected WebElement getElement(By locator) {
        return driver.findElement(locator);
    }

    /**
     * Scroll to element
     */
    protected void scrollToElement(By locator) {
        WebElement element = driver.findElement(locator);
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }
}

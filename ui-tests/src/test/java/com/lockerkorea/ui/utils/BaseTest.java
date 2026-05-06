package com.lockerkorea.ui.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Base test class with WebDriver setup/teardown and utilities
 * Use JUnit 5 lifecycle annotations
 */
public class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected DBConnector dbConnector;

    @BeforeEach
    public void setUp() throws Exception {
        // Initialize DB connector
        dbConnector = new DBConnector();
        try {
            dbConnector.connect();
            // Run cleanup before each test for isolation
            // Uncomment if you want automatic cleanup
            // dbConnector.cleanupDatabase();
        } catch (SQLException e) {
            System.err.println("Warning: Could not connect to database: " + e.getMessage());
            // Continue even if DB is not available
        }

        // Setup browser
        setupDriver();

        // Configure implicit wait (optional, explicit waits preferred)
        driver.manage().timeouts().implicitlyWait(
            Duration.ofSeconds(ConfigReader.getInt("implicit.wait", 10))
        );

        // Maximize window (optional)
        try {
            driver.manage().window().maximize();
        } catch (Exception e) {
            System.out.println("Could not maximize window: " + e.getMessage());
        }

        // Set page load timeout
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        System.out.println("Browser started: " + ConfigReader.getString("browser"));
    }

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        boolean testFailed = testInfo.getTags().contains("failed") ||
                             testInfo.getTestMethod().isPresent() &&
                             testInfo.getTestMethod().get().isAnnotationPresent(Test.class);

        // Note: JUnit 5 doesn't directly provide failure status in @AfterEach without extensions
        // For simplicity, always take screenshot or based on config
        if (ConfigReader.getBoolean("screenshot.on.failure", true)) {
            // You could implement TestWatcher extension for better failure detection
            takeScreenshot(testInfo.getDisplayName());
        }

        // Quit driver
        if (driver != null) {
            driver.quit();
            driver = null;
        }

        // Disconnect DB
        if (dbConnector != null) {
            dbConnector.disconnect();
        }

        // Small delay between tests
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ... rest of the class remains the same

    /**
     * Setup WebDriver based on configuration
     */
    private void setupDriver() {
        String browser = ConfigReader.getString("browser", "chrome").toLowerCase();
        boolean headless = ConfigReader.getBoolean("headless", false);

        switch (browser) {
            case "firefox":
                setupFirefoxDriver(headless);
                break;
            case "edge":
                setupEdgeDriver(headless);
                break;
            case "chrome":
            default:
                setupChromeDriver(headless);
                break;
        }

        // Create explicit wait
        wait = new WebDriverWait(driver, Duration.ofSeconds(
            ConfigReader.getInt("explicit.wait", 20)
        ));
    }

    private void setupChromeDriver(boolean headless) {
        io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        if (headless) {
            options.addArguments("--headless=new");
        }

        // Additional Chrome options for stability
        options.setAcceptInsecureCerts(true);
        options.addArguments("--disable-blink-features=AutomationControlled");

        driver = new ChromeDriver(options);
    }

    private void setupFirefoxDriver(boolean headless) {
        io.github.bonigarcia.wdm.WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");

        if (headless) {
            options.addArguments("--headless");
        }

        driver = new FirefoxDriver(options);
    }

    private void setupEdgeDriver(boolean headless) {
        io.github.bonigarcia.wdm.WebDriverManager.edgedriver().setup();

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--window-size=1920,1080");

        if (headless) {
            options.addArguments("--headless");
        }

        driver = new EdgeDriver(options);
    }

    /**
     * Take screenshot and save to file
     */
    protected void takeScreenshot(String testName) {
        if (driver == null) {
            return;
        }

        try {
            // Ensure directory exists
            String screenshotDir = ConfigReader.getString("screenshot.dir", "target/screenshots");
            Path dir = Paths.get(screenshotDir);
            Files.createDirectories(dir);

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String safeName = testName.replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = String.format("%s_%s.png", safeName, timestamp);
            String filepath = Paths.get(screenshotDir, filename).toString();

            // Take screenshot
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshotFile.toPath(), Paths.get(filepath), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Screenshot saved: " + filepath);
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    /**
     * Navigate to URL
     */
    protected void navigateTo(String url) {
        driver.get(url);
    }

    /**
     * Get current URL
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Get page title
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Wait for element to be visible
     */
    protected WebElement waitForVisibility(By locator, int timeoutInSeconds) {
        wait.withTimeout(Duration.ofSeconds(timeoutInSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait for element to be clickable
     */
    protected WebElement waitForClickable(By locator, int timeoutInSeconds) {
        wait.withTimeout(Duration.ofSeconds(timeoutInSeconds));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait for page to load (document.readyState == complete)
     */
    protected void waitForPageLoad() {
        wait.until(driver -> {
            String state = ((JavascriptExecutor) driver)
                .executeScript("return document.readyState").toString();
            return "complete".equals(state);
        });
    }

    /**
     * Scroll to element
     */
    protected void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    /**
     * Scroll to bottom of page
     */
    protected void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    /**
     * Switch to frame
     */
    protected void switchToFrame(String frameNameOrId) {
        driver.switchTo().frame(frameNameOrId);
    }

    /**
     * Switch to default content
     */
    protected void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    /**
     * Execute JavaScript
     */
    protected Object executeJavaScript(String script, Object... args) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        return js.executeScript(script, args);
    }

    /**
     * Get cookie by name
     */
    protected String getCookie(String name) {
        Cookie cookie = driver.manage().getCookieNamed(name);
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * Delete all cookies
     */
    protected void deleteAllCookies() {
        driver.manage().deleteAllCookies();
    }

    /**
     * Get text from element safely (returns empty string if not found)
     */
    protected String getText(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            return element.getText().trim();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    /**
     * Check if element is displayed
     */
    protected boolean isElementDisplayed(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            return element.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Wait until element is visible (default timeout)
     */
    protected WebElement waitForVisibility(By locator) {
        return waitForVisibility(locator, ConfigReader.getInt("explicit.wait", 20));
    }

    /**
     * Wait until element is clickable (default timeout)
     */
    protected WebElement waitForClickable(By locator) {
        return waitForClickable(locator, ConfigReader.getInt("explicit.wait", 20));
    }

    /**
     * Sleep for specified milliseconds (use sparingly)
     */
    protected void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;

/**
 * Login Page Object
 * URL: /auth-login
 */
public class LoginPage extends BasePage {

    // Locators
    private static final By LOGIN_CONTAINER = By.cssSelector(".login-page");
    public static final By USERNAME_INPUT = By.id("userName");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BUTTON = By.cssSelector(".login-btn");
    public static final By REMEMBER_ME_CHECKBOX = By.cssSelector("input[formControlName='rememberMe']");
    private static final By FORGOT_PASSWORD_LINK = By.cssSelector(".forgot-link");
    private static final By REGISTER_LINK = By.cssSelector(".register-link");
    private static final By GOOGLE_LOGIN_BUTTON = By.cssSelector(".google-login-btn");
    public static final By ERROR_MESSAGE = By.cssSelector(".error-message small");
    private static final By TOAST_MESSAGE = By.cssSelector(".p-toast .p-toast-message");

    public LoginPage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        // Get base URL from config file
        String baseUrl = com.lockerkorea.ui.utils.ConfigReader.getString("frontend.url", "http://localhost:4200");
        driver.get(baseUrl + "/auth-login");
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        return isElementDisplayed(LOGIN_CONTAINER) &&
               isElementDisplayed(USERNAME_INPUT) &&
               isElementDisplayed(PASSWORD_INPUT);
    }

    /**
     * Login with credentials
     * @param phoneNumber phone number (username)
     * @param password user password
     */
    public void login(String phoneNumber, String password) {
        type(USERNAME_INPUT, phoneNumber);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
        waitForPageLoad();
        try {
            Thread.sleep(1500); // Wait for API response and Angular router navigation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Login with credentials and remember me option
     */
    public void login(String phoneNumber, String password, boolean rememberMe) {
        type(USERNAME_INPUT, phoneNumber);
        type(PASSWORD_INPUT, password);
        if (rememberMe) {
            click(REMEMBER_ME_CHECKBOX);
        }
        click(LOGIN_BUTTON);
        waitForPageLoad();
    }

    /**
     * Click login button without filling fields
     */
    public void clickLoginButton() {
        click(LOGIN_BUTTON);
        waitForPageLoad();
    }

    /**
     * Navigate to forgot password page
     */
    public void clickForgotPassword() {
        click(FORGOT_PASSWORD_LINK);
        waitForPageLoad();
    }

    /**
     * Navigate to register page
     */
    public void clickRegisterLink() {
        click(REGISTER_LINK);
    }

    /**
     * Click Google login button
     */
    public void clickGoogleLogin() {
        click(GOOGLE_LOGIN_BUTTON);
    }

    /**
     * Get error message if login fails
     */
    public String getErrorMessage() {
        try {
            return getText(ERROR_MESSAGE);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get toast message (for success/error notifications)
     */
    public String getToastMessage() {
        try {
            return getText(TOAST_MESSAGE);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return isElementDisplayed(ERROR_MESSAGE);
    }

    /**
     * Clear username field
     */
    public void clearUsername() {
        driver.findElement(USERNAME_INPUT).clear();
    }

    /**
     * Clear password field
     */
    public void clearPassword() {
        driver.findElement(PASSWORD_INPUT).clear();
    }

    /**
     * Check if login button is enabled
     */
    public boolean isLoginButtonEnabled() {
        return driver.findElement(LOGIN_BUTTON).isEnabled();
    }
}

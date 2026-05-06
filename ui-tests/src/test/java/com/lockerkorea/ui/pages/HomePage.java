package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Home Page Object
 * URL: /
 */
public class HomePage extends BasePage {

    // Locators
    private static final By CART_ICON = By.cssSelector("a[routerLink='/shoppingCart'], .pi-shopping-cart, .cart-icon");
    private static final By CART_BADGE = By.cssSelector(".p-badge, .cart-badge, .badge");

    public HomePage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        String baseUrl = com.lockerkorea.ui.utils.ConfigReader.getString("frontend.url", "http://localhost:4200");
        driver.get(baseUrl);
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        return driver.getCurrentUrl().contains("localhost") ||
               driver.getCurrentUrl().contains("shopping-cart") == false;
    }

    /**
     * Click cart icon and go to cart page
     */
    public CartPage clickCartIcon() {
        click(CART_ICON);
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        // If it's a link, it will navigate to cart page
        // If it's just an icon that shows a dropdown, return current page
        CartPage cartPage = new CartPage(driver);
        cartPage.waitForPageLoad();
        return cartPage;
    }

    /**
     * Get cart badge text (item count)
     */
    public String getCartBadgeText() {
        try {
            WebElement badge = driver.findElement(CART_BADGE);
            return badge.getText().trim();
        } catch (Exception e) {
            // Try to find badge near cart icon
            try {
                WebElement cartIcon = driver.findElement(CART_ICON);
                WebElement parent = cartIcon.findElement(By.xpath(".."));
                WebElement badge = parent.findElement(By.cssSelector(".p-badge, .badge"));
                return badge.getText().trim();
            } catch (Exception e2) {
                return "0";
            }
        }
    }

    /**
     * Check if cart badge is displayed
     */
    public boolean isCartBadgeDisplayed() {
        try {
            return isElementDisplayed(CART_BADGE);
        } catch (Exception e) {
            return false;
        }
    }
}

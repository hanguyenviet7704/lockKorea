package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;

import java.util.List;

/**
 * Shopping Cart Page Object
 * URL: /shoppingCart
 */
public class CartPage extends BasePage {

    // Locators
    private static final By CART_CONTAINER = By.cssSelector(".cart-wrapper");
    private static final By CART_ITEMS = By.cssSelector(".cart-item");
    private static final By CART_ITEM_PRODUCT_NAME = By.cssSelector(".product-name");
    private static final By CART_ITEM_PRICE = By.cssSelector(".item-info .price");
    private static final By CART_ITEM_QUANTITY = By.cssSelector(".quantity-input input");
    private static final By CART_ITEM_TOTAL = By.cssSelector(".item-total");
    private static final By REMOVE_ITEM_BUTTON = By.cssSelector(".p-button-danger");
    private static final By SELECT_CHECKBOX = By.cssSelector(".select-product input");
    private static final By QUANTITY_INCREMENT = By.cssSelector(".p-button-sm .pi-plus");
    private static final By QUANTITY_DECREMENT = By.cssSelector(".p-button-sm .pi-minus");
    private static final By CHECKOUT_BUTTON = By.cssSelector(".checkout-btn");
    private static final By CONTINUE_SHOPPING_BUTTON = By.cssSelector("a[routerLink='/all-product']");
    private static final By DELETE_ALL_BUTTON = By.cssSelector("button .pi-trash");
    private static final By EMPTY_CART_MESSAGE = By.cssSelector(".empty-cart");
    private static final By SELECTED_COUNT = By.cssSelector(".selected-count");
    private static final By SUBTOTAL = By.cssSelector(".summary-details .amount");
    private static final By SHIPPING_COST = By.cssSelector(".summary-details .amount:nth-of-type(2)");
    private static final By TOTAL_AMOUNT = By.cssSelector(".total-amount");
    private static final By NO_SELECTION_MESSAGE = By.cssSelector(".checkout-hint");

    public CartPage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        String baseUrl = System.getProperty("frontend.url", "http://localhost");
        driver.get(baseUrl + "/shoppingCart");
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        return isElementDisplayed(CART_CONTAINER);
    }

    /**
     * Get number of items in cart
     */
    public int getCartItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    /**
     * Check if cart is empty
     */
    public boolean isEmpty() {
        return isElementDisplayed(EMPTY_CART_MESSAGE) || getCartItemCount() == 0;
    }

    /**
     * Get product name at index
     */
    public String getProductName(int index) {
        List<org.openqa.selenium.WebElement> names = driver.findElements(CART_ITEM_PRODUCT_NAME);
        if (index >= 0 && index < names.size()) {
            return names.get(index).getText().trim();
        }
        throw new IndexOutOfBoundsException("No cart item at index " + index);
    }

    /**
     * Get product price at index
     */
    public String getProductPrice(int index) {
        List<org.openqa.selenium.WebElement> prices = driver.findElements(CART_ITEM_PRICE);
        if (index >= 0 && index < prices.size()) {
            return prices.get(index).getText().trim();
        }
        throw new IndexOutOfBoundsException("No cart item at index " + index);
    }

    /**
     * Get product quantity at index
     */
    public int getProductQuantity(int index) {
        List<org.openqa.selenium.WebElement> quantities = driver.findElements(CART_ITEM_QUANTITY);
        if (index >= 0 && index < quantities.size()) {
            return Integer.parseInt(quantities.get(index).getAttribute("value"));
        }
        throw new IndexOutOfBoundsException("No cart item at index " + index);
    }

    /**
     * Set product quantity at index
     */
    public void setProductQuantity(int index, int quantity) {
        List<org.openqa.selenium.WebElement> quantityInputs = driver.findElements(CART_ITEM_QUANTITY);
        if (index >= 0 && index < quantityInputs.size()) {
            quantityInputs.get(index).clear();
            quantityInputs.get(index).sendKeys(String.valueOf(quantity));
            // Trigger change event if needed
            try {
                Thread.sleep(500); // Wait for price recalculation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new IndexOutOfBoundsException("No cart item at index " + index);
        }
    }

    /**
     * Click increment button for item at index
     */
    public void incrementQuantity(int index) {
        List<org.openqa.selenium.WebElement> cartItems = driver.findElements(CART_ITEMS);
        if (index >= 0 && index < cartItems.size()) {
            org.openqa.selenium.WebElement item = cartItems.get(index);
            item.findElement(QUANTITY_INCREMENT).click();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Click decrement button for item at index
     */
    public void decrementQuantity(int index) {
        List<org.openqa.selenium.WebElement> cartItems = driver.findElements(CART_ITEMS);
        if (index >= 0 && index < cartItems.size()) {
            org.openqa.selenium.WebElement item = cartItems.get(index);
            item.findElement(QUANTITY_DECREMENT).click();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Select product for checkout at index
     */
    public void selectProduct(int index) {
        List<org.openqa.selenium.WebElement> checkboxes = driver.findElements(SELECT_CHECKBOX);
        if (index >= 0 && index < checkboxes.size()) {
            org.openqa.selenium.WebElement checkbox = checkboxes.get(index);
            if (!checkbox.isSelected()) {
                checkbox.click();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Remove product at index
     */
    public void removeProduct(int index) {
        List<org.openqa.selenium.WebElement> cartItems = driver.findElements(CART_ITEMS);
        if (index >= 0 && index < cartItems.size()) {
            org.openqa.selenium.WebElement item = cartItems.get(index);
            item.findElement(REMOVE_ITEM_BUTTON).click();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Click delete all button
     */
    public void deleteAllItems() {
        if (isElementDisplayed(DELETE_ALL_BUTTON)) {
            click(DELETE_ALL_BUTTON);
            // Handle confirmation dialog if present
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get selected products count
     */
    public int getSelectedProductsCount() {
        try {
            String text = getText(SELECTED_COUNT);
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get subtotal amount
     */
    public String getSubtotal() {
        try {
            List<org.openqa.selenium.WebElement> amounts = driver.findElements(SUBTOTAL);
            if (!amounts.isEmpty()) {
                return amounts.get(0).getText().trim();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }

    /**
     * Get shipping cost
     */
    public String getShippingCost() {
        try {
            List<org.openqa.selenium.WebElement> amounts = driver.findElements(SHIPPING_COST);
            if (amounts.size() > 1) {
                return amounts.get(1).getText().trim();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }

    /**
     * Get total amount
     */
    public String getTotalAmount() {
        try {
            return getText(TOTAL_AMOUNT);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Click continue shopping
     */
    public ProductListPage continueShopping() {
        click(CONTINUE_SHOPPING_BUTTON);
        waitForPageLoad();
        return new ProductListPage(driver);
    }

    /**
     * Proceed to checkout
     */
    public void proceedToCheckout() {
        click(CHECKOUT_BUTTON);
        waitForPageLoad();
    }

    /**
     * Check if checkout button is enabled
     */
    public boolean isCheckoutButtonEnabled() {
        return !driver.findElement(CHECKOUT_BUTTON).getAttribute("class").contains("p-button-disabled");
    }

    /**
     * Get product total at index
     */
    public String getProductTotal(int index) {
        List<org.openqa.selenium.WebElement> totals = driver.findElements(CART_ITEM_TOTAL);
        if (index >= 0 && index < totals.size()) {
            return totals.get(index).getText().trim();
        }
        throw new IndexOutOfBoundsException("No cart item at index " + index);
    }

    /**
     * Get cart item by product name
     */
    public CartItem getCartItemByProductName(String productName) {
        List<org.openqa.selenium.WebElement> items = driver.findElements(CART_ITEMS);
        for (org.openqa.selenium.WebElement item : items) {
            String name = item.findElement(CART_ITEM_PRODUCT_NAME).getText().trim();
            if (name.equals(productName)) {
                return new CartItem(item);
            }
        }
        return null;
    }

    /**
     * Inner class representing a cart item
     */
    public static class CartItem {
        private org.openqa.selenium.WebElement element;

        public CartItem(org.openqa.selenium.WebElement element) {
            this.element = element;
        }

        public String getName() {
            return element.findElement(CART_ITEM_PRODUCT_NAME).getText().trim();
        }

        public String getPrice() {
            return element.findElement(CART_ITEM_PRICE).getText().trim();
        }

        public int getQuantity() {
            return Integer.parseInt(element.findElement(CART_ITEM_QUANTITY).getAttribute("value"));
        }

        public void setQuantity(int qty) {
            element.findElement(CART_ITEM_QUANTITY).clear();
            element.findElement(CART_ITEM_QUANTITY).sendKeys(String.valueOf(qty));
        }

        public String getTotal() {
            return element.findElement(CART_ITEM_TOTAL).getText().trim();
        }

        public void select() {
            element.findElement(SELECT_CHECKBOX).click();
        }

        public void remove() {
            element.findElement(REMOVE_ITEM_BUTTON).click();
        }
    }
}

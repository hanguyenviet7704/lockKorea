package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Product Detail Page Object
 * URL: /detailProduct/:id
 */
public class ProductDetailPage extends BasePage {

    // Locators
    private static final By PRODUCT_DETAIL_CONTAINER = By.cssSelector(".product-detail, .detail-product");
    private static final By PRODUCT_NAME = By.cssSelector(".product-name h1, .product-name h3, h1.product-name");
    private static final By PRODUCT_PRICE = By.cssSelector(".product-price .current-price, .detail-price");
    private static final By PRODUCT_DESCRIPTION = By.cssSelector(".product-description, .description");
    private static final By PRODUCT_IMAGE = By.cssSelector(".product-image img, img.product-image");
    private static final By QUANTITY_INPUT = By.cssSelector("input[formControlName='quantity']");
    private static final By ADD_TO_CART_BUTTON = By.cssSelector("button[type='submit'], .add-to-cart-btn, .p-button-success");
    private static final By SIZE_SELECTION = By.cssSelector(".size-selector, .p-radiobutton");
    private static final By FEATURE_TAGS = By.cssSelector(".feature-tag, .product-features .badge");
    private static final By REVIEW_SECTION = By.cssSelector(".review-section, .reviews");
    private static final By RATING_STARS = By.cssSelector(".product-rating .pi-star-fill");
    private static final By SOLD_COUNT = By.cssSelector(".product-sold span");
    private static final By DISCOUNT_BADGE = By.cssSelector(".discount-badge");
    private static final By BACK_TO_LIST_LINK = By.cssSelector("a[routerLink='/allProduct']");
    private static final By RELATED_PRODUCTS = By.cssSelector(".related-products .product-card");

    public ProductDetailPage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        // Cannot navigate without product ID
        throw new UnsupportedOperationException("Navigate with product ID: navigateTo(productId)");
    }

    /**
     * Navigate to product detail with specific ID
     */
    public void navigateTo(String productId) {
        String baseUrl = System.getProperty("frontend.url", "http://localhost");
        driver.get(baseUrl + "/detailProduct/" + productId);
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        // Check if either container exists and product name is present
        try {
            return isElementDisplayed(PRODUCT_NAME) ||
                   (isElementDisplayed(PRODUCT_DETAIL_CONTAINER) && getProductName().length() > 0);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get product name
     */
    public String getProductName() {
        try {
            return getText(PRODUCT_NAME);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get product price
     */
    public String getProductPrice() {
        try {
            return getText(PRODUCT_PRICE);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get product description
     */
    public String getProductDescription() {
        try {
            return getText(PRODUCT_DESCRIPTION);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get average rating (number of filled stars)
     */
    public double getRating() {
        try {
            List<org.openqa.selenium.WebElement> stars = driver.findElements(RATING_STARS);
            return stars.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get sold count
     */
    public int getSoldCount() {
        try {
            String text = getText(SOLD_COUNT);
            // Extract number from text like "Đã bán: 150"
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get discount percentage
     */
    public int getDiscountPercentage() {
        try {
            String text = getText(DISCOUNT_BADGE);
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Set quantity
     */
    public void setQuantity(int quantity) {
        type(QUANTITY_INPUT, String.valueOf(quantity));
    }

    /**
     * Get current quantity
     */
    public int getQuantity() {
        try {
            String value = driver.findElement(QUANTITY_INPUT).getAttribute("value");
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Click add to cart button
     */
    public void clickAddToCart() {
        click(ADD_TO_CART_BUTTON);
        // Wait for toast or confirmation
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Select size (if available)
     */
    public void selectSize(String size) {
        // Implementation depends on actual size selector UI
        // Could be radio buttons, dropdown, or buttons
        try {
            By sizeLocator = By.xpath("//label[contains(text(),'" + size + "')]/preceding-sibling::input");
            click(sizeLocator);
        } catch (Exception e) {
            System.out.println("Size selection not found: " + e.getMessage());
        }
    }

    /**
     * Get list of related products
     */
    public int getRelatedProductCount() {
        return driver.findElements(RELATED_PRODUCTS).size();
    }

    /**
     * Click related product at index
     */
    public ProductDetailPage clickRelatedProduct(int index) {
        List<org.openqa.selenium.WebElement> relatedProducts = driver.findElements(RELATED_PRODUCTS);
        if (index >= 0 && index < relatedProducts.size()) {
            relatedProducts.get(index).click();
            waitForPageLoad();
            return this;
        }
        throw new IndexOutOfBoundsException("No related product at index " + index);
    }

    /**
     * Navigate back to product list
     */
    public ProductListPage navigateBackToList() {
        click(BACK_TO_LIST_LINK);
        waitForPageLoad();
        return new ProductListPage(driver);
    }

    /**
     * Get product image URL
     */
    public String getProductImageUrl() {
        try {
            return driver.findElement(PRODUCT_IMAGE).getAttribute("src");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if add to cart button is displayed
     */
    public boolean isAddToCartButtonDisplayed() {
        return isElementDisplayed(ADD_TO_CART_BUTTON);
    }

    /**
     * Check if product is in stock (quantity > 0)
     */
    public boolean isInStock() {
        try {
            int qty = getQuantity();
            return qty > 0;
        } catch (Exception e) {
            return true; // Assume in stock if cannot check
        }
    }

    /**
     * Check if toast message is displayed
     */
    public boolean isToastDisplayed(String expectedText) {
        try {
            WebElement toast = driver.findElement(By.cssSelector(".p-toast-message, .toast-message, .p-toast"));
            String toastText = toast.getText();
            return toastText.contains(expectedText);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get error message (validation)
     */
    public String getErrorMessage() {
        try {
            WebElement error = driver.findElement(By.cssSelector(".p-error, .error-message, .text-danger, small.text-danger"));
            return error.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Select color if available
     */
    public void selectColor(String color) {
        try {
            By colorOption = By.xpath("//label[contains(text(),'" + color + "')]/preceding-sibling::input");
            click(colorOption);
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            // Color selector might not exist
        }
    }
}

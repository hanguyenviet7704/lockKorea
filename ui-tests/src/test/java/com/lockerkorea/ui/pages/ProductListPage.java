package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;

import java.util.List;

/**
 * Product List Page Object
 * URL: /allProduct
 */
public class ProductListPage extends BasePage {

    // Locators
    private static final By PRODUCT_LIST_CONTAINER = By.cssSelector(".allProduct");
    private static final By PRODUCT_GRID = By.cssSelector(".product-grid");
    private static final By PRODUCT_CARDS = By.cssSelector(".product-card");
    private static final By PRODUCT_NAMES = By.cssSelector(".product-name");
    private static final By PRODUCT_PRICES = By.cssSelector(".current-price");
    private static final By PRODUCT_IMAGES = By.cssSelector(".product-card img");
    private static final By CATEGORY_FILTER = By.cssSelector(".category-item");
    private static final By SORT_DROPDOWN = By.cssSelector(".p-dropdown");
    private static final By PRICE_SLIDER = By.id("range1");
    private static final By FILTER_BUTTON = By.cssSelector(".btn-danger"); // Lọc button
    private static final By PAGINATOR = By.cssSelector(".p-paginator");
    private static final By SEARCH_INPUT = By.cssSelector("input[type='search']");
    private static final By DISCOUNT_BADGE = By.cssSelector(".discount-badge");
    private static final By OUT_OF_STOCK_BADGE = By.cssSelector(".p-badge.warning");
    private static final By LOADING_SPINNER = By.cssSelector(".spinner-border");

    public ProductListPage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        String baseUrl = System.getProperty("frontend.url", "http://localhost");
        driver.get(baseUrl + "/allProduct");
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        return isElementDisplayed(PRODUCT_LIST_CONTAINER) &&
               isElementDisplayed(PRODUCT_GRID);
    }

    /**
     * Get number of products displayed
     */
    public int getProductCount() {
        return driver.findElements(PRODUCT_CARDS).size();
    }

    /**
     * Click on product at specific index
     */
    public ProductDetailPage clickProduct(int index) {
        List<org.openqa.selenium.WebElement> products = driver.findElements(PRODUCT_CARDS);
        if (index >= 0 && index < products.size()) {
            products.get(index).click();
            waitForPageLoad();
            return new ProductDetailPage(driver);
        }
        throw new IndexOutOfBoundsException("No product at index " + index);
    }

    /**
     * Get product name at index
     */
    public String getProductName(int index) {
        List<org.openqa.selenium.WebElement> names = driver.findElements(PRODUCT_NAMES);
        if (index >= 0 && index < names.size()) {
            return names.get(index).getText().trim();
        }
        throw new IndexOutOfBoundsException("No product name at index " + index);
    }

    /**
     * Get product price at index
     */
    public String getProductPrice(int index) {
        List<org.openqa.selenium.WebElement> prices = driver.findElements(PRODUCT_PRICES);
        if (index >= 0 && index < prices.size()) {
            return prices.get(index).getText().trim();
        }
        throw new IndexOutOfBoundsException("No product price at index " + index);
    }

    /**
     * Search for products by keyword
     */
    public void searchProducts(String keyword) {
        type(SEARCH_INPUT, keyword);
        // Wait for search results (debounce may apply)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        waitForPageLoad();
    }

    /**
     * Filter by category
     */
    public void filterByCategory(String categoryName) {
        List<org.openqa.selenium.WebElement> categories = driver.findElements(CATEGORY_FILTER);
        for (org.openqa.selenium.WebElement category : categories) {
            if (category.getText().trim().contains(categoryName)) {
                category.click();
                waitForPageLoad();
                return;
            }
        }
        throw new RuntimeException("Category not found: " + categoryName);
    }

    /**
     * Select sort option
     */
    public void sortBy(String option) {
        // Click dropdown
        click(SORT_DROPDOWN);
        // Select option (would need to interact with dropdown items)
        // Implementation depends on PrimeNG dropdown structure
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Select by visible text
        By optionLocator = By.xpath("//li[contains(@class,'p-dropdown-item') and text()='" + option + "']");
        click(optionLocator);
        waitForPageLoad();
    }

    /**
     * Set price range filter
     */
    public void setPriceRange(int min, int max) {
        // Price slider uses multiplied values (x500000 according to HTML)
        // Implementation depends on slider component
        scrollToElement(PRICE_SLIDER);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Click filter button after setting slider
        click(FILTER_BUTTON);
        waitForPageLoad();
    }

    /**
     * Click filter button
     */
    public void applyFilter() {
        click(FILTER_BUTTON);
        waitForPageLoad();
    }

    /**
     * Get count of discounted products
     */
    public int getDiscountedProductCount() {
        return driver.findElements(DISCOUNT_BADGE).size();
    }

    /**
     * Check if product at index has discount
     */
    public boolean hasDiscount(int index) {
        List<org.openqa.selenium.WebElement> cards = driver.findElements(PRODUCT_CARDS);
        if (index >= 0 && index < cards.size()) {
            return cards.get(index).findElements(DISCOUNT_BADGE).size() > 0;
        }
        return false;
    }

    /**
     * Check if product at index is out of stock
     */
    public boolean isOutOfStock(int index) {
        List<org.openqa.selenium.WebElement> cards = driver.findElements(PRODUCT_CARDS);
        if (index >= 0 && index < cards.size()) {
            return cards.get(index).findElements(OUT_OF_STOCK_BADGE).size() > 0;
        }
        return false;
    }

    /**
     * Wait for products to load
     */
    public void waitForProductsToLoad() {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                .until(webDriver -> getProductCount() > 0);
        } catch (Exception e) {
            System.out.println("Products may not have loaded: " + e.getMessage());
        }
    }

    /**
     * Get first product name (convenience method)
     */
    public String getFirstProductName() {
        return getProductName(0);
    }

    /**
     * Click first product
     */
    public ProductDetailPage clickFirstProduct() {
        return clickProduct(0);
    }

    /**
     * Navigate to next page (pagination)
     */
    public void nextPage() {
        // Click next button on paginator
        By nextButton = By.cssSelector(".p-paginator-next");
        if (isElementDisplayed(nextButton)) {
            click(nextButton);
            waitForPageLoad();
        }
    }

    /**
     * Get total record count from paginator
     */
    public int getTotalRecords() {
        try {
            String totalText = getText(By.cssSelector(".p-paginator-current"));
            // Parse number from text like "1-12 of 150"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+$");
            java.util.regex.Matcher matcher = pattern.matcher(totalText);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    /**
     * Click product by name
     */
    public ProductDetailPage clickProductByName(String productName) {
        List<org.openqa.selenium.WebElement> cards = driver.findElements(PRODUCT_CARDS);
        for (org.openqa.selenium.WebElement card : cards) {
            try {
                String name = card.findElement(PRODUCT_NAMES).getText().trim();
                if (name.contains(productName) || productName.contains(name)) {
                    card.click();
                    waitForPageLoad();
                    return new ProductDetailPage(driver);
                }
            } catch (Exception e) {
                // Continue to next card
            }
        }
        throw new RuntimeException("Product not found: " + productName);
    }
}

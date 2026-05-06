package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;

import java.util.List;

/**
 * Product Management Page (Admin)
 * URL: /productManage
 */
public class ProductManagePage extends BasePage {

    // Locators — synchronized with product-manage.component.html
    private static final By PRODUCT_MANAGE_CONTAINER = By.cssSelector(".product-manage-container");
    // Form inputs (inside p-dialog)
    private static final By PRODUCT_NAME_INPUT = By.cssSelector("input#name, input[id='name']");
    private static final By CATEGORY_DROPDOWN = By.cssSelector("p-dropdown#category_id");
    private static final By PRICE_INPUT = By.cssSelector("p-inputNumber#price input");
    private static final By DISCOUNT_INPUT = By.cssSelector("p-inputNumber#discount input");
    private static final By QUANTITY_INPUT = By.cssSelector("p-inputNumber#quantity input");
    private static final By DESCRIPTION_TEXTAREA = By.cssSelector("textarea[formControlName='description']");
    private static final By FEATURES_MULTISELECT = By.cssSelector("p-multiSelect#features");
    private static final By FILE_UPLOAD = By.cssSelector(".p-fileupload");
    private static final By SUBMIT_BUTTON = By.cssSelector("p-dialog p-button[label='Lưu'] button");
    private static final By CANCEL_BUTTON = By.cssSelector("p-dialog p-button[label='Hủy'] button");
    private static final By SUCCESS_TOAST = By.cssSelector(".p-toast-message-success");
    private static final By PRODUCT_TABLE = By.cssSelector(".manage-container table, p-table table");
    private static final By PRODUCT_ROWS = By.cssSelector("p-table tbody tr, table tbody tr");
    private static final By EDIT_BUTTON = By.cssSelector("p-button[ptoolip='Chỉnh sửa'] button, p-button[ptooltip='Chỉnh sửa'] button");
    private static final By DELETE_BUTTON = By.cssSelector("p-button[ptoolip='Xóa'] button, p-button[ptooltip='Xóa'] button");
    // Actual search input — class="search-input" in product-manage.component.html
    private static final By SEARCH_INPUT = By.cssSelector("input.search-input");
    private static final By ADD_PRODUCT_BUTTON = By.cssSelector(".manage-header button.p-button-success, .header-actions button.p-button-success");

    public ProductManagePage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        String baseUrl = com.lockerkorea.ui.utils.ConfigReader.getString("frontend.url", "http://localhost:4200");
        driver.get(baseUrl + "/productManage");
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        // Check container OR URL contains productManage (covers post-edit/delete states)
        if (isElementDisplayed(PRODUCT_MANAGE_CONTAINER)) return true;
        try {
            String url = driver.getCurrentUrl();
            return url != null && url.contains("productManage");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click the Add Product button to open the form
     */
    public void clickAddProductButton() {
        click(ADD_PRODUCT_BUTTON);
        try {
            Thread.sleep(1000); // wait for dialog to open
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Fill product form for creating/editing
     */
    public void fillProductForm(String name, String category, String price, Integer discount, Integer quantity, String description) {
        if (isElementDisplayed(PRODUCT_NAME_INPUT)) {
            // clear first if needed, though type might do it
            driver.findElement(PRODUCT_NAME_INPUT).clear();
            if (name != null) type(PRODUCT_NAME_INPUT, name);
        }
        if (isElementDisplayed(PRICE_INPUT)) {
            driver.findElement(PRICE_INPUT).clear();
            if (price != null) type(PRICE_INPUT, price);
        }
        if (discount != null && isElementDisplayed(DISCOUNT_INPUT)) {
            driver.findElement(DISCOUNT_INPUT).clear();
            type(DISCOUNT_INPUT, discount.toString());
        }
        if (quantity != null && isElementDisplayed(QUANTITY_INPUT)) {
            driver.findElement(QUANTITY_INPUT).clear();
            type(QUANTITY_INPUT, quantity.toString());
        }
        if (description != null && isElementDisplayed(DESCRIPTION_TEXTAREA)) {
            driver.findElement(DESCRIPTION_TEXTAREA).clear();
            type(DESCRIPTION_TEXTAREA, description);
        }
        // Category selection would need more complex interaction with dropdown
    }

    /**
     * Select category from dropdown
     */
    public void selectCategory(String categoryName) {
        click(CATEGORY_DROPDOWN);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        By optionLocator = By.xpath("//li[contains(@class,'p-dropdown-item') and text()='" + categoryName + "']");
        click(optionLocator);
    }

    /**
     * Select features (multi-select)
     */
    public void selectFeatures(List<String> featureNames) {
        click(FEATURES_MULTISELECT);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (String feature : featureNames) {
            By checkboxLocator = By.xpath("//label[contains(text(),'" + feature + "')]/preceding-sibling::input");
            click(checkboxLocator);
        }
        // Click outside to close dropdown
        click(PRODUCT_NAME_INPUT);
    }

    /**
     * Upload product images
     */
    public void uploadImages(String... filePaths) {
        // File upload with Selenium requires sending file path to input
        try {
            org.openqa.selenium.WebElement fileInput = driver.findElement(By.cssSelector("input[type='file']"));
            for (String filePath : filePaths) {
                fileInput.sendKeys(filePath);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            System.out.println("File upload not supported or file input not found: " + e.getMessage());
        }
    }

    /**
     * Submit product form
     */
    public void submitProduct() {
        click(SUBMIT_BUTTON);
        try {
            Thread.sleep(1000); // wait for API call to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Force submit even if disabled (for testing validation)
     */
    public void submitProductForce() {
        try {
            org.openqa.selenium.WebElement btn = driver.findElement(SUBMIT_BUTTON);
            if (btn.isEnabled() && btn.getAttribute("disabled") == null) {
                btn.click();
            } else {
                // If button is strictly disabled, we can use JS to click or just log it
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            }
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Force click failed: " + e.getMessage());
        }
    }

    /**
     * Check if product form (dialog) is still open
     */
    public boolean isFormOpen() {
        return isElementDisplayed(By.cssSelector("p-dialog .p-dialog-content"));
    }

    /**
     * Check if submit button is disabled
     */
    public boolean isSubmitButtonDisabled() {
        try {
            org.openqa.selenium.WebElement btn = driver.findElement(SUBMIT_BUTTON);
            return !btn.isEnabled() || btn.getAttribute("disabled") != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cancel operation
     */
    public void cancel() {
        click(CANCEL_BUTTON);
    }

    /**
     * Get product count from table
     */
    public int getProductCountInTable() {
        return driver.findElements(PRODUCT_ROWS).size();
    }

    /**
     * Search product by name
     */
    public void searchProduct(String keyword) {
        type(SEARCH_INPUT, keyword);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get product row by name
     */
    public org.openqa.selenium.WebElement getProductRowByName(String productName) {
        List<org.openqa.selenium.WebElement> rows = driver.findElements(PRODUCT_ROWS);
        for (org.openqa.selenium.WebElement row : rows) {
            List<org.openqa.selenium.WebElement> cells = row.findElements(By.tagName("td"));
            for (org.openqa.selenium.WebElement cell : cells) {
                if (cell.getText().trim().equals(productName)) {
                    return row;
                }
            }
        }
        return null;
    }

    /**
     * Edit product by name
     */
    public void editProduct(String productName) {
        org.openqa.selenium.WebElement row = getProductRowByName(productName);
        if (row != null) {
            row.findElement(EDIT_BUTTON).click();
            waitForPageLoad();
        }
    }

    /**
     * Delete product by name with confirmation option
     */
    public void deleteProduct(String productName, boolean confirm) {
        org.openqa.selenium.WebElement row = getProductRowByName(productName);
        if (row != null) {
            row.findElement(DELETE_BUTTON).click();
            // Handle confirmation dialog
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (confirm) {
                click(By.cssSelector(".p-confirmDialog .p-button-primary")); // Xác nhận
                waitForPageLoad();
            } else {
                click(By.cssSelector(".p-confirmDialog .p-button-text")); // Hủy
            }
        }
    }
    
    /**
     * Delete product by name (default confirms)
     */
    public void deleteProduct(String productName) {
        deleteProduct(productName, true);
    }

    /**
     * Check if success toast is displayed
     */
    public boolean isSuccessToastDisplayed() {
        return isElementDisplayed(SUCCESS_TOAST);
    }

    /**
     * Get success message
     */
    public String getSuccessMessage() {
        try {
            return getText(SUCCESS_TOAST);
        } catch (Exception e) {
            return "";
        }
    }
}

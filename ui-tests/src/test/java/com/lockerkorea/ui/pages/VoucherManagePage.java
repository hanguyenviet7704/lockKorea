package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Voucher Management Page (Admin)
 * URL: /voucherManage
 */
public class VoucherManagePage extends BasePage {

    // Locators - Container & Table
    private static final By VOUCHER_MANAGE_CONTAINER = By.cssSelector(".voucher-manage");
    private static final By VOUCHER_TABLE = By.cssSelector(".vouchers-table, table");
    private static final By VOUCHER_ROWS = By.cssSelector(".vouchers-table tbody tr, table tbody tr");
    private static final By VOUCHER_TABLE_INFO = By.cssSelector(".voucher-count, .p-datatable-footer, .table-info");

    // Locators - Form inputs
    private static final By ADD_VOUCHER_BUTTON = By.cssSelector(".add-voucher-btn, .p-button-primary, button[routerLink*='add'], button:has(.pi-plus)");
    private static final By VOUCHER_CODE_INPUT = By.cssSelector("input[formControlName='code'], #code");
    private static final By DISCOUNT_TYPE_DROPDOWN = By.cssSelector("select[formControlName='discountType'], p-dropdown[formControlName='discountType']");
    private static final By DISCOUNT_VALUE_INPUT = By.cssSelector("input[formControlName='discountValue'], #discountValue");
    private static final By MIN_ORDER_INPUT = By.cssSelector("input[formControlName='minOrderAmount'], #minOrderAmount");
    private static final By MAX_DISCOUNT_INPUT = By.cssSelector("input[formControlName='maxDiscount'], #maxDiscount");
    private static final By USAGE_LIMIT_INPUT = By.cssSelector("input[formControlName='usageLimit'], #usageLimit");
    private static final By EXPIRY_DATE_INPUT = By.cssSelector("input[formControlName='expiryDate'], input[type='date'], #expiryDate");
    private static final By VALID_FROM_INPUT = By.cssSelector("input[type='datetime-local']");
    private static final By ACTIVE_CHECKBOX = By.cssSelector("input[formControlName='active'], p-checkbox[formControlName='active']");

    // Locators - Buttons
    private static final By SAVE_BUTTON = By.cssSelector("button[type='submit'], .save-btn, .p-button-success");
    private static final By UPDATE_BUTTON = By.cssSelector(".update-btn, button:has(.pi-check), .p-button-success");
    private static final By CANCEL_BUTTON = By.cssSelector(".cancel-btn, .p-button-secondary, button:has(.pi-times)");
    private static final By EDIT_BUTTON = By.cssSelector(".edit-btn, .p-button-warning, button:has(.pi-pencil)");
    private static final By DELETE_BUTTON = By.cssSelector(".delete-btn, .p-button-danger, button:has(.pi-trash)");
    private static final By SEARCH_INPUT = By.cssSelector("input[type='search'], input[placeholder*='Search'], input[formControlName='search']");

    // Locators - Confirm Dialog
    private static final By CONFIRM_DIALOG = By.cssSelector(".p-confirmDialog, .confirm-dialog, .p-dialog");
    private static final By CONFIRM_YES_BUTTON = By.cssSelector(".p-confirmDialog .p-button-primary, .confirm-dialog .p-button-danger, .p-dialog .p-button-danger");
    private static final By CONFIRM_NO_BUTTON = By.cssSelector(".p-confirmDialog .p-button-secondary, .confirm-dialog .p-button-secondary, .p-dialog .p-button-secondary");

    private static final By TOAST_SUCCESS = By.cssSelector(".p-toast-message-success, .toast-success, .alert-success");
    private static final By TOAST_ERROR   = By.cssSelector(".p-toast-message-error, .toast-error, .alert-danger, .error-message");
    private static final By ERROR_MESSAGES = By.cssSelector(".p-error, .error-text, .invalid-feedback, .text-danger");
    // Shorter CSS selector for toast detail text (PrimeNG)
    private static final By TOAST_DETAIL  = By.cssSelector(".p-toast-detail");

    // Locators - Pagination
    private static final By PAGINATION_INFO = By.cssSelector(".p-paginator, .pagination, .p-datatable-footer");
    private static final By PAGE_INFO_TEXT = By.cssSelector(".p-paginator-current, .page-info, .total-records");

    public VoucherManagePage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        String baseUrl = com.lockerkorea.ui.utils.ConfigReader.getString("frontend.url", "http://localhost:4200");
        driver.get(baseUrl + "/voucherManage");
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        return isElementDisplayed(VOUCHER_MANAGE_CONTAINER) ||
               isElementDisplayed(VOUCHER_TABLE);
    }

    // ==================== Table Operations ====================

    public int getVoucherCount() {
        return driver.findElements(VOUCHER_ROWS).size();
    }

    public List<String> getAllVoucherCodes() {
        List<String> codes = new java.util.ArrayList<>();
        List<WebElement> rows = driver.findElements(VOUCHER_ROWS);
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (!cells.isEmpty()) {
                codes.add(cells.get(0).getText().trim());
            }
        }
        return codes;
    }

    public boolean voucherExists(String code) {
        // Search by exact code first (reduces false negatives when table is paginated)
        searchVoucher(code);
        List<String> codes = getAllVoucherCodes();
        // Reset search after check
        return codes.stream().anyMatch(c -> c.equalsIgnoreCase(code));
    }

    /**
     * Get voucher row data by code
     * Returns a map: id, code, type, value, qty, expiryDate
     */
    public java.util.Map<String, String> getVoucherRowData(String code) {
        List<WebElement> rows = driver.findElements(VOUCHER_ROWS);
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (!cells.isEmpty() && cells.get(0).getText().trim().equalsIgnoreCase(code)) {
                java.util.Map<String, String> data = new java.util.LinkedHashMap<>();
                for (int i = 0; i < cells.size(); i++) {
                    data.put("cell_" + i, cells.get(i).getText().trim());
                }
                return data;
            }
        }
        return null;
    }

    // ==================== Pagination ====================

    public String getPaginationInfo() {
        if (isElementDisplayed(PAGINATION_INFO)) {
            return driver.findElement(PAGINATION_INFO).getText();
        }
        return "";
    }

    public int getTotalVoucherCountFromFooter() {
        String info = getPaginationInfo();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)(tổng|total|count)\\s*[:\\s]*(\\d+)");
        java.util.regex.Matcher m = p.matcher(info);
        if (m.find()) {
            return Integer.parseInt(m.group(2));
        }
        return getVoucherCount();
    }

    // ==================== Search ====================

    public void searchVoucher(String code) {
        try {
            waitForElementVisible(SEARCH_INPUT);
            WebElement searchBox = driver.findElement(SEARCH_INPUT);
            searchBox.clear();
            if (code != null && !code.isEmpty()) {
                type(SEARCH_INPUT, code);
                searchBox.sendKeys(org.openqa.selenium.Keys.ENTER);
            }
            // Wait for table to re-render after search
            try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            System.out.println("Search input not found: " + e.getMessage());
        }
    }

    public void clearSearch() {
        searchVoucher("");
    }

    // ==================== Add Voucher Form ====================

    public void clickAddVoucher() {
        click(ADD_VOUCHER_BUTTON);
        waitForPageLoad();
    }

    public void fillVoucherForm(String code, String discountType, String value,
                                 String usageLimit, String expiryDate) {
        if (code != null) {
            type(VOUCHER_CODE_INPUT, code);
        }
        if (discountType != null) {
            selectDiscountType(discountType);
        }
        if (value != null) {
            type(DISCOUNT_VALUE_INPUT, value);
        }
        if (usageLimit != null) {
            type(USAGE_LIMIT_INPUT, usageLimit);
        }
        if (expiryDate != null) {
            type(EXPIRY_DATE_INPUT, expiryDate);
        }
    }

    private void selectDiscountType(String discountType) {
        click(DISCOUNT_TYPE_DROPDOWN);
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        By optionLocator = By.xpath("//option[contains(text(),'" + discountType + "')] | //li[contains(text(),'" + discountType + "')]");
        click(optionLocator);
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void clickSaveVoucher() {
        waitForElementClickable(SAVE_BUTTON);
        click(SAVE_BUTTON);
        // Wait for API response and toast
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void addVoucher(String code, String discountType, double value, double minOrder,
                           double maxDiscount, int usageLimit, boolean active) {
        clickAddVoucher();
        fillVoucherForm(code, discountType, String.valueOf(value), String.valueOf(usageLimit), null);
        if (minOrder > 0) type(MIN_ORDER_INPUT, String.valueOf(minOrder));
        if (maxDiscount > 0) type(MAX_DISCOUNT_INPUT, String.valueOf(maxDiscount));
        clickSaveVoucher();
    }

    // ==================== Edit Voucher Form ====================

    public void clickEditVoucher(String code) {
        List<WebElement> rows = driver.findElements(VOUCHER_ROWS);
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (!cells.isEmpty() && cells.get(0).getText().trim().equalsIgnoreCase(code)) {
                row.findElement(EDIT_BUTTON).click();
                waitForElementVisible(VOUCHER_CODE_INPUT);
                return;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("Voucher not found in table: " + code);
    }

    public void editVoucherUsageLimit(String code, String newLimit) {
        clickEditVoucher(code);
        type(USAGE_LIMIT_INPUT, newLimit);
        clickUpdateVoucher();
    }

    public void editVoucherValue(String code, String newValue) {
        clickEditVoucher(code);
        type(DISCOUNT_VALUE_INPUT, newValue);
        clickUpdateVoucher();
    }

    public void clickUpdateVoucher() {
        click(UPDATE_BUTTON);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void clickCancelEdit() {
        click(CANCEL_BUTTON);
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ==================== Delete Voucher ====================

    public void clickDeleteVoucher(String code) {
        searchVoucher("");
        List<WebElement> rows = driver.findElements(VOUCHER_ROWS);
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (!cells.isEmpty() && cells.get(0).getText().trim().equals(code)) {
                WebElement deleteBtn = row.findElement(DELETE_BUTTON);
                click(deleteBtn);
                try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return;
            }
        }
    }

    public void confirmDelete() {
        waitForElementClickable(CONFIRM_YES_BUTTON);
        click(CONFIRM_YES_BUTTON);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void cancelDelete() {
        waitForElementClickable(CONFIRM_NO_BUTTON);
        click(CONFIRM_NO_BUTTON);
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void deleteVoucher(String code) {
        clickDeleteVoucher(code);
        confirmDelete();
    }

    // ==================== Toast & Error Messages ====================

    public String getToastMessage() {
        // Try success first, then error
        String msg = waitForToast(TOAST_SUCCESS);
        if (!msg.isEmpty()) return msg;
        msg = waitForToast(TOAST_ERROR);
        return msg;
    }

    public String getErrorMessage() {
        try {
            if (isElementDisplayed(ERROR_MESSAGES)) {
                return driver.findElement(ERROR_MESSAGES).getText();
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    public boolean isSuccessToastDisplayed() {
        return !waitForToast(TOAST_SUCCESS).isEmpty();
    }

    public boolean isErrorToastDisplayed() {
        return !waitForToast(TOAST_ERROR).isEmpty() || !waitForToast(ERROR_MESSAGES).isEmpty();
    }

    // ==================== Navigation ====================

    public void refreshPage() {
        driver.navigate().refresh();
        waitForPageLoad();
    }
}

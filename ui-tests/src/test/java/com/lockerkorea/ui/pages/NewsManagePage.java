package com.lockerkorea.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;

/**
 * News Management Page (Admin)
 * URL: /newsManage
 */
public class NewsManagePage extends BasePage {

    // Locators
    private static final By NEWS_CONTAINER = By.cssSelector(".card");
    private static final By NEWS_TABLE = By.cssSelector("p-table, .p-datatable");
    private static final By NEWS_ROWS = By.cssSelector("p-table tbody tr, .p-datatable tbody tr");
    private static final By ADD_NEWS_BUTTON = By.cssSelector("button[label='Thêm tin tức']");
    private static final By TITLE_INPUT = By.id("title");
    private static final By CONTENT_TEXTAREA = By.cssSelector("textarea[formControlName='content']");
    private static final By CATEGORY_INPUT = By.id("category");
    private static final By STATUS_DROPDOWN = By.id("status");
    private static final By SAVE_BUTTON = By.cssSelector("p-dialog button[label='Lưu']");
    private static final By CANCEL_BUTTON = By.cssSelector("p-dialog button[label='Hủy']");
    private static final By EDIT_BUTTON = By.cssSelector("button[icon='pi pi-pencil']");
    private static final By DELETE_BUTTON = By.cssSelector("button[icon='pi pi-trash']");
    private static final By CONFIRM_DELETE_BUTTON = By.cssSelector(".p-confirmDialog .p-button-primary");

    public NewsManagePage(org.openqa.selenium.WebDriver driver) {
        super(driver);
    }

    @Override
    public void navigateTo() {
        String baseUrl = com.lockerkorea.ui.utils.ConfigReader.getString("frontend.url", "http://localhost:4200");
        driver.get(baseUrl + "/newsManage");
        waitForPageLoad();
    }

    @Override
    public boolean isLoaded() {
        return isElementDisplayed(NEWS_CONTAINER) ||
               isElementDisplayed(NEWS_TABLE);
    }

    /**
     * Check if any dialog is displayed (using JavaScript for PrimeNG dialog)
     */
    public boolean isDialogDisplayed() {
        try {
            String script = "return document.querySelector('p-dialog .p-dialog-content') != null && " +
                          "window.getComputedStyle(document.querySelector('p-dialog')).display !== 'none'";
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            // Fallback: check if dialog mask is present
            try {
                return driver.findElements(By.cssSelector(".p-dialog-mask")).size() > 0;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Get number of news articles
     */
    public int getNewsCount() {
        try {
            return driver.findElements(NEWS_ROWS).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Click add news button
     */
    public void clickAddNewsButton() {
        waitForElementClickable(ADD_NEWS_BUTTON);
        click(ADD_NEWS_BUTTON);
        // Wait for dialog to open
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Add new news article
     */
    public void addNews(String title, String content, boolean publish) {
        clickAddNewsButton();
        fillNewsForm(title, content, null, publish);
        clickSaveButton();
    }

    /**
     * Add news with category
     */
    public void addNewsWithCategory(String title, String content, String category, boolean publish) {
        clickAddNewsButton();
        fillNewsForm(title, content, category, publish);
        clickSaveButton();
    }

    /**
     * Add news without title (validation test)
     */
    public void addNewsWithoutTitle(String content) {
        clickAddNewsButton();
        // Only fill content, skip title
        try {
            WebElement contentArea = driver.findElement(CONTENT_TEXTAREA);
            contentArea.clear();
            contentArea.sendKeys(content);
        } catch (Exception e) {
            // Ignore
        }
        // Don't click save - button should be disabled
        // Just verify the error message is shown
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Check if save button is enabled
     */
    public boolean isSaveButtonEnabled() {
        try {
            WebElement saveBtn = driver.findElement(SAVE_BUTTON);
            return saveBtn.isEnabled() && saveBtn.getDomAttribute("disabled") == null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Search news by keyword
     */
    public void searchNews(String keyword) {
        try {
            WebElement searchInput = driver.findElement(By.cssSelector("input[type='text'], .p-inputtext[placeholder*='tìm']"));
            searchInput.clear();
            searchInput.sendKeys(keyword);
            searchInput.sendKeys(org.openqa.selenium.Keys.ENTER);
            try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            // Ignore if search not available
        }
    }

    /**
     * Fill the news form
     */
    public void fillNewsForm(String title, String content, String category, boolean publish) {
        // Fill title
        if (title != null) {
            type(TITLE_INPUT, title);
        }

        // Fill content
        if (content != null) {
            try {
                WebElement contentArea = driver.findElement(CONTENT_TEXTAREA);
                contentArea.clear();
                contentArea.sendKeys(content);
            } catch (Exception e) {
                // Ignore if not found
            }
        }

        // Fill category
        if (category != null) {
            try {
                type(CATEGORY_INPUT, category);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Select status
        if (publish) {
            selectStatus("PUBLISHED");
        }
    }

    /**
     * Click save button and wait
     */
    private void clickSaveButton() {
        // Wait a bit for form validation
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        waitForElementClickable(SAVE_BUTTON);
        click(SAVE_BUTTON);
        // Wait for dialog to close
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Force submit form for validation tests
     */
    public void submitFormForce() {
        try {
            WebElement saveBtn = driver.findElement(SAVE_BUTTON);
            if (saveBtn.isEnabled() && saveBtn.getAttribute("disabled") == null) {
                saveBtn.click();
            } else {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", saveBtn);
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            System.out.println("Force submit failed: " + e.getMessage());
        }
    }

    /**
     * Select status from PrimeNG dropdown
     */
    private void selectStatus(String status) {
        try {
            click(STATUS_DROPDOWN);
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            click(By.xpath("//li[contains(@class,'p-dropdown-item') and contains(text(),'" + status + "')]"));
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            // Ignore if dropdown not found
        }
    }

    /**
     * Cancel/close dialog
     */
    public void cancelDialog() {
        try {
            click(CANCEL_BUTTON);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            // Dialog might already be closed
        }
    }

    /**
     * Get all news titles
     */
    public List<String> getAllNewsTitles() {
        List<String> titles = new ArrayList<>();
        List<WebElement> rows = driver.findElements(NEWS_ROWS);
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() > 1) {
                titles.add(cells.get(1).getText().trim());
            }
        }
        return titles;
    }

    /**
     * Delete news by title
     */
    public void deleteNews(String title, boolean confirm) {
        List<WebElement> rows = driver.findElements(NEWS_ROWS);
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() > 1 && cells.get(1).getText().trim().equals(title)) {
                row.findElement(DELETE_BUTTON).click();
                try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                // Confirm delete - try multiple selectors for PrimeNG confirm dialog
                if (confirm) {
                    try {
                        // Try clicking the confirm button in the dialog
                        WebElement confirmBtn = driver.findElement(By.cssSelector(".p-confirmDialog .p-button-primary, .p-dialog-mask .p-button-primary, .p-component-overlay .p-button-primary"));
                        confirmBtn.click();
                    } catch (Exception e) {
                        // If not found, try by text
                        try {
                            click(By.xpath("//button[contains(text(),'Có') or contains(text(),'Yes') or contains(text(),'Đồng ý')]"));
                        } catch (Exception e2) {
                            // Ignore
                        }
                    }
                } else {
                    try {
                        // Click Cancel button
                        WebElement rejectBtn = driver.findElement(By.cssSelector(".p-confirmDialog .p-button-text, .p-dialog-mask .p-button-text"));
                        rejectBtn.click();
                    } catch (Exception e) {
                        try {
                            click(By.xpath("//button[contains(text(),'Không') or contains(text(),'No') or contains(text(),'Hủy')]"));
                        } catch (Exception e2) {
                            // Ignore
                        }
                    }
                }
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return;
            }
        }
    }

    /**
     * Legacy deleteNews wrapper
     */
    public void deleteNews(String title) {
        deleteNews(title, true);
    }

    /**
     * Get error message for title validation
     */
    public String getTitleErrorMessage() {
        try {
            WebElement error = driver.findElement(By.cssSelector("small.text-danger, .p-error, .error-message"));
            return error.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if add/edit dialog is displayed
     */
    public boolean isAddDialogDisplayed() {
        try {
            return driver.findElement(By.cssSelector("p-dialog[header*='Thêm']")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if edit dialog is displayed
     */
    public boolean isEditDialogDisplayed() {
        try {
            return driver.findElement(By.cssSelector("p-dialog[header*='Chỉnh']")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click edit button for first row
     */
    public void clickEditButton(int rowIndex) {
        List<WebElement> editButtons = driver.findElements(EDIT_BUTTON);
        if (rowIndex < editButtons.size()) {
            editButtons.get(rowIndex).click();
            try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}

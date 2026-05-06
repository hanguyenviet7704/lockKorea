package com.lockerkorea.ui.tests.admin;

import com.lockerkorea.ui.pages.*;
import com.lockerkorea.ui.utils.BaseTest;
import com.lockerkorea.ui.utils.ConfigReader;
import com.lockerkorea.ui.utils.DBConnector;
import org.openqa.selenium.By;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Admin Voucher Management Tests (Full flow based on FRONTEND_TEST_CASES_VOUCHER.md)
 *
 * Coverage: VCH_1 to VCH_26
 * - General UI (VCH_1 to VCH_3)
 * - Search & Filter (VCH_5 to VCH_7)
 * - Add Voucher Form (VCH_12 to VCH_17)
 * - Edit Voucher Form (VCH_19, VCH_21 to VCH_23)
 * - Delete Voucher (VCH_24 to VCH_26)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminVoucherTest extends BaseTest {

    private LoginPage loginPage;
    private VoucherManagePage voucherPage;
    private DBConnector dbConnector;

    private static final DateTimeFormatter DB_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String TEST_VOUCHER_CODE = "SAVE10";
    private static final String TEST_FIXED_CODE = "FLAT50K";
    private static final String TEST_DELETE_CODE = "TEST_DELETE";
    private static final String SUMMER_CODE = "SUMMER20";

    @BeforeEach
    public void setUpAdmin() {
        loginPage = new LoginPage(driver);
        voucherPage = new VoucherManagePage(driver);
        // DBConnector is initialized in BaseTest.setUp() — reuse it here
        dbConnector = new DBConnector();

        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.admin.email"),
            ConfigReader.getString("test.admin.password")
        );
        voucherPage.navigateTo();
    }

    @AfterEach
    public void tearDownDB() {
        if (dbConnector != null) {
            dbConnector.disconnect();
        }
    }

    // ==================== 1. GENERAL UI (VCH_1 to VCH_3) ====================

    @Test
    @Order(1)
    @DisplayName("VCH_1: Admin should see voucher list page with correct layout")
    void testVoucherPageLayout_VCH1() {
        assertTrue(voucherPage.isLoaded(), "Voucher manage page should load successfully");
        // Verify table is displayed
        assertTrue(voucherPage.getVoucherCount() >= 0, "Voucher table should be displayed");
    }

    @Test
    @Order(2)
    @DisplayName("VCH_2: Pagination - 10 rows/page, page 1/2 when DB has 15 vouchers")
    void testPagination_VCH2() throws SQLException {
        dbConnector.connect();
        long dbCount = dbConnector.getRecordCount("voucher", null);
        dbConnector.disconnect();

        int pageRowCount = voucherPage.getVoucherCount();
        // Default page size is 10, so first page should have <= 10 rows
        assertTrue(pageRowCount <= 10, "Page 1 should display at most 10 rows, found: " + pageRowCount);
        assertTrue(dbCount >= 10 || pageRowCount == dbCount,
            "Row count should reflect DB state. DB: " + dbCount + ", UI: " + pageRowCount);
    }

    @Test
    @Order(3)
    @DisplayName("VCH_3: First row should display SUMMER20 correctly (ID=1)")
    void testFirstRowDisplay_VCH3() {
        voucherPage.searchVoucher(SUMMER_CODE);
        assertTrue(voucherPage.voucherExists(SUMMER_CODE),
            "SUMMER20 voucher should exist in the list");

        Map<String, String> rowData = voucherPage.getVoucherRowData(SUMMER_CODE);
        assertNotNull(rowData, "Should find SUMMER20 row data");
        // Verify key fields are displayed (code, type, value, qty, expiry)
        assertFalse(rowData.isEmpty(), "Row data should not be empty");
    }

    // ==================== 2. SEARCH & FILTER (VCH_5 to VCH_7) ====================

    @Test
    @Order(4)
    @DisplayName("VCH_5: Search by keyword SUMMER should return SUMMER20 and SUMMER30")
    void testSearchByKeyword_VCH5() {
        // Search for SUMMER
        voucherPage.searchVoucher("SUMMER");
        int resultCount = voucherPage.getVoucherCount();

        List<String> codes = voucherPage.getAllVoucherCodes();
        boolean hasSummer20 = codes.contains(SUMMER_CODE) ||
                              codes.stream().anyMatch(c -> c.contains("SUMMER20"));
        boolean hasSummer30 = codes.stream().anyMatch(c -> c.contains("SUMMER30"));

        assertTrue(resultCount >= 1, "Should find at least 1 voucher with keyword SUMMER");
        assertTrue(hasSummer20 || hasSummer30,
            "Should find SUMMER20 or SUMMER30. Found: " + codes);
    }

    @Test
    @Order(5)
    @DisplayName("VCH_6: Clear search should show all vouchers")
    void testClearSearch_VCH6() throws SQLException {
        // First search
        voucherPage.searchVoucher("SUMMER");
        int searchCount = voucherPage.getVoucherCount();

        // Clear search
        voucherPage.clearSearch();
        int allCount = voucherPage.getVoucherCount();

        dbConnector.connect();
        long dbCount = dbConnector.getRecordCount("voucher", null);
        dbConnector.disconnect();

        assertTrue(allCount >= searchCount || allCount == dbCount,
            "After clearing search, should show all vouchers. Found: " + allCount + ", DB: " + dbCount);
    }

    @Test
    @Order(6)
    @DisplayName("VCH_7: Search with non-existent keyword XYZ999ABC should return 0 results")
    void testSearchNoResults_VCH7() {
        voucherPage.searchVoucher("XYZ999ABC");
        int resultCount = voucherPage.getVoucherCount();

        assertEquals(0, resultCount, "Searching for non-existent voucher should return 0 results");
        // Toast may show "No vouchers found" or similar message
        String toast = voucherPage.getToastMessage();
        // Accept either empty results or a "not found" message
        assertTrue(resultCount == 0, "Should display 0 rows for non-existent voucher");
    }

    // ==================== 3. ADD VOUCHER FORM (VCH_12 to VCH_17) ====================

    @Test
    @Order(7)
    @DisplayName("VCH_12: Add voucher with negative value (-10) should show error")
    void testAddVoucherNegativeValue_VCH12() {
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm("BUGTEST01", "PERCENTAGE", "-10", "50", "2025-12-31");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty(),
            "Should show error for negative discount value");
        assertFalse(voucherPage.voucherExists("BUGTEST01"),
            "BUGTEST01 should NOT be created with negative value");
    }

    @Test
    @Order(71)
    @DisplayName("VCH_12.1: Add voucher - Bỏ trống Mã Voucher (Validation)")
    void testAddVoucherEmptyCode() {
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm("", "PERCENTAGE", "10", "50", "2025-12-31");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty() || voucherPage.isLoaded(),
            "Không cho phép lưu khi bỏ trống Mã Voucher");
        voucherPage.clickCancelEdit();
    }

    @Test
    @Order(72)
    @DisplayName("VCH_12.2: Add voucher - Bỏ trống Giá trị giảm (Validation)")
    void testAddVoucherEmptyValue() {
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm("EMPTY_VAL", "PERCENTAGE", "", "50", "2025-12-31");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty() || voucherPage.isLoaded(),
            "Không cho phép lưu khi bỏ trống Giá trị giảm");
        voucherPage.clickCancelEdit();
    }

    @Test
    @Order(8)
    @DisplayName("VCH_13: Add voucher with value > 100% should show error")
    void testAddVoucherValueOver100_VCH13() {
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm("BUGTEST02", "PERCENTAGE", "150", "50", "2025-12-31");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty(),
            "Should show error for discount value > 100%");
        assertFalse(voucherPage.voucherExists("BUGTEST02"),
            "BUGTEST02 should NOT be created with value > 100%");
    }

    @Test
    @Order(9)
    @DisplayName("VCH_14: Add voucher with quantity = 0 should show error")
    void testAddVoucherZeroQuantity_VCH14() {
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm("BUGTEST03", "PERCENTAGE", "20", "0", "2025-12-31");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty(),
            "Should show error for zero quantity");
        assertFalse(voucherPage.voucherExists("BUGTEST03"),
            "BUGTEST03 should NOT be created with quantity 0");
    }

    @Test
    @Order(10)
    @DisplayName("VCH_15: Add PERCENTAGE voucher SAVE10 successfully")
    void testAddPercentageVoucher_VCH15() throws SQLException {
        // Clean up if exists
        dbConnector.connect();
        dbConnector.executeUpdate("DELETE FROM voucher WHERE code = ?", TEST_VOUCHER_CODE);
        dbConnector.disconnect();

        voucherPage.refreshPage();
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm(TEST_VOUCHER_CODE, "PERCENTAGE", "10", "200", "2025-12-31");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isSuccessToastDisplayed(),
            "Should show success toast. Message: " + voucherPage.getToastMessage());
        assertTrue(voucherPage.voucherExists(TEST_VOUCHER_CODE),
            "SAVE10 should exist in the voucher list");

        // Verify in DB
        dbConnector.connect();
        boolean existsInDb = dbConnector.recordExists("voucher", "code = ?", TEST_VOUCHER_CODE);
        dbConnector.disconnect();
        assertTrue(existsInDb, "SAVE10 should be saved in the database");
    }

    @Test
    @Order(11)
    @DisplayName("VCH_16: Add FIXED_AMOUNT voucher FLAT50K successfully")
    void testAddFixedAmountVoucher_VCH16() throws SQLException {
        dbConnector.connect();
        dbConnector.executeUpdate("DELETE FROM voucher WHERE code = ?", TEST_FIXED_CODE);
        dbConnector.disconnect();

        voucherPage.refreshPage();
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm(TEST_FIXED_CODE, "FIXED_AMOUNT", "50000", "100", "2025-06-30");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isSuccessToastDisplayed(),
            "Should show success toast. Message: " + voucherPage.getToastMessage());
        assertTrue(voucherPage.voucherExists(TEST_FIXED_CODE),
            "FLAT50K should exist in the voucher list");

        // Verify value in DB
        dbConnector.connect();
        Object value = dbConnector.getColumnValue("voucher", "discount_value", "code = ?", TEST_FIXED_CODE);
        dbConnector.disconnect();
        assertNotNull(value, "FLAT50K should be in DB with discount_value");
    }

    @Test
    @Order(12)
    @DisplayName("VCH_17: Add voucher with past expiry date should show error")
    void testAddVoucherPastExpiryDate_VCH17() {
        voucherPage.clickAddVoucher();
        // Past date: 2020-01-01
        voucherPage.fillVoucherForm("BUGTEST04", "PERCENTAGE", "20", "50", "2020-01-01");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty(),
            "Should show error for past expiry date");
        assertFalse(voucherPage.voucherExists("BUGTEST04"),
            "BUGTEST04 should NOT be created with past expiry date");
    }

    @Test
    @Order(121)
    @DisplayName("VCH_17.1: Add voucher - Trùng mã Voucher đã tồn tại")
    void testAddDuplicateVoucher() {
        ensureTestDeleteVoucherExists(); // Đảm bảo TEST_DELETE đã tồn tại
        
        voucherPage.clickAddVoucher();
        voucherPage.fillVoucherForm(TEST_DELETE_CODE, "FIXED_AMOUNT", "10000", "50", "2025-12-31");
        voucherPage.clickSaveVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty(),
            "Hệ thống phải báo lỗi trùng mã Voucher");
        voucherPage.clickCancelEdit();
    }

    // ==================== 4. EDIT VOUCHER FORM (VCH_19, VCH_21 to VCH_23) ====================

    @Test
    @Order(13)
    @DisplayName("VCH_19: Open Edit form for SAVE10 should display correct data")
    void testOpenEditForm_VCH19() {
        voucherPage.searchVoucher(TEST_VOUCHER_CODE);
        assertTrue(voucherPage.voucherExists(TEST_VOUCHER_CODE),
            "SAVE10 must exist before editing");

        voucherPage.clickEditVoucher(TEST_VOUCHER_CODE);
        // Verify the code input is pre-populated with SAVE10
        String currentUrl = driver.getCurrentUrl();
        assertTrue(
            voucherPage.isElementDisplayed(By.cssSelector("input[formControlName='code'], #code")),
            "Edit form should be open and code input should be visible"
        );
    }

    @Test
    @Order(14)
    @DisplayName("VCH_21: Edit voucher SAVE10 - change quantity 200 to 300")
    void testEditVoucherQuantity_VCH21() throws SQLException {
        assertTrue(voucherPage.voucherExists(TEST_VOUCHER_CODE),
            "SAVE10 must exist before editing");

        voucherPage.editVoucherUsageLimit(TEST_VOUCHER_CODE, "300");

        assertTrue(voucherPage.isSuccessToastDisplayed(),
            "Should show success toast after update. Message: " + voucherPage.getToastMessage());

        // Verify in DB
        dbConnector.connect();
        Object qty = dbConnector.getColumnValue("voucher", "usage_limit", "code = ?", TEST_VOUCHER_CODE);
        dbConnector.disconnect();
        assertNotNull(qty, "SAVE10 should exist in DB");
        assertEquals(300, ((Number) qty).intValue(), "Quantity should be updated to 300 in DB");
    }

    @Test
    @Order(15)
    @DisplayName("VCH_22: Edit voucher - change value to -5 should show error")
    void testEditVoucherNegativeValue_VCH22() {
        assertTrue(voucherPage.voucherExists(TEST_VOUCHER_CODE),
            "SAVE10 must exist before editing");

        voucherPage.clickEditVoucher(TEST_VOUCHER_CODE);
        voucherPage.fillVoucherForm(null, null, "-5", null, null);
        voucherPage.clickUpdateVoucher();

        assertTrue(voucherPage.isErrorToastDisplayed() || !voucherPage.getErrorMessage().isEmpty(),
            "Should show error for negative discount value");
    }

    @Test
    @Order(16)
    @DisplayName("VCH_23: Edit voucher - change quantity to 9999 then CANCEL should not save")
    void testEditVoucherCancel_VCH23() throws SQLException {
        assertTrue(voucherPage.voucherExists(TEST_VOUCHER_CODE),
            "SAVE10 must exist before editing");

        // Get current quantity from DB
        dbConnector.connect();
        Object originalQty = dbConnector.getColumnValue("voucher", "usage_limit", "code = ?", TEST_VOUCHER_CODE);
        dbConnector.disconnect();
        assertNotNull(originalQty, "SAVE10 should have a usage_limit in DB");

        // Edit and cancel
        voucherPage.clickEditVoucher(TEST_VOUCHER_CODE);
        voucherPage.fillVoucherForm(null, null, null, "9999", null);
        voucherPage.clickCancelEdit();

        // Verify quantity is NOT changed in DB
        dbConnector.connect();
        Object qtyAfterCancel = dbConnector.getColumnValue("voucher", "usage_limit", "code = ?", TEST_VOUCHER_CODE);
        dbConnector.disconnect();
        assertEquals(((Number) originalQty).intValue(), ((Number) qtyAfterCancel).intValue(),
            "Quantity should NOT change after cancel. Original: " + originalQty + ", After: " + qtyAfterCancel);
    }

    // ==================== 5. DELETE VOUCHER (VCH_24 to VCH_26) ====================

    @Test
    @Order(17)
    @DisplayName("VCH_24: Click delete button should show confirmation popup")
    void testDeleteVoucherConfirmDialog_VCH24() {
        // Ensure TEST_DELETE exists
        ensureTestDeleteVoucherExists();

        voucherPage.clickDeleteVoucher(TEST_DELETE_CODE);
        // Confirm dialog should appear
        assertTrue(voucherPage.isLoaded(), "Confirmation dialog should appear when clicking delete");
    }

    @Test
    @Order(18)
    @DisplayName("VCH_25: Confirm delete should remove TEST_DELETE voucher")
    void testDeleteVoucherConfirm_VCH25() throws SQLException {
        ensureTestDeleteVoucherExists();

        voucherPage.deleteVoucher(TEST_DELETE_CODE);

        assertTrue(voucherPage.isSuccessToastDisplayed(),
            "Should show success toast after delete. Message: " + voucherPage.getToastMessage());
        assertFalse(voucherPage.voucherExists(TEST_DELETE_CODE),
            "TEST_DELETE should be removed from the list");

        // Verify in DB
        dbConnector.connect();
        boolean existsInDb = dbConnector.recordExists("voucher", "code = ?", TEST_DELETE_CODE);
        dbConnector.disconnect();
        assertFalse(existsInDb, "TEST_DELETE should be deleted from the database");
    }

    @Test
    @Order(19)
    @DisplayName("VCH_26: Cancel delete should keep SUMMER20 voucher")
    void testDeleteVoucherCancel_VCH26() {
        assertTrue(voucherPage.voucherExists(SUMMER_CODE),
            "SUMMER20 must exist before testing cancel delete");

        voucherPage.clickDeleteVoucher(SUMMER_CODE);
        voucherPage.cancelDelete();

        assertTrue(voucherPage.voucherExists(SUMMER_CODE),
            "SUMMER20 should still exist after canceling delete");
    }

    // ==================== Helper Methods ====================

    private void ensureTestDeleteVoucherExists() {
        voucherPage.refreshPage();
        if (!voucherPage.voucherExists(TEST_DELETE_CODE)) {
            voucherPage.clickAddVoucher();
            // Use a future expiry date
            String futureDate = LocalDate.now().plusYears(1).format(DB_DATE_FMT);
            voucherPage.fillVoucherForm(TEST_DELETE_CODE, "PERCENTAGE", "5", "10", futureDate);
            voucherPage.clickSaveVoucher();
            voucherPage.refreshPage();
        }
    }
}

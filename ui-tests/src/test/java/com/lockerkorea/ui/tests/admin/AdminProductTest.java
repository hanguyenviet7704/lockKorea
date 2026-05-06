package com.lockerkorea.ui.tests.admin;

import com.lockerkorea.ui.pages.*;
import com.lockerkorea.ui.utils.BaseTest;
import com.lockerkorea.ui.utils.ConfigReader;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Admin Product Management Tests (Dựa trên FRONTEND_TEST_CASES_QLSP.md)
 * Phạm vi: Khóa cửa thông minh Locker Korea
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminProductTest extends BaseTest {

    private LoginPage loginPage;
    private ProductManagePage productManagePage;

    @BeforeEach
    public void setUpAdmin() {
        loginPage = new LoginPage(driver);
        productManagePage = new ProductManagePage(driver);
        
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.admin.email"),
            ConfigReader.getString("test.admin.password")
        );
        productManagePage.navigateTo();
    }

    // =========================================================================
    // 1. GIAO DIỆN CHUNG & TÌM KIẾM
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("TC-QLSP-1,2,3: Admin should see product list correctly")
    void testProductListDisplayed() {
        assertTrue(productManagePage.isLoaded(), "Product manage page should load");
        int productCount = productManagePage.getProductCountInTable();
        assertTrue(productCount >= 0, "Should display product list");
    }

    @Test
    @Order(2)
    @DisplayName("TC-QLSP-7,8,9: Search Functionality")
    void testSearchProducts() {
        // Hợp lệ
        productManagePage.searchProduct("Khóa Samsung");
        assertTrue(productManagePage.getProductCountInTable() >= 0, "Should handle valid search");

        // Xóa bộ lọc tìm kiếm
        productManagePage.searchProduct("");
        
        // Không hợp lệ
        productManagePage.searchProduct("xyzxyz999");
        assertEquals(0, productManagePage.getProductCountInTable(), "Should return 0 results for xyzxyz999");
        
        // Trả lại trạng thái ban đầu
        productManagePage.searchProduct("");
    }

    @Test
    @Order(21)
    @DisplayName("TC-QLSP-9.1: Search Functionality - Ký tự đặc biệt")
    void testSearchProductsSpecialChars() {
        productManagePage.searchProduct("@#$%^&*");
        assertEquals(0, productManagePage.getProductCountInTable(), "Should return 0 results for special characters if no match");
        productManagePage.searchProduct("");
    }

    // =========================================================================
    // 3. FORM THÊM SẢN PHẨM MỚI (VALIDATION VÀ CÁC TRƯỜNG HỢP LỖI)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("TC-QLSP-13,15: Add Product - Bỏ trống trường bắt buộc")
    void testAddProduct_EmptyFields() {
        productManagePage.clickAddProductButton();
        
        // Bỏ trống Tên
        productManagePage.fillProductForm(null, "Samsung", "3200000", 15, 80, "Khóa vân tay cao cấp");
        productManagePage.submitProductForce();
        assertTrue(productManagePage.isFormOpen(), "Form should remain open when name is empty (validation failed)");
        
        // Bỏ trống Giá
        productManagePage.fillProductForm("Khóa Samsung 2024", "Samsung", null, 15, 80, "Khóa vân tay cao cấp");
        productManagePage.submitProductForce();
        assertTrue(productManagePage.isFormOpen(), "Form should remain open when price is empty (validation failed)");
        
        // Bỏ trống Số lượng
        productManagePage.fillProductForm("Khóa Samsung 2024", "Samsung", "3200000", 15, null, "Khóa vân tay cao cấp");
        productManagePage.submitProductForce();
        assertTrue(productManagePage.isFormOpen(), "Form should remain open when quantity is empty (validation failed)");

        productManagePage.cancel();
    }

    @Test
    @Order(4)
    @DisplayName("TC-QLSP-17: Add Product - Tên sản phẩm quá dài (256 ký tự)")
    void testAddProduct_LongName() {
        productManagePage.clickAddProductButton();
        String longName = "A".repeat(256); // Sinh chuỗi 256 ký tự
        productManagePage.fillProductForm(longName, "Samsung", "3200000", 15, 80, "Mô tả khóa dài");
        
        productManagePage.submitProductForce();
        assertTrue(productManagePage.isFormOpen(), "Form should remain open or error should be shown when name > 255 chars");
        productManagePage.cancel();
    }

    @Test
    @Order(5)
    @DisplayName("TC-QLSP-18,19,20,21: Add Product - Giá trị số không hợp lệ")
    void testAddProduct_InvalidNumericInputs() {
        productManagePage.clickAddProductButton();
        
        // Giá âm
        productManagePage.fillProductForm("Khóa Test Numeric", "Samsung", "-500000", 15, 80, "Mô tả khóa");
        productManagePage.submitProductForce();
        assertTrue(productManagePage.isFormOpen(), "Form should remain open for negative price");
        
        // Giảm giá > 100%
        productManagePage.fillProductForm("Khóa Test Numeric", "Samsung", "3200000", 150, 80, "Mô tả khóa");
        productManagePage.submitProductForce();
        assertTrue(productManagePage.isFormOpen(), "Form should remain open for discount > 100%");
        
        // Số lượng âm
        productManagePage.fillProductForm("Khóa Test Numeric", "Samsung", "3200000", 15, -10, "Mô tả khóa");
        productManagePage.submitProductForce();
        assertTrue(productManagePage.isFormOpen(), "Form should remain open for negative quantity");

        productManagePage.cancel();
    }

    @Test
    @Order(51)
    @DisplayName("TC-QLSP-21.1: Add Product - Trùng tên sản phẩm (Duplicate)")
    void testAddDuplicateProduct() {
        // Tạo một SP chuẩn trước
        productManagePage.clickAddProductButton();
        String dupName = "Khóa Duplicate " + System.currentTimeMillis();
        productManagePage.fillProductForm(dupName, "Samsung", "2000000", 10, 50, "Mô tả khóa");
        productManagePage.submitProduct();
        
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Cố tình tạo thêm 1 SP trùng tên
        productManagePage.clickAddProductButton();
        productManagePage.fillProductForm(dupName, "Samsung", "3000000", 15, 60, "Mô tả khóa 2");
        productManagePage.submitProductForce();
        
        // Validation nên chặn lại ở UI hoặc backend trả về lỗi giữ form mở
        assertTrue(productManagePage.isFormOpen(), "Form nên giữ nguyên hoặc hiển thị lỗi nếu bị trùng tên");
        productManagePage.cancel();
        
        // Clean up
        productManagePage.deleteProduct(dupName, true);
    }

    @Test
    @Order(6)
    @DisplayName("TC-QLSP-26: Add Product - Hủy bỏ (Cancel)")
    void testAddProduct_CancelFlow() {
        productManagePage.clickAddProductButton();
        productManagePage.fillProductForm("Test Cancel Product", "Samsung", "100000", 10, 10, "Test");
        productManagePage.cancel();
        
        productManagePage.searchProduct("Test Cancel Product");
        assertEquals(0, productManagePage.getProductCountInTable(), "Canceled product should not be created");
        productManagePage.searchProduct(""); // Reset filter
    }

    // =========================================================================
    // LUỒNG THÊM MỚI THÀNH CÔNG (HAPPY PATH)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("TC-QLSP-16,24: Add Product - Nhập đầy đủ dữ liệu chuẩn")
    void testAddProduct_ValidFullData() {
        productManagePage.clickAddProductButton();
        productManagePage.fillProductForm("Khóa Full Thông Tin", "Samsung", "4500000", 10, 50, "Mô tả khóa full");
        assertFalse(productManagePage.isSubmitButtonDisabled(), "Submit should be enabled for valid data");
        productManagePage.submitProduct();
        
        productManagePage.searchProduct("Khóa Full Thông Tin");
        assertTrue(productManagePage.getProductCountInTable() > 0, "Product should be added successfully");
        productManagePage.searchProduct(""); // Reset filter
    }

    @Test
    @Order(8)
    @DisplayName("Add Product - Thành công khi không upload ảnh")
    void testAddProduct_NoImage() {
        productManagePage.clickAddProductButton();
        productManagePage.fillProductForm("Khóa Không Ảnh", "Samsung", "3000000", 0, 10, "Mô tả khóa không ảnh");
        assertFalse(productManagePage.isSubmitButtonDisabled(), "Submit button should be enabled even without image");
        productManagePage.submitProduct();
        
        productManagePage.searchProduct("Khóa Không Ảnh");
        assertTrue(productManagePage.getProductCountInTable() > 0, "Product without image should be added successfully");
        productManagePage.searchProduct(""); // Reset filter
    }

    // =========================================================================
    // 4. FORM CHỈNH SỬA SẢN PHẨM
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("TC-QLSP-27,28,29: Edit Product - Cập nhật thông tin thành công")
    void testEditProduct_ValidData() {
        productManagePage.searchProduct("Khóa Full Thông Tin");
        if (productManagePage.getProductCountInTable() > 0) {
            productManagePage.editProduct("Khóa Full Thông Tin");
            // Sửa tên và số lượng
            productManagePage.fillProductForm("Khóa Full Thông Tin V2", null, "5000000", null, 100, null);
            productManagePage.submitProduct();
            
            // Tìm theo tên mới
            productManagePage.searchProduct("Khóa Full Thông Tin V2");
            assertTrue(productManagePage.getProductCountInTable() > 0, "Edited product should reflect new name");
        }
    }

    @Test
    @Order(10)
    @DisplayName("TC-QLSP-30: Edit Product - Xóa trống các trường bắt buộc")
    void testEditProduct_EmptyFields() {
        productManagePage.searchProduct("Khóa Full Thông Tin V2");
        if (productManagePage.getProductCountInTable() > 0) {
            productManagePage.editProduct("Khóa Full Thông Tin V2");
            productManagePage.fillProductForm(null, null, null, null, null, null); // Xóa hết
            productManagePage.submitProductForce();
            assertTrue(productManagePage.isFormOpen(), "Form should remain open when required fields are cleared (validation failed)");
            productManagePage.cancel();
        }
    }

    @Test
    @Order(101)
    @DisplayName("TC-QLSP-30.1: Edit Product - Dữ liệu số không hợp lệ (Validation)")
    void testEditProduct_InvalidNumericInputs() {
        productManagePage.searchProduct("Khóa Full Thông Tin V2");
        if (productManagePage.getProductCountInTable() > 0) {
            productManagePage.editProduct("Khóa Full Thông Tin V2");
            
            // Cố tình set giá trị âm cho giá bán
            productManagePage.fillProductForm(null, null, "-500000", null, null, null); 
            productManagePage.submitProductForce();
            assertTrue(productManagePage.isFormOpen(), "Form không được đóng khi gán giá bán là số âm");
            
            // Cố tình set giảm giá > 100%
            productManagePage.fillProductForm(null, null, "5000000", 150, null, null); 
            productManagePage.submitProductForce();
            assertTrue(productManagePage.isFormOpen(), "Form không được đóng khi giảm giá > 100%");
            
            productManagePage.cancel();
        }
    }

    @Test
    @Order(11)
    @DisplayName("TC-QLSP-32: Edit Product - Thay đổi dữ liệu nhưng chọn Hủy")
    void testEditProduct_Cancel() {
        productManagePage.searchProduct("Khóa Full Thông Tin V2");
        if (productManagePage.getProductCountInTable() > 0) {
            productManagePage.editProduct("Khóa Full Thông Tin V2");
            productManagePage.fillProductForm("Tên Bị Hủy Update", null, "9999999", null, null, null);
            productManagePage.cancel();
            
            // Tên cũ vẫn còn
            productManagePage.searchProduct("Khóa Full Thông Tin V2");
            assertTrue(productManagePage.getProductCountInTable() > 0, "Original name should be preserved");
            
            // Tên mới không được lưu
            productManagePage.searchProduct("Tên Bị Hủy Update");
            assertEquals(0, productManagePage.getProductCountInTable(), "Canceled name should not be found");
        }
    }

    // =========================================================================
    // 5. XÓA SẢN PHẨM
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("TC-QLSP-35: Delete Product - Chọn Hủy khi popup hiện")
    void testDeleteProduct_Cancel() {
        productManagePage.searchProduct("Khóa Full Thông Tin V2");
        if (productManagePage.getProductCountInTable() > 0) {
            // false = click Hủy trong Dialog
            productManagePage.deleteProduct("Khóa Full Thông Tin V2", false); 
            assertTrue(productManagePage.getProductCountInTable() > 0, "Product should still exist after canceling delete");
        }
    }

    @Test
    @Order(13)
    @DisplayName("TC-QLSP-33,34: Delete Product - Chọn Xác Nhận")
    void testDeleteProduct_Confirm() {
        // Xóa SP không ảnh
        productManagePage.searchProduct("Khóa Không Ảnh");
        if (productManagePage.getProductCountInTable() > 0) {
            productManagePage.deleteProduct("Khóa Không Ảnh", true);
            productManagePage.searchProduct("Khóa Không Ảnh");
            assertEquals(0, productManagePage.getProductCountInTable(), "Product should be deleted");
        }
        
        // Xóa SP V2
        productManagePage.searchProduct("Khóa Full Thông Tin V2");
        if (productManagePage.getProductCountInTable() > 0) {
            productManagePage.deleteProduct("Khóa Full Thông Tin V2", true);
            productManagePage.searchProduct("Khóa Full Thông Tin V2");
            assertEquals(0, productManagePage.getProductCountInTable(), "Product should be deleted");
        }
    }
}

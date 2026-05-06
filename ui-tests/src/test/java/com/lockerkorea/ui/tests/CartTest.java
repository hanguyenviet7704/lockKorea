package com.lockerkorea.ui.tests;

import com.lockerkorea.ui.pages.*;
import com.lockerkorea.ui.utils.BaseTest;
import com.lockerkorea.ui.utils.ConfigReader;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cart Functionality Tests (Based on CART_TEST_CASES.md)
 * Pham vi: Giỏ hàng - Locker Korea
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CartTest extends BaseTest {

    private CartPage cartPage;
    private ProductListPage productListPage;
    private ProductDetailPage productDetailPage;
    private LoginPage loginPage;
    private HomePage homePage;

    @BeforeEach
    public void setUp() throws Exception {
        // Call parent setUp() to initialize driver
        super.setUp();

        // Initialize page objects now that driver is available
        loginPage = new LoginPage(driver);
        cartPage = new CartPage(driver);
        productListPage = new ProductListPage(driver);
        productDetailPage = new ProductDetailPage(driver);
        homePage = new HomePage(driver);
    }

    // ==================== 1. GIAO DIEN CHUNG ====================

    @Test
    @Order(1)
    @DisplayName("CART_1: User xem giỏ hàng - Kiểm tra giao diện")
    void testCartUI() {
        // Login as user
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        // Click cart icon
        homePage.navigateTo();
        cartPage = homePage.clickCartIcon();

        // Verify cart page loads with correct layout
        assertTrue(cartPage.isLoaded(), "Giỏ hàng phải hiển thị");
        assertTrue(cartPage.isElementDisplayed(By.cssSelector(".cart-header")),
            "Header giỏ hàng phải hiển thị");
        // Check font, color - basic visibility check
        assertTrue(cartPage.isElementDisplayed(By.cssSelector(".cart-content, .empty-cart")),
            "Nội dung giỏ hàng phải hiển thị");
    }

    @Test
    @Order(2)
    @DisplayName("CART_2: User mới - Giỏ hàng trống")
    void testEmptyCartForNewUser() {
        // Login as new user (chưa có giỏ hàng)
        loginPage.navigateTo();
        loginPage.login("newuser@gmail.com", "User@123");

        // Click cart icon
        homePage.navigateTo();
        cartPage = homePage.clickCartIcon();

        // Verify empty cart message
        assertTrue(cartPage.isEmpty(), "Giỏ hàng phải trống");
        assertTrue(cartPage.isElementDisplayed(By.cssSelector(".empty-cart")),
            "Hiển thị 'Giỏ hàng đang trống'");
        assertTrue(cartPage.isElementDisplayed(By.cssSelector("a[routerLink*='product'], .start-shopping-btn")),
            "Button 'Tiếp tục mua sắm' phải hiển thị");
    }

    @Test
    @Order(3)
    @DisplayName("CART_3: Kiểm tra badge số lượng trên icon giỏ hàng")
    void testCartBadge() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        // Clear cart first
        cartPage.navigateTo();
        if (!cartPage.isEmpty()) {
            cartPage.deleteAllItems();
        }

        // Add 2 products to cart
        productListPage.navigateTo();
        productListPage.clickProductByName("Khóa vân tay Samsung SHP-DH538");
        productDetailPage.selectSize("42");
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        productListPage.navigateTo();
        productListPage.clickProductByName("Khóa Kaadas K9");
        productDetailPage.selectSize("43");
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Check badge on cart icon
        homePage.navigateTo();
        String badgeText = homePage.getCartBadgeText();
        assertTrue(badgeText.contains("2"), "Badge phải hiển thị số 2");
    }

    // ==================== 2. THÊM SẢN PHẨM ====================

    @Test
    @Order(4)
    @DisplayName("CART_5: Thêm SP - Samsung SHP-DH538, Đen, Qty=2")
    void testAddProductToCart() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        // Add product
        productListPage.navigateTo();
        productListPage.clickProductByName("Khóa vân tay Samsung SHP-DH538");
        productDetailPage.selectColor("Đen");
        productDetailPage.selectSize("42");
        productDetailPage.setQuantity(2);
        productDetailPage.clickAddToCart();

        // Verify toast message
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertTrue(productDetailPage.isToastDisplayed("Đã thêm vào giỏ"),
            "Toast 'Đã thêm vào giỏ' phải hiển thị");

        // Verify cart badge
        homePage.navigateTo();
        String badgeText = homePage.getCartBadgeText();
        assertTrue(badgeText.contains("2"), "Badge phải = 2");
    }

    @Test
    @Order(5)
    @DisplayName("CART_6: Guest thêm SP - Khóa Kaadas K9, Bạc, Qty=1")
    void testAddProductAsGuest() {
        // Don't login - stay as guest
        productListPage.navigateTo();
        productListPage.clickProductByName("Khóa Kaadas K9");
        productDetailPage.selectColor("Bạc");
        productDetailPage.selectSize("43");
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();

        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Verify badge = 1 (stored in session)
        homePage.navigateTo();
        String badgeText = homePage.getCartBadgeText();
        assertTrue(badgeText.contains("1"), "Badge phải = 1 cho guest");
    }

    @Test
    @Order(6)
    @DisplayName("CART_7: Thêm cùng SP 2 lần -> Gộp thành 1 dòng qty=2")
    void testAddSameProductTwice() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        // Clear cart first
        cartPage.navigateTo();
        if (!cartPage.isEmpty()) {
            cartPage.deleteAllItems();
        }

        // Add same product twice
        productListPage.navigateTo();
        productListPage.clickProductByName("Khóa vân tay Samsung SHP-DH538");
        productDetailPage.selectColor("Đen");
        productDetailPage.selectSize("42");
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Add again with qty=1
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Check cart: 1 item with qty=2
        cartPage.navigateTo();
        assertEquals(1, cartPage.getCartItemCount(), "Chỉ có 1 dòng cho cùng SP");
        assertEquals(2, cartPage.getProductQuantity(0), "Qty phải = 2");
    }

    @Test
    @Order(7)
    @DisplayName("CART_8: Thêm 2 màu khác nhau -> 2 dòng riêng biệt")
    void testAddDifferentColors() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        // Clear cart
        cartPage.navigateTo();
        if (!cartPage.isEmpty()) {
            cartPage.deleteAllItems();
        }

        // Add Đen
        productListPage.navigateTo();
        productListPage.clickProductByName("Khóa vân tay Samsung SHP-DH538");
        productDetailPage.selectColor("Đen");
        productDetailPage.selectSize("42");
        productDetailPage.clickAddToCart();
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Add Bạc
        productDetailPage.selectColor("Bạc");
        productDetailPage.clickAddToCart();
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Check cart: 2 items
        cartPage.navigateTo();
        assertEquals(2, cartPage.getCartItemCount(), "Phải có 2 dòng riêng biệt: màu Đen và màu Bạc");
    }

    @Test
    @Order(8)
    @DisplayName("CART_9: Thêm SP với qty=0 -> Báo lỗi")
    void testAddProductWithZeroQuantity() {
        productListPage.navigateTo();
        productListPage.clickProductByName("Khóa vân tay Samsung SHP-DH538");
        productDetailPage.selectColor("Đen");
        productDetailPage.selectSize("42");
        productDetailPage.setQuantity(0);
        productDetailPage.clickAddToCart();

        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Verify error message
        String errorMsg = productDetailPage.getErrorMessage();
        assertFalse(errorMsg.isEmpty(), "Phải hiển thị lỗi 'Số lượng phải ít nhất là 1'");
    }

    // ==================== 3. XEM GIỎ HÀNG ====================

    @Test
    @Order(9)
    @DisplayName("CART_12: Xem giỏ hàng - Kiểm tra thông tin SP")
    void testViewCartItemDetails() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        // Clear and add 2 products
        cartPage.navigateTo();
        if (!cartPage.isEmpty()) {
            cartPage.deleteAllItems();
        }

        addProductToCart("Khóa vân tay Samsung SHP-DH538", "Đen", "42", 2, 2500000);
        addProductToCart("Khóa Kaadas K9", "Bạc", "43", 1, 3200000);

        // View cart
        cartPage.navigateTo();

        // Verify item 1: Samsung
        assertEquals("Khóa vân tay Samsung SHP-DH538", cartPage.getProductName(0));
        assertEquals(2, cartPage.getProductQuantity(0));
        String item1Total = cartPage.getProductTotal(0);
        assertTrue(item1Total.contains("5.000.000") || item1Total.contains("5000000"),
            "Tổng Samsung = 5.000.000đ");

        // Verify item 2: Kaadas
        assertEquals("Khóa Kaadas K9", cartPage.getProductName(1));
        assertEquals(1, cartPage.getProductQuantity(1));
        String item2Total = cartPage.getProductTotal(1);
        assertTrue(item2Total.contains("3.200.000") || item2Total.contains("3200000"),
            "Giá Kaadas = 3.200.000đ");

        // Verify total
        String total = cartPage.getTotalAmount();
        assertTrue(total.contains("8.200.000") || total.contains("8200000"),
            "Tổng = 8.200.000đ");
    }

    @Test
    @Order(10)
    @DisplayName("CART_14: Kiểm tra tổng tiền nhiều SP")
    void testCartTotalForMultipleProducts() {
        cartPage.navigateTo();

        // Add SP_A (100k x 2 = 200k)
        addProductToCart("Product A", null, null, 2, 100000);
        // Add SP_B (200k x 1 = 200k)
        addProductToCart("Product B", null, null, 1, 200000);

        cartPage.navigateTo();
        String total = cartPage.getTotalAmount();
        assertTrue(total.contains("400.000") || total.contains("400000"),
            "Tổng = 400.000đ");
    }

    @Test
    @Order(11)
    @DisplayName("CART_15: SP có giảm giá 20%")
    void testCartWithDiscountedProduct() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        // Add product with 20% discount (500k -> 400k)
        addProductToCart("Discounted Product", null, null, 1, 400000);

        cartPage.navigateTo();

        // Verify: show 400k (after discount), strikethrough 500k (original)
        String itemPrice = cartPage.getProductPrice(0);
        assertTrue(itemPrice.contains("400.000") || itemPrice.contains("400000"),
            "Hiển thị 400.000đ (sau giảm)");
        // Check for strikethrough original price
        assertTrue(cartPage.isElementDisplayed(By.cssSelector(".original-price, .strikethrough, del, s")),
            "Phải có giá gốc bị gạch ngang");
    }

    // ==================== 4. CẬP NHẬT GIỎ HÀNG ====================

    @Test
    @Order(12)
    @DisplayName("CART_16: Tăng số lượng (+), qty=1 -> qty=2")
    void testIncrementQuantity() {
        cartPage.navigateTo();

        if (cartPage.getCartItemCount() > 0) {
            int initialQty = cartPage.getProductQuantity(0);
            cartPage.incrementQuantity(0);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            assertEquals(initialQty + 1, cartPage.getProductQuantity(0), "Qty phải tăng lên 1");
        }
    }

    @Test
    @Order(13)
    @DisplayName("CART_17: Giảm số lượng (-), qty=3 -> qty=2")
    void testDecrementQuantity() {
        cartPage.navigateTo();

        // Set qty to 3 first
        if (cartPage.getCartItemCount() > 0) {
            cartPage.setProductQuantity(0, 3);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            cartPage.decrementQuantity(0);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            assertEquals(2, cartPage.getProductQuantity(0), "Qty phải giảm xuống 2");
        }
    }

    @Test
    @Order(14)
    @DisplayName("CART_18: Giảm số lượng xuống 0 -> Hệ thống ngăn hoặc tự xóa")
    void testDecrementToZero() {
        cartPage.navigateTo();

        if (cartPage.getCartItemCount() > 0) {
            // Try to set qty to 0
            cartPage.setProductQuantity(0, 0);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // Verify: system prevents or auto-deletes
            assertTrue(cartPage.getProductQuantity(0) >= 1 ||
                       cartPage.getCartItemCount() == 0,
                "Hệ thống phải ngăn (không cho xuống 0) hoặc tự xóa item");
        }
    }

    @Test
    @Order(15)
    @DisplayName("CART_19: Nhập mới qty=5")
    void testSetQuantityDirectly() {
        cartPage.navigateTo();

        if (cartPage.getCartItemCount() > 0) {
            cartPage.setProductQuantity(0, 5);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            assertEquals(5, cartPage.getProductQuantity(0), "Qty phải = 5");
        }
    }

    @Test
    @Order(151)
    @DisplayName("CART_19.1: Nhập số lượng âm (-5)")
    void testSetNegativeQuantity() {
        cartPage.navigateTo();

        if (cartPage.getCartItemCount() > 0) {
            cartPage.setProductQuantity(0, -5);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            assertTrue(cartPage.getProductQuantity(0) >= 1, "Số lượng không được âm, phải tự động reset về 1 hoặc giữ nguyên");
        }
    }

    @Test
    @Order(152)
    @DisplayName("CART_19.2: Tiến hành thanh toán khi chưa chọn SP")
    void testCheckoutWithoutSelection() {
        cartPage.navigateTo();
        
        // Bỏ chọn tất cả checkbox nếu có
        try {
            org.openqa.selenium.WebElement checkbox = driver.findElement(By.cssSelector(".select-all input"));
            if (checkbox.isSelected()) {
                checkbox.click();
            }
        } catch (Exception e) {}

        if (cartPage.getCartItemCount() > 0) {
            assertFalse(cartPage.isCheckoutButtonEnabled(), "Nút thanh toán phải bị disable khi chưa chọn SP");
        }
    }

    // ==================== 5. XÓA SẢN PHẨM ====================

    @Test
    @Order(16)
    @DisplayName("CART_20: Xóa 1 item, còn lại 1 item")
    void testDeleteOneItem() {
        cartPage.navigateTo();

        int initialCount = cartPage.getCartItemCount();
        if (initialCount >= 2) {
            cartPage.removeProduct(0);
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            assertEquals(initialCount - 1, cartPage.getCartItemCount(), "Còn lại 1 item sau khi xóa");
        }
    }

    @Test
    @Order(17)
    @DisplayName("CART_21: Xóa tất cả sản phẩm")
    void testDeleteAllItems() {
        cartPage.navigateTo();

        if (!cartPage.isEmpty()) {
            cartPage.deleteAllItems();
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            assertTrue(cartPage.isEmpty(), "Giỏ hàng phải trống");
            assertTrue(cartPage.isElementDisplayed(By.cssSelector(".empty-cart")),
                "Hiển thị 'Giỏ hàng đang trống'");
        }
    }

    @Test
    @Order(18)
    @DisplayName("CART_22: Xóa item duy nhất -> Giỏ hàng trống")
    void testDeleteLastItem() {
        // Clear cart and add 1 item
        cartPage.navigateTo();
        if (!cartPage.isEmpty()) {
            cartPage.deleteAllItems();
        }

        addProductToCart("Test Product", null, null, 1, 100000);

        cartPage.navigateTo();
        assertEquals(1, cartPage.getCartItemCount());

        // Delete the only item
        cartPage.removeProduct(0);
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertTrue(cartPage.isEmpty(), "Giỏ hàng phải trống");
        assertTrue(cartPage.isElementDisplayed(By.cssSelector(".empty-cart")),
            "Hiển thị 'Giỏ hàng đang trống'");
    }

    // ==================== Helper Methods ====================

    private void addProductToCart(String productName, String color, String size, int qty, int expectedPrice) {
        productListPage.navigateTo();
        productListPage.clickProductByName(productName);

        if (color != null) productDetailPage.selectColor(color);
        if (size != null) productDetailPage.selectSize(size);
        if (qty > 0) productDetailPage.setQuantity(qty);
        productDetailPage.clickAddToCart();
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

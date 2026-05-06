package com.example.Sneakers.integration;

import com.example.Sneakers.dtos.CartItemDTO;
import com.example.Sneakers.models.Cart;
import com.example.Sneakers.models.Category;
import com.example.Sneakers.models.Product;
import com.example.Sneakers.models.User;
import com.example.Sneakers.repositories.CartRepository;
import com.example.Sneakers.repositories.CategoryRepository;
import com.example.Sneakers.repositories.ProductRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.responses.CartResponse;
import com.example.Sneakers.responses.ListCartResponse;
import com.example.Sneakers.services.CartService;
import com.example.Sneakers.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Integration Tests for CartService.
 * 
 * @MockBean
 *           private dev.langchain4j.model.chat.ChatModel chatModel;
 *           <p>
 *           Tests: Cart CRUD operations, user/session-based cart management,
 *           quantity updates, ownership validation.
 *           Uses H2 in-memory database with real repositories.
 *           Each test runs in a transaction and rolls back automatically.
 *           </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    private Category testCategory;
    private Product testProduct;
    private User testUser;
    private CartItemDTO cartItemDTO;
    private final String testToken = "Bearer valid-token";
    private final String testSessionId = "test-session-123";

    @BeforeEach
    void setUp() throws Exception {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create category
        testCategory = Category.builder()
                .name("Sneakers")
                .build();
        testCategory = categoryRepository.save(testCategory);

        // Create product
        testProduct = Product.builder()
                .name("Test Sneakers")
                .price(100000L)
                .thumbnail("thumbnail.jpg")
                .description("Test description")
                .category(testCategory)
                .discount(10L)
                .quantity(50L)
                .build();
        testProduct = productRepository.save(testProduct);

        // Create user
        testUser = com.example.Sneakers.models.User.builder()
                .fullName("Test User")
                .phoneNumber("0123456789")
                .password("encodedPass")
                .email("testuser@example.com")
                .address("Test Address")
                .dateOfBirth(new Date())
                .active(true)
                .role(null) // role not needed for cart
                .build();
        testUser = userRepository.save(testUser);

        // Create cart item DTO
        cartItemDTO = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(2L)
                .size(42L)
                .build();

        // Mock UserService to return testUser for any token
        org.mockito.Mockito.when(userService.getUserDetailsFromToken(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(testUser);
    }

    // ==================== Test Case ID: TC-IT-CART-001 ====================
    // Test Objective: createCart with user token - should create new cart item
    // Expected: Cart saved with user, product, quantity, size; sessionId null
    // ====================
    @Test
    void TC_IT_CART_001_createCart_WithUserToken_ShouldCreateCart() throws Exception {
        // Act
        Cart result = cartService.createCart(cartItemDTO, testToken, null);

        // Assert - return value
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testUser, result.getUser());
        assertEquals(testProduct, result.getProduct());
        assertEquals(2L, result.getQuantity());
        assertEquals(42L, result.getSize());
        assertNull(result.getSessionId());

        // ✅ DB Check
        Optional<Cart> fromDb = cartRepository.findById(result.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(testUser.getId(), fromDb.get().getUser().getId());
    }

    // ==================== Test Case ID: TC-IT-CART-002 ====================
    // Test Objective: createCart with sessionId - should create cart for guest
    // Expected: Cart saved with sessionId, user null
    // ====================
    @Test
    void TC_IT_CART_002_createCart_WithSessionId_ShouldCreateGuestCart() throws Exception {
        // Act
        Cart result = cartService.createCart(cartItemDTO, null, testSessionId);

        // Assert
        assertNotNull(result);
        assertEquals(testSessionId, result.getSessionId());
        assertNull(result.getUser());
        assertEquals(testProduct.getId(), result.getProduct().getId());
        assertEquals(2L, result.getQuantity());
        assertEquals(42L, result.getSize());

        // ✅ DB Check
        Optional<Cart> fromDb = cartRepository.findById(result.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(testSessionId, fromDb.get().getSessionId());
    }

    // ==================== Test Case ID: TC-IT-CART-003 ====================
    // Test Objective: createCart - duplicate product+size for same user should
    // increment quantity
    // Expected: Existing cart quantity increases
    // ====================
    @Test
    void TC_IT_CART_003_createCart_DuplicateUserProduct_ShouldIncrementQuantity() throws Exception {
        // Arrange - create first cart item
        cartService.createCart(cartItemDTO, testToken, null);

        // Act - add same product+size again
        Cart result = cartService.createCart(cartItemDTO, testToken, null);

        // Assert
        assertNotNull(result);
        // Should return the existing cart with updated quantity (2+2=4)
        Optional<Cart> fromDb = cartRepository.findByUserAndProductAndSize(testUser, testProduct, 42L);
        assertTrue(fromDb.isPresent());
        assertEquals(4L, fromDb.get().getQuantity());
        // Total carts for user should be 1
        assertEquals(1, cartRepository.findByUserId(testUser.getId()).size());
    }

    // ==================== Test Case ID: TC-IT-CART-004 ====================
    // Test Objective: createCart - same product but different size should create
    // new entry
    // Expected: Separate cart items for each size
    // ====================
    @Test
    void TC_IT_CART_004_createCart_DifferentSize_ShouldCreateNewEntry() throws Exception {
        // Arrange - first item with size 42
        cartService.createCart(cartItemDTO, testToken, null);

        // Act - second item with same product but size 43
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(43L)
                .build();
        Cart result = cartService.createCart(dto2, testToken, null);

        // Assert
        assertNotNull(result);
        assertEquals(43L, result.getSize());
        assertEquals(1L, result.getQuantity());

        // User should have 2 cart items
        List<Cart> userCarts = cartRepository.findByUserId(testUser.getId());
        assertEquals(2, userCarts.size());
    }

    // ==================== Test Case ID: TC-IT-CART-005 ====================
    // Test Objective: createCart without token and sessionId - should throw
    // exception
    // Expected: Exception "Either User Token or Session ID is required"
    // ====================
    @Test
    void TC_IT_CART_005_createCart_NoAuth_ShouldThrowException() throws Exception {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> cartService.createCart(cartItemDTO, null, null));
        assertTrue(exception.getMessage().contains("Either User Token or Session ID is required"));
    }

    // ==================== Test Case ID: TC-IT-CART-006 ====================
    // Test Objective: createCart with non-existent product - should throw exception
    // Expected: DataNotFoundException
    // ====================
    @Test
    void TC_IT_CART_006_createCart_NonExistentProduct_ShouldThrowException() throws Exception {
        // Arrange - DTO with invalid productId
        CartItemDTO invalidDto = CartItemDTO.builder()
                .productId(99999L)
                .quantity(1L)
                .size(42L)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> cartService.createCart(invalidDto, testToken, null));
    }

    // ==================== Test Case ID: TC-IT-CART-007 ====================
    // Test Objective: getCarts with user token - should return user's cart items
    // Expected: List containing user's cart entries with total count
    // ====================
    @Test
    void TC_IT_CART_007_getCarts_WithUserToken_ShouldReturnUserCarts() throws Exception {
        // Arrange - create multiple cart items for user
        cartService.createCart(cartItemDTO, testToken, null);
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(43L)
                .build();
        cartService.createCart(dto2, testToken, null);

        // Act
        ListCartResponse response = cartService.getCarts(testToken, null);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalCartItems());
        assertEquals(2, response.getCarts().size());
    }

    // ==================== Test Case ID: TC-IT-CART-008 ====================
    // Test Objective: getCarts with sessionId - should return session cart items
    // Expected: List containing session's cart entries
    // ====================
    @Test
    void TC_IT_CART_008_getCarts_WithSessionId_ShouldReturnSessionCarts() throws Exception {
        // Arrange - create cart items with sessionId
        cartService.createCart(cartItemDTO, null, testSessionId);
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(3L)
                .size(44L)
                .build();
        cartService.createCart(dto2, null, testSessionId);

        // Act
        ListCartResponse response = cartService.getCarts(null, testSessionId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalCartItems());
        assertEquals(2, response.getCarts().size());
    }

    // ==================== Test Case ID: TC-IT-CART-009 ====================
    // Test Objective: getCarts without token or sessionId - should return empty
    // Expected: Empty cart list with totalCartItems=0
    // ====================
    @Test
    void TC_IT_CART_009_getCarts_NoAuth_ShouldReturnEmpty() throws Exception {
        // Act
        ListCartResponse response = cartService.getCarts(null, null);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalCartItems());
        assertTrue(response.getCarts().isEmpty());
    }

    // ==================== Test Case ID: TC-IT-CART-010 ====================
    // Test Objective: updateCart - valid update should modify quantity and size
    // Expected: Cart updated with new values
    // ====================
    @Test
    void TC_IT_CART_010_updateCart_ValidUpdate_ShouldSucceed() throws Exception {
        // Arrange - create cart item
        Cart cart = cartService.createCart(cartItemDTO, testToken, null);

        // Update DTO
        CartItemDTO updateDTO = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(5L)
                .size(44L)
                .build();

        // Act
        Cart result = cartService.updateCart(cart.getId(), updateDTO, testToken, null);

        // Assert
        assertEquals(5L, result.getQuantity());
        assertEquals(44L, result.getSize());
        assertEquals(testProduct.getId(), result.getProduct().getId());

        // ✅ DB Check
        Optional<Cart> fromDb = cartRepository.findById(cart.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(5L, fromDb.get().getQuantity());
    }

    // ==================== Test Case ID: TC-IT-CART-011 ====================
    // Test Objective: updateCart - changing productId should throw
    // DataNotFoundException
    // Expected: DataNotFoundException
    // ====================
    @Test
    void TC_IT_CART_011_updateCart_DifferentProduct_ShouldThrowException() throws Exception {
        // Arrange - create cart item
        Cart cart = cartService.createCart(cartItemDTO, testToken, null);

        // Update DTO with different productId (need to create another product)
        Product anotherProduct = Product.builder()
                .name("Another Product")
                .price(200000L)
                .thumbnail("thumb2.jpg")
                .description("Another description")
                .category(testCategory)
                .discount(0L)
                .quantity(100L)
                .build();
        anotherProduct = productRepository.save(anotherProduct);

        CartItemDTO updateDTO = CartItemDTO.builder()
                .productId(anotherProduct.getId())
                .quantity(1L)
                .size(42L)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> cartService.updateCart(cart.getId(), updateDTO, testToken, null));
    }

    // ==================== Test Case ID: TC-IT-CART-012 ====================
    // Test Objective: updateCart - unauthorized user (different user) should throw
    // exception
    // Expected: Exception "Unauthorized access to cart item"
    // ====================
    @Test
    void TC_IT_CART_012_updateCart_UnauthorizedUser_ShouldThrowException() throws Exception {
        // Arrange - create cart item as testUser
        Cart cart = cartService.createCart(cartItemDTO, testToken, null);

        // Create another user and mock token to return that user
        User anotherUser = User.builder()
                .fullName("Another User")
                .phoneNumber("0987654321")
                .password("pass")
                .email("another@example.com")
                .address("Address")
                .dateOfBirth(new Date())
                .active(true)
                .role(null)
                .build();
        anotherUser = userRepository.save(anotherUser);

        org.mockito.Mockito.when(userService.getUserDetailsFromToken(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(anotherUser);

        CartItemDTO updateDTO = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(3L)
                .size(43L)
                .build();

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> cartService.updateCart(cart.getId(), updateDTO, testToken, null));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    // ==================== Test Case ID: TC-IT-CART-013 ====================
    // Test Objective: updateCart - unauthorized session (different session) should
    // throw exception
    // Expected: Exception "Unauthorized access to cart item"
    // ====================
    @Test
    void TC_IT_CART_013_updateCart_UnauthorizedSession_ShouldThrowException() throws Exception {
        // Arrange - create cart item with sessionId
        Cart cart = cartService.createCart(cartItemDTO, null, testSessionId);

        // Act - try to update with different sessionId
        CartItemDTO updateDTO = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(3L)
                .size(43L)
                .build();

        Exception exception = assertThrows(Exception.class,
                () -> cartService.updateCart(cart.getId(), updateDTO, null, "different-session"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    // ==================== Test Case ID: TC-IT-CART-014 ====================
    // Test Objective: deleteCart - should remove cart item from database
    // Expected: Cart no longer exists
    // ====================
    @Test
    void TC_IT_CART_014_deleteCart_ShouldRemoveCart() throws Exception {
        // Arrange - create cart item
        Cart cart = cartService.createCart(cartItemDTO, testToken, null);
        Long cartId = cart.getId();

        // Act
        cartService.deleteCart(cartId);

        // Assert
        assertTrue(cartRepository.findById(cartId).isEmpty());
    }

    // ==================== Test Case ID: TC-IT-CART-015 ====================
    // Test Objective: deleteCartByUserOrSession with user - should delete all user
    // cart items
    // Expected: No carts remain for user
    // ====================
    @Test
    void TC_IT_CART_015_deleteCartByUserOrSession_WithUser_ShouldClearAll() throws Exception {
        // Arrange - create multiple cart items for user
        cartService.createCart(cartItemDTO, testToken, null);
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(43L)
                .build();
        cartService.createCart(dto2, testToken, null);

        // Act
        cartService.deleteCartByUserOrSession(testToken, null);

        // Assert
        List<Cart> userCarts = cartRepository.findByUserId(testUser.getId());
        assertTrue(userCarts.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-CART-016 ====================
    // Test Objective: deleteCartByUserOrSession with session - should delete all
    // session cart items
    // Expected: No carts remain for session
    // ====================
    @Test
    void TC_IT_CART_016_deleteCartByUserOrSession_WithSession_ShouldClearAll() throws Exception {
        // Arrange - create multiple cart items for session
        cartService.createCart(cartItemDTO, null, testSessionId);
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(44L)
                .build();
        cartService.createCart(dto2, null, testSessionId);

        // Act
        cartService.deleteCartByUserOrSession(null, testSessionId);

        // Assert
        List<Cart> sessionCarts = cartRepository.findBySessionId(testSessionId);
        assertTrue(sessionCarts.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-CART-017 ====================
    // Test Objective: countCarts with userId - should return correct count
    // Expected: Count equals number of user's cart items
    // ====================
    @Test
    void TC_IT_CART_017_countCarts_WithUserId_ShouldReturnCount() throws Exception {
        // Arrange - create 3 items
        cartService.createCart(cartItemDTO, testToken, null);
        cartService.createCart(cartItemDTO, testToken, null); // duplicate size -> quantity increment but same cart item
        // Actually duplicate size merges, so we'll create different sizes to get count
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(43L)
                .build();
        cartService.createCart(dto2, testToken, null);
        CartItemDTO dto3 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(44L)
                .build();
        cartService.createCart(dto3, testToken, null);

        // Act
        Long count = cartService.countCarts(testUser.getId(), null);

        // Assert - we have 3 distinct cart entries (sizes 42,43,44)
        assertEquals(3L, count);
    }

    // ==================== Test Case ID: TC-IT-CART-018 ====================
    // Test Objective: countCarts with sessionId - should return correct count
    // Expected: Count equals number of session's cart items
    // ====================
    @Test
    void TC_IT_CART_018_countCarts_WithSessionId_ShouldReturnCount() throws Exception {
        // Arrange - create 2 items for session
        cartService.createCart(cartItemDTO, null, testSessionId);
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(43L)
                .build();
        cartService.createCart(dto2, null, testSessionId);

        // Act
        Long count = cartService.countCarts(null, testSessionId);

        // Assert
        assertEquals(2L, count);
    }

    // ==================== Test Case ID: TC-IT-CART-019 ====================
    // Test Objective: countCartsByUserId - should delegate correctly
    // Expected: Same as countCarts(userId, null)
    // ====================
    @Test
    void TC_IT_CART_019_countCartsByUserId_ShouldReturnCorrectCount() throws Exception {
        // Arrange
        cartService.createCart(cartItemDTO, testToken, null);
        CartItemDTO dto2 = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(43L)
                .build();
        cartService.createCart(dto2, testToken, null);

        // Act
        Long count = cartService.countCartsByUserId(testUser.getId());

        // Assert
        assertEquals(2L, count);
    }

    // ==================== Test Case ID: TC-IT-CART-020 ====================
    // Test Objective: getCarts - response should contain CartResponse with correct
    // product details
    // Expected: Each CartResponse has id, quantity, size, and non-null products
    // ====================
    @Test
    void TC_IT_CART_020_getCarts_ResponseShouldContainProductDetails() throws Exception {
        // Arrange
        cartService.createCart(cartItemDTO, testToken, null);

        // Act
        ListCartResponse response = cartService.getCarts(testToken, null);

        // Assert
        assertFalse(response.getCarts().isEmpty());
        CartResponse cartResponse = response.getCarts().get(0);
        assertNotNull(cartResponse.getProducts());
        assertEquals(testProduct.getId(), cartResponse.getProducts().getId());
        assertEquals(testProduct.getName(), cartResponse.getProducts().getName());
    }

    // ==================== Test Case ID: TC-IT-CART-021 ====================
    // Test Objective: createCart - quantity should be positive (>0)
    // Expected: Cart created with quantity >=1
    // ====================
    @Test
    void TC_IT_CART_021_createCart_QuantityMustBePositive() throws Exception {
        // Arrange - DTO with quantity 1 (minimum valid)
        CartItemDTO dto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(42L)
                .build();

        // Act
        Cart result = cartService.createCart(dto, testToken, null);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getQuantity());
    }

    // ==================== Test Case ID: TC-IT-CART-022 ====================
    // Test Objective: updateCart - decreasing quantity should update correctly
    // Expected: Quantity decreases
    // ====================
    @Test
    void TC_IT_CART_022_updateCart_DecreaseQuantity_ShouldSucceed() throws Exception {
        // Arrange - create cart with quantity 5
        CartItemDTO createDto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(5L)
                .size(42L)
                .build();
        Cart cart = cartService.createCart(createDto, testToken, null);

        // Update to quantity 2
        CartItemDTO updateDto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(2L)
                .size(42L)
                .build();

        // Act
        Cart result = cartService.updateCart(cart.getId(), updateDto, testToken, null);

        // Assert
        assertEquals(2L, result.getQuantity());
    }

    // ==================== Test Case ID: TC-IT-CART-023 ====================
    // Test Objective: createCart - guest user (session) and then getCarts should
    // return same items
    // Expected: Session-based cart persists
    // ====================
    @Test
    void TC_IT_CART_023_createCart_GuestSession_PersistsAcrossGetCarts() throws Exception {
        // Arrange - create with session
        cartService.createCart(cartItemDTO, null, testSessionId);

        // Act - retrieve carts for same session
        ListCartResponse response = cartService.getCarts(null, testSessionId);

        // Assert
        assertEquals(1, response.getTotalCartItems());
        assertEquals(testProduct.getId(), response.getCarts().get(0).getProducts().getId());
    }

    // ==================== Test Case ID: TC-IT-CART-024 ====================
    // Test Objective: createCart - when both token and sessionId provided, token
    // takes precedence
    // Expected: Cart associated with user, sessionId null
    // ====================
    @Test
    void TC_IT_CART_024_createCart_BothAuthAndSession_UsesUser() throws Exception {
        // Act - provide both token and sessionId
        Cart result = cartService.createCart(cartItemDTO, testToken, testSessionId);

        // Assert
        assertNotNull(result.getUser());
        assertEquals(testUser.getId(), result.getUser().getId());
        assertNull(result.getSessionId());
    }

    // ==================== Test Case ID: TC-IT-CART-025 ====================
    // Test Objective: deleteCart - deleting non-existent ID should be handled
    // gracefully (idempotent)
    // Expected: No exception
    // ====================
    @Test
    void TC_IT_CART_025_deleteCart_NonExistent_ShouldHandleGracefully() {
        // Act - should not throw
        cartService.deleteCart(99999L);
    }

    // ==================== Test Case ID: TC-IT-CART-026 ====================
    // Test Objective: createCart - update existing cart's quantity adds to existing
    // Expected: Quantity becomes sum of old + new
    // ====================
    @Test
    void TC_IT_CART_026_createCart_UpdateExistingQuantity_ShouldAdd() throws Exception {
        // Arrange - first add 2
        cartService.createCart(cartItemDTO, testToken, null);

        // Second add 3 more (same product+size)
        CartItemDTO moreDto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(3L)
                .size(42L)
                .build();
        Cart result = cartService.createCart(moreDto, testToken, null);

        // Assert
        Optional<Cart> fromDb = cartRepository.findByUserAndProductAndSize(testUser, testProduct, 42L);
        assertTrue(fromDb.isPresent());
        assertEquals(5L, fromDb.get().getQuantity()); // 2 + 3 = 5
    }

    // ==================== Test Case ID: TC-IT-CART-027 ====================
    // Test Objective: getCarts - when user has no carts, should return empty list
    // Expected: totalCartItems = 0, empty carts
    // ====================
    @Test
    void TC_IT_CART_027_getCarts_NoCarts_ShouldReturnEmpty() throws Exception {
        // Act - user has no carts
        ListCartResponse response = cartService.getCarts(testToken, null);

        // Assert
        assertEquals(0L, response.getTotalCartItems());
        assertTrue(response.getCarts().isEmpty());
    }

    // ==================== Test Case ID: TC-IT-CART-028 ====================
    // Mục tiêu: createCart - Backend CÓ CHẶN quantity = 0 không?
    // BUG DETECTION: Nếu test FAIL = Bug tồn tại (cho phép thêm 0 sản phẩm vào giỏ)
    // ====================
    @Test
    void TC_IT_CART_028_createCart_ZeroQuantity_ShouldThrowException() throws Exception {
        CartItemDTO invalidDto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(0L) // Số lượng = 0
                .size(42L)
                .build();

        assertThrows(Exception.class, () -> cartService.createCart(invalidDto, testToken, null),
                "[BUG] Backend đang bị lỗi: Cho phép thêm sản phẩm với số lượng = 0 vào giỏ hàng!");
    }

    // ==================== Test Case ID: TC-IT-CART-029 ====================
    // Mục tiêu: createCart - Backend CÓ CHẶN quantity âm không?
    // BUG DETECTION: Nếu test FAIL = Bug tồn tại
    // ====================
    @Test
    void TC_IT_CART_029_createCart_NegativeQuantity_ShouldThrowException() throws Exception {
        CartItemDTO invalidDto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(-1L) // Số lượng âm
                .size(42L)
                .build();

        assertThrows(Exception.class, () -> cartService.createCart(invalidDto, testToken, null),
                "[BUG] Backend đang bị lỗi: Cho phép thêm sản phẩm với số lượng âm vào giỏ hàng!");
    }

    // ==================== Test Case ID: TC-IT-CART-030 ====================
    // Mục tiêu: updateCart - Backend CÓ CHẶN update quantity = 0 không?
    // BUG DETECTION: Nếu test FAIL = Bug tồn tại (Cho phép cập nhật số lượng về 0)
    // ====================
    @Test
    void TC_IT_CART_030_updateCart_ZeroQuantity_ShouldThrowException() throws Exception {
        // Tạo cart trước
        Cart cart = cartService.createCart(cartItemDTO, testToken, null);

        CartItemDTO updateDto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(0L) // Cập nhật về 0
                .size(42L)
                .build();

        assertThrows(Exception.class, () -> cartService.updateCart(cart.getId(), updateDto, testToken, null),
                "[BUG] Backend đang bị lỗi: Cho phép cập nhật số lượng trong giỏ hàng về 0!");
    }

    // ==================== Test Case ID: TC-IT-CART-031 ====================
    // Mục tiêu: createCart - Backend CÓ CHẶN size âm hoặc không hợp lệ không?
    // BUG DETECTION: Nếu test FAIL = Bug tồn tại (Cho phép size âm -> dữ liệu rác)
    // ====================
    @Test
    void TC_IT_CART_031_createCart_NegativeSize_ShouldThrowException() throws Exception {
        CartItemDTO invalidDto = CartItemDTO.builder()
                .productId(testProduct.getId())
                .quantity(1L)
                .size(-1L) // Size âm
                .build();

        assertThrows(Exception.class, () -> cartService.createCart(invalidDto, testToken, null),
                "[BUG] Backend đang bị lỗi: Cho phép thêm sản phẩm vào giỏ với Size âm!");
    }
}

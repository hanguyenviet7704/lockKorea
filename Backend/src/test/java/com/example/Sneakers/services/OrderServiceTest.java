package com.example.Sneakers.services;

import com.example.Sneakers.dtos.CartItemDTO;
import com.example.Sneakers.dtos.DashboardStatsDTO;
import com.example.Sneakers.dtos.OrderDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.*;
import com.example.Sneakers.repositories.*;
import com.example.Sneakers.responses.*;
import com.example.Sneakers.utils.BuilderEmailContent;
import com.example.Sneakers.utils.Email;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.ExpressionMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherUsageRepository voucherUsageRepository;

    @Mock
    private UserService userService;

    @Mock
    private GhnService ghnService;

    @Mock
    private AsyncOrderService asyncOrderService;

    @Mock
    private Email emailService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<VoucherUsage> voucherUsageCaptor;

    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private Voucher testVoucher;
    private OrderDTO orderDTO;
    private CartItemDTO cartItemDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .fullName("Test User")
                .email("test@example.com")
                .role(Role.builder().name("USER").build())
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Sneakers")
                .price(100000L)
                .quantity(50L)
                .build();

        testOrder = Order.builder()
                .id(1L)
                .user(testUser)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PROCESSING)
                .totalMoney(100000L)
                .active(true)
                .build();

        testVoucher = Voucher.builder()
                .id(1L)
                .code("TEST10")
                .name("Test Voucher")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .maxDiscountAmount(50000L)
                .remainingQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        cartItemDTO = new CartItemDTO();
        cartItemDTO.setProductId(1L);
        cartItemDTO.setQuantity(2L);
        cartItemDTO.setSize(42L);

        orderDTO = new OrderDTO();
        orderDTO.setCartItems(List.of(cartItemDTO));
        orderDTO.setSubTotal(200000L);
        orderDTO.setTotalMoney(200000L);
        orderDTO.setFullName("Test User");
        orderDTO.setEmail("test@example.com");
        orderDTO.setPhoneNumber("0123456789");
        orderDTO.setAddress("Test Address");
        orderDTO.setShippingMethod("Tiêu chuẩn");
        orderDTO.setPaymentMethod("Cash");
        orderDTO.setDistrictId(1);
        orderDTO.setWardCode("001");
    }

    // ==================== Test Case ID: TC-ORDER-001 ====================
    // Test Objective: Verify that createOrder successfully creates a new order with valid data
    // Input: Valid OrderDTO with cart items, user token
    // Expected Output: OrderIdResponse with order ID, status PROCESSING for Cash payment
    // DB Check: INSERT orders, INSERT order_details, UPDATE product quantity, INSERT voucher_usage (if voucher)
    // Rollback: Transaction rolls back after test
    // ====================
    @Test
    void TC_ORDER_001_createOrder_ShouldCreateOrderSuccessfully() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderDetailRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        OrderIdResponse response = orderService.createOrder(orderDTO, token);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId().longValue());
        // OrderIdResponse does not include status, so we verify status via savedOrder

        // Verify order was saved
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(testUser, savedOrder.getUser());
        assertEquals(200000L, savedOrder.getTotalMoney().longValue());
        assertEquals("Tiêu chuẩn", savedOrder.getShippingMethod());
        assertEquals("Cash", savedOrder.getPaymentMethod());

        // Verify product quantity was decremented
        assertEquals(48L, testProduct.getQuantity().longValue());

        // Verify async processing was called
        verify(asyncOrderService).processAfterOrderCreation(any(Order.class), eq(1), eq("001"));
    }

    // ==================== Test Case ID: TC-ORDER-002 ====================
    // Test Objective: Verify that createOrder applies voucher correctly
    // Input: OrderDTO with voucher code, order total meets min requirement
    // Expected Output: Order with discount applied, voucher usage recorded, remaining quantity decreased
    // ====================
    @Test
    void TC_ORDER_002_createOrder_ShouldApplyVoucherSuccessfully() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        orderDTO.setVoucherCode("TEST10");
        orderDTO.setSubTotal(200000L); // 10% = 20000 discount
        orderDTO.setTotalMoney(210000L); // 200000 - 20000 + 30000 shipping = 210000

        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(testVoucher));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderDetailRepository.saveAll(anyList())).thenReturn(List.of());
        when(voucherUsageRepository.save(any(VoucherUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderIdResponse response = orderService.createOrder(orderDTO, token);

        // Assert
        assertNotNull(response);
        assertEquals(210000L, response.getTotalMoney());

        // Verify voucher was used
        verify(voucherUsageRepository).save(voucherUsageCaptor.capture());
        VoucherUsage usage = voucherUsageCaptor.getValue();
        assertEquals(testVoucher, usage.getVoucher());
        assertEquals(testUser, usage.getUser());
        assertEquals(20000L, usage.getDiscountAmount().longValue());

        // Verify voucher remaining quantity decreased
        assertEquals(99, testVoucher.getRemainingQuantity().intValue());
    }

    // ==================== Test Case ID: TC-ORDER-003 ====================
    // Test Objective: Verify that createOrder throws exception for empty cart
    // Input: OrderDTO with null or empty cartItems
    // Expected Output: Exception with message "Cart items are null or empty"
    // ====================
    @Test
    void TC_ORDER_003_createOrder_ShouldThrowException_WhenCartItemsEmpty() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        orderDTO.setCartItems(null);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.createOrder(orderDTO, token));
        assertEquals("Cart items are null or empty", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    // ==================== Test Case ID: TC-ORDER-004 ====================
    // Test Objective: Verify that createOrder throws exception when product not found
    // Input: CartItemDTO with non-existent productId
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_ORDER_004_createOrder_ShouldThrowException_WhenProductNotFound() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                orderService.createOrder(orderDTO, token));
        assertTrue(exception.getMessage().contains("Product not found"));
    }

    // ==================== Test Case ID: TC-ORDER-005 ====================
    // Test Objective: Verify that createOrder throws exception when product out of stock
    // Input: Product with quantity less than requested
    // Expected Output: Exception with product out of stock message
    // ====================
    @Test
    void TC_ORDER_005_createOrder_ShouldThrowException_WhenProductOutOfStock() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        Product lowStockProduct = Product.builder()
                .id(1L)
                .name("Low Stock Product")
                .price(100000L)
                .quantity(1L)
                .build();

        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);
        when(productRepository.findById(1L)).thenReturn(Optional.of(lowStockProduct));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.createOrder(orderDTO, token));
        assertTrue(exception.getMessage().contains("is out of stock"));
    }

    // ==================== Test Case ID: TC-ORDER-006 ====================
    // Test Objective: Verify that createOrder handles invalid shipping method
    // Input: OrderDTO with invalid shipping method
    // Expected Output: Exception with "Shipping method is unavailable"
    // ====================
    @Test
    void TC_ORDER_006_createOrder_ShouldThrowException_WhenInvalidShippingMethod() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        orderDTO.setShippingMethod("Invalid Method");
        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.createOrder(orderDTO, token));
        assertEquals("Shipping method is unavailable", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-007 ====================
    // Test Objective: Verify that getOrder returns order with tracking info for GHN
    // Input: Valid order ID with tracking number
    // Expected Output: OrderResponse with tracking info from GHN service
    // ====================
    @Test
    void TC_ORDER_007_getOrder_ShouldReturnOrderWithTrackingInfo() throws Exception {
        // Arrange
        Long orderId = 1L;
        Order orderWithTracking = Order.builder()
                .id(orderId)
                .user(testUser)
                .trackingNumber("GHN123456")
                .carrier("GHN")
                .build();

        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(orderWithTracking));
        Map<String, Object> trackingInfo = Map.of("status", "delivered");
        when(ghnService.getOrderInfo("GHN123456")).thenReturn(trackingInfo);

        // Act
        OrderResponse response = orderService.getOrder(orderId);

        // Assert
        assertNotNull(response);
        assertEquals("GHN123456", response.getTrackingNumber());
        assertEquals(trackingInfo, response.getTrackingInfo());
    }

    // ==================== Test Case ID: TC-ORDER-041 ====================
    // Test Objective: Verify that getOrder throws exception when order not found (BUG DETECTION)
    // Input: Non-existent order ID
    // Expected Output: Should throw exception, not return null
    // ====================
    @Test
    void TC_ORDER_041_getOrder_ShouldThrowException_WhenOrderNotFound() {
        // Arrange
        Long nonExistentOrderId = 999L;
        when(orderRepository.findByIdWithDetails(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert - BUG: Current implementation returns null instead of throwing exception
        // This test SHOULD FAIL
        Exception exception = assertThrows(Exception.class, () ->
                orderService.getOrder(nonExistentOrderId));
        assertTrue(exception.getMessage().contains("Cannot find order") ||
                   exception.getMessage().contains("not found"));
    }

    // ==================== Test Case ID: TC-ORDER-008 ====================
    // Test Objective: Verify that getOrder returns null when order not found
    // Input: Non-existent order ID
    // Expected Output: null
    // ====================
    @Test
    void TC_ORDER_008_getOrder_ShouldReturnNull_WhenOrderNotFound() {
        // Arrange
        Long orderId = 999L;
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.empty());

        // Act
        OrderResponse response = orderService.getOrder(orderId);

        // Assert
        assertNull(response);
    }

    // ==================== Test Case ID: TC-ORDER-009 ====================
    // Test Objective: Verify that getOrderByUser allows admin to view any order
    // Input: Admin user token, any order ID
    // Expected Output: OrderResponse returned successfully
    // ====================
    @Test
    void TC_ORDER_009_getOrderByUser_AdminCanViewAnyOrder() throws Exception {
        // Arrange
        String token = "Bearer admin-token";
        User adminUser = User.builder()
                .id(1L)
                .role(Role.builder().name("ADMIN").build())
                .build();
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .user(testUser)
                .build();

        when(userService.getUserDetailsFromToken("admin-token")).thenReturn(adminUser);
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));

        // Act
        OrderResponse response = orderService.getOrderByUser(orderId, token);

        // Assert
        assertNotNull(response);
        verify(ghnService, never()).getOrderInfo(anyString());
    }

    // ==================== Test Case ID: TC-ORDER-010 ====================
    // Test Objective: Verify that getOrderByUser throws exception when user tries to view another user's order
    // Input: Regular user token, order belonging to different user
    // Expected Output: Exception "Cannot get order of another user"
    // ====================
    @Test
    void TC_ORDER_010_getOrderByUser_ShouldThrowException_WhenUserViewsOtherOrder() throws Exception {
        // Arrange
        String token = "Bearer user-token";
        User requestingUser = User.builder().id(1L).build();
        Order differentUserOrder = Order.builder()
                .id(1L)
                .user(User.builder().id(2L).build())
                .build();

        when(userService.getUserDetailsFromToken("user-token")).thenReturn(requestingUser);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(differentUserOrder));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.getOrderByUser(1L, token));
        assertEquals("Cannot get order of another user", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-011 ====================
    // Test Objective: Verify that updateOrder updates order details successfully
    // Input: Valid order ID, OrderDTO with updates
    // Expected Output: Updated Order entity
    // DB Check: UPDATE orders SET fields WHERE id = ?
    // ====================
    @Test
    void TC_ORDER_011_updateOrder_ShouldUpdateOrderSuccessfully() throws Exception {
        // Arrange
        Long orderId = 1L;
        Order existingOrder = Order.builder()
                .id(orderId)
                .fullName("Old Name")
                .email("old@example.com")
                .build();

        OrderDTO updateDTO = new OrderDTO();
        updateDTO.setUserId(1L);
        updateDTO.setFullName("Updated Name");
        updateDTO.setEmail("updated@example.com");

        User existingUser = User.builder().id(1L).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Stub ModelMapper typeMap to return a mock TypeMap
        TypeMap<OrderDTO, Order> typeMap = mock(TypeMap.class);
        when(modelMapper.typeMap(eq(OrderDTO.class), eq(Order.class))).thenReturn(typeMap);
        // Stub addMappings to return the typeMap for chaining (no-op)
        doAnswer(invocation -> typeMap).when(typeMap).addMappings(any(ExpressionMap.class));

        // Stub ModelMapper.map to copy properties
        doAnswer(invocation -> {
            OrderDTO dto = invocation.getArgument(0);
            Order target = invocation.getArgument(1);
            if (dto.getFullName() != null) {
                target.setFullName(dto.getFullName());
            }
            if (dto.getEmail() != null) {
                target.setEmail(dto.getEmail());
            }
            return target;
        }).when(modelMapper).map(any(OrderDTO.class), any(Order.class));

        // Act
        Order result = orderService.updateOrder(orderId, updateDTO);

        // Assert
        assertEquals("Updated Name", result.getFullName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals(existingUser, result.getUser());
    }

    // ==================== Test Case ID: TC-ORDER-012 ====================
    // Test Objective: Verify that deleteOrder performs soft delete (sets active=false)
    // Input: Valid order ID
    // Expected Output: Order.active set to false and saved
    // ====================
    @Test
    void TC_ORDER_012_deleteOrder_ShouldPerformSoftDelete() {
        // Arrange
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .active(true)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        orderService.deleteOrder(orderId);

        // Assert
        assertFalse(order.getActive());
        verify(orderRepository).save(order);
    }

    // ==================== Test Case ID: TC-ORDER-013 ====================
    // Test Objective: Verify that findByUserId returns order history for user
    // Input: Valid user token
    // Expected Output: List of OrderHistoryResponse objects
    // ====================
    @Test
    void TC_ORDER_013_findByUserId_ShouldReturnUserOrderHistory() throws Exception {
        // Arrange
        String token = "Bearer user-token";
        Long userId = 1L;
        Order order1 = Order.builder().id(1L).orderDate(LocalDateTime.now()).totalMoney(100000L).user(testUser).build();
        Order order2 = Order.builder().id(2L).orderDate(LocalDateTime.now().minusDays(1)).totalMoney(200000L).user(testUser).build();

        when(userService.getUserDetailsFromToken("user-token")).thenReturn(testUser);
        when(orderRepository.findByUserId(userId)).thenReturn(List.of(order1, order2));

        // Act
        List<OrderHistoryResponse> responses = orderService.findByUserId(token);

        // Assert
        assertEquals(2, responses.size());
        verify(orderRepository).findByUserId(userId);
    }

    // ==================== Test Case ID: TC-ORDER-014 ====================
    // Test Objective: Verify that getAllOrders returns all orders for admin
    // Input: Admin user token
    // Expected Output: All orders in system
    // ====================
    @Test
    void TC_ORDER_014_getAllOrders_AdminShouldSeeAllOrders() throws Exception {
        // Arrange
        String token = "Bearer admin-token";
        User adminUser = User.builder()
                .id(1L)
                .role(Role.builder().name("ADMIN").build())
                .build();
        Order order1 = Order.builder().id(1L).user(testUser).build();
        Order order2 = Order.builder().id(2L).user(testUser).build();

        when(userService.getUserDetailsFromToken("admin-token")).thenReturn(adminUser);
        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        // Act
        List<OrderHistoryResponse> responses = orderService.getAllOrders(token);

        // Assert
        assertEquals(2, responses.size());
    }

    // ==================== Test Case ID: TC-ORDER-015 ====================
    // Test Objective: Verify that assignStaff assigns staff to order successfully
    // Input: Order ID, staff ID (with STAFF role)
    // Expected Output: Order with assignedStaff set, email notification sent
    // ====================
    @Test
    void TC_ORDER_015_assignStaff_ShouldAssignStaffToOrder() throws Exception {
        // Arrange
        Long orderId = 1L;
        Long staffId = 2L;
        Order order = Order.builder()
                .id(orderId)
                .email("customer@example.com")
                .build();
        User staff = User.builder()
                .id(staffId)
                .role(Role.builder().name("STAFF").build())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        // Act
        orderService.assignStaff(orderId, staffId);

        // Assert
        assertEquals(staff, order.getAssignedStaff());
        verify(orderRepository).save(order);
        verify(emailService).sendEmail(eq("customer@example.com"), anyString(), anyString());
    }

    // ==================== Test Case ID: TC-ORDER-016 ====================
    // Test Objective: Verify that assignStaff throws exception for non-staff user
    // Input: Order ID, user ID with non-STAFF role
    // Expected Output: Exception "User is not a staff member"
    // ====================
    @Test
    void TC_ORDER_016_assignStaff_ShouldThrowException_WhenUserNotStaff() throws Exception {
        // Arrange
        Long orderId = 1L;
        Long userId = 2L;
        User nonStaffUser = User.builder()
                .id(userId)
                .role(Role.builder().name("USER").build())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(new Order()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(nonStaffUser));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.assignStaff(orderId, userId));
        assertEquals("User is not a staff member", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-017 ====================
    // Test Objective: Verify that updateOrderStatus updates order status
    // Input: Order ID, new status string
    // Expected Output: Order with updated status
    // ====================
    @Test
    void TC_ORDER_017_updateOrderStatus_ShouldUpdateStatus() throws Exception {
        // Arrange
        Long orderId = 1L;
        String newStatus = OrderStatus.SHIPPED;
        Order order = Order.builder().id(orderId).status(OrderStatus.PROCESSING).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.updateOrderStatus(orderId, newStatus);

        // Assert
        assertEquals(newStatus, result.getStatus());
    }

    // ==================== Test Case ID: TC-ORDER-018 ====================
    // Test Objective: Verify that getTotalRevenue returns total revenue from all orders
    // Input: No parameters
    // Expected Output: Sum of all order totals
    // ====================
    @Test
    void TC_ORDER_018_getTotalRevenue_ShouldReturnTotalRevenue() {
        // Arrange
        when(orderRepository.calculateTotalRevenue()).thenReturn(5000000L);

        // Act
        Long revenue = orderService.getTotalRevenue();

        // Assert
        assertEquals(5000000L, revenue);
    }

    // ==================== Test Case ID: TC-ORDER-019 ====================
    // Test Objective: Verify that getDashboardStats returns aggregated statistics
    // Input: No parameters
    // Expected Output: DashboardStatsDTO with totalRevenue, todayOrders, totalProductsSold
    // ====================
    @Test
    void TC_ORDER_019_getDashboardStats_ShouldReturnAllStatistics() {
        // Arrange
        when(orderRepository.calculateTotalRevenue()).thenReturn(1000000L);
        when(orderRepository.countOrdersByDate(LocalDate.now())).thenReturn(5L);
        when(orderRepository.countTotalProductsSold()).thenReturn(50L);

        // Act
        DashboardStatsDTO stats = orderService.getDashboardStats();

        // Assert
        assertEquals(1000000L, stats.getTotalRevenue().longValue());
        assertEquals(5L, stats.getTodayOrders().longValue());
        assertEquals(50L, stats.getTotalProductsSold().longValue());
    }

    // ==================== Test Case ID: TC-ORDER-020 ====================
    // Test Objective: Verify that getOrdersByKeyword with pagination returns correct page
    // Input: Keyword, status, date range, Pageable
    // Expected Output: Page<Order> with filtered results
    // ====================
    @Test
    void TC_ORDER_020_getOrdersByKeyword_ShouldReturnFilteredPage() {
        // Arrange
        String keyword = "test";
        String status = "PROCESSING";
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);

        Order order = Order.builder().id(1L).build();
        Page<Order> expectedPage = new PageImpl<>(List.of(order));

        when(orderRepository.findByKeyword(keyword, status, startDate, endDate, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Order> result = orderService.getOrdersByKeyword(keyword, status, startDate, endDate, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-ORDER-021 ====================
    // Test Objective: Verify that countOrders returns total order count
    // Input: No parameters
    // Expected Output: Total count of orders
    // ====================
    @Test
    void TC_ORDER_021_countOrders_ShouldReturnTotalCount() {
        // Arrange
        when(orderRepository.count()).thenReturn(100L);

        // Act
        long count = orderService.countOrders();

        // Assert
        assertEquals(100L, count);
    }

    // ==================== Test Case ID: TC-ORDER-022 ====================
    // Test Objective: Verify that getOrdersByDateRange returns orders within range
    // Input: Start date, end date
    // Expected Output: List of orders within date range
    // ====================
    @Test
    void TC_ORDER_022_getOrdersByDateRange_ShouldReturnOrdersInRange() {
        // Arrange
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        Order order1 = Order.builder().id(1L).build();
        Order order2 = Order.builder().id(2L).build();

        when(orderRepository.findByOrderDateBetween(start, end)).thenReturn(List.of(order1, order2));

        // Act
        List<Order> orders = orderService.getOrdersByDateRange(start, end);

        // Assert
        assertEquals(2, orders.size());
    }

    // ==================== Test Case ID: TC-ORDER-023 ====================
    // Test Objective: Verify that createWaybill creates GHN waybill successfully
    // Input: Order ID, shipping parameters (district, ward, dimensions, weight)
    // Expected Output: Order with tracking number and carrier set
    // ====================
    @Test
    void TC_ORDER_023_createWaybill_ShouldCreateWaybillSuccessfully() throws Exception {
        // Arrange
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .email("customer@example.com")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(ghnService.createOrder(order, 1, "001", 10, 10, 10, 1000)).thenReturn("GHN123456");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        // Act
        Order result = orderService.createWaybill(orderId, 1, "001", 10, 10, 10, 1000);

        // Assert
        assertEquals("GHN123456", result.getTrackingNumber());
        assertEquals("GHN", result.getCarrier());
        assertEquals(1, result.getDistrictId());
        assertEquals("001", result.getWardCode());
        verify(emailService).sendEmail(eq("customer@example.com"), anyString(), anyString());
    }

    // ==================== Test Case ID: TC-ORDER-024 ====================
    // Test Objective: Verify that createWaybill throws exception when waybill already exists
    // Input: Order that already has tracking number
    // Expected Output: Exception "Order already has a waybill"
    // ====================
    @Test
    void TC_ORDER_024_createWaybill_ShouldThrowException_WhenWaybillExists() throws Exception {
        // Arrange
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .trackingNumber("GHN123456")
                .carrier("GHN")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.createWaybill(orderId, 1, "001", 10, 10, 10, 1000));
        assertEquals("Order already has a waybill: GHN123456", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-025 ====================
    // Test Objective: Verify that getTrackingInfo returns tracking info for GHN orders
    // Input: Order ID with tracking number and GHN carrier
    // Expected Output: Tracking info from GHN service
    // ====================
    @Test
    void TC_ORDER_025_getTrackingInfo_ShouldReturnTrackingInfo() throws Exception {
        // Arrange
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .trackingNumber("GHN123456")
                .carrier("GHN")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Map<String, Object> trackingInfoResponse = Map.of("status", "in_transit");
        when(ghnService.getOrderInfo("GHN123456")).thenReturn(trackingInfoResponse);

        // Act
        Object trackingInfo = orderService.getTrackingInfo(orderId);

        // Assert
        assertEquals(trackingInfoResponse, trackingInfo);
    }

    // ==================== Test Case ID: TC-ORDER-026 ====================
    // Test Objective: Verify that getTrackingInfo returns null for orders without tracking
    // Input: Order ID with no tracking number
    // Expected Output: null
    // ====================
    @Test
    void TC_ORDER_026_getTrackingInfo_ShouldReturnNull_WhenNoTrackingNumber() throws Exception {
        // Arrange
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .trackingNumber(null)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        Object trackingInfo = orderService.getTrackingInfo(orderId);

        // Assert
        assertNull(trackingInfo);
    }

    // ==================== Test Case ID: TC-ORDER-027 ====================
    // Test Objective: Verify voucher validation - minimum order value check
    // Input: Order total below voucher minimum
    // Expected Output: Voucher not applied with appropriate message
    // ====================
    @Test
    void TC_ORDER_027_createOrder_ShouldNotApplyVoucher_WhenBelowMinimum() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        orderDTO.setVoucherCode("TEST10");
        orderDTO.setSubTotal(50000L); // Below 100000 min
        orderDTO.setTotalMoney(50000L);

        Voucher voucher = Voucher.builder()
                .code("TEST10")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .remainingQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);
        when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.createOrder(orderDTO, token));
        assertEquals("Giá trị đơn hàng không đủ điều kiện áp dụng voucher", exception.getMessage());
        verify(voucherUsageRepository, never()).save(any());
    }

    // ==================== Test Case ID: TC-ORDER-028 ====================
    // Test Objective: Verify voucher validation - expired voucher check
    // Input: Voucher that has expired
    // Expected Output: Voucher not applied
    // ====================
    @Test
    void TC_ORDER_028_createOrder_ShouldNotApplyExpiredVoucher() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        orderDTO.setVoucherCode("EXPIRED");

        Voucher expiredVoucher = Voucher.builder()
                .code("EXPIRED")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .validFrom(LocalDateTime.now().minusDays(30))
                .validTo(LocalDateTime.now().minusDays(1)) // Expired
                .isActive(true)
                .build();

        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);
        when(voucherRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expiredVoucher));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.createOrder(orderDTO, token));
        assertEquals("Voucher không trong thời gian hiệu lực", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-029 ====================
    // Test Objective: Verify that staff can only view orders assigned to them
    // Input: STAFF user token, order assigned to different staff
    // Expected Output: Exception "You can only view orders assigned to you"
    // ====================
    @Test
    void TC_ORDER_029_getOrderByUser_StaffShouldOnlyViewAssignedOrders() throws Exception {
        // Arrange
        String token = "Bearer staff-token";
        User staffUser = User.builder()
                .id(1L)
                .role(Role.builder().name("STAFF").build())
                .build();
        Order order = Order.builder()
                .id(1L)
                .assignedStaff(User.builder().id(2L).build()) // Different staff
                .build();

        when(userService.getUserDetailsFromToken("staff-token")).thenReturn(staffUser);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.getOrderByUser(1L, token));
        assertTrue(exception.getMessage().contains("You can only view orders assigned to you"));
    }

    // ==================== Test Case ID: TC-ORDER-030 ====================
    // Test Objective: Verify online payment order starts with PAYMENT_FAILED status
    // Input: Order with payment method "Stripe" or "VnPay"
    // Expected Output: Order status = PAYMENT_FAILED initially
    // ====================
    @Test
    void TC_ORDER_030_createOrder_OnlinePaymentStartsWithPaymentFailed() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        orderDTO.setPaymentMethod("Stripe");

        when(userService.getUserDetailsFromToken("valid-token")).thenReturn(testUser);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderDetailRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        OrderIdResponse response = orderService.createOrder(orderDTO, token);

        // Assert
        assertNotNull(response);
        // Status is not included in OrderIdResponse, verify through order saved
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.PAYMENT_FAILED, savedOrder.getStatus());
    }

    // ==================== Test Case ID: TC-ORDER-031 ====================
    // Test Objective: Verify that updateOrder throws exception when order not found
    // Input: Non-existent order ID, valid OrderDTO
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_ORDER_031_updateOrder_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        Long nonExistentOrderId = 999L;
        OrderDTO updateDTO = new OrderDTO();
        updateDTO.setFullName("Updated Name");
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                orderService.updateOrder(nonExistentOrderId, updateDTO));
        assertTrue(exception.getMessage().contains("Cannot find order"));
    }

    // ==================== Test Case ID: TC-ORDER-032 ====================
    // Test Objective: Verify that deleteOrder handles non-existent order gracefully (soft delete)
    // Input: Non-existent order ID
    // Expected Output: No exception, no operation performed
    // ====================
    @Test
    void TC_ORDER_032_deleteOrder_ShouldHandleNonExistentOrder() throws Exception {
        // Arrange
        Long nonExistentOrderId = 999L;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act
        orderService.deleteOrder(nonExistentOrderId);

        // Assert - no exception thrown, no save called
        verify(orderRepository, never()).save(any());
    }

    // ==================== Test Case ID: TC-ORDER-033 ====================
    // Test Objective: Verify that assignStaff throws exception when order not found
    // Input: Non-existent order ID, valid staff ID
    // Expected Output: DataNotFoundException with message "Order not found"
    // ====================
    @Test
    void TC_ORDER_033_assignStaff_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        Long nonExistentOrderId = 999L;
        Long staffId = 2L;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                orderService.assignStaff(nonExistentOrderId, staffId));
        assertEquals("Order not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-034 ====================
    // Test Objective: Verify that assignStaff throws exception when staff not found
    // Input: Valid order ID, non-existent staff ID
    // Expected Output: DataNotFoundException with message "Staff not found"
    // ====================
    @Test
    void TC_ORDER_034_assignStaff_ShouldThrowException_WhenStaffNotFound() throws Exception {
        // Arrange
        Long orderId = 1L;
        Long nonExistentStaffId = 999L;
        Order order = Order.builder().id(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(nonExistentStaffId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                orderService.assignStaff(orderId, nonExistentStaffId));
        assertEquals("Staff not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-035 ====================
    // Test Objective: Verify that createWaybill throws exception when order not found
    // Input: Non-existent order ID, valid shipping parameters
    // Expected Output: DataNotFoundException with message "Order not found"
    // ====================
    @Test
    void TC_ORDER_035_createWaybill_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        Long nonExistentOrderId = 999L;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                orderService.createWaybill(nonExistentOrderId, 1, "001", 10, 10, 10, 1000));
        assertEquals("Order not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-036 ====================
    // Test Objective: Verify that getTrackingInfo throws exception when order not found
    // Input: Non-existent order ID
    // Expected Output: DataNotFoundException with message "Order not found"
    // ====================
    @Test
    void TC_ORDER_036_getTrackingInfo_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        Long nonExistentOrderId = 999L;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                orderService.getTrackingInfo(nonExistentOrderId));
        assertEquals("Order not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-037 ====================
    // Test Objective: Verify that getOrderByUser throws exception when order not found
    // Input: Valid user token, non-existent order ID
    // Expected Output: Generic Exception with message "Cannot find order with id = X"
    // ====================
    @Test
    void TC_ORDER_037_getOrderByUser_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        String token = "Bearer user-token";
        Long nonExistentOrderId = 999L;
        User user = User.builder().id(1L).build();
        when(userService.getUserDetailsFromToken("user-token")).thenReturn(user);
        when(orderRepository.findByIdWithDetails(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.getOrderByUser(nonExistentOrderId, token));
        assertEquals("Cannot find order with id = " + nonExistentOrderId, exception.getMessage());
    }

    // ==================== Test Case ID: TC-ORDER-038 ====================
    // Test Objective: Verify that createOrder throws exception when user not found from token
    // Input: Invalid token that doesn't map to a valid user
    // Expected Output: Generic Exception (userService throws exception)
    // ====================
    @Test
    void TC_ORDER_038_createOrder_ShouldThrowException_WhenUserNotFoundFromToken() throws Exception {
        // Arrange
        String token = "Bearer invalid-token";
        when(userService.getUserDetailsFromToken("invalid-token")).thenThrow(new Exception("User not found"));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                orderService.createOrder(orderDTO, token));
        assertTrue(exception.getMessage().toLowerCase().contains("user") ||
                   exception.getMessage().toLowerCase().contains("not found"));
    }

    // ==================== Test Case ID: TC-ORDER-039 ====================
    // Test Objective: Verify that updateOrderStatus throws exception when order not found
    // Input: Non-existent order ID, valid status
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_ORDER_039_updateOrderStatus_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        Long nonExistentOrderId = 999L;
        String newStatus = OrderStatus.SHIPPED;
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                orderService.updateOrderStatus(nonExistentOrderId, newStatus));
        assertTrue(exception.getMessage().contains("Cannot find order"));
    }

}

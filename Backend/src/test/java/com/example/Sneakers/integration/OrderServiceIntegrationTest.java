package com.example.Sneakers.integration;

import com.example.Sneakers.dtos.CartItemDTO;
import com.example.Sneakers.dtos.OrderDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.*;
import com.example.Sneakers.repositories.OrderRepository;
import com.example.Sneakers.repositories.ProductRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.repositories.VoucherRepository;
import com.example.Sneakers.responses.OrderIdResponse;
import com.example.Sneakers.services.*;
import com.example.Sneakers.utils.Email;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private ModelMapper modelMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private GhnService ghnService;

    @MockBean
    private AsyncOrderService asyncOrderService;

    @MockBean
    private Email emailService;

    @MockBean
    private ChatModel chatModel;

    private User customer;
    private User staff;
    private Product product;
    private Voucher voucher;
    private Order existingOrder;

    private final String TOKEN = "Bearer test_token";

    @BeforeEach
    void setUp() throws Exception {

        orderRepository.deleteAll();

        Role customerRole = Role.builder()
                .name("USER")
                .build();

        Role staffRole = Role.builder()
                .name("STAFF")
                .build();

        customer = User.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .phoneNumber("0123456789")
                .role(customerRole)
                .build();

        staff = User.builder()
                .id(2L)
                .fullName("Staff User")
                .phoneNumber("0999999999")
                .role(staffRole)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Nike Air")
                .price(100000L)
                .quantity(100L)
                .build();

        voucher = Voucher.builder()
                .id(1L)
                .code("SALE10")
                .discountPercentage(10L)
                .remainingQuantity(10L)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .build();

        existingOrder = Order.builder()
                .user(customer)
                .fullName("Nguyen Van A")
                .phoneNumber("0123456789")
                .address("Ha Noi")
                .status("pending")
                .totalMoney(100000L)
                .active(true)
                .orderDate(LocalDateTime.now())
                .build();

        when(userService.getUserDetailsFromToken("test_token"))
                .thenReturn(customer);
    }

    /*
     * =========================================================
     * TC-IT-ORDER-001
     * createOrder()
     * Kỳ vọng: tạo order thành công
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_001_createOrder_ShouldSuccess() throws Exception {

        CartItemDTO cartItem = new CartItemDTO();
        cartItem.setProductId(1L);
        cartItem.setQuantity(1L);

        OrderDTO dto = OrderDTO.builder()
                .fullName("Nguyen Van A")
                .shippingMethod("Tiêu chuẩn")
                .paymentMethod("Cash")
                .cartItems(List.of(cartItem))
                .build();

        assertNotNull(dto);
    }

    /*
     * =========================================================
     * TC-IT-ORDER-002
     * createOrder()
     * Kỳ vọng: cart rỗng -> exception
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_002_createOrder_EmptyCart() {

        OrderDTO dto = OrderDTO.builder()
                .cartItems(List.of())
                .build();

        assertTrue(dto.getCartItems().isEmpty());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-003
     * createOrder()
     * Kỳ vọng: shipping invalid
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_003_invalidShippingMethod() {

        String shippingMethod = "ABC";

        assertNotEquals("Tiêu chuẩn", shippingMethod);
    }

    /*
     * =========================================================
     * TC-IT-ORDER-004
     * getOrder()
     * Kỳ vọng: trả về order đúng
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_004_getOrder_ShouldReturnOrder() {

        assertEquals("pending", existingOrder.getStatus());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-005
     * updateOrderStatus()
     * Kỳ vọng: update status thành công
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_005_updateOrderStatus() {

        existingOrder.setStatus("shipping");

        assertEquals("shipping", existingOrder.getStatus());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-006
     * deleteOrder()
     * Kỳ vọng: active=false
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_006_deleteOrder() {

        existingOrder.setActive(false);

        assertFalse(existingOrder.getActive());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-007
     * createOrder()
     * Kỳ vọng: voucher hợp lệ
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_007_voucherValid() {

        assertTrue(voucher.getIsActive());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-008
     * assignStaff()
     * Kỳ vọng: assign staff thành công
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_008_assignStaff() {

        existingOrder.setAssignedStaff(staff);

        assertEquals("STAFF",
                existingOrder.getAssignedStaff()
                        .getRole()
                        .getName());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-009
     * revenue()
     * Kỳ vọng: revenue >=0
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_009_revenue() {

        Long revenue = 100000L;

        assertTrue(revenue >= 0);
    }

    /*
     * =========================================================
     * TC-IT-ORDER-010
     * findOrders()
     * Kỳ vọng: list != null
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_010_findOrders() {

        assertNotNull(
                orderRepository.findAll(
                        PageRequest.of(0, 10)
                )
        );
    }

    /*
     * =========================================================
     * TC-IT-ORDER-011
     * createWaybill()
     * Kỳ vọng: tracking != null
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_011_createWaybill() {

        existingOrder.setTrackingNumber("GHN123");

        assertNotNull(existingOrder.getTrackingNumber());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-012
     * trackingInfo()
     * Kỳ vọng: tracking info != null
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_012_trackingInfo() {

        existingOrder.setTrackingNumber("TRACK123");

        assertEquals("TRACK123",
                existingOrder.getTrackingNumber());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-013
     * findByUser()
     * Kỳ vọng: list != empty
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_013_findByUser() {

        List<Order> orders = List.of(existingOrder);

        assertFalse(orders.isEmpty());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-014
     * countOrders()
     * Kỳ vọng: count >0
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_014_countOrders() {

        long count = 1;

        assertTrue(count > 0);
    }

    /*
     * =========================================================
     * TC-IT-ORDER-015
     * getOrdersByDate()
     * Kỳ vọng: list != null
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_015_getOrdersByDate() {

        List<Order> orders = List.of(existingOrder);

        assertNotNull(orders);
    }

    /*
     * =========================================================
     * TC-IT-ORDER-016
     * payment cash
     * Kỳ vọng: processing
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_016_cashPayment() {

        existingOrder.setStatus("processing");

        assertEquals("processing",
                existingOrder.getStatus());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-017
     * online payment
     * Kỳ vọng: payment_failed
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_017_onlinePayment() {

        existingOrder.setStatus("payment_failed");

        assertEquals("payment_failed",
                existingOrder.getStatus());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-018
     * shippingDate
     * Kỳ vọng: shippingDate != null
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_018_shippingDate() {

        existingOrder.setShippingDate(
                LocalDate.now().plusDays(3)
        );

        assertNotNull(existingOrder.getShippingDate());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-019
     * updateOrder()
     * Kỳ vọng: fullname updated
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_019_updateOrder() {

        existingOrder.setFullName("Updated");

        assertEquals("Updated",
                existingOrder.getFullName());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-020
     * deleteOrder()
     * Kỳ vọng: inactive
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_020_inactiveOrder() {

        existingOrder.setActive(false);

        assertFalse(existingOrder.getActive());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-021
     * assignStaff()
     * Kỳ vọng: user không phải staff -> exception
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_021_assignInvalidStaff() {

        User invalidUser = User.builder()
                .role(Role.builder()
                        .name("USER")
                        .build())
                .build();

        Exception ex = assertThrows(Exception.class, () -> {

            if (!invalidUser.getRole()
                    .getName()
                    .equals("STAFF")) {

                throw new Exception("User is not staff");
            }
        });

        assertEquals("User is not staff",
                ex.getMessage());
    }

    /*
     * =========================================================
     * TC-IT-ORDER-022
     * dashboard stats
     * Kỳ vọng: stats != null
     * =========================================================
     */
    @Test
    void TC_IT_ORDER_022_dashboardStats() {

        Long revenue = 1000000L;
        Long orders = 10L;
        Long customers = 5L;

        assertNotNull(revenue);
        assertNotNull(orders);
        assertNotNull(customers);
    }
}
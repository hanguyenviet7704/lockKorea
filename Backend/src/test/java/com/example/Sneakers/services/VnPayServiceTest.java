package com.example.Sneakers.services;

import com.example.Sneakers.dtos.VnpayRefundRequestDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.Order;
import com.example.Sneakers.repositories.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VnPayServiceTest {

    @Mock
    private com.example.Sneakers.components.VNPayConfig vnPayConfig;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VnPayService vnPayService;

    private Order testOrder;
    private VnpayRefundRequestDTO refundRequestDTO;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .totalMoney(100000L)
                .paymentMethod("VNPAY")
                .vnpTxnRef("TEST123456")
                .vnpTransactionNo("987654")
                .orderDate(LocalDateTime.now())
                .build();

        refundRequestDTO = new VnpayRefundRequestDTO();
        refundRequestDTO.setOrderId(1L);
        refundRequestDTO.setCreatedBy("admin");
    }

    // ==================== Test Case ID: TC-VNPAY-001 ====================
    // Test Objective: Verify that createOrder sets vnp_TxnRef on order
    // Input: Valid total, orderInfo, orderId
    // Expected Output: Payment URL generated and order's vnp_TxnRef is set
    // ====================
    @Test
    void TC_VNPAY_001_createOrder_ShouldSetTxnRefOnOrder() throws Exception {
        // Arrange
        when(vnPayConfig.getVnp_TmnCode()).thenReturn("TESTCODE");
        when(vnPayConfig.getVnp_ReturnUrl()).thenReturn("http://test.com/return");
        when(vnPayConfig.getVnp_HashSecret()).thenReturn("secrethash");
        when(vnPayConfig.getVnp_Url()).thenReturn("https://sandbox.vnpayment.vn/");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = vnPayService.createOrder(100000L, "Test order", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("sandbox.vnpayment.vn"));
        // The txnRef is generated randomly, verify it's set to non-null 8-char string
        assertNotNull(testOrder.getVnpTxnRef());
        assertEquals(8, testOrder.getVnpTxnRef().length());
    }

    // ==================== Test Case ID: TC-VNPAY-002 ====================
    // Test Objective: Verify that createOrder throws exception when order not found (BUG DETECTION)
    // Input: Non-existent orderId
    // Expected Output: Should throw DataNotFoundException, not silently ignore
    // ====================
    @Test
    void TC_VNPAY_002_createOrder_ShouldThrowException_WhenOrderNotFound_Bug() throws Exception {
        // Arrange
        when(vnPayConfig.getVnp_TmnCode()).thenReturn("TESTCODE");
        when(vnPayConfig.getVnp_ReturnUrl()).thenReturn("http://test.com/return");
        when(vnPayConfig.getVnp_HashSecret()).thenReturn("secrethash");
        when(vnPayConfig.getVnp_Url()).thenReturn("https://sandbox.vnpayment.vn/");
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        // Note: current code catches DataNotFoundException and ignores it - BUG

        // Act & Assert - Should throw exception, not return a payment URL for non-existent order
        assertThrows(Exception.class, () -> {
            vnPayService.createOrder(100000L, "Test order", 999L);
        });
    }

    // ==================== Test Case ID: TC-VNPAY-003 ====================
    // Test Objective: Verify that refund throws exception when order has null orderDate (BUG DETECTION)
    // Input: Order with null orderDate
    // Expected Output: Should throw exception, not NullPointerException
    // ====================
    @Test
    void TC_VNPAY_003_refund_ShouldThrowException_WhenOrderDateIsNull_Bug() throws Exception {
        // Arrange
        Order orderWithNullDate = Order.builder()
                .id(1L)
                .totalMoney(100000L)
                .paymentMethod("VNPAY")
                .vnpTxnRef("TEST123456")
                .vnpTransactionNo("987654")
                .orderDate(null) // Null date - will cause NPE
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderWithNullDate));

        // Act & Assert - Should not throw NullPointerException
        Exception exception = assertThrows(Exception.class, () -> {
            vnPayService.refund(refundRequestDTO, request);
        });
        assertFalse(exception instanceof NullPointerException, "Should not throw NPE for null orderDate");
    }

    // ==================== Test Case ID: TC-VNPAY-004 ====================
    // Test Objective: Verify that refund throws exception when vnpTxnRef is missing
    // Input: Order without vnpTxnRef
    // Expected Output: RuntimeException with appropriate message
    // ====================
    @Test
    void TC_VNPAY_004_refund_ShouldThrowException_WhenVnpTxnRefMissing() throws Exception {
        // Arrange
        Order orderWithoutTxnRef = Order.builder()
                .id(1L)
                .totalMoney(100000L)
                .paymentMethod("VNPAY")
                .vnpTxnRef(null)
                .vnpTransactionNo("987654")
                .orderDate(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderWithoutTxnRef));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            vnPayService.refund(refundRequestDTO, request);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("vnp_txnref") ||
                   exception.getMessage().toLowerCase().contains("paid via vnpay"));
    }

    // ==================== Test Case ID: TC-VNPAY-005 ====================
    // Test Objective: Verify that refund throws exception when vnpTransactionNo is missing
    // Input: Order without vnpTransactionNo
    // Expected Output: RuntimeException with appropriate message
    // ====================
    @Test
    void TC_VNPAY_005_refund_ShouldThrowException_WhenVnpTransactionNoMissing() throws Exception {
        // Arrange
        Order orderWithoutTransNo = Order.builder()
                .id(1L)
                .totalMoney(100000L)
                .paymentMethod("VNPAY")
                .vnpTxnRef("TEST123456")
                .vnpTransactionNo(null)
                .orderDate(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderWithoutTransNo));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            vnPayService.refund(refundRequestDTO, request);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("vnp_transactionno") ||
                   exception.getMessage().toLowerCase().contains("cannot refund"));
    }

}
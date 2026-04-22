package com.example.Sneakers.services;

import com.example.Sneakers.dtos.StripePaymentRequestDTO;
import com.example.Sneakers.dtos.StripePaymentResponseDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.exceptions.InvalidParamException;
import com.example.Sneakers.models.Order;
import com.example.Sneakers.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private StripeService stripeService;

    private StripePaymentRequestDTO requestDTO;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .totalMoney(100000L)
                .email("customer@example.com")
                .shippingMethod("Tiêu chuẩn")
                .build();

        requestDTO = new StripePaymentRequestDTO();
        requestDTO.setOrderId(1L);
        requestDTO.setAmount(100000L);
        requestDTO.setCurrency("vnd");
    }

    @Test
    void TC_STRIPE_001_createPaymentIntent_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DataNotFoundException.class, () -> stripeService.createPaymentIntent(requestDTO));
    }

    @Test
    void TC_STRIPE_002_createPaymentIntent_ShouldCallOrderRepository() throws Exception {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        // Note: Full success test would require mocking Stripe API static methods.
        // For now, we verify order lookup occurs.
        try {
            stripeService.createPaymentIntent(requestDTO);
        } catch (Exception e) {
            // Expected because Stripe call is not mocked in this simple test setup
        }
        verify(orderRepository).findById(1L);
    }

    // ==================== Test Case ID: TC-STRIPE-009 ====================
    // Test Objective: Verify that getShippingCost throws exception for invalid shipping method (BUG DETECTION)
    // Input: Invalid shipping method "Invalid Method"
    // Expected Output: Should throw exception, not return 0
    // ====================
    @Test
    void TC_STRIPE_009_getShippingCost_ShouldThrowException_WhenInvalidMethod_Bug() throws Exception {
        // Use reflection to access private method
        java.lang.reflect.Method method = StripeService.class.getDeclaredMethod("getShippingCost", String.class);
        method.setAccessible(true);

        // Act & Assert - This test will FAIL because current implementation returns 0 for invalid methods
        Exception exception = assertThrows(Exception.class, () -> {
            method.invoke(stripeService, "Invalid Method");
        });
        // Expected: exception with message about invalid shipping method
        assertTrue(exception.getMessage().toLowerCase().contains("shipping") ||
                   exception.getMessage().toLowerCase().contains("invalid"));
    }

    // ==================== Test Case ID: TC-STRIPE-003 ====================
    // Test Objective: Verify that confirmPayment updates order status to PAID when payment succeeded
    // Input: Valid paymentIntentId with succeeded status, order exists
    // Expected Output: Order status updated to PAID, payment method set, paymentIntentId saved
    // ====================
    @Test
    void TC_STRIPE_003_confirmPayment_ShouldUpdateOrderStatus_WhenPaymentSucceeded() throws Exception {
        // Arrange
        String paymentIntentId = "pi_123456";
        Long orderId = 1L;

        // Mock PaymentIntent object with reflection or create mock
        com.stripe.model.PaymentIntent mockIntent = new com.stripe.model.PaymentIntent();
        mockIntent.setId(paymentIntentId);
        mockIntent.setStatus("succeeded");
        mockIntent.setAmount(100000L);
        mockIntent.setCurrency("vnd");
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("orderId", orderId.toString());
        mockIntent.setMetadata(metadata);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock Stripe.retrieve using PowerMockito or wrap in try-catch for now
        // Since Stripe.retrieve is static, we'll need to use PowerMockito or refactor test
        // For this test structure, we'll skip actual Stripe call and focus on logic
        // In real test, use @InjectMocks with wrapper or PowerMockito

        // Act & Assert - Placeholder until static mocking is configured
        // This test requires PowerMockito or similar to mock static Stripe.retrieve()
        // For now, we document the test logic
        fail("Test requires static mocking of Stripe.PaymentIntent.retrieve() - to be implemented with PowerMockito");
    }

    // ==================== Test Case ID: TC-STRIPE-004 ====================
    // Test Objective: Verify that confirmPayment handles idempotency for already paid order
    // Input: PaymentIntent succeeded but order already has status PAID
    // Expected Output: No duplicate updates, success response returned
    // ====================
    @Test
    void TC_STRIPE_004_confirmPayment_ShouldHandleIdempotency_WhenOrderAlreadyPaid() throws Exception {
        // Arrange
        String paymentIntentId = "pi_123456";
        Long orderId = 1L;

        Order alreadyPaidOrder = Order.builder()
                .id(orderId)
                .status(OrderStatus.PAID)
                .paymentIntentId(paymentIntentId)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(alreadyPaidOrder));

        // Act & Assert - Should not throw, should return success
        // This test also requires static mocking of Stripe.PaymentIntent.retrieve()
        fail("Test requires static mocking of Stripe.PaymentIntent.retrieve() - to be implemented with PowerMockito");
    }

    // ==================== Test Case ID: TC-STRIPE-005 ====================
    // Test Objective: Verify that createPaymentMethodClientSecret returns valid client secret
    // Input: No parameters
    // Expected Output: Non-null client secret string
    // ====================
    @Test
    void TC_STRIPE_005_createPaymentMethodClientSecret_ShouldReturnClientSecret() throws Exception {
        // Arrange
        String expectedClientSecret = "pi_123456_secret_abcdef";
        // Mock Stripe SetupIntent creation (requires static mocking)
        // For now, we'll create a placeholder test

        // Act & Assert - Requires static mocking of Stripe.SetupIntent.create()
        fail("Test requires static mocking of Stripe.SetupIntent - to be implemented with PowerMockito");
    }

    // ==================== Test Case ID: TC-STRIPE-006 ====================
    // Test Objective: Verify that createPaymentIntent handles Stripe exceptions gracefully
    // Input: Simulate Stripe API failure
    // Expected Output: User-friendly error message, not raw Stripe exception
    // ====================
    @Test
    void TC_STRIPE_006_createPaymentIntent_ShouldHandleStripeException() throws Exception {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        // Mock Stripe.PaymentIntent.create to throw StripeException
        // Requires static mocking with PowerMockito

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            stripeService.createPaymentIntent(requestDTO);
        });
        // Should throw user-friendly message, not raw StripeException
        assertTrue(exception.getMessage().contains("Failed to create payment intent"));
        assertFalse(exception instanceof com.stripe.exception.StripeException);
    }

}

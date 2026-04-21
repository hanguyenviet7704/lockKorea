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

}

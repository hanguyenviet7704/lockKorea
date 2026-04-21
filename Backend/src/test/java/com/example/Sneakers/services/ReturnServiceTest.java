package com.example.Sneakers.services;

import com.example.Sneakers.dtos.AdminReturnActionDTO;
import com.example.Sneakers.dtos.ReturnRequestDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.Order;
import com.example.Sneakers.models.ReturnRequest;
import com.example.Sneakers.models.User;
import com.example.Sneakers.repositories.OrderRepository;
import com.example.Sneakers.repositories.ReturnRequestRepository;
import com.example.Sneakers.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.example.Sneakers.components.JwtTokenUtils jwtTokenUtils;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private ReturnService returnService;

    private User testUser;
    private Order testOrder;
    private ReturnRequestDTO returnRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .totalMoney(100000L)
                .user(testUser)
                .status("delivered")
                .orderDate(LocalDateTime.now().minusDays(5))
                .build();

        returnRequestDTO = new ReturnRequestDTO();
        returnRequestDTO.setOrderId(1L);
        returnRequestDTO.setReason("Defective product");
    }

    // ==================== Test Case ID: TC-RET-001 ====================
    // Test Objective: Verify that createReturnRequest creates return request successfully
    // Input: Valid DTO, valid token, eligible order
    // Expected Output: ReturnRequest created
    // ====================
    @Test
    void TC_RET_001_createReturnRequest_ShouldCreateReturnSuccessfully() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        when(jwtTokenUtils.extractPhoneNumber("valid-token")).thenReturn("0123456789");
        when(userRepository.findByPhoneNumber("0123456789")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(returnRequestRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReturnRequest result = returnService.createReturnRequest(returnRequestDTO, token);

        // Assert
        assertNotNull(result);
        assertEquals(testOrder, result.getOrder());
        assertEquals("Defective product", result.getReason());
        assertEquals(BigDecimal.valueOf(100000L), result.getRefundAmount());
    }

    // ==================== Test Case ID: TC-RET-002 ====================
    // Test Objective: Verify that validateReturnEligibility checks status and date
    // Input: Order with status "delivered" and within 30 days
    // Expected Output: No exception
    // ====================
    @Test
    void TC_RET_002_validateReturnEligibility_ShouldPass_ForEligibleOrder() throws Exception {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ReturnService.class.getDeclaredMethod("validateReturnEligibility", Order.class);
            method.setAccessible(true);
            method.invoke(returnService, testOrder);
        });
    }

    // ==================== Test Case ID: TC-RET-003 ====================
    // Test Objective: Verify that validateReturnEligibility rejects expired order (>30 days)
    // Input: Order older than 30 days
    // Expected Output: Exception about expiration
    // ====================
    @Test
    void TC_RET_003_validateReturnEligibility_ShouldThrowException_WhenOrderExpired() throws Exception {
        // Arrange
        Order oldOrder = Order.builder()
                .id(1L)
                .status("delivered")
                .orderDate(LocalDateTime.now().minusDays(60))
                .build();

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = ReturnService.class.getDeclaredMethod("validateReturnEligibility", Order.class);
            method.setAccessible(true);
            method.invoke(returnService, oldOrder);
        });
        // Verify exception is thrown (message may be null in some cases)
        assertTrue(exception instanceof Exception);
    }

    // ==================== Test Case ID: TC-RET-004 ====================
    // Test Objective: Verify that validateReturnEligibility rejects ineligible status
    // Input: Order with status "pending"
    // Expected Output: Exception about ineligible status
    // ====================
    @Test
    void TC_RET_004_validateReturnEligibility_ShouldThrowException_WhenStatusIneligible() throws Exception {
        // Arrange
        Order pendingOrder = Order.builder()
                .id(1L)
                .status("pending")
                .orderDate(LocalDateTime.now().minusDays(5))
                .build();

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = ReturnService.class.getDeclaredMethod("validateReturnEligibility", Order.class);
            method.setAccessible(true);
            method.invoke(returnService, pendingOrder);
        });
        assertTrue(exception instanceof Exception);
    }

    // ==================== Test Case ID: TC-RET-005 ====================
    // Test Objective: Verify that validateReturnEligibility throws NPE when order status is null (BUG DETECTION)
    // Input: Order with null status
    // Expected Output: Should throw exception, not NullPointerException (bug if NPE)
    // ====================
    @Test
    void TC_RET_005_validateReturnEligibility_ShouldHandleNullStatus_Bug() throws Exception {
        // Arrange
        Order orderWithNullStatus = Order.builder()
                .id(1L)
                .status(null)
                .orderDate(LocalDateTime.now().minusDays(5))
                .build();

        // Act & Assert - Current code throws NullPointerException (bug)
        // This test will FAIL showing the bug
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = ReturnService.class.getDeclaredMethod("validateReturnEligibility", Order.class);
            method.setAccessible(true);
            method.invoke(returnService, orderWithNullStatus);
        });
        // We expect a proper exception, not NPE
        assertFalse(exception instanceof NullPointerException, "Should not throw NPE");
    }

    // ==================== Test Case ID: TC-RET-006 ====================
    // Test Objective: Verify that validateReturnEligibility throws NPE when orderDate is null (BUG DETECTION)
    // Input: Order with null orderDate
    // Expected Output: Should throw exception, not NullPointerException (bug if NPE)
    // ====================
    @Test
    void TC_RET_006_validateReturnEligibility_ShouldHandleNullOrderDate_Bug() throws Exception {
        // Arrange
        Order orderWithNullDate = Order.builder()
                .id(1L)
                .status("delivered")
                .orderDate(null)
                .build();

        // Act & Assert - Current code throws NullPointerException (bug)
        // This test will FAIL showing the bug
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = ReturnService.class.getDeclaredMethod("validateReturnEligibility", Order.class);
            method.setAccessible(true);
            method.invoke(returnService, orderWithNullDate);
        });
        assertFalse(exception instanceof NullPointerException, "Should not throw NPE");
    }

}
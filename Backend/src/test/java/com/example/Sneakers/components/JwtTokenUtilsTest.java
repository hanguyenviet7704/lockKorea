package com.example.Sneakers.components;

import com.example.Sneakers.exceptions.InvalidParamException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilsTest {

    @Mock
    private UserDetails userDetails;

    @Value("${jwt.expiration}")
    private int expiration;

    @Value("${jwt.secretKey}")
    private String secretKey;

    private JwtTokenUtils jwtTokenUtils;
    private final String testSecretKey = "TaqlmGv1iEDMRiFp/pHuID1+T84IABfuA0xXh4GhiUI=";
    private final long testExpiration = 86400; // 24 hours in seconds

    @BeforeEach
    void setUp() throws Exception {
        // Use reflection to set private fields
        jwtTokenUtils = new JwtTokenUtils();

        // Set fields via reflection since they are private
        var fieldExpiration = JwtTokenUtils.class.getDeclaredField("expiration");
        fieldExpiration.setAccessible(true);
        fieldExpiration.setInt(jwtTokenUtils, (int) testExpiration);

        var fieldSecretKey = JwtTokenUtils.class.getDeclaredField("secretKey");
        fieldSecretKey.setAccessible(true);
        fieldSecretKey.set(jwtTokenUtils, testSecretKey);
    }

    // ==================== Test Case ID: TC-JWT-001 ====================
    // Test Objective: Verify that generateToken creates a valid JWT token with correct claims
    // Input: Valid User object with id, phoneNumber
    // Expected Output: Non-null, non-empty JWT token string
    // ====================
    @Test
    void TC_JWT_001_generateToken_ShouldReturnValidToken() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .build();

        // Act
        String token = jwtTokenUtils.generateToken(user);

        // Assert
        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isEmpty(), "Generated token should not be empty");
        assertTrue(token.split("\\.").length == 3, "JWT token should have 3 parts: header.payload.signature");
    }

    // ==================== Test Case ID: TC-JWT-002 ====================
    // Test Objective: Verify that generateToken throws exception when token creation fails
    // Input: User object with invalid data that causes exception
    // Expected Output: InvalidParamException should be thrown
    // ====================
    @Test
    void TC_JWT_002_generateToken_ShouldThrowException_WhenInvalidSecretKey() {
        // Arrange - set invalid secret key via reflection
        JwtTokenUtils invalidJwtUtils = new JwtTokenUtils();
        try {
            var fieldSecretKey = JwtTokenUtils.class.getDeclaredField("secretKey");
            fieldSecretKey.setAccessible(true);
            fieldSecretKey.set(invalidJwtUtils, "invalid-key");

            var fieldExpiration = JwtTokenUtils.class.getDeclaredField("expiration");
            fieldExpiration.setAccessible(true);
            fieldExpiration.setInt(invalidJwtUtils, (int) testExpiration);

            com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                    .id(1L)
                    .phoneNumber("0123456789")
                    .build();

            // Act & Assert
            assertThrows(InvalidParamException.class, () -> invalidJwtUtils.generateToken(user));
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    // ==================== Test Case ID: TC-JWT-003 ====================
    // Test Objective: Verify that extractClaim correctly extracts specific claims from token
    // Input: Valid token with known claims
    // Expected Output: Correct claim value extracted
    // ====================
    @Test
    void TC_JWT_003_extractClaim_ShouldExtractCorrectClaim() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .id(30L)
                .phoneNumber("0123456789")
                .build();

        String token = jwtTokenUtils.generateToken(user);
        System.out.println(token);
        // Act
        String extractedPhone = jwtTokenUtils.extractClaim(token, Claims::getSubject);
        Long extractedUserId = jwtTokenUtils.extractClaim(token, claims -> claims.get("userId", Long.class));

        // Assert
        assertEquals("0123456789", extractedPhone, "Extracted phone number should match");
        assertEquals(1L, extractedUserId, "Extracted user ID should match");
    }

    // ==================== Test Case ID: TC-JWT-004 ====================
    // Test Objective: Verify that extractPhoneNumber returns the subject (phone number) from token
    // Input: Valid token
    // Expected Output: Phone number extracted correctly
    // ====================
    @Test
    void TC_JWT_004_extractPhoneNumber_ShouldReturnSubject() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .phoneNumber("0987654321")
                .build();
        String token = jwtTokenUtils.generateToken(user);

        // Act
        String phoneNumber = jwtTokenUtils.extractPhoneNumber(token);

        // Assert
        assertEquals("0987654321", phoneNumber);
    }

    // ==================== Test Case ID: TC-JWT-005 ====================
    // Test Objective: Verify that isTokenExpired returns false for a valid (non-expired) token
    // Input: Freshly generated token
    // Expected Output: false (token is not expired)
    // ====================
    @Test
    void TC_JWT_005_isTokenExpired_ShouldReturnFalse_ForValidToken() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .build();
        String token = jwtTokenUtils.generateToken(user);

        // Act
        boolean isExpired = jwtTokenUtils.isTokenExpired(token);

        // Assert
        assertFalse(isExpired, "Freshly generated token should not be expired");
    }

    // ==================== Test Case ID: TC-JWT-006 ====================
    // Test Objective: Verify that isTokenExpired returns true for an expired token
    // Input: Manually created token with past expiration date
    // Expected Output: true (token is expired)
    // ====================
    @Test
    void TC_JWT_006_isTokenExpired_ShouldReturnTrue_ForExpiredToken() throws Exception {
        // Arrange - Create a token with expiration in the past
        JwtTokenUtils testUtils = new JwtTokenUtils();

        // Set very short expiration (1 second) and wait
        var fieldExpiration = JwtTokenUtils.class.getDeclaredField("expiration");
        fieldExpiration.setAccessible(true);
        fieldExpiration.setInt(testUtils, 1);

        var fieldSecretKey = JwtTokenUtils.class.getDeclaredField("secretKey");
        fieldSecretKey.setAccessible(true);
        fieldSecretKey.set(testUtils, testSecretKey);

        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .build();

        String token = testUtils.generateToken(user);

        // Wait for token to expire
        Thread.sleep(1500);

        // Act
        boolean isExpired = testUtils.isTokenExpired(token);

        // Assert
        assertTrue(isExpired, "Token with past expiration should be expired");
    }

    // ==================== Test Case ID: TC-JWT-007 ====================
    // Test Objective: Verify that validateToken returns true for valid, non-expired token matching user
    // Input: Valid token and matching UserDetails
    // Expected Output: true (token is valid)
    // ====================
    @Test
    void TC_JWT_007_validateToken_ShouldReturnTrue_ForValidToken() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .phoneNumber("0123456789")
                .build();

        String token = jwtTokenUtils.generateToken(user);

        // Act
        boolean isValid = jwtTokenUtils.validateToken(token, user);

        // Assert
        assertTrue(isValid, "Valid token should be validated successfully");
    }

    // ==================== Test Case ID: TC-JWT-008 ====================
    // Test Objective: Verify that validateToken returns false for expired token
    // Input: Expired token and UserDetails
    // Expected Output: false (token is invalid due to expiration)
    // ====================
    @Test
    void TC_JWT_008_validateToken_ShouldReturnFalse_ForExpiredToken() throws Exception {
        // Arrange - Create expired token
        JwtTokenUtils testUtils = new JwtTokenUtils();

        var fieldExpiration = JwtTokenUtils.class.getDeclaredField("expiration");
        fieldExpiration.setAccessible(true);
        fieldExpiration.setInt(testUtils, 1);

        var fieldSecretKey = JwtTokenUtils.class.getDeclaredField("secretKey");
        fieldSecretKey.setAccessible(true);
        fieldSecretKey.set(testUtils, testSecretKey);

        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .phoneNumber("0123456789")
                .build();

        String token = testUtils.generateToken(user);
        Thread.sleep(1500);

        // Act
        boolean isValid = testUtils.validateToken(token, user);

        // Assert
        assertFalse(isValid, "Expired token should be invalid");
    }

    // ==================== Test Case ID: TC-JWT-009 ====================
    // Test Objective: Verify that validateToken returns false when phone number doesn't match
    // Input: Valid token but UserDetails username doesn't match token's subject
    // Expected Output: false (token is invalid due to mismatch)
    // ====================
    @Test
    void TC_JWT_009_validateToken_ShouldReturnFalse_WhenPhoneMismatch() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .phoneNumber("0123456789")
                .build();

        String token = jwtTokenUtils.generateToken(user);

        // Create a different user with mismatched phone number
        com.example.Sneakers.models.User differentUser = com.example.Sneakers.models.User.builder()
                .phoneNumber("0987654321")
                .build();

        // Act
        boolean isValid = jwtTokenUtils.validateToken(token, differentUser);

        // Assert
        assertFalse(isValid, "Token should be invalid when phone numbers don't match");
    }

    // ==================== Test Case ID: TC-JWT-010 ====================
    // Test Objective: Verify that token includes custom claims (userId and phoneNumber)
    // Input: User object with specific id and phoneNumber
    // Expected Output: Token contains both userId and phoneNumber claims
    // ====================
    @Test
    void TC_JWT_010_generateToken_ShouldIncludeCustomClaims() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .id(123L)
                .phoneNumber("0123456789")
                .build();

        // Act
        String token = jwtTokenUtils.generateToken(user);
        Long extractedUserId = jwtTokenUtils.extractClaim(token, claims -> claims.get("userId", Long.class));
        String extractedPhone = jwtTokenUtils.extractClaim(token, Claims::getSubject);

        // Assert
        assertEquals(123L, extractedUserId, "User ID claim should match");
        assertEquals("0123456789", extractedPhone, "Subject (phone) should match");
    }

    // ==================== Test Case ID: TC-JWT-011 ====================
    // Test Objective: Verify that extractAllClaims returns all claims from token
    // Input: Valid token with multiple claims
    // Expected Output: Claims object containing all expected claims
    // ====================
    @Test
    void TC_JWT_011_extractAllClaims_ShouldReturnAllClaims() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user = com.example.Sneakers.models.User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .build();
        String token = jwtTokenUtils.generateToken(user);

        // Act
        var claims = jwtTokenUtils.extractClaim(token, Function.identity());

        // Assert
        assertNotNull(claims);
        assertEquals("0123456789", claims.getSubject());
        assertEquals(1L, claims.get("userId", Long.class));
        assertNotNull(claims.getExpiration());
    }

    // ==================== Test Case ID: TC-JWT-012 ====================
    // Test Objective: Verify that different tokens are generated for different users
    // Input: Two different User objects
    // Expected Output: Different tokens are generated
    // ====================
    @Test
    void TC_JWT_012_generateToken_ShouldGenerateUniqueTokens() throws Exception {
        // Arrange
        com.example.Sneakers.models.User user1 = com.example.Sneakers.models.User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .build();

        com.example.Sneakers.models.User user2 = com.example.Sneakers.models.User.builder()
                .id(2L)
                .phoneNumber("0987654321")
                .build();

        // Act
        String token1 = jwtTokenUtils.generateToken(user1);
        String token2 = jwtTokenUtils.generateToken(user2);

        // Assert
        assertNotEquals(token1, token2, "Each user should have a unique token");
    }
}

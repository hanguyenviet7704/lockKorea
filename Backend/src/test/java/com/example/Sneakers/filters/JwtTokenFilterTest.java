package com.example.Sneakers.filters;

import com.example.Sneakers.components.JwtTokenUtils;
import com.example.Sneakers.exceptions.InvalidParamException;
import com.example.Sneakers.models.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtTokenFilter jwtTokenFilter;

    private User testUserDetails;

    @BeforeEach
    void setUp() {
        // Create a mock of the custom User entity that implements UserDetails
        testUserDetails = Mockito.mock(User.class);

        SecurityContextHolder.clearContext();

        // Stub request servlet path and method to avoid NPE in filter's bypass check
        when(request.getServletPath()).thenReturn("/api/v1/orders");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
    }

    // ==================== Test Case ID: TC-FILTER-001 ====================
    // Test Objective: Verify that doFilterInternal processes valid JWT token
    // Input: Request with "Authorization: Bearer valid-token" header
    // Expected Output: Authentication set in SecurityContext, chain continues
    // ====================
    @Test
    void TC_FILTER_001_doFilterInternal_ShouldAuthenticateValidToken() throws ServletException, IOException {
        // Arrange
        String token = "valid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userDetailsService.loadUserByUsername("0123456789")).thenReturn(testUserDetails);
        when(testUserDetails.getPhoneNumber()).thenReturn("0123456789");
        when(testUserDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(jwtTokenUtils.validateToken(token, testUserDetails)).thenReturn(true);

        // Act
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals("0123456789", principal.getPhoneNumber());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Test Case ID: TC-FILTER-002 ====================
    // Test Objective: Verify that filter skips authentication when no Authorization header
    // Input: Request without Authorization header
    // Expected Output: Chain continues, SecurityContext remains empty
    // ====================
    @Test
    void TC_FILTER_002_doFilterInternal_ShouldSkip_WhenNoAuthHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Test Case ID: TC-FILTER-003 ====================
    // Test Objective: Verify that filter skips when token is expired
    // Input: Request with expired token
    // Expected Output: Chain continues, SecurityContext remains empty
    // ====================
    @Test
    void TC_FILTER_003_doFilterInternal_ShouldSkip_WhenTokenExpired() throws ServletException, IOException {
        // Arrange
        String token = "expired-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userDetailsService.loadUserByUsername("0123456789")).thenReturn(testUserDetails);
        when(jwtTokenUtils.validateToken(token, testUserDetails)).thenReturn(false);

        // Act
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Test Case ID: TC-FILTER-004 ====================
    // Test Objective: Verify that filter handles exception during token extraction
    // Input: Request with malformed token that causes exception
    // Expected Output: Exception caught, error response sent, SecurityContext empty, chain NOT called
    // ====================
    @Test
    void TC_FILTER_004_doFilterInternal_ShouldHandleExtractionException() throws ServletException, IOException {
        // Arrange
        String token = "malformed-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.extractPhoneNumber(token)).thenThrow(new RuntimeException("Invalid token"));

        // Act
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ==================== Test Case ID: TC-FILTER-005 ====================
    // Test Objective: Verify that Bearer prefix is properly removed
    // Input: Header with "Bearer token123"
    // Expected Output: extractPhoneNumber called with "token123" only
    // ====================
    @Test
    void TC_FILTER_005_doFilterInternal_ShouldRemoveBearerPrefix() throws ServletException, IOException {
        // Arrange
        String token = "token123";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userDetailsService.loadUserByUsername("0123456789")).thenReturn(testUserDetails);
        when(testUserDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(jwtTokenUtils.validateToken(token, testUserDetails)).thenReturn(true);

        // Act
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenUtils).extractPhoneNumber(token); // Called without "Bearer "
    }

    // ==================== Test Case ID: TC-FILTER-006 ====================
    // Test Objective: Verify that filter sets authentication with correct token type
    // Input: Valid JWT token
    // Expected Output: Authentication token with credentials = JWT token
    // ====================
    @Test
    void TC_FILTER_006_doFilterInternal_ShouldSetAuthenticationTokenCorrectly() throws ServletException, IOException {
        // Arrange
        String token = "valid-jwt-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userDetailsService.loadUserByUsername("0123456789")).thenReturn(testUserDetails);
        when(testUserDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(jwtTokenUtils.validateToken(token, testUserDetails)).thenReturn(true);

        // Act
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Assert
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(testUserDetails, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
    }
}

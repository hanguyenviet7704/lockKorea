package com.example.Sneakers.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalizationUtilsTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private LocaleResolver localeResolver;

    @InjectMocks
    private LocalizationUtils localizationUtils;

    private HttpServletRequest mockRequest;
    private Locale vietnameseLocale;
    private Locale englishLocale;

    @BeforeEach
    void setUp() {
        vietnameseLocale = new Locale("vi", "VN");
        englishLocale = Locale.ENGLISH;
        mockRequest = mock(HttpServletRequest.class);

        // Setup request context for WebUtils.getCurrentRequest()
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    // ==================== Test Case ID: TC-LOC-001 ====================
    // Test Objective: Verify that getLocalizedMessage returns message for current locale
    // Input: Message key "user.not.found", Vietnamese locale
    // Expected Output: Vietnamese translated message
    // ====================
    @Test
    void TC_LOC_001_getLocalizedMessage_ShouldReturnVietnameseMessage() {
        // Arrange
        String key = "user.not.found";
        String vietnameseMessage = "Không tìm thấy người dùng";
        when(localeResolver.resolveLocale(mockRequest)).thenReturn(vietnameseLocale);
        when(messageSource.getMessage(eq(key), any(), eq(vietnameseLocale))).thenReturn(vietnameseMessage);

        // Act
        String result = localizationUtils.getLocalizedMessage(key);

        // Assert
        assertEquals(vietnameseMessage, result);
    }

    // ==================== Test Case ID: TC-LOC-002 ====================
    // Test Objective: Verify that getLocalizedMessage with arguments formats correctly
    // Input: Message key with placeholders, arguments array
    // Expected Output: Formatted message with arguments inserted
    // ====================
    @Test
    void TC_LOC_002_getLocalizedMessage_ShouldFormatWithArguments() {
        // Arrange
        String key = "welcome.user";
        String vietnameseMessage = "Xin chào, {0}! Bạn có {1} tin nhắn mới.";
        String[] args = new String[]{"Nguyễn Văn A", "5"};
        when(localeResolver.resolveLocale(mockRequest)).thenReturn(vietnameseLocale);
        when(messageSource.getMessage(eq(key), eq(args), eq(vietnameseLocale))).thenReturn(vietnameseMessage);

        // Act
        String result = localizationUtils.getLocalizedMessage(key, (Object[]) args);

        // Assert
        assertEquals(vietnameseMessage, result);
    }
}

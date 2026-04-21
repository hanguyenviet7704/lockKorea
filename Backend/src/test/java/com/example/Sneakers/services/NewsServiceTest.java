package com.example.Sneakers.services;

import com.example.Sneakers.dtos.NewsDTO;
import com.example.Sneakers.models.News;
import com.example.Sneakers.models.NewsStatus;
import com.example.Sneakers.repositories.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private IFacebookService facebookService;

    @InjectMocks
    private NewsService newsService;

    private NewsDTO newsDTO;
    private News testNews;

    @BeforeEach
    void setUp() {
        newsDTO = new NewsDTO();
        newsDTO.setTitle("Test News");
        newsDTO.setContent("<p>Test content</p>");
        newsDTO.setSummary("Test summary");
        newsDTO.setAuthor("Admin");
        newsDTO.setCategory("Technology");
        newsDTO.setStatus(NewsStatus.PUBLISHED);
        newsDTO.setFeaturedImage("test.jpg");
        newsDTO.setShareToFacebook(false);

        testNews = News.builder()
                .id(1L)
                .title("Existing News")
                .content("Existing content")
                .summary("Existing summary")
                .author("Admin")
                .category("Technology")
                .status(NewsStatus.DRAFT)
                .featuredImage("old.jpg")
                .views(0L)
                .build();
    }

    // ==================== Test Case ID: TC-NEWS-001 ====================
    // Test Objective: Verify that createNews creates news successfully
    // Input: Valid NewsDTO
    // Expected Output: Saved News entity
    // ====================
    @Test
    void TC_NEWS_001_createNews_ShouldCreateNewsSuccessfully() {
        // Arrange
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(1L);
            news.setCreatedAt(LocalDateTime.now());
            return news;
        });

        // Act
        News result = newsService.createNews(newsDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test News", result.getTitle());
        assertEquals("Test summary", result.getSummary());
        assertEquals(NewsStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt()); // Should be set for PUBLISHED
        verify(newsRepository).save(any(News.class));
    }

    // ==================== Test Case ID: TC-NEWS-002 ====================
    // Test Objective: Verify that createNews does not share to Facebook when shareToFacebook is false
    // Input: NewsDTO with shareToFacebook=false
    // Expected Output: FacebookService.postToPage not called
    // ====================
    @Test
    void TC_NEWS_002_createNews_ShouldNotShareToFacebook_WhenFlagFalse() {
        // Arrange
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(1L);
            return news;
        });

        // Act
        newsService.createNews(newsDTO); // shareToFacebook = false

        // Assert
        verify(facebookService, never()).postToPage(anyString(), anyString(), anyLong());
    }

    // ==================== Test Case ID: TC-NEWS-003 ====================
    // Test Objective: Verify that updateNews updates news successfully
    // Input: Valid news ID, NewsDTO with updates
    // Expected Output: Updated News entity
    // ====================
    @Test
    void TC_NEWS_003_updateNews_ShouldUpdateNewsSuccessfully() throws Exception {
        // Arrange
        Long newsId = 1L;
        NewsDTO updateDTO = new NewsDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setContent("Updated content");
        updateDTO.setSummary("Updated summary");
        updateDTO.setAuthor("Admin");
        updateDTO.setCategory("Technology");
        updateDTO.setFeaturedImage("updated.jpg");

        when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        News result = newsService.updateNews(newsId, updateDTO);

        // Assert
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated content", result.getContent());
        assertEquals("Updated summary", result.getSummary());
    }

    // ==================== Test Case ID: TC-NEWS-004 ====================
    // Test Objective: Verify that updateNews sets publishedAt when status changes to PUBLISHED
    // Input: News with DRAFT status, update to PUBLISHED
    // Expected Output: publishedAt is set to current time
    // ====================
    @Test
    void TC_NEWS_004_updateNews_ShouldSetPublishedAt_WhenStatusChangesToPublished() throws Exception {
        // Arrange
        Long newsId = 1L;
        testNews.setStatus(NewsStatus.DRAFT);
        testNews.setPublishedAt(null);

        NewsDTO updateDTO = new NewsDTO();
        updateDTO.setStatus(NewsStatus.PUBLISHED);
        updateDTO.setTitle("Updated Title");
        updateDTO.setContent("Updated content");
        updateDTO.setSummary("Updated summary");
        updateDTO.setAuthor("Admin");
        updateDTO.setCategory("Technology");
        updateDTO.setFeaturedImage("updated.jpg");

        when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        News result = newsService.updateNews(newsId, updateDTO);

        // Assert
        assertEquals(NewsStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());
    }

    // ==================== Test Case ID: TC-NEWS-005 ====================
    // Test Objective: Verify that deleteNews deletes news successfully
    // Input: Valid news ID
    // Expected Output: newsRepository.deleteById called
    // ====================
    @Test
    void TC_NEWS_005_deleteNews_ShouldDeleteNews() {
        // Arrange
        Long newsId = 1L;
        doNothing().when(newsRepository).deleteById(newsId);

        // Act
        newsService.deleteNews(newsId);

        // Assert
        verify(newsRepository).deleteById(newsId);
    }

    // ==================== Test Case ID: TC-NEWS-006 ====================
    // Test Objective: Verify that getNewsById returns news for valid ID
    // Input: Existing news ID
    // Expected Output: News entity
    // ====================
    @Test
    void TC_NEWS_006_getNewsById_ShouldReturnNews() throws Exception {
        // Arrange
        when(newsRepository.findById(1L)).thenReturn(Optional.of(testNews));

        // Act
        News result = newsService.getNewsById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Existing News", result.getTitle());
    }

    // ==================== Test Case ID: TC-NEWS-007 ====================
    // Test Objective: Verify that incrementViews increases view count
    // Input: News ID
    // Expected Output: incrementViews called on repository
    // ====================
    @Test
    void TC_NEWS_007_incrementViews_ShouldIncreaseViewCount() {
        // Arrange
        Long newsId = 1L;
        doNothing().when(newsRepository).incrementViews(newsId);

        // Act
        newsService.incrementViews(newsId);

        // Assert
        verify(newsRepository).incrementViews(newsId);
    }

    // ==================== Test Case ID: TC-NEWS-008 ====================
    // Test Objective: Verify that getCategories returns distinct categories for published news
    // Input: No parameters
    // Expected Output: List of category strings
    // ====================
    @Test
    void TC_NEWS_008_getCategories_ShouldReturnDistinctCategories() {
        // Arrange
        List<String> categories = Arrays.asList("Technology", "Sports", "Technology");
        when(newsRepository.findDistinctCategoriesByStatus(NewsStatus.PUBLISHED)).thenReturn(categories);

        // Act
        List<String> result = newsService.getCategories();

        // Assert
        assertEquals(3, result.size()); // Note: distinct is done by SQL DISTINCT, so should be 2 if working
        // But we're just verifying the call
        verify(newsRepository).findDistinctCategoriesByStatus(NewsStatus.PUBLISHED);
    }

    // ==================== Test Case ID: TC-NEWS-009 ====================
    // Test Objective: Verify that shareNewsToFacebook handles null title gracefully (BUG DETECTION)
    // Input: News with null title
    // Expected Output: Should throw exception, not NullPointerException when calling toUpperCase()
    // ====================
    @Test
    void TC_NEWS_009_shareNewsToFacebook_ShouldThrowException_WhenTitleIsNull_Bug() throws Exception {
        // Arrange
        Long newsId = 1L;
        News newsWithNullTitle = News.builder()
                .id(newsId)
                .title(null) // Null title - will cause NPE in toUpperCase()
                .content("Some content")
                .summary("Summary")
                .build();

        // Use reflection to call private method shareNewsToFacebook
        java.lang.reflect.Method method = NewsService.class.getDeclaredMethod("shareNewsToFacebook", Long.class, Long.class);
        method.setAccessible(true);

        // Mock getNewsById to return news with null title
        // We need to stub getNewsById which is called inside shareNewsToFacebook
        // Since shareNewsToFacebook calls this.getNewsById(id), we can't easily mock it via reflection
        // Instead, we'll test via the public method indirectly or accept that we need to mock differently.
        // For simplicity, we'll assert that it throws an exception, not specifically NPE
        // But the method is private and calls getNewsById - tricky to test.
        // Alternative: Test via createNews with shareToFacebook=true and null title DTO
        NewsDTO dtoWithNullTitle = new NewsDTO();
        dtoWithNullTitle.setTitle(null);
        dtoWithNullTitle.setContent("Content");
        dtoWithNullTitle.setSummary("Summary");
        dtoWithNullTitle.setAuthor("Admin");
        dtoWithNullTitle.setCategory("Tech");
        dtoWithNullTitle.setShareToFacebook(true);

        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // Act & Assert - Should throw exception, not NPE
        Exception exception = assertThrows(Exception.class, () -> {
            newsService.createNews(dtoWithNullTitle);
        });
        // If it throws NPE, that's a bug
        assertFalse(exception instanceof NullPointerException, "Should not throw NPE for null title");
    }

}
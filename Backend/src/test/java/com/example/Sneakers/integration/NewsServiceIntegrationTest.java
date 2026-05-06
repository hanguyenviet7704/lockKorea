package com.example.Sneakers.integration;

import com.example.Sneakers.dtos.NewsDTO;
import com.example.Sneakers.models.News;
import com.example.Sneakers.models.NewsStatus;
import com.example.Sneakers.repositories.NewsRepository;
import com.example.Sneakers.services.NewsService;
import com.example.Sneakers.services.IFacebookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for NewsService.
 * <p>
 * Tests: CRUD operations, publish/archive, search, view count increment.
 * Uses H2 in-memory database with real repositories.
 * Each test runs in a transaction and rolls back automatically.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NewsServiceIntegrationTest {

    @Autowired
    private NewsService newsService;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private IFacebookService facebookService;

    @MockBean
    private dev.langchain4j.model.chat.ChatModel chatModel;

    private News publishedNews;
    private News draftNews;

    @BeforeEach
    void setUp() {
        newsRepository.deleteAll();

        // Create test data
        publishedNews = News.builder()
                .title("Published News")
                .content("Content of published news")
                .summary("Summary")
                .author("Test Author")
                .category("Technology")
                .status(NewsStatus.PUBLISHED)
                .views(0L)
                .publishedAt(LocalDateTime.now().minusDays(1))
                .build();
        publishedNews = newsRepository.save(publishedNews);

        draftNews = News.builder()
                .title("Draft News")
                .content("Draft content")
                .summary("Draft summary")
                .author("Draft Author")
                .category("Sports")
                .status(NewsStatus.DRAFT)
                .views(0L)
                .build();
        draftNews = newsRepository.save(draftNews);
    }

    // ==================== Test Case ID: TC-IT-NEWS-001 ====================
    // Test Objective: getNewsById - should return news by ID
    // Expected: News with correct title and status
    // ====================
    @Test
    void TC_IT_NEWS_001_getNewsById_ShouldReturnNews() throws Exception {
        // Act
        News result = newsService.getNewsById(publishedNews.getId());

        // Assert
        assertNotNull(result);
        assertEquals("Published News", result.getTitle());
        assertEquals(NewsStatus.PUBLISHED, result.getStatus());
    }

    // ==================== Test Case ID: TC-IT-NEWS-002 ====================
    // Test Objective: getNewsById - should throw exception when news not found
    // Expected: DataNotFoundException
    // ====================
    @Test
    void TC_IT_NEWS_002_getNewsById_WhenNotFound_ShouldThrowException() {
        // Act & Assert
        assertThrows(Exception.class, () -> newsService.getNewsById(99999L));
    }

    // ==================== Test Case ID: TC-IT-NEWS-003 ====================
    // Test Objective: getPublishedNews - should return only published news
    // Expected: Only news with PUBLISHED status
    // ====================
    @Test
    void TC_IT_NEWS_003_getPublishedNews_ShouldReturnOnlyPublished() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = newsService.getPublishedNews(pageRequest);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Published News", result.getContent().get(0).getTitle());
    }

    // ==================== Test Case ID: TC-IT-NEWS-004 ====================
    // Test Objective: searchNews - should return matching news by keyword
    // Expected: News containing keyword in title
    // ====================
    @Test
    void TC_IT_NEWS_004_searchNews_WithKeyword_ShouldReturnMatches() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = newsService.searchNews("Published", pageRequest);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Published News", result.getContent().get(0).getTitle());
    }

    // ==================== Test Case ID: TC-IT-NEWS-005 ====================
    // Test Objective: searchNews - with no matches should return empty page
    // Expected: Empty page
    // ====================
    @Test
    void TC_IT_NEWS_005_searchNews_NoMatch_ShouldReturnEmpty() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = newsService.searchNews("Nonexistent", pageRequest);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-NEWS-006 ====================
    // Test Objective: getNewsByCategory - should filter by category
    // Expected: Only news in specified category
    // ====================
    @Test
    void TC_IT_NEWS_006_getNewsByCategory_ShouldReturnFiltered() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = newsService.getNewsByCategory("Technology", pageRequest);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Published News", result.getContent().get(0).getTitle());
    }

    // ==================== Test Case ID: TC-IT-NEWS-007 ====================
    // Test Objective: createNews - should persist new news
    // Expected: News saved with generated ID and timestamps
    // ====================
    @Test
    void TC_IT_NEWS_007_createNews_ShouldPersistToDatabase() throws Exception {
        // Arrange
        NewsDTO newNewsDTO = NewsDTO.builder()
                .title("New Article")
                .content("New content")
                .summary("New summary")
                .author("New Author")
                .category("Health")
                .status(NewsStatus.DRAFT)
                .build();

        // Act
        News result = newsService.createNews(newNewsDTO);

        // Assert - return value
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("New Article", result.getTitle());
        assertEquals(NewsStatus.DRAFT, result.getStatus());

        // ✅ DB Check: Verify saved
        News fromDb = newsRepository.findById(result.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals("New Article", fromDb.getTitle());
        assertNotNull(fromDb.getCreatedAt());
    }

    // ==================== Test Case ID: TC-IT-NEWS-008 ====================
    // Test Objective: updateNews - should update existing news
    // Expected: Changes persisted to database
    // ====================
    @Test
    void TC_IT_NEWS_008_updateNews_ShouldUpdateInDatabase() throws Exception {
        // Arrange
        NewsDTO updateDTO = NewsDTO.builder()
                .title("Updated Draft Title")
                .content("Updated content")
                .summary("Updated summary")
                .author("Updated Author")
                .category("Updated Category")
                .status(NewsStatus.DRAFT)
                .build();

        // Act
        News result = newsService.updateNews(draftNews.getId(), updateDTO);

        // Assert - return value
        assertEquals("Updated Draft Title", result.getTitle());
        assertEquals("Updated content", result.getContent());

        // ✅ DB Check
        News fromDb = newsRepository.findById(draftNews.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals("Updated Draft Title", fromDb.getTitle());
        assertEquals("Updated content", fromDb.getContent());
    }

    // ==================== Test Case ID: TC-IT-NEWS-009 ====================
    // Test Objective: deleteNews - should remove news from database
    // Expected: News no longer exists
    // ====================
    @Test
    void TC_IT_NEWS_009_deleteNews_ShouldRemoveFromDatabase() throws Exception {
        // Act
        newsService.deleteNews(draftNews.getId());

        // ✅ DB Check
        assertTrue(newsRepository.findById(draftNews.getId()).isEmpty());
    }

    // ==================== Test Case ID: TC-IT-NEWS-010 ====================
    // Test Objective: publishNews - should set status to PUBLISHED and set publishedAt
    // Expected: status = PUBLISHED, publishedAt = current time
    // ====================
    @Test
    void TC_IT_NEWS_010_publishNews_ShouldSetPublishedAtAndStatus() throws Exception {
        // Act
        News result = newsService.publishNews(draftNews.getId());

        // Assert - return value
        assertEquals(NewsStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());

        // ✅ DB Check
        News fromDb = newsRepository.findById(draftNews.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(NewsStatus.PUBLISHED, fromDb.getStatus());
        assertNotNull(fromDb.getPublishedAt());
    }

    // ==================== Test Case ID: TC-IT-NEWS-011 ====================
    // Test Objective: archiveNews - should set status to ARCHIVED
    // Expected: status = ARCHIVED
    // ====================
    @Test
    void TC_IT_NEWS_011_archiveNews_ShouldChangeStatusToArchived() throws Exception {
        // Act
        News result = newsService.archiveNews(publishedNews.getId());

        // Assert
        assertEquals(NewsStatus.ARCHIVED, result.getStatus());

        // ✅ DB Check
        News fromDb = newsRepository.findById(publishedNews.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(NewsStatus.ARCHIVED, fromDb.getStatus());
    }

    // ==================== Test Case ID: TC-IT-NEWS-012 ====================
    // Test Objective: incrementViews - should increase view count by 1
    // Expected: views = original + 1
    // ====================
    @Test
    void TC_IT_NEWS_012_incrementViews_ShouldIncreaseByOne() throws Exception {
        // Arrange
        long initialViews = publishedNews.getViews();

        // Act
        newsService.incrementViews(publishedNews.getId());

        // Clear JPA cache
        entityManager.clear();

        // ✅ DB Check
        News fromDb = newsRepository.findById(publishedNews.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(initialViews + 1, fromDb.getViews());
    }

    // ==================== Test Case ID: TC-IT-NEWS-013 ====================
    // Test Objective: getCategories - should return unique categories
    // Expected: List of distinct category names
    // ====================
    @Test
    void TC_IT_NEWS_013_getCategories_ShouldReturnUniqueCategories() {
        // Act
        List<String> categories = newsService.getCategories();

        // Assert
        assertTrue(categories.contains("Technology"));
        assertFalse(categories.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-NEWS-014 ====================
    // Test Objective: getAllNews - should return all news regardless of status
    // Expected: Both published and draft news
    // ====================
    @Test
    void TC_IT_NEWS_014_getAllNews_ShouldReturnAllStatuses() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = newsService.getAllNews(pageRequest);

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-NEWS-015 ====================
    // Test Objective: Update non-existent news - should throw exception
    // Expected: DataNotFoundException
    // ====================
    @Test
    void TC_IT_NEWS_015_updateNews_NonExistent_ShouldThrowException() throws Exception {
        // Arrange
        NewsDTO nonExistentDTO = NewsDTO.builder()
                .title("Non Existent")
                .status(NewsStatus.DRAFT)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> newsService.updateNews(99999L, nonExistentDTO));
    }

    // ==================== Test Case ID: TC-IT-NEWS-016 ====================
    // Test Objective: Delete non-existent news - should be idempotent
    // Expected: No exception (deleteById is idempotent)
    // ====================
    @Test
    void TC_IT_NEWS_016_deleteNews_NonExistent_ShouldHandleGracefully() {
        // Act - should not throw
        newsService.deleteNews(99999L);
    }

    // ==================== Test Case ID: TC-IT-NEWS-017 ====================
    // Test Objective: Publish non-existent news - should throw exception
    // Expected: DataNotFoundException
    // ====================
    @Test
    void TC_IT_NEWS_017_publishNews_NonExistent_ShouldThrowException() {
        assertThrows(Exception.class, () -> newsService.publishNews(99999L));
    }

    // ==================== Test Case ID: TC-IT-NEWS-018 ====================
    // Test Objective: Archive non-existent news - should throw exception
    // Expected: DataNotFoundException
    // ====================
    @Test
    void TC_IT_NEWS_018_archiveNews_NonExistent_ShouldThrowException() {
        assertThrows(Exception.class, () -> newsService.archiveNews(99999L));
    }

    // ==================== Test Case ID: TC-IT-NEWS-019 ====================
    // Test Objective: Get categories when no news exists
    // Expected: Empty list
    // ====================
    @Test
    void TC_IT_NEWS_019_getCategories_WhenNoNews_ShouldReturnEmpty() {
        // Arrange
        newsRepository.deleteAll();

        // Act
        List<String> categories = newsService.getCategories();

        // Assert
        assertTrue(categories.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-NEWS-020 ====================
    // Test Objective: Archive already archived news - should be idempotent
    // Expected: No exception, status remains ARCHIVED
    // ====================
    @Test
    void TC_IT_NEWS_020_archiveNews_AlreadyArchived_ShouldBeIdempotent() throws Exception {
        // Arrange - archive first
        newsService.archiveNews(publishedNews.getId());

        // Act - archive again
        News result = newsService.archiveNews(publishedNews.getId());

        // Assert
        assertEquals(NewsStatus.ARCHIVED, result.getStatus());
    }

    // ==================== Test Case ID: TC-IT-NEWS-021 ====================
    // Test Objective: getAllNews with pagination - should return correct page size
    // Expected: Page respects size limit
    // ====================
    @Test
    void TC_IT_NEWS_021_getAllNews_Pagination_ShouldReturnCorrectSize() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 1);
        var result = newsService.getAllNews(pageRequest);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-NEWS-022 ====================
    // Test Objective: getPublishedNews with pagination page 2 - should return second page
    // Expected: Second page with remaining items
    // ====================
    @Test
    void TC_IT_NEWS_022_getPublishedNews_PaginationPage2_ShouldReturnSecondPage() {
        // Act
        PageRequest pageRequest = PageRequest.of(1, 1);
        var result = newsService.getPublishedNews(pageRequest);

        // Assert
        assertEquals(0, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-NEWS-023 ====================
    // Test Objective: incrementViews multiple times - should accumulate
    // Expected: views increase each call
    // ====================
    @Test
    void TC_IT_NEWS_023_incrementViews_MultipleTimes_ShouldAccumulate() throws Exception {
        // Arrange
        long initialViews = publishedNews.getViews();

        // Act - increment 5 times
        for (int i = 0; i < 5; i++) {
            newsService.incrementViews(publishedNews.getId());
        }

        // Clear JPA cache
        entityManager.clear();

        // Assert
        News fromDb = newsRepository.findById(publishedNews.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(initialViews + 5, fromDb.getViews());
    }

    // ==================== Test Case ID: TC-IT-NEWS-024 ====================
    // Test Objective: publishNews on already published news - should be idempotent
    // Expected: Status remains PUBLISHED, publishedAt already set
    // ====================
    @Test
    void TC_IT_NEWS_024_publishNews_AlreadyPublished_ShouldBeIdempotent() throws Exception {
        // Act - publish already published news
        News result = newsService.publishNews(publishedNews.getId());

        // Assert
        assertEquals(NewsStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());
        // publishedAt should remain the original (or be same)
    }

    // ==================== Test Case ID: TC-IT-NEWS-025 ====================
    // Test Objective: getNewsByCategory with non-existent category - should return empty
    // Expected: Empty page
    // ====================
    @Test
    void TC_IT_NEWS_025_getNewsByCategory_NonExistent_ShouldReturnEmpty() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = newsService.getNewsByCategory("NonExistentCategory", pageRequest);

        // Assert
        assertEquals(0, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-NEWS-026 ====================
    // Test Objective: createNews with null status - should default to DRAFT
    // Expected: status = DRAFT
    // ====================
    @Test
    void TC_IT_NEWS_026_createNews_WithNullStatus_ShouldDefaultToDraft() throws Exception {
        // Arrange - status is null
        NewsDTO newNewsDTO = NewsDTO.builder()
                .title("News without status")
                .content("Content")
                .author("Author")
                .category("General")
                .status(null)
                .build();

        // Act
        News result = newsService.createNews(newNewsDTO);

        // Assert
        assertEquals(NewsStatus.DRAFT, result.getStatus());
    }

    // ==================== Test Case ID: TC-IT-NEWS-027 ====================
    // Mục tiêu kiểm thử: createNews - Backend CÓ CHẶN Tiêu đề rỗng không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ khi title = ""
    // BUG DETECTION: Nếu test PASS = Backend đã fix. Nếu test FAIL = Bug đang tồn tại.
    // ====================
    @Test
    void TC_IT_NEWS_027_createNews_WithEmptyTitle_ShouldThrowException() {
        NewsDTO invalidDTO = NewsDTO.builder()
                .title("") // Rỗng
                .content("Valid content")
                .author("Author")
                .status(NewsStatus.PUBLISHED)
                .build();

        assertThrows(Exception.class, () -> {
            newsService.createNews(invalidDTO);
        }, "[BUG] Backend đang bị lỗi: Cho phép tạo Tin tức với Tiêu đề rỗng!");
    }

    // ==================== Test Case ID: TC-IT-NEWS-028 ====================
    // Mục tiêu kiểm thử: createNews - Backend CÓ CHẶN Nội dung rỗng không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ khi content = ""
    // BUG DETECTION: Nếu test PASS = Backend đã fix. Nếu test FAIL = Bug đang tồn tại.
    // ====================
    @Test
    void TC_IT_NEWS_028_createNews_WithEmptyContent_ShouldThrowException() {
        NewsDTO invalidDTO = NewsDTO.builder()
                .title("Unique Title For Content Test")
                .content("") // Rỗng
                .author("Author")
                .status(NewsStatus.PUBLISHED)
                .build();

        assertThrows(Exception.class, () -> {
            newsService.createNews(invalidDTO);
        }, "[BUG] Backend đang bị lỗi: Cho phép tạo Tin tức với Nội dung rỗng!");
    }

    // ==================== Test Case ID: TC-IT-NEWS-029 ====================
    // Mục tiêu kiểm thử: createNews - Backend CÓ CHẶN Trùng lặp Tiêu đề không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ khi title đã tồn tại trong DB
    // BUG DETECTION: Nếu test PASS = Backend đã fix. Nếu test FAIL = Bug đang tồn tại.
    // ====================
    @Test
    void TC_IT_NEWS_029_createNews_WithDuplicateTitle_ShouldThrowException() {
        // "Published News" đã được tạo sẵn trong @BeforeEach setUp()
        NewsDTO invalidDTO = NewsDTO.builder()
                .title("Published News") // Trùng tên với publishedNews
                .content("Nội dung khác hoàn toàn")
                .author("Author Khác")
                .status(NewsStatus.DRAFT)
                .build();

        assertThrows(Exception.class, () -> {
            newsService.createNews(invalidDTO);
        }, "[BUG] Backend đang bị lỗi: Cho phép tạo Tin tức với Tiêu đề trùng lặp!");
    }

    // ==================== Test Case ID: TC-IT-NEWS-030 ====================
    // Mục tiêu kiểm thử: updateNews - Backend CÓ CHẶN Tiêu đề rỗng không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ khi cập nhật title = ""
    // BUG DETECTION: Nếu test PASS = Backend đã fix. Nếu test FAIL = Bug đang tồn tại.
    // ====================
    @Test
    void TC_IT_NEWS_030_updateNews_WithEmptyTitle_ShouldThrowException() {
        NewsDTO invalidDTO = NewsDTO.builder()
                .title("") // Rỗng
                .content("Valid content")
                .author("Author")
                .status(NewsStatus.PUBLISHED)
                .build();

        assertThrows(Exception.class, () -> {
            newsService.updateNews(publishedNews.getId(), invalidDTO);
        }, "[BUG] Backend đang bị lỗi: Cho phép cập nhật Tin tức với Tiêu đề rỗng!");
    }

    // ==================== Test Case ID: TC-IT-NEWS-031 ====================
    // Mục tiêu kiểm thử: updateNews - Backend CÓ CHẶN đổi tên trùng Tin tức khác không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ khi cập nhật title trùng với bài khác
    // BUG DETECTION: Nếu test PASS = Backend đã fix. Nếu test FAIL = Bug đang tồn tại.
    // ====================
    @Test
    void TC_IT_NEWS_031_updateNews_WithDuplicateTitle_ShouldThrowException() {
        // draftNews đang có title = "Draft News"
        // Thử đổi title thành "Published News" (trùng với publishedNews)
        NewsDTO invalidDTO = NewsDTO.builder()
                .title("Published News") // Trùng với publishedNews
                .content("Nội dung mới")
                .author("Author")
                .status(NewsStatus.DRAFT)
                .build();

        assertThrows(Exception.class, () -> {
            newsService.updateNews(draftNews.getId(), invalidDTO);
        }, "[BUG] Backend đang bị lỗi: Cho phép cập nhật Tin tức sang Tiêu đề đã tồn tại ở bài khác!");
    }

    // ==================== Test Case ID: TC-IT-NEWS-032 ====================
    // Mục tiêu kiểm thử: updateNews - Backend CÓ CHẶN Nội dung rỗng khi update không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ khi update content = ""
    // BUG DETECTION: Nếu test PASS = Backend đã fix. Nếu test FAIL = Bug đang tồn tại.
    // ====================
    @Test
    void TC_IT_NEWS_032_updateNews_WithEmptyContent_ShouldThrowException() {
        NewsDTO invalidDTO = NewsDTO.builder()
                .title("Tiêu Đề Hợp Lệ")
                .content("") // Rỗng nội dung
                .author("Author")
                .status(NewsStatus.PUBLISHED)
                .build();

        assertThrows(Exception.class, () -> {
            newsService.updateNews(publishedNews.getId(), invalidDTO);
        }, "[BUG] Backend đang bị lỗi: Cho phép cập nhật Tin tức với Nội dung rỗng!");
    }
}

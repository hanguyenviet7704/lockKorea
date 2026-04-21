package com.example.Sneakers.services;

import com.example.Sneakers.dtos.BannerDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.Banner;
import com.example.Sneakers.repositories.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @InjectMocks
    private BannerService bannerService;

    @Captor
    private ArgumentCaptor<Banner> bannerCaptor;

    private Banner testBanner;
    private BannerDTO bannerDTO;

    @BeforeEach
    void setUp() {
        testBanner = Banner.builder()
                .id(1L)
                .title("Home Banner")
                .description("Test banner")
                .imageUrl("banner.jpg")
                .buttonLink("/home")
                .buttonText("Click here")
                .buttonStyle("primary")
                .displayOrder(1)
                .isActive(true)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(7))
                .build();

        bannerDTO = new BannerDTO();
        bannerDTO.setTitle("New Banner");
        bannerDTO.setDescription("New description");
        bannerDTO.setImageUrl("new-banner.jpg");
        bannerDTO.setButtonLink("/promo");
        bannerDTO.setButtonText("Shop now");
        bannerDTO.setButtonStyle("secondary");
        bannerDTO.setDisplayOrder(2);
        bannerDTO.setIsActive(true);
        bannerDTO.setStartDate(LocalDateTime.now());
        bannerDTO.setEndDate(LocalDateTime.now().plusDays(7));
    }

    // ==================== Test Case ID: TC-BANNER-001 ====================
    // Test Objective: Verify that createBanner creates banner successfully
    // Input: Valid BannerDTO
    // Expected Output: Saved Banner entity
    // ====================
    @Test
    void TC_BANNER_001_createBanner_ShouldCreateBannerSuccessfully() {
        // Arrange
        when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> {
            Banner banner = invocation.getArgument(0);
            banner.setId(1L);
            return banner;
        });

        // Act
        BannerDTO result = bannerService.createBanner(bannerDTO);

        // Assert
        assertNotNull(result);
        assertEquals("New Banner", result.getTitle());
        assertEquals("new-banner.jpg", result.getImageUrl());
        assertEquals("/promo", result.getButtonLink());
        assertEquals(2, result.getDisplayOrder().intValue());
        assertTrue(result.getIsActive());

        // Verify banner was saved
        verify(bannerRepository).save(bannerCaptor.capture());
        Banner savedBanner = bannerCaptor.getValue();
        assertEquals("New Banner", savedBanner.getTitle());
    }

    // ==================== Test Case ID: TC-BANNER-002 ====================
    // Test Objective: Verify that getActiveBannersInDateRange returns active banners in date range
    // Input: No parameters
    // Expected Output: List of Banners sorted by displayOrder
    // ====================
    @Test
    void TC_BANNER_002_getActiveBannersInDateRange_ShouldReturnOrderedBanners() {
        // Arrange
        Banner banner1 = Banner.builder().id(1L).displayOrder(2).title("Banner 2").build();
        Banner banner2 = Banner.builder().id(2L).displayOrder(1).title("Banner 1").build();
        Banner banner3 = Banner.builder().id(3L).displayOrder(3).title("Banner 3").build();

        LocalDateTime now = LocalDateTime.now();
        when(bannerRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfterOrderByDisplayOrderAsc(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(banner2, banner1, banner3));

        // Act
        List<BannerDTO> result = bannerService.getActiveBannersInDateRange();

        // Assert
        assertEquals(3, result.size());
        // Verify order by displayOrder (checking via banner titles since DTOs are sorted)
        assertEquals("Banner 1", result.get(0).getTitle());
        assertEquals("Banner 2", result.get(1).getTitle());
        assertEquals("Banner 3", result.get(2).getTitle());
    }

    // ==================== Test Case ID: TC-BANNER-003 ====================
    // Test Objective: Verify that getBannerById returns banner for valid ID
    // Input: Existing banner ID
    // Expected Output: BannerDTO
    // ====================
    @Test
    void TC_BANNER_003_getBannerById_ShouldReturnBanner() throws Exception {
        // Arrange
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(testBanner));

        // Act
        BannerDTO result = bannerService.getBannerById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Home Banner", result.getTitle());
        assertEquals("banner.jpg", result.getImageUrl());
    }

    // ==================== Test Case ID: TC-BANNER-004 ====================
    // Test Objective: Verify that updateBanner updates banner successfully
    // Input: Valid banner ID, BannerDTO with updates
    // Expected Output: Updated Banner entity
    // ====================
    @Test
    void TC_BANNER_004_updateBanner_ShouldUpdateBannerSuccessfully() throws Exception {
        // Arrange
        Long bannerId = 1L;
        Banner existingBanner = Banner.builder()
                .id(bannerId)
                .title("Old Title")
                .description("Old description")
                .imageUrl("old.jpg")
                .buttonLink("/old-link")
                .buttonText("Old text")
                .buttonStyle("old-style")
                .displayOrder(1)
                .isActive(false)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        BannerDTO updateDTO = new BannerDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated description");
        updateDTO.setImageUrl("updated.jpg");
        updateDTO.setButtonLink("/new-link");
        updateDTO.setButtonText("New text");
        updateDTO.setButtonStyle("new-style");
        updateDTO.setDisplayOrder(5);
        updateDTO.setIsActive(true);
        updateDTO.setStartDate(LocalDateTime.now());
        updateDTO.setEndDate(LocalDateTime.now().plusDays(7));

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(existingBanner));
        when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BannerDTO result = bannerService.updateBanner(bannerId, updateDTO);

        // Assert
        assertEquals("Updated Title", result.getTitle());
        assertEquals("updated.jpg", result.getImageUrl());
        assertEquals("/new-link", result.getButtonLink());
        assertEquals(5, result.getDisplayOrder().intValue());
        assertTrue(result.getIsActive());
    }

    // ==================== Test Case ID: TC-BANNER-005 ====================
    // Test Objective: Verify that deleteBanner removes banner
    // Input: Valid banner ID
    // Expected Output: Banner deleted from repository
    // ====================
    @Test
    void TC_BANNER_005_deleteBanner_ShouldDeleteBanner() throws Exception {
        // Arrange
        Long bannerId = 1L;
        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(testBanner));
        doNothing().when(bannerRepository).delete(any(Banner.class));

        // Act
        bannerService.deleteBanner(bannerId);

        // Assert
        verify(bannerRepository).findById(bannerId);
        verify(bannerRepository).delete(testBanner);
    }

    // ==================== Test Case ID: TC-BANNER-006 ====================
    // Test Objective: Verify that getActiveBannersInDateRange returns only active banners in date range
    // Input: No parameters
    // Expected Output: List of banner DTOs where isActive = true and within date range
    // ====================
    @Test
    void TC_BANNER_006_getActiveBannersInDateRange_ShouldReturnOnlyActive() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Banner active1 = Banner.builder().id(1L).isActive(true).displayOrder(1).title("Active 1")
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build();
        Banner active2 = Banner.builder().id(2L).isActive(true).displayOrder(2).title("Active 2")
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build();
        Banner inactive = Banner.builder().id(3L).isActive(false).displayOrder(3).title("Inactive")
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build();

        when(bannerRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfterOrderByDisplayOrderAsc(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(active1, active2));

        // Act
        List<BannerDTO> result = bannerService.getActiveBannersInDateRange();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(BannerDTO::getIsActive));
        // Verify order is preserved
        assertEquals("Active 1", result.get(0).getTitle());
        assertEquals("Active 2", result.get(1).getTitle());
    }

    // ==================== Test Case ID: TC-BANNER-007 ====================
    // Test Objective: Verify that getBannerById throws exception when banner not found
    // Input: Non-existent banner ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_BANNER_007_getBannerById_ShouldThrowException_WhenBannerNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(bannerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                bannerService.getBannerById(nonExistentId));
        assertEquals("Banner not found with id: " + nonExistentId, exception.getMessage());
    }

    // ==================== Test Case ID: TC-BANNER-008 ====================
    // Test Objective: Verify that updateBanner throws exception when banner not found
    // Input: Non-existent banner ID, valid BannerDTO
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_BANNER_008_updateBanner_ShouldThrowException_WhenBannerNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        BannerDTO updateDTO = new BannerDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated description");

        when(bannerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                bannerService.updateBanner(nonExistentId, updateDTO));
        assertEquals("Banner not found with id: " + nonExistentId, exception.getMessage());
    }

    // ==================== Test Case ID: TC-BANNER-009 ====================
    // Test Objective: Verify that deleteBanner throws exception when banner not found
    // Input: Non-existent banner ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_BANNER_009_deleteBanner_ShouldThrowException_WhenBannerNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        when(bannerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                bannerService.deleteBanner(nonExistentId));
        assertEquals("Banner not found with id: " + nonExistentId, exception.getMessage());
    }

    // ==================== Test Case ID: TC-BANNER-010 ====================
    // Test Objective: Verify that createBanner validates date range (BUG DETECTION)
    // Input: BannerDTO with startDate after endDate
    // Expected Output: Should throw exception, not accept invalid date range
    // ====================
    @Test
    void TC_BANNER_010_createBanner_ShouldThrowException_WhenInvalidDateRange_Bug() throws Exception {
        // Arrange
        BannerDTO invalidDTO = new BannerDTO();
        invalidDTO.setTitle("Test Banner");
        invalidDTO.setImageUrl("test.jpg");
        invalidDTO.setStartDate(LocalDateTime.now().plusDays(10));
        invalidDTO.setEndDate(LocalDateTime.now().plusDays(1)); // end before start

        // Act & Assert - Should throw exception
        Exception exception = assertThrows(Exception.class, () ->
                bannerService.createBanner(invalidDTO));
        assertTrue(exception.getMessage().toLowerCase().contains("date") ||
                   exception.getMessage().toLowerCase().contains("valid"));
    }

    // ==================== Test Case ID: TC-BANNER-011 ====================
    // Test Objective: Verify that updateBanner validates date range (BUG DETECTION)
    // Input: Existing banner, BannerDTO with startDate after endDate
    // Expected Output: Should throw exception, not accept invalid date range
    // ====================
    @Test
    void TC_BANNER_011_updateBanner_ShouldThrowException_WhenInvalidDateRange_Bug() throws Exception {
        // Arrange
        Long bannerId = 1L;
        Banner existingBanner = Banner.builder()
                .id(bannerId)
                .title("Old Title")
                .imageUrl("old.jpg")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        BannerDTO updateDTO = new BannerDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setImageUrl("updated.jpg");
        updateDTO.setStartDate(LocalDateTime.now().plusDays(10));
        updateDTO.setEndDate(LocalDateTime.now().plusDays(1)); // end before start

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(existingBanner));

        // Act & Assert - Should throw exception
        Exception exception = assertThrows(Exception.class, () ->
                bannerService.updateBanner(bannerId, updateDTO));
        assertTrue(exception.getMessage().toLowerCase().contains("date") ||
                   exception.getMessage().toLowerCase().contains("valid"));
    }

}

package com.example.Sneakers.integration;

import com.example.Sneakers.dtos.BannerDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.Banner;
import com.example.Sneakers.repositories.BannerRepository;
import com.example.Sneakers.services.BannerService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BannerServiceIntegrationTest {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private BannerRepository bannerRepository;

    @MockBean
    private ChatModel chatModel;

    private Banner activeBanner;
    private Banner inactiveBanner;
    private Banner expiredBanner;

    @BeforeEach
    void setUp() {

        bannerRepository.deleteAll();

        activeBanner = Banner.builder()
                .title("Summer Sale")
                .description("Sale up to 50%")
                .imageUrl("banner1.jpg")
                .buttonText("Shop Now")
                .buttonLink("/shop")
                .buttonStyle("primary")
                .displayOrder(1)
                .isActive(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        inactiveBanner = Banner.builder()
                .title("Inactive Banner")
                .description("Inactive")
                .imageUrl("banner2.jpg")
                .buttonText("View")
                .buttonLink("/view")
                .buttonStyle("secondary")
                .displayOrder(2)
                .isActive(false)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        expiredBanner = Banner.builder()
                .title("Expired Banner")
                .description("Expired")
                .imageUrl("banner3.jpg")
                .buttonText("Expired")
                .buttonLink("/expired")
                .buttonStyle("danger")
                .displayOrder(3)
                .isActive(true)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1))
                .build();

        activeBanner = bannerRepository.save(activeBanner);
        inactiveBanner = bannerRepository.save(inactiveBanner);
        expiredBanner = bannerRepository.save(expiredBanner);
    }

    // ==================== TC-IT-BANNER-001 ====================
    @Test
    void TC_IT_BANNER_001_getAllBanners_ShouldReturnAll() {

        List<BannerDTO> result = bannerService.getAllBanners();

        assertEquals(3, result.size());
    }

    // ==================== TC-IT-BANNER-002 ====================
    @Test
    void TC_IT_BANNER_002_getBannerById_ShouldReturnBanner() throws Exception {

        BannerDTO result = bannerService.getBannerById(activeBanner.getId());

        assertNotNull(result);
        assertEquals("Summer Sale", result.getTitle());
    }

    // ==================== TC-IT-BANNER-003 ====================
    @Test
    void TC_IT_BANNER_003_getBannerById_NotFound_ShouldThrowException() {

        assertThrows(DataNotFoundException.class,
                () -> bannerService.getBannerById(99999L));
    }

    // ==================== TC-IT-BANNER-004 ====================
    @Test
    void TC_IT_BANNER_004_getActiveBanners_ShouldReturnOnlyActive() {

        List<BannerDTO> result =
                bannerService.getActiveBannersInDateRange();

        assertEquals(1, result.size());
    }

    // ==================== TC-IT-BANNER-005 ====================
    @Test
    void TC_IT_BANNER_005_createBanner_ShouldPersistToDatabase() {

        BannerDTO dto = BannerDTO.builder()
                .title("New Banner")
                .description("New Desc")
                .imageUrl("new.jpg")
                .displayOrder(4)
                .isActive(true)
                .build();

        BannerDTO result = bannerService.createBanner(dto);

        assertNotNull(result.getId());

        Banner fromDb =
                bannerRepository.findById(result.getId()).orElse(null);

        assertNotNull(fromDb);
        assertEquals("New Banner", fromDb.getTitle());
    }

    // ==================== TC-IT-BANNER-006 ====================
    @Test
    void TC_IT_BANNER_006_createBanner_DefaultIsActive_ShouldBeTrue() {

        BannerDTO dto = BannerDTO.builder()
                .title("Default")
                .imageUrl("default.jpg")
                .build();

        BannerDTO result = bannerService.createBanner(dto);

        assertTrue(result.getIsActive());
    }

    // ==================== TC-IT-BANNER-007 ====================
    @Test
    void TC_IT_BANNER_007_updateBanner_ShouldUpdateDatabase() throws Exception {

        BannerDTO dto = BannerDTO.builder()
                .title("Updated Banner")
                .description("Updated Desc")
                .imageUrl("updated.jpg")
                .isActive(false)
                .build();

        BannerDTO result =
                bannerService.updateBanner(activeBanner.getId(), dto);

        assertEquals("Updated Banner", result.getTitle());

        Banner fromDb =
                bannerRepository.findById(activeBanner.getId()).orElse(null);

        assertNotNull(fromDb);
        assertEquals("Updated Banner", fromDb.getTitle());
    }

    // ==================== TC-IT-BANNER-008 ====================
    @Test
    void TC_IT_BANNER_008_updateBanner_NotFound_ShouldThrowException() {

        BannerDTO dto = BannerDTO.builder()
                .title("Updated")
                .imageUrl("updated.jpg")
                .build();

        assertThrows(DataNotFoundException.class,
                () -> bannerService.updateBanner(99999L, dto));
    }

    // ==================== TC-IT-BANNER-009 ====================
    @Test
    void TC_IT_BANNER_009_deleteBanner_ShouldDeleteFromDatabase()
            throws Exception {

        bannerService.deleteBanner(activeBanner.getId());

        assertTrue(
                bannerRepository.findById(activeBanner.getId()).isEmpty()
        );
    }

    // ==================== TC-IT-BANNER-010 ====================
    @Test
    void TC_IT_BANNER_010_deleteBanner_NotFound_ShouldThrowException() {

        assertThrows(DataNotFoundException.class,
                () -> bannerService.deleteBanner(99999L));
    }

    // ==================== TC-IT-BANNER-011 ====================
    @Test
    void TC_IT_BANNER_011_toggleBannerStatus_ShouldToggleToFalse()
            throws Exception {

        BannerDTO result =
                bannerService.toggleBannerStatus(activeBanner.getId());

        assertFalse(result.getIsActive());
    }

    // ==================== TC-IT-BANNER-012 ====================
    @Test
    void TC_IT_BANNER_012_toggleBannerStatus_ShouldToggleToTrue()
            throws Exception {

        BannerDTO result =
                bannerService.toggleBannerStatus(inactiveBanner.getId());

        assertTrue(result.getIsActive());
    }

    // ==================== TC-IT-BANNER-013 ====================
    @Test
    void TC_IT_BANNER_013_toggleBannerStatus_NotFound_ShouldThrowException() {

        assertThrows(DataNotFoundException.class,
                () -> bannerService.toggleBannerStatus(99999L));
    }

    // ==================== TC-IT-BANNER-014 ====================
    @Test
    void TC_IT_BANNER_014_getAllBanners_WhenEmpty_ShouldReturnEmpty() {

        bannerRepository.deleteAll();

        List<BannerDTO> result = bannerService.getAllBanners();

        assertTrue(result.isEmpty());
    }

    // ==================== TC-IT-BANNER-015 ====================
    @Test
    void TC_IT_BANNER_015_getActiveBanners_WhenNoActive_ShouldReturnEmpty() {

        activeBanner.setIsActive(false);
        bannerRepository.save(activeBanner);

        List<BannerDTO> result =
                bannerService.getActiveBannersInDateRange();

        assertTrue(result.isEmpty());
    }

    // ==================== TC-IT-BANNER-016 ====================
    @Test
    void TC_IT_BANNER_016_getActiveBanners_ShouldIgnoreExpiredBanner() {

        List<BannerDTO> result =
                bannerService.getActiveBannersInDateRange();

        boolean exists = result.stream()
                .anyMatch(b -> b.getTitle().equals("Expired Banner"));

        assertFalse(exists);
    }

    // ==================== TC-IT-BANNER-017 ====================
    @Test
    void TC_IT_BANNER_017_createBanner_ShouldSaveButtonFields() {

        BannerDTO dto = BannerDTO.builder()
                .title("Button Test")
                .imageUrl("btn.jpg")
                .buttonText("Click")
                .buttonLink("/click")
                .buttonStyle("info")
                .build();

        BannerDTO result = bannerService.createBanner(dto);

        assertEquals("Click", result.getButtonText());
    }

    // ==================== TC-IT-BANNER-018 ====================
    @Test
    void TC_IT_BANNER_018_updateBanner_ShouldUpdateDisplayOrder()
            throws Exception {

        BannerDTO dto = BannerDTO.builder()
                .title("Display")
                .imageUrl("display.jpg")
                .displayOrder(99)
                .build();

        bannerService.updateBanner(activeBanner.getId(), dto);

        Banner fromDb =
                bannerRepository.findById(activeBanner.getId()).orElse(null);

        assertEquals(99, fromDb.getDisplayOrder());
    }

    // ==================== TC-IT-BANNER-019 ====================
    @Test
    void TC_IT_BANNER_019_createBanner_ShouldStoreDates() {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(7);

        BannerDTO dto = BannerDTO.builder()
                .title("Date Test")
                .imageUrl("date.jpg")
                .startDate(start)
                .endDate(end)
                .build();

        BannerDTO result = bannerService.createBanner(dto);

        assertEquals(start, result.getStartDate());
    }

    // ==================== TC-IT-BANNER-020 ====================
    @Test
    void TC_IT_BANNER_020_toggleBannerStatus_Twice_ShouldReturnOriginal()
            throws Exception {

        bannerService.toggleBannerStatus(activeBanner.getId());

        BannerDTO result =
                bannerService.toggleBannerStatus(activeBanner.getId());

        assertTrue(result.getIsActive());
    }

    // ==================== TC-IT-BANNER-021 ====================
    @Test
    void TC_IT_BANNER_021_createBanner_ShouldSaveDescription() {

        BannerDTO dto = BannerDTO.builder()
                .title("Description Test")
                .imageUrl("desc.jpg")
                .description("Banner Description")
                .build();

        BannerDTO result = bannerService.createBanner(dto);

        assertEquals("Banner Description", result.getDescription());
    }

    // ==================== TC-IT-BANNER-022 ====================
    @Test
    void TC_IT_BANNER_022_getAllBanners_ShouldContainCorrectTitles() {

        List<BannerDTO> result = bannerService.getAllBanners();

        assertTrue(
                result.stream()
                        .anyMatch(b -> b.getTitle().equals("Summer Sale"))
        );
    }

    // ==================== TC-IT-BANNER-023 ====================
    @Test
    void TC_IT_BANNER_023_deleteAllBanners_ShouldRemoveEverything()
            throws Exception {

        bannerService.deleteBanner(activeBanner.getId());
        bannerService.deleteBanner(inactiveBanner.getId());
        bannerService.deleteBanner(expiredBanner.getId());

        assertEquals(0, bannerRepository.count());
    }

    // ==================== TC-IT-BANNER-024 ====================
    @Test
    void TC_IT_BANNER_024_getActiveBanners_ShouldSortByDisplayOrder() {

        List<BannerDTO> result =
                bannerService.getActiveBannersInDateRange();

        assertFalse(result.isEmpty());

        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(
                    result.get(i).getDisplayOrder()
                            <= result.get(i + 1).getDisplayOrder()
            );
        }
    }
}
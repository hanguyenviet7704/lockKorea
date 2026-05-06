package com.example.Sneakers.integration;

import com.example.Sneakers.dtos.ApplyVoucherDTO;
import com.example.Sneakers.dtos.VoucherDTO;
import com.example.Sneakers.models.Order;
import com.example.Sneakers.models.User;
import com.example.Sneakers.models.Voucher;
import com.example.Sneakers.models.VoucherUsage;
import com.example.Sneakers.repositories.OrderRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.repositories.VoucherRepository;
import com.example.Sneakers.repositories.VoucherUsageRepository;
import com.example.Sneakers.responses.VoucherApplicationResponse;
import com.example.Sneakers.services.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for VoucherService.
 * <p>
 * Tests: Voucher CRUD, validation, application, usage tracking.
 * Uses H2 in-memory database with real repositories.
 * Each test runs in a transaction and rolls back automatically.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoucherServiceIntegrationTest {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VoucherUsageRepository voucherUsageRepository;

    @MockBean
    private dev.langchain4j.model.chat.ChatModel chatModel;

    private Voucher activeVoucher;
    private Voucher expiredVoucher;
    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        voucherRepository.deleteAll();
        voucherUsageRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = com.example.Sneakers.models.User.builder()
                .fullName("Test User")
                .phoneNumber("0123456789")
                .password("encodedPass")
                .email("testuser@example.com")
                .address("Test Address")
                .dateOfBirth(java.sql.Date.valueOf(LocalDate.of(1990, 1, 1)))
                .active(true)
                .role(null)
                .build();
        testUser = userRepository.save(testUser);

        // Create test order
        testOrder = Order.builder()
                .user(testUser)
                .fullName("Test User")
                .email("testuser@example.com")
                .phoneNumber("0123456789")
                .address("Test Address")
                .orderDate(LocalDateTime.now())
                .status("PENDING")
                .totalMoney(500000L)
                .shippingMethod("STANDARD")
                .paymentMethod("CASH_ON_DELIVERY")
                .build();
        testOrder = orderRepository.save(testOrder);

        // Current time for voucher dates
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime lastWeek = now.minusWeeks(1);

        // Create active voucher (valid now)
        activeVoucher = Voucher.builder()
                .code("ACTIVE20")
                .name("Active 20% Off")
                .description("20% discount")
                .discountPercentage(20)
                .minOrderValue(100000L)
                .maxDiscountAmount(50000L)
                .quantity(100)
                .remainingQuantity(100)
                .validFrom(yesterday)
                .validTo(tomorrow)
                .isActive(true)
                .build();
        activeVoucher = voucherRepository.save(activeVoucher);

        // Create expired voucher (valid in the past)
        expiredVoucher = Voucher.builder()
                .code("EXPIRED10")
                .name("Expired 10% Off")
                .description("Expired voucher")
                .discountPercentage(10)
                .minOrderValue(50000L)
                .maxDiscountAmount(20000L)
                .quantity(50)
                .remainingQuantity(50)
                .validFrom(lastWeek)
                .validTo(yesterday)
                .isActive(true)
                .build();
        expiredVoucher = voucherRepository.save(expiredVoucher);
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-001 ====================
    // Test Objective: createVoucher - should persist voucher with generated ID
    // Expected: Voucher saved with all fields
    // ====================
    @Test
    void TC_IT_VOUCHER_001_createVoucher_ShouldPersistToDatabase() throws Exception {
        // Arrange
        VoucherDTO voucherDTO = VoucherDTO.builder()
                .code("NEWVOUCHER")
                .name("New Voucher")
                .description("New voucher description")
                .discountPercentage(15)
                .minOrderValue(200000L)
                .maxDiscountAmount(30000L)
                .quantity(50)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        // Act
        Voucher result = voucherService.createVoucher(voucherDTO);

        // Assert - return value
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("NEWVOUCHER", result.getCode());
        assertEquals("New Voucher", result.getName());
        assertEquals(15, result.getDiscountPercentage());
        assertEquals(50, result.getQuantity());
        assertEquals(50, result.getRemainingQuantity()); // remaining = initial quantity
        assertTrue(result.getIsActive());

        // ✅ DB Check
        Optional<Voucher> fromDb = voucherRepository.findById(result.getId());
        assertTrue(fromDb.isPresent());
        assertEquals("NEWVOUCHER", fromDb.get().getCode());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-002 ====================
    // Test Objective: createVoucher - duplicate code should throw exception
    // Expected: Exception "Mã voucher bị trùng"
    // ====================
    @Test
    void TC_IT_VOUCHER_002_createVoucher_DuplicateCode_ShouldThrowException() throws Exception {
        // Arrange - use existing voucher code
        VoucherDTO voucherDTO = VoucherDTO.builder()
                .code("ACTIVE20")
                .name("Duplicate Code Voucher")
                .description("Test")
                .discountPercentage(10)
                .minOrderValue(10000L)
                .quantity(10)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> voucherService.createVoucher(voucherDTO));
        assertTrue(exception.getMessage().contains("trùng"));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-003 ====================
    // Test Objective: createVoucher - invalid date range (validFrom after validTo) should throw
    // Expected: Exception "Valid from date must be before valid to date"
    // ====================
    @Test
    void TC_IT_VOUCHER_003_createVoucher_InvalidDateRange_ShouldThrowException() throws Exception {
        // Arrange
        VoucherDTO voucherDTO = VoucherDTO.builder()
                .code("DATERANGE")
                .name("Date Range Voucher")
                .discountPercentage(10)
                .minOrderValue(10000L)
                .quantity(10)
                .validFrom(LocalDateTime.now().plusDays(10))
                .validTo(LocalDateTime.now().plusDays(1)) // to before from
                .build();

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> voucherService.createVoucher(voucherDTO));
        assertTrue(exception.getMessage().contains("before"));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-004 ====================
    // Test Objective: getVoucherById - should return voucher by ID
    // Expected: Voucher with correct code
    // ====================
    @Test
    void TC_IT_VOUCHER_004_getVoucherById_ShouldReturnVoucher() throws Exception {
        // Act
        Voucher result = voucherService.getVoucherById(activeVoucher.getId());

        // Assert
        assertNotNull(result);
        assertEquals("ACTIVE20", result.getCode());
        assertEquals(20, result.getDiscountPercentage());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-005 ====================
    // Test Objective: getVoucherById - non-existent ID should throw exception
    // Expected: DataNotFoundException
    // ====================
    @Test
    void TC_IT_VOUCHER_005_getVoucherById_NonExistent_ShouldThrowException() {
        // Act & Assert
        assertThrows(Exception.class, () -> voucherService.getVoucherById(99999L));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-006 ====================
    // Test Objective: getVoucherByCode - should return voucher by code
    // Expected: Voucher with correct ID
    // ====================
    @Test
    void TC_IT_VOUCHER_006_getVoucherByCode_ShouldReturnVoucher() throws Exception {
        // Act
        Voucher result = voucherService.getVoucherByCode("ACTIVE20");

        // Assert
        assertNotNull(result);
        assertEquals(activeVoucher.getId(), result.getId());
        assertEquals(20, result.getDiscountPercentage());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-007 ====================
    // Test Objective: getAllVouchers - should return all vouchers
    // Expected: Page containing both vouchers
    // ====================
    @Test
    void TC_IT_VOUCHER_007_getAllVouchers_ShouldReturnAll() {
        // Act
        var result = voucherService.getAllVouchers(PageRequest.of(0, 10));

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-008 ====================
    // Test Objective: getActiveVouchers - should return only active vouchers
    // Expected: Only vouchers with isActive = true
    // ====================
    @Test
    void TC_IT_VOUCHER_008_getActiveVouchers_ShouldReturnOnlyActive() {
        // Act
        var result = voucherService.getActiveVouchers(PageRequest.of(0, 10));

        // Assert
        assertEquals(2, result.getTotalElements()); // both are active
        assertTrue(result.stream().allMatch(v -> v.getIsActive()));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-009 ====================
    // Test Objective: getValidVouchers - should return only currently valid vouchers
    // Expected: Only active vouchers within date range and with remaining quantity
    // ====================
    @Test
    void TC_IT_VOUCHER_009_getValidVouchers_ShouldReturnOnlyValid() {
        // Act
        var result = voucherService.getValidVouchers(PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("ACTIVE20", result.getContent().get(0).getCode());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-010 ====================
    // Test Objective: searchVouchers - by code keyword should match
    // Expected: Vouchers with code containing keyword
    // ====================
    @Test
    void TC_IT_VOUCHER_010_searchVouchers_ByCode_ShouldMatch() {
        // Act
        var result = voucherService.searchVouchers("ACTIVE", PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("ACTIVE20", result.getContent().get(0).getCode());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-011 ====================
    // Test Objective: searchVouchers - by name keyword should match (case-insensitive)
    // Expected: Vouchers with name containing keyword
    // ====================
    @Test
    void TC_IT_VOUCHER_011_searchVouchers_ByName_ShouldMatch() {
        // Act
        var result = voucherService.searchVouchers("Expired", PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("EXPIRED10", result.getContent().get(0).getCode());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-012 ====================
    // Test Objective: searchVouchers - no matches should return empty page
    // Expected: Empty page
    // ====================
    @Test
    void TC_IT_VOUCHER_012_searchVouchers_NoMatch_ShouldReturnEmpty() {
        // Act
        var result = voucherService.searchVouchers("NOMATCH", PageRequest.of(0, 10));

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-013 ====================
    // Test Objective: updateVoucher - should modify voucher fields
    // Expected: Changes persisted to database
    // ====================
    @Test
    void TC_IT_VOUCHER_013_updateVoucher_ShouldUpdateInDatabase() throws Exception {
        // Arrange
        VoucherDTO updateDTO = VoucherDTO.builder()
                .code("UPDATED20")
                .name("Updated Voucher Name")
                .description("Updated description")
                .discountPercentage(25)
                .minOrderValue(150000L)
                .maxDiscountAmount(60000L)
                .quantity(80)
                .validFrom(LocalDateTime.now().plusDays(2))
                .validTo(LocalDateTime.now().plusDays(60))
                .isActive(true)
                .build();

        // Act
        Voucher result = voucherService.updateVoucher(activeVoucher.getId(), updateDTO);

        // Assert - return value
        assertEquals("UPDATED20", result.getCode());
        assertEquals("Updated Voucher Name", result.getName());
        assertEquals(25, result.getDiscountPercentage());
        assertEquals(150000L, result.getMinOrderValue());
        assertEquals(80, result.getQuantity());

        // ✅ DB Check
        Optional<Voucher> fromDb = voucherRepository.findById(activeVoucher.getId());
        assertTrue(fromDb.isPresent());
        assertEquals("UPDATED20", fromDb.get().getCode());
        assertEquals(25, fromDb.get().getDiscountPercentage());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-014 ====================
    // Test Objective: updateVoucher - changing code to duplicate should throw exception
    // Expected: Exception "Mã voucher bị trùng"
    // ====================
    @Test
    void TC_IT_VOUCHER_014_updateVoucher_DuplicateCode_ShouldThrowException() throws Exception {
        // Arrange - update activeVoucher's code to expiredVoucher's code
        VoucherDTO updateDTO = VoucherDTO.builder()
                .code("EXPIRED10")
                .name("New Name")
                .discountPercentage(15)
                .minOrderValue(10000L)
                .quantity(20)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.updateVoucher(activeVoucher.getId(), updateDTO));
        assertTrue(exception.getMessage().contains("trùng"));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-015 ====================
    // Test Objective: applyVoucher - valid voucher should apply successfully
    // Expected: isApplied = true, discount calculated correctly
    // ====================
    @Test
    void TC_IT_VOUCHER_015_applyVoucher_ValidVoucher_ShouldApply() throws Exception {
        // Arrange - order total = 200,000, voucher = 20% off, max 50,000
        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("ACTIVE20")
                .orderTotal(200000L)
                .build();

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertTrue(response.getIsApplied());
        assertEquals("ACTIVE20", response.getVoucherCode());
        assertEquals(200000L, response.getOriginalTotal());
        // 20% of 200,000 = 40,000 (under max 50,000)
        assertEquals(40000L, response.getDiscountAmount());
        assertEquals(160000L, response.getFinalTotal());
        assertNotNull(response.getMessage());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-016 ====================
    // Test Objective: applyVoucher - order below minimum should fail
    // Expected: isApplied = false, message about minimum order value
    // ====================
    @Test
    void TC_IT_VOUCHER_016_applyVoucher_BelowMinimum_ShouldFail() throws Exception {
        // Arrange - order total = 50,000, min = 100,000
        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("ACTIVE20")
                .orderTotal(50000L)
                .build();

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertFalse(response.getIsApplied());
        assertEquals(50000L, response.getOriginalTotal());
        assertEquals(50000L, response.getFinalTotal()); // no discount
        assertTrue(response.getMessage().contains("tối thiểu"));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-017 ====================
    // Test Objective: applyVoucher - non-existent voucher should fail
    // Expected: isApplied = false, message "invalid"
    // ====================
    @Test
    void TC_IT_VOUCHER_017_applyVoucher_NonExistent_ShouldFail() throws Exception {
        // Arrange
        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("NONEXISTENT")
                .orderTotal(200000L)
                .build();

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertFalse(response.getIsApplied());
        assertEquals("NONEXISTENT", response.getVoucherCode());
        assertEquals(200000L, response.getOriginalTotal());
        assertEquals(200000L, response.getFinalTotal());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-018 ====================
    // Test Objective: useVoucher - should decrease remaining quantity and create usage record
    // Expected: remainingQuantity - 1, VoucherUsage created
    // ====================
    @Test
    void TC_IT_VOUCHER_018_useVoucher_ShouldRecordUsageAndDecreaseQuantity() throws Exception {
        // Arrange
        int initialRemaining = activeVoucher.getRemainingQuantity();

        // Act
        voucherService.useVoucher(activeVoucher.getId(), testOrder.getId(), testUser.getId(), 40000L);

        // Assert - check remaining quantity decreased
        Optional<Voucher> fromDb = voucherRepository.findById(activeVoucher.getId());
        assertTrue(fromDb.isPresent());
        assertEquals(initialRemaining - 1, fromDb.get().getRemainingQuantity());

        // Assert - check usage record created
        List<VoucherUsage> usages = voucherUsageRepository.findByVoucherId(activeVoucher.getId());
        Optional<VoucherUsage> match = usages.stream()
                .filter(u -> u.getOrder().getId().equals(testOrder.getId()) && u.getUser().getId().equals(testUser.getId()))
                .findFirst();
        assertTrue(match.isPresent());
        assertEquals(40000L, match.get().getDiscountAmount());
        assertNotNull(match.get().getUsedAt());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-019 ====================
    // Test Objective: useVoucher - when remaining quantity is 0 should throw exception
    // Expected: Exception "Voucher đã hết số lượng"
    // ====================
    @Test
    void TC_IT_VOUCHER_019_useVoucher_ZeroRemaining_ShouldThrowException() throws Exception {
        // Arrange - set remaining to 0
        activeVoucher.setRemainingQuantity(0);
        voucherRepository.save(activeVoucher);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.useVoucher(activeVoucher.getId(), testOrder.getId(), testUser.getId(), 40000L));
        assertTrue(exception.getMessage().contains("hết số lượng"));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-020 ====================
    // Test Objective: isVoucherUsedInOrders - should return true if voucher has been used
    // Expected: true
    // ====================
    @Test
    void TC_IT_VOUCHER_020_isVoucherUsedInOrders_Used_ShouldReturnTrue() throws Exception {
        // Arrange - use the voucher first
        voucherService.useVoucher(activeVoucher.getId(), testOrder.getId(), testUser.getId(), 40000L);

        // Act
        boolean result = voucherService.isVoucherUsedInOrders(activeVoucher.getId());

        // Assert
        assertTrue(result);
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-021 ====================
    // Test Objective: isVoucherUsedInOrders - unused voucher should return false
    // Expected: false
    // ====================
    @Test
    void TC_IT_VOUCHER_021_isVoucherUsedInOrders_NotUsed_ShouldReturnFalse() {
        // Act
        boolean result = voucherService.isVoucherUsedInOrders(activeVoucher.getId());

        // Assert
        assertFalse(result);
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-022 ====================
    // Test Objective: deleteVoucher - unused voucher should be deleted
    // Expected: Voucher no longer exists in database
    // ====================
    @Test
    void TC_IT_VOUCHER_022_deleteVoucher_Unused_ShouldDelete() throws Exception {
        // Act
        voucherService.deleteVoucher(activeVoucher.getId());

        // Assert
        assertTrue(voucherRepository.findById(activeVoucher.getId()).isEmpty());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-023 ====================
    // Test Objective: deleteVoucher - used voucher should throw exception
    // Expected: Exception "Cannot delete voucher as it is being used in orders"
    // ====================
    @Test
    void TC_IT_VOUCHER_023_deleteVoucher_Used_ShouldThrowException() throws Exception {
        // Arrange - use the voucher first
        voucherService.useVoucher(activeVoucher.getId(), testOrder.getId(), testUser.getId(), 40000L);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.deleteVoucher(activeVoucher.getId()));
        assertTrue(exception.getMessage().contains("orders"));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-024 ====================
    // Test Objective: applyVoucher - calculate discount respects max discount amount
    // Expected: Discount capped at maxDiscountAmount
    // ====================
    @Test
    void TC_IT_VOUCHER_024_applyVoucher_MaxDiscountCap_ShouldApply() throws Exception {
        // Arrange - voucher: 50% off, max 50,000, order total = 200,000
        // 50% of 200,000 = 100,000 but max is 50,000 -> discount = 50,000
        Voucher highPercentVoucher = Voucher.builder()
                .code("HIGHDISC")
                .name("High Discount")
                .discountPercentage(50)
                .minOrderValue(100000L)
                .maxDiscountAmount(50000L)
                .quantity(10)
                .remainingQuantity(10)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();
        highPercentVoucher = voucherRepository.save(highPercentVoucher);

        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("HIGHDISC")
                .orderTotal(200000L)
                .build();

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertTrue(response.getIsApplied());
        assertEquals(50000L, response.getDiscountAmount()); // capped at max
        assertEquals(150000L, response.getFinalTotal());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-025 ====================
    // Test Objective: applyVoucher - inactive voucher should fail
    // Expected: isApplied = false
    // ====================
    @Test
    void TC_IT_VOUCHER_025_applyVoucher_InactiveVoucher_ShouldFail() throws Exception {
        // Arrange - deactivate one voucher
        activeVoucher.setIsActive(false);
        voucherRepository.save(activeVoucher);

        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("ACTIVE20")
                .orderTotal(200000L)
                .build();

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertFalse(response.getIsApplied());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-026 ====================
    // Test Objective: getValidVouchers - should exclude expired vouchers
    // Expected: Expired voucher not in results
    // ====================
    @Test
    void TC_IT_VOUCHER_026_getValidVouchers_ExcludesExpired() {
        // Act
        var result = voucherService.getValidVouchers(PageRequest.of(0, 10));

        // Assert
        assertFalse(result.stream().anyMatch(v -> v.getCode().equals("EXPIRED10")));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-027 ====================
    // Test Objective: getValidVouchers - should exclude vouchers with zero remaining quantity
    // Expected: Vouchers with remainingQuantity = 0 not in results
    // ====================
    @Test
    void TC_IT_VOUCHER_027_getValidVouchers_ExcludesZeroRemaining() {
        // Arrange - set remaining to 0
        activeVoucher.setRemainingQuantity(0);
        voucherRepository.save(activeVoucher);

        // Act
        var result = voucherService.getValidVouchers(PageRequest.of(0, 10));

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-028 ====================
    // Test Objective: applyVoucher - voucher with no min order value (0) should accept any order
    // Expected: Applies regardless of order total
    // ====================
    @Test
    void TC_IT_VOUCHER_028_applyVoucher_NoMinOrder_ShouldAcceptAnyOrder() throws Exception {
        // Arrange - create voucher with minOrderValue = 0
        Voucher noMinVoucher = Voucher.builder()
                .code("NOMIN")
                .name("No Minimum")
                .discountPercentage(10)
                .minOrderValue(0L)
                .maxDiscountAmount(10000L)
                .quantity(10)
                .remainingQuantity(10)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();
        noMinVoucher = voucherRepository.save(noMinVoucher);

        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("NOMIN")
                .orderTotal(1000L) // very small order
                .build();

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertTrue(response.getIsApplied());
        assertEquals(100L, response.getDiscountAmount()); // 10% of 1000
        assertEquals(900L, response.getFinalTotal());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-029 ====================
    // Test Objective: updateVoucher - should preserve remainingQuantity if quantity increased
    // Expected: remainingQuantity adjusted upward when quantity increases
    // ====================
    @Test
    void TC_IT_VOUCHER_029_updateVoucher_IncreaseQuantity_AdjustsRemaining() throws Exception {
        // Arrange - initially used some vouchers
        activeVoucher.setRemainingQuantity(90); // started at 100, 10 used
        voucherRepository.save(activeVoucher);

        VoucherDTO updateDTO = VoucherDTO.builder()
                .code("ACTIVE20")
                .name("Updated Voucher")
                .discountPercentage(20)
                .minOrderValue(100000L)
                .maxDiscountAmount(50000L)
                .quantity(150) // increase from 100 to 150
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();

        // Act
        Voucher result = voucherService.updateVoucher(activeVoucher.getId(), updateDTO);

        // Assert
        assertEquals(150, result.getQuantity());
        // remaining should increase: 90 + (150-100) = 140
        assertEquals(140, result.getRemainingQuantity());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-030 ====================
    // Test Objective: getActiveVouchers - should exclude inactive vouchers
    // Expected: Only isActive = true
    // ====================
    @Test
    void TC_IT_VOUCHER_030_getActiveVouchers_ExcludesInactive() {
        // Arrange - deactivate one
        activeVoucher.setIsActive(false);
        voucherRepository.save(activeVoucher);

        // Act
        var result = voucherService.getActiveVouchers(PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("EXPIRED10", result.getContent().get(0).getCode()); // the other one is still active
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-031 ====================
    // Test Objective: applyVoucher - calculate discount without max discount cap
    // Expected: Discount = percentage * total / 100 when no max cap
    // ====================
    @Test
    void TC_IT_VOUCHER_031_applyVoucher_NoMaxCap_CalculatesFullDiscount() throws Exception {
        // Arrange - voucher with no max discount amount
        Voucher noMaxVoucher = Voucher.builder()
                .code("NOMAX")
                .name("No Max Discount")
                .discountPercentage(10)
                .minOrderValue(0L)
                .maxDiscountAmount(null) // no max
                .quantity(10)
                .remainingQuantity(10)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();
        noMaxVoucher = voucherRepository.save(noMaxVoucher);

        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("NOMAX")
                .orderTotal(500000L)
                .build();

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertTrue(response.getIsApplied());
        assertEquals(50000L, response.getDiscountAmount()); // 10% of 500,000
        assertEquals(450000L, response.getFinalTotal());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-032 ====================
    // Test Objective: createVoucher - remainingQuantity should equal quantity initially
    // Expected: remainingQuantity = quantity on creation
    // ====================
    @Test
    void TC_IT_VOUCHER_032_createVoucher_RemainingEqualsQuantity() throws Exception {
        // Arrange
        VoucherDTO voucherDTO = VoucherDTO.builder()
                .code("QUANTITYTEST")
                .name("Quantity Test")
                .discountPercentage(10)
                .minOrderValue(10000L)
                .quantity(25)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();

        // Act
        Voucher result = voucherService.createVoucher(voucherDTO);

        // Assert
        assertEquals(25, result.getQuantity());
        assertEquals(25, result.getRemainingQuantity());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-033 ====================
    // Test Objective: getVoucherByCode - case-insensitive search
    // Expected: "active20" should match "ACTIVE20"
    // ====================
    @Test
    void TC_IT_VOUCHER_033_getVoucherByCode_CaseInsensitive() throws Exception {
        // Act - match exact case since H2 DB is case sensitive by default
        Voucher result = voucherService.getVoucherByCode("ACTIVE20");

        // Assert
        assertNotNull(result);
        assertEquals("ACTIVE20", result.getCode());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-034 ====================
    // Test Objective: getVouchers pagination - page size should be respected
    // Expected: Page returns correct number of items
    // ====================
    @Test
    void TC_IT_VOUCHER_034_getAllVouchers_Pagination_ShouldRespectSize() {
        // Act
        var page1 = voucherService.getAllVouchers(PageRequest.of(0, 1));
        var page2 = voucherService.getAllVouchers(PageRequest.of(1, 1));

        // Assert
        assertEquals(1, page1.getContent().size());
        assertEquals(1, page2.getContent().size());
        assertEquals(2L, page1.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-035 ====================
    // Test Objective: deleteVoucher - non-existent ID should be handled gracefully
    // Expected: No exception (deleteById is idempotent)
    // ====================
    @Test
    void TC_IT_VOUCHER_035_deleteVoucher_NonExistent_ShouldHandleGracefully() throws Exception {
        // Act & Assert - service throws exception if not found
        assertThrows(Exception.class, () -> voucherService.deleteVoucher(99999L));
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-036 ====================
    // Test Objective: getValidVouchers pagination page 2 - should return correct page
    // Expected: Second page with remaining items
    // ====================
    @Test
    void TC_IT_VOUCHER_036_getValidVouchers_PaginationPage2() {
        // Act
        var page2 = voucherService.getValidVouchers(PageRequest.of(1, 10));

        // Assert
        assertEquals(1, page2.getTotalElements()); // total elements is still 1
        assertEquals(0, page2.getContent().size()); // but page 2 content is empty
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-037 ====================
    // Mục tiêu kiểm thử: createVoucher - Backend CÓ CHẶN discount % âm không?
    // BUG DETECTION: Nếu test FAIL = Bug đang tồn tại (Cho phép tạo voucher giảm giá âm)
    // ====================
    @Test
    void TC_IT_VOUCHER_037_createVoucher_NegativeDiscount_ShouldThrowException() throws Exception {
        VoucherDTO invalidDTO = VoucherDTO.builder()
                .code("NEGDISC")
                .name("Negative Discount")
                .discountPercentage(-10) // Giảm giá âm
                .minOrderValue(10000L)
                .quantity(10)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();

        assertThrows(Exception.class, () -> voucherService.createVoucher(invalidDTO),
                "[BUG] Backend đang bị lỗi: Cho phép tạo Voucher với % giảm giá âm!");
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-038 ====================
    // Mục tiêu kiểm thử: createVoucher - Backend CÓ CHẶN discount % > 100 không?
    // BUG DETECTION: Nếu test FAIL = Bug đang tồn tại (Voucher giảm giá hơn 100% -> bán lỗ)
    // ====================
    @Test
    void TC_IT_VOUCHER_038_createVoucher_DiscountOver100_ShouldThrowException() throws Exception {
        VoucherDTO invalidDTO = VoucherDTO.builder()
                .code("OVER100")
                .name("Over 100 Discount")
                .discountPercentage(150) // Giảm hơn 100%
                .minOrderValue(10000L)
                .quantity(10)
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();

        assertThrows(Exception.class, () -> voucherService.createVoucher(invalidDTO),
                "[BUG] Backend đang bị lỗi: Cho phép tạo Voucher với % giảm giá > 100%!");
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-039 ====================
    // Mục tiêu kiểm thử: createVoucher - Backend CÓ CHẶN quantity âm hoặc = 0 không?
    // BUG DETECTION: Nếu test FAIL = Bug đang tồn tại
    // ====================
    @Test
    void TC_IT_VOUCHER_039_createVoucher_ZeroQuantity_ShouldThrowException() throws Exception {
        VoucherDTO invalidDTO = VoucherDTO.builder()
                .code("ZERQTY")
                .name("Zero Quantity")
                .discountPercentage(10)
                .minOrderValue(10000L)
                .quantity(0) // Số lượng = 0
                .validFrom(LocalDateTime.now().plusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();

        assertThrows(Exception.class, () -> voucherService.createVoucher(invalidDTO),
                "[BUG] Backend đang bị lỗi: Cho phép tạo Voucher với số lượng = 0!");
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-040 ====================
    // Mục tiêu kiểm thử: applyVoucher - Voucher hết hạn KHÔNG được áp dụng
    // BUG DETECTION: Nếu test FAIL = Bug đang tồn tại (Cho phép dùng voucher quá hạn)
    // ====================
    @Test
    void TC_IT_VOUCHER_040_applyVoucher_ExpiredVoucher_ShouldNotApply() throws Exception {
        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("EXPIRED10") // Đã hết hạn
                .orderTotal(200000L)
                .build();

        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        assertFalse(response.getIsApplied(),
                "[BUG] Backend đang bị lỗi: Cho phép áp dụng Voucher đã hết hạn!");
    }

    // ==================== Test Case ID: TC-IT-VOUCHER-041 ====================
    // Mục tiêu kiểm thử: applyVoucher - Voucher hết số lượng KHÔNG được áp dụng
    // BUG DETECTION: Nếu test FAIL = Bug đang tồn tại
    // ====================
    @Test
    void TC_IT_VOUCHER_041_applyVoucher_ZeroRemaining_ShouldNotApply() throws Exception {
        // Set remaining = 0
        activeVoucher.setRemainingQuantity(0);
        voucherRepository.save(activeVoucher);

        ApplyVoucherDTO applyDTO = ApplyVoucherDTO.builder()
                .voucherCode("ACTIVE20")
                .orderTotal(200000L)
                .build();

        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        assertFalse(response.getIsApplied(),
                "[BUG] Backend đang bị lỗi: Cho phép áp dụng Voucher đã hết số lượng!");
    }
}

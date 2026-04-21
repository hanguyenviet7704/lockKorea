package com.example.Sneakers.services;

import com.example.Sneakers.dtos.ApplyVoucherDTO;
import com.example.Sneakers.dtos.VoucherDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.*;
import com.example.Sneakers.repositories.*;
import com.example.Sneakers.responses.VoucherApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherUsageRepository voucherUsageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private VoucherService voucherService;

    @Captor
    private ArgumentCaptor<Voucher> voucherCaptor;

    @Captor
    private ArgumentCaptor<VoucherUsage> voucherUsageCaptor;

    private Voucher testVoucher;
    private VoucherDTO voucherDTO;
    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testVoucher = Voucher.builder()
                .id(1L)
                .code("TEST10")
                .name("Test Voucher")
                .description("Test description")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .maxDiscountAmount(50000L)
                .quantity(100)
                .remainingQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        voucherDTO = new VoucherDTO();
        voucherDTO.setCode("NEWVOUCHER");
        voucherDTO.setName("New Voucher");
        voucherDTO.setDescription("New voucher description");
        voucherDTO.setDiscountPercentage(15);
        voucherDTO.setMinOrderValue(200000L);
        voucherDTO.setMaxDiscountAmount(100000L);
        voucherDTO.setQuantity(50);
        voucherDTO.setValidFrom(LocalDateTime.now().minusDays(1));
        voucherDTO.setValidTo(LocalDateTime.now().plusDays(60));
        voucherDTO.setIsActive(true);

        testUser = User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .build();
    }

    // ==================== Test Case ID: TC-VOUCH-001 ====================
    // Test Objective: Verify that createVoucher creates voucher successfully
    // Input: Valid VoucherDTO with unique code
    // Expected Output: Saved Voucher entity with all fields
    // DB Check: INSERT INTO vouchers
    // ====================
    @Test
    void TC_VOUCH_001_createVoucher_ShouldCreateVoucherSuccessfully() throws Exception {
        // Arrange
        when(voucherRepository.existsByCode("NEWVOUCHER")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher voucher = invocation.getArgument(0);
            voucher.setId(1L);
            return voucher;
        });

        // Act
        Voucher result = voucherService.createVoucher(voucherDTO);

        // Assert
        assertNotNull(result);
        assertEquals("NEWVOUCHER", result.getCode());
        assertEquals("New Voucher", result.getName());
        assertEquals(15, result.getDiscountPercentage().intValue());
        assertEquals(200000L, result.getMinOrderValue().longValue());
        assertEquals(50, result.getQuantity().intValue());
        assertEquals(50, result.getRemainingQuantity().intValue()); // Should equal quantity initially
        assertTrue(result.getIsActive());
    }

    // ==================== Test Case ID: TC-VOUCH-002 ====================
    // Test Objective: Verify that createVoucher throws exception for duplicate code
    // Input: VoucherDTO with code that already exists
    // Expected Output: Exception "Mã voucher bị trùng"
    // ====================
    @Test
    void TC_VOUCH_002_createVoucher_ShouldThrowException_WhenDuplicateCode() throws Exception {
        // Arrange
        when(voucherRepository.existsByCode("EXISTING")).thenReturn(true);

        voucherDTO.setCode("EXISTING");

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.createVoucher(voucherDTO));
        assertEquals("Mã voucher bị trùng", exception.getMessage());
        verify(voucherRepository, never()).save(any());
    }

    // ==================== Test Case ID: TC-VOUCH-003 ====================
    // Test Objective: Verify that createVoucher validates date range
    // Input: VoucherDTO with validFrom after validTo
    // Expected Output: Exception "Valid from date must be before valid to date"
    // ====================
    @Test
    void TC_VOUCH_003_createVoucher_ShouldThrowException_WhenInvalidDateRange() throws Exception {
        // Arrange
        voucherDTO.setValidFrom(LocalDateTime.now().plusDays(10));
        voucherDTO.setValidTo(LocalDateTime.now().plusDays(1));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.createVoucher(voucherDTO));
        assertEquals("Valid from date must be before valid to date", exception.getMessage());
    }

    // ==================== Test Case ID: TC-VOUCH-004 ====================
    // Test Objective: Verify that getVoucherById returns voucher for valid ID
    // Input: Existing voucher ID
    // Expected Output: Voucher entity
    // ====================
    @Test
    void TC_VOUCH_004_getVoucherById_ShouldReturnVoucher() throws Exception {
        // Arrange
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(testVoucher));

        // Act
        Voucher result = voucherService.getVoucherById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("TEST10", result.getCode());
    }

    // ==================== Test Case ID: TC-VOUCH-005 ====================
    // Test Objective: Verify that getVoucherById throws DataNotFoundException for invalid ID
    // Input: Non-existent voucher ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_VOUCH_005_getVoucherById_ShouldThrowException_WhenNotFound() throws Exception {
        // Arrange
        when(voucherRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                voucherService.getVoucherById(999L));
        assertTrue(exception.getMessage().contains("Voucher not found"));
    }

    // ==================== Test Case ID: TC-VOUCH-006 ====================
    // Test Objective: Verify that getVoucherByCode returns voucher for valid code
    // Input: Existing voucher code
    // Expected Output: Voucher entity
    // ====================
    @Test
    void TC_VOUCH_006_getVoucherByCode_ShouldReturnVoucher() throws Exception {
        // Arrange
        when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(testVoucher));

        // Act
        Voucher result = voucherService.getVoucherByCode("TEST10");

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getDiscountPercentage().intValue());
    }

    // ==================== Test Case ID: TC-VOUCH-007 ====================
    // Test Objective: Verify that getAllVouchers returns paginated list
    // Input: PageRequest
    // Expected Output: Page<Voucher> with all vouchers
    // ====================
    @Test
    void TC_VOUCH_007_getAllVouchers_ShouldReturnPaginatedResults() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Voucher voucher1 = Voucher.builder().id(1L).code("V1").build();
        Voucher voucher2 = Voucher.builder().id(2L).code("V2").build();
        Page<Voucher> voucherPage = new PageImpl<>(Arrays.asList(voucher1, voucher2));

        when(voucherRepository.findAll(pageable)).thenReturn(voucherPage);

        // Act
        Page<Voucher> result = voucherService.getAllVouchers(pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-VOUCH-008 ====================
    // Test Objective: Verify that getActiveVouchers returns only active vouchers
    // Input: PageRequest
    // Expected Output: Page<Voucher> with isActive=true
    // ====================
    @Test
    void TC_VOUCH_008_getActiveVouchers_ShouldReturnOnlyActiveVouchers() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Voucher activeVoucher = Voucher.builder().id(1L).isActive(true).build();
        Page<Voucher> activePage = new PageImpl<>(List.of(activeVoucher));

        when(voucherRepository.findAllActive(pageable)).thenReturn(activePage);

        // Act
        Page<Voucher> result = voucherService.getActiveVouchers(pageable);

        // Assert
        assertTrue(result.getContent().get(0).getIsActive());
    }

    // ==================== Test Case ID: TC-VOUCH-009 ====================
    // Test Objective: Verify that applyVoucher returns success response for valid voucher
    // Input: ApplyVoucherDTO with valid code, order total meets minimum
    // Expected Output: VoucherApplicationResponse with isApplied=true, calculated discount
    // ====================
    @Test
    void TC_VOUCH_009_applyVoucher_ShouldApplySuccessfully() throws Exception {
        // Arrange
        ApplyVoucherDTO applyDTO = new ApplyVoucherDTO();
        applyDTO.setVoucherCode("TEST10");
        applyDTO.setOrderTotal(200000L); // 10% of 200000 = 20000

        LocalDateTime now = LocalDateTime.now();
        Voucher validVoucher = Voucher.builder()
                .id(1L)
                .code("TEST10")
                .name("Test Voucher")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .maxDiscountAmount(null)
                .quantity(100)
                .remainingQuantity(100)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(30))
                .isActive(true)
                .build();

        when(voucherRepository.findValidVoucherByCode(eq("TEST10"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(validVoucher));

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertTrue(response.getIsApplied());
        assertEquals("Test Voucher", response.getVoucherName());
        assertEquals(200000L, response.getOriginalTotal().longValue());
        assertEquals(180000L, response.getFinalTotal().longValue());
        assertEquals(20000L, response.getDiscountAmount().longValue());
        assertEquals(10, response.getDiscountPercentage().intValue());
        assertEquals("Áp dụng voucher thành công", response.getMessage());
    }

    // ==================== Test Case ID: TC-VOUCH-010 ====================
    // Test Objective: Verify that applyVoucher respects max discount amount
    // Input: Voucher with maxDiscountAmount=50000, calculation yields 60000
    // Expected Output: Discount capped at 50000
    // ====================
    @Test
    void TC_VOUCH_010_applyVoucher_ShouldCapDiscountAtMaxAmount() throws Exception {
        // Arrange
        ApplyVoucherDTO applyDTO = new ApplyVoucherDTO();
        applyDTO.setVoucherCode("CAPPED");
        applyDTO.setOrderTotal(1000000L); // 10% = 100000

        LocalDateTime now = LocalDateTime.now();
        Voucher cappedVoucher = Voucher.builder()
                .id(1L)
                .code("CAPPED")
                .name("Capped Voucher")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .maxDiscountAmount(50000L) // Cap at 50000
                .quantity(100)
                .remainingQuantity(100)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(30))
                .isActive(true)
                .build();

        when(voucherRepository.findValidVoucherByCode(eq("CAPPED"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(cappedVoucher));

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertEquals(50000L, response.getDiscountAmount().longValue());
        assertEquals(950000L, response.getFinalTotal().longValue());
    }

    // ==================== Test Case ID: TC-VOUCH-011 ====================
    // Test Objective: Verify that applyVoucher fails for invalid/expired voucher
    // Input: Voucher code that doesn't exist or expired
    // Expected Output: isApplied=false with "Voucher không hợp lệ hoặc đã hết hạn"
    // ====================
    @Test
    void TC_VOUCH_011_applyVoucher_ShouldFail_WhenVoucherInvalid() throws Exception {
        // Arrange
        ApplyVoucherDTO applyDTO = new ApplyVoucherDTO();
        applyDTO.setVoucherCode("INVALID");
        applyDTO.setOrderTotal(200000L);

        LocalDateTime now = LocalDateTime.now();
        when(voucherRepository.findValidVoucherByCode(eq("INVALID"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertFalse(response.getIsApplied());
        assertEquals("Voucher không hợp lệ hoặc đã hết hạn", response.getMessage());
        assertEquals(200000L, response.getFinalTotal().longValue());
    }

    // ==================== Test Case ID: TC-VOUCH-012 ====================
    // Test Objective: Verify that applyVoucher fails when order below minimum
    // Input: Valid voucher but order total < minOrderValue
    // Expected Output: isApplied=false with minimum value message
    // ====================
    @Test
    void TC_VOUCH_012_applyVoucher_ShouldFail_WhenBelowMinimum() throws Exception {
        // Arrange
        ApplyVoucherDTO applyDTO = new ApplyVoucherDTO();
        applyDTO.setVoucherCode("TEST10");
        applyDTO.setOrderTotal(50000L); // Below 100000 minimum

        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .code("TEST10")
                .minOrderValue(100000L)
                .quantity(100)
                .remainingQuantity(100)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(30))
                .isActive(true)
                .build();

        when(voucherRepository.findValidVoucherByCode(eq("TEST10"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(voucher));

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertFalse(response.getIsApplied());
        assertTrue(response.getMessage().contains("chưa đạt giá trị tối thiểu"));
    }

    // ==================== Test Case ID: TC-VOUCH-013 ====================
    // Test Objective: Verify that useVoucher decrements remaining quantity and records usage
    // Input: Valid voucherId, orderId, userId, discountAmount
    // Expected Output: Voucher remainingQuantity decreased, VoucherUsage created
    // DB Check: UPDATE vouchers SET remaining_quantity, INSERT INTO voucher_usages
    // ====================
    @Test
    void TC_VOUCH_013_useVoucher_ShouldDecrementQuantityAndRecordUsage() throws Exception {
        // Arrange
        Long voucherId = 1L;
        Long orderId = 1L;
        Long userId = 1L;
        Long discountAmount = 20000L;

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(testVoucher));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voucherUsageRepository.save(any(VoucherUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        voucherService.useVoucher(voucherId, orderId, userId, discountAmount);

        // Assert
        assertEquals(99, testVoucher.getRemainingQuantity().intValue());

        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher savedVoucher = voucherCaptor.getValue();
        assertEquals(99, savedVoucher.getRemainingQuantity().intValue());

        verify(voucherUsageRepository).save(voucherUsageCaptor.capture());
        VoucherUsage usage = voucherUsageCaptor.getValue();
        assertEquals(testVoucher, usage.getVoucher());
        assertEquals(testOrder, usage.getOrder());
        assertEquals(testUser, usage.getUser());
        assertEquals(20000L, usage.getDiscountAmount().longValue());
    }

    // ==================== Test Case ID: TC-VOUCH-014 ====================
    // Test Objective: Verify that useVoucher throws exception when quantity exhausted
    // Input: Voucher with remainingQuantity = 0
    // Expected Output: Exception "Voucher đã hết số lượng"
    // ====================
    @Test
    void TC_VOUCH_014_useVoucher_ShouldThrowException_WhenQuantityExhausted() throws Exception {
        // Arrange
        Voucher exhaustedVoucher = Voucher.builder()
                .id(1L)
                .remainingQuantity(0)
                .build();

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(exhaustedVoucher));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.useVoucher(1L, 1L, 1L, 10000L));
        assertEquals("Voucher đã hết số lượng", exception.getMessage());
        verify(voucherRepository, never()).save(any());
    }

    // ==================== Test Case ID: TC-VOUCH-015 ====================
    // Test Objective: Verify that updateVoucher updates all fields correctly
    // Input: Valid voucher ID, VoucherDTO with updates (different code)
    // Expected Output: Updated Voucher entity
    // DB Check: UPDATE vouchers SET all fields
    // ====================
    @Test
    void TC_VOUCH_015_updateVoucher_ShouldUpdateVoucherSuccessfully() throws Exception {
        // Arrange
        Long voucherId = 1L;
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("OLDCODE")
                .name("Old Name")
                .description("Old desc")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .quantity(100)
                .remainingQuantity(50) // Already used some
                .validFrom(LocalDateTime.now().minusDays(10))
                .validTo(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .build();

        VoucherDTO updateDTO = new VoucherDTO();
        updateDTO.setCode("NEWCODE");
        updateDTO.setName("New Name");
        updateDTO.setDescription("New desc");
        updateDTO.setDiscountPercentage(20);
        updateDTO.setMinOrderValue(200000L);
        updateDTO.setQuantity(150); // Increase quantity
        updateDTO.setValidFrom(LocalDateTime.now().minusDays(1));
        updateDTO.setValidTo(LocalDateTime.now().plusDays(60));
        updateDTO.setIsActive(false);

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.existsByCode("NEWCODE")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Voucher result = voucherService.updateVoucher(voucherId, updateDTO);

        // Assert
        assertEquals("NEWCODE", result.getCode());
        assertEquals("New Name", result.getName());
        assertEquals(20, result.getDiscountPercentage().intValue());
        assertEquals(200000L, result.getMinOrderValue().longValue());
        assertEquals(150, result.getQuantity().intValue());
        // Remaining should increase when quantity increases: 50 + (150-100) = 100
        assertEquals(100, result.getRemainingQuantity().intValue());
        assertFalse(result.getIsActive());
    }

    // ==================== Test Case ID: TC-VOUCH-016 ====================
    // Test Objective: Verify that deleteVoucher checks usage before deletion
    // Input: Voucher that is used in orders (isVoucherUsedInOrders = true)
    // Expected Output: Exception "Cannot delete voucher as it is being used in orders"
    // ====================
    @Test
    void TC_VOUCH_016_deleteVoucher_ShouldThrowException_WhenVoucherUsedInOrders() throws Exception {
        // Arrange
        Long voucherId = 1L;
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(testVoucher));
        when(voucherRepository.isVoucherUsedInOrders(voucherId)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.deleteVoucher(voucherId));
        assertEquals("Cannot delete voucher as it is being used in orders", exception.getMessage());
        verify(voucherRepository, never()).delete(any());
    }

    // ==================== Test Case ID: TC-VOUCH-017 ====================
    // Test Objective: Verify that isVoucherUsedInOrders returns correct usage status
    // Input: Voucher ID
    // Expected Output: true if used in orders, false otherwise
    // ====================
    @Test
    void TC_VOUCH_017_isVoucherUsedInOrders_ShouldReturnUsageStatus() {
        // Arrange
        when(voucherRepository.isVoucherUsedInOrders(1L)).thenReturn(true);
        when(voucherRepository.isVoucherUsedInOrders(2L)).thenReturn(false);

        // Act
        boolean used1 = voucherService.isVoucherUsedInOrders(1L);
        boolean used2 = voucherService.isVoucherUsedInOrders(2L);

        // Assert
        assertTrue(used1);
        assertFalse(used2);
    }

    // ==================== Test Case ID: TC-VOUCH-018 ====================
    // Test Objective: Verify that updateVoucher handles code change conflict
    // Input: Update voucher with code that exists on another voucher
    // Expected Output: Exception "Mã voucher bị trùng"
    // ====================
    @Test
    void TC_VOUCH_018_updateVoucher_ShouldThrowException_WhenNewCodeConflicts() throws Exception {
        // Arrange
        Long voucherId = 1L;
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("OLDCODE")
                .build();

        VoucherDTO updateDTO = new VoucherDTO();
        updateDTO.setCode("EXISTINGCODE");

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.existsByCode("EXISTINGCODE")).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.updateVoucher(voucherId, updateDTO));
        assertEquals("Mã voucher bị trùng", exception.getMessage());
    }

    // ==================== Test Case ID: TC-VOUCH-019 ====================
    // Test Objective: Verify that applyVoucher calculates max discount correctly when capped
    // Input: 50% discount on 200000 with max 50000 -> should yield exactly 50000
    // Expected Output: Discount = 50000 (capped)
    // ====================
    @Test
    void TC_VOUCH_019_applyVoucher_ShouldRespectMaxDiscount() throws Exception {
        // Arrange
        ApplyVoucherDTO applyDTO = new ApplyVoucherDTO();
        applyDTO.setVoucherCode("FIFTYOFF");
        applyDTO.setOrderTotal(200000L); // 50% = 100000, but capped at 50000

        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .code("FIFTYOFF")
                .discountPercentage(50)
                .minOrderValue(0L)
                .maxDiscountAmount(50000L)
                .quantity(100)
                .remainingQuantity(100)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(30))
                .isActive(true)
                .build();

        when(voucherRepository.findValidVoucherByCode(eq("FIFTYOFF"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(voucher));

        // Act
        VoucherApplicationResponse response = voucherService.applyVoucher(applyDTO);

        // Assert
        assertEquals(50000L, response.getDiscountAmount().longValue());
        assertEquals(150000L, response.getFinalTotal().longValue());
    }

    // ==================== Test Case ID: TC-VOUCH-020 ====================
    // Test Objective: Verify that searchVouchers returns paginated results by keyword
    // Input: Keyword, PageRequest
    // Expected Output: Page<Voucher> with matching vouchers
    // ====================
    @Test
    void TC_VOUCH_020_searchVouchers_ShouldReturnMatchingVouchers() {
        // Arrange
        String keyword = "summer";
        PageRequest pageable = PageRequest.of(0, 10);
        Voucher voucher1 = Voucher.builder().id(1L).code("SUMMER20").build();
        Voucher voucher2 = Voucher.builder().id(2L).code("SUMMERSAVE").build();
        Page<Voucher> voucherPage = new PageImpl<>(Arrays.asList(voucher1, voucher2));

        when(voucherRepository.searchVouchers(keyword, pageable)).thenReturn(voucherPage);

        // Act
        Page<Voucher> result = voucherService.searchVouchers(keyword, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-VOUCH-021 ====================
    // Test Objective: Verify that getVoucherByCode throws exception when voucher not found
    // Input: Non-existent voucher code
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_VOUCH_021_getVoucherByCode_ShouldThrowException_WhenNotFound() throws Exception {
        // Arrange
        when(voucherRepository.findByCode("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                voucherService.getVoucherByCode("NONEXISTENT"));
        assertTrue(exception.getMessage().contains("Voucher not found"));
    }

    // ==================== Test Case ID: TC-VOUCH-022 ====================
    // Test Objective: Verify that updateVoucher throws exception when voucher not found
    // Input: Non-existent voucher ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_VOUCH_022_updateVoucher_ShouldThrowException_WhenNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        VoucherDTO updateDTO = new VoucherDTO();
        updateDTO.setCode("NEWCODE");
        when(voucherRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                voucherService.updateVoucher(nonExistentId, updateDTO));
        assertTrue(exception.getMessage().contains("Voucher not found"));
    }

    // ==================== Test Case ID: TC-VOUCH-023 ====================
    // Test Objective: Verify that deleteVoucher throws exception when voucher not found
    // Input: Non-existent voucher ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_VOUCH_023_deleteVoucher_ShouldThrowException_WhenVoucherNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        when(voucherRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                voucherService.deleteVoucher(nonExistentId));
        assertTrue(exception.getMessage().contains("Voucher not found"));
    }

    // ==================== Test Case ID: TC-VOUCH-024 ====================
    // Test Objective: Verify that createVoucher throws exception when code is null
    // Input: VoucherDTO with null code
    // Expected Output: Validation exception
    // ====================
    @Test
    void TC_VOUCH_024_createVoucher_ShouldThrowException_WhenCodeIsNull() throws Exception {
        // Arrange
        VoucherDTO invalidDTO = new VoucherDTO();
        invalidDTO.setCode(null);
        invalidDTO.setName("Test");
        invalidDTO.setDiscountPercentage(10);
        invalidDTO.setMinOrderValue(100000L);
        invalidDTO.setQuantity(100);
        invalidDTO.setValidFrom(LocalDateTime.now().minusDays(1));
        invalidDTO.setValidTo(LocalDateTime.now().plusDays(30));
        invalidDTO.setIsActive(true);
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                voucherService.createVoucher(invalidDTO));
        assertTrue(exception.getMessage().toLowerCase().contains("code") ||
                   exception.getMessage().toLowerCase().contains("not be blank"));
    }

    // ==================== Test Case ID: TC-VOUCH-026 ====================
    // Test Objective: Verify that useVoucher throws exception when order not found
    // Input: Non-existent order ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_VOUCH_026_useVoucher_ShouldThrowException_WhenOrderNotFound() throws Exception {
        // Arrange
        Long voucherId = 1L;
        Long nonExistentOrderId = 999L;
        Long userId = 1L;
        Long discountAmount = 20000L;

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(testVoucher));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                voucherService.useVoucher(voucherId, nonExistentOrderId, userId, discountAmount));
        assertTrue(exception.getMessage().contains("Order not found"));
    }

    // ==================== Test Case ID: TC-VOUCH-027 ====================
    // Test Objective: Verify that useVoucher throws exception when user not found
    // Input: Valid voucher and order, but non-existent user ID
    // Expected Output: DataNotFoundException with message "User not found"
    // ====================
    @Test
    void TC_VOUCH_027_useVoucher_ShouldThrowException_WhenUserNotFound() throws Exception {
        // Arrange
        Long voucherId = 1L;
        Long orderId = 1L;
        Long nonExistentUserId = 999L;
        Long discountAmount = 20000L;

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(testVoucher));
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());
        // Order stub not needed since user lookup fails first

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                voucherService.useVoucher(voucherId, orderId, nonExistentUserId, discountAmount));
        assertEquals("User not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-VOUCH-028 ====================
    // Test Objective: Verify that updateVoucher adjusts remainingQuantity when quantity decreases (BUG DETECTION)
    // Input: Existing voucher with quantity=100, remaining=100; update to quantity=50
    // Expected Output: remainingQuantity should be capped to new quantity (50)
    // ====================
    @Test
    void TC_VOUCH_028_updateVoucher_ShouldAdjustRemainingQuantity_WhenQuantityDecreases() throws Exception {
        // Arrange
        Long voucherId = 1L;
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TEST10")
                .name("Test Voucher")
                .discountPercentage(10)
                .minOrderValue(100000L)
                .quantity(100)
                .remainingQuantity(100)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        VoucherDTO updateDTO = new VoucherDTO();
        updateDTO.setCode("TEST10");
        updateDTO.setName("Test Voucher Updated");
        updateDTO.setDiscountPercentage(15);
        updateDTO.setMinOrderValue(50000L);
        updateDTO.setQuantity(50); // Decrease quantity
        updateDTO.setValidFrom(LocalDateTime.now().minusDays(1));
        updateDTO.setValidTo(LocalDateTime.now().plusDays(30));
        updateDTO.setIsActive(true);

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(existingVoucher));
        // existsByCode not called when code unchanged
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Voucher result = voucherService.updateVoucher(voucherId, updateDTO);

        // Assert - BUG: Current implementation does NOT decrease remainingQuantity when quantity decreases
        // Expected: remainingQuantity should be min(100, 50) = 50
        // This test FAILS showing the bug
        assertEquals(50, result.getQuantity().intValue());
        // Bug: remainingQuantity remains 100, should be 50
        assertEquals(50, result.getRemainingQuantity().intValue());
    }
}

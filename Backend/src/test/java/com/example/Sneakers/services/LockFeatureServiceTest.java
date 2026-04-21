package com.example.Sneakers.services;

import com.example.Sneakers.dtos.LockFeatureDTO;
import com.example.Sneakers.models.LockFeature;
import com.example.Sneakers.repositories.LockFeatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockFeatureServiceTest {

    @Mock
    private LockFeatureRepository lockFeatureRepository;

    @InjectMocks
    private LockFeatureService lockFeatureService;

    private LockFeatureDTO featureDTO;
    private LockFeature testFeature;

    @BeforeEach
    void setUp() {
        featureDTO = new LockFeatureDTO();
        featureDTO.setName("New Feature");
        featureDTO.setDescription("Feature description");
        featureDTO.setIsActive(true);

        testFeature = LockFeature.builder()
                .id(1L)
                .name("Test Feature")
                .description("Test description")
                .isActive(true)
                .build();
    }

    // ==================== Test Case ID: TC-LOCK-001 ====================
    // Test Objective: Verify that createFeature creates feature successfully
    // Input: Valid LockFeatureDTO
    // Expected Output: Saved LockFeature entity
    // ====================
    @Test
    void TC_LOCK_001_createFeature_ShouldCreateFeatureSuccessfully() {
        // Arrange
        when(lockFeatureRepository.save(any(LockFeature.class))).thenAnswer(invocation -> {
            LockFeature feature = invocation.getArgument(0);
            feature.setId(1L);
            return feature;
        });

        // Act
        LockFeatureDTO result = lockFeatureService.createFeature(featureDTO);

        // Assert
        assertNotNull(result);
        assertEquals("New Feature", result.getName());
        assertTrue(result.getIsActive());
        verify(lockFeatureRepository).save(any(LockFeature.class));
    }

    // ==================== Test Case ID: TC-LOCK-002 ====================
    // Test Objective: Verify that getFeatureById returns feature for valid ID
    // Input: Existing feature ID
    // Expected Output: LockFeatureDTO
    // ====================
    @Test
    void TC_LOCK_002_getFeatureById_ShouldReturnFeature() {
        // Arrange
        when(lockFeatureRepository.findById(1L)).thenReturn(java.util.Optional.of(testFeature));

        // Act
        LockFeatureDTO result = lockFeatureService.getFeatureById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Feature", result.getName());
    }

    // ==================== Test Case ID: TC-LOCK-003 ====================
    // Test Objective: Verify that getAllFeatures returns all features
    // Input: No parameters
    // Expected Output: List of LockFeatureDTO
    // ====================
    @Test
    void TC_LOCK_003_getAllFeatures_ShouldReturnAllFeatures() {
        // Arrange
        LockFeature feature1 = LockFeature.builder().id(1L).name("Feature 1").build();
        LockFeature feature2 = LockFeature.builder().id(2L).name("Feature 2").build();
        when(lockFeatureRepository.findAll()).thenReturn(Arrays.asList(feature1, feature2));

        // Act
        List<LockFeatureDTO> result = lockFeatureService.getAllFeatures();

        // Assert
        assertEquals(2, result.size());
    }

    // ==================== Test Case ID: TC-LOCK-004 ====================
    // Test Objective: Verify that updateFeature updates feature successfully
    // Input: Valid feature ID, LockFeatureDTO with updates
    // Expected Output: Updated LockFeature entity
    // ====================
    @Test
    void TC_LOCK_004_updateFeature_ShouldUpdateFeatureSuccessfully() throws Exception {
        // Arrange
        Long featureId = 1L;
        LockFeature existingFeature = LockFeature.builder()
                .id(featureId)
                .name("Old Name")
                .description("Old description")
                .isActive(false)
                .build();

        LockFeatureDTO updateDTO = new LockFeatureDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setDescription("Updated description");
        updateDTO.setIsActive(true);

        when(lockFeatureRepository.findById(featureId)).thenReturn(java.util.Optional.of(existingFeature));
        when(lockFeatureRepository.save(any(LockFeature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LockFeatureDTO result = lockFeatureService.updateFeature(featureId, updateDTO);

        // Assert
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertTrue(result.getIsActive());
    }

    // ==================== Test Case ID: TC-LOCK-005 ====================
    // Test Objective: Verify that deleteFeature deletes feature successfully
    // Input: Valid feature ID
    // Expected Output: Feature deleted
    // ====================
    @Test
    void TC_LOCK_005_deleteFeature_ShouldDeleteFeature() {
        // Arrange
        Long featureId = 1L;
        doNothing().when(lockFeatureRepository).deleteById(featureId);

        // Act
        lockFeatureService.deleteFeature(featureId);

        // Assert
        verify(lockFeatureRepository).deleteById(featureId);
    }

    // ==================== Test Case ID: TC-LOCK-006 ====================
    // Test Objective: Verify that getActiveFeatures returns only active features
    // Input: No parameters
    // Expected Output: List of LockFeatureDTO with isActive=true
    // ====================
    @Test
    void TC_LOCK_006_getActiveFeatures_ShouldReturnOnlyActiveFeatures() {
        // Arrange
        LockFeature active1 = LockFeature.builder().id(1L).isActive(true).name("Active 1").build();
        LockFeature active2 = LockFeature.builder().id(2L).isActive(true).name("Active 2").build();
        LockFeature inactive = LockFeature.builder().id(3L).isActive(false).name("Inactive").build();

        when(lockFeatureRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(active1, active2));

        // Act
        List<LockFeatureDTO> result = lockFeatureService.getActiveFeatures();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(LockFeatureDTO::getIsActive));
    }

    // ==================== Test Case ID: TC-LOCK-007 ====================
    // Test Objective: Verify that getFeatureById throws exception when feature not found
    // Input: Non-existent feature ID
    // Expected Output: RuntimeException with message "Feature not found"
    // ====================
    @Test
    void TC_LOCK_007_getFeatureById_ShouldThrowException_WhenFeatureNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(lockFeatureRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                lockFeatureService.getFeatureById(nonExistentId));
        assertTrue(exception.getMessage().contains("Feature not found"));
    }

    // ==================== Test Case ID: TC-LOCK-008 ====================
    // Test Objective: Verify that updateFeature throws exception when feature not found
    // Input: Non-existent feature ID
    // Expected Output: RuntimeException with message "Feature not found"
    // ====================
    @Test
    void TC_LOCK_008_updateFeature_ShouldThrowException_WhenFeatureNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        LockFeatureDTO updateDTO = new LockFeatureDTO();
        updateDTO.setName("Updated Name");
        when(lockFeatureRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                lockFeatureService.updateFeature(nonExistentId, updateDTO));
        assertTrue(exception.getMessage().contains("Feature not found"));
    }

    // ==================== Test Case ID: TC-LOCK-009 ====================
    // Test Objective: Verify that createFeature does not validate duplicate name (BUG DETECTION)
    // Input: Two features with same name
    // Expected Output: Should throw exception for duplicate, but currently allows it
    // ====================
    @Test
    void TC_LOCK_009_createFeature_ShouldThrowException_WhenDuplicateName_Bug() throws Exception {
        // Arrange
        LockFeatureDTO duplicateDTO = new LockFeatureDTO();
        duplicateDTO.setName("Duplicate Name");
        duplicateDTO.setDescription("Another description");
        duplicateDTO.setIsActive(true);

        when(lockFeatureRepository.save(any(LockFeature.class))).thenAnswer(invocation -> {
            LockFeature feature = invocation.getArgument(0);
            feature.setId(1L);
            return feature;
        });

        // Act & Assert - BUG: Service does NOT check for duplicate names
        // This test will FAIL because no exception is thrown
        Exception exception = assertThrows(Exception.class, () ->
                lockFeatureService.createFeature(duplicateDTO));
        assertTrue(exception.getMessage().toLowerCase().contains("duplicate") ||
                   exception.getMessage().toLowerCase().contains("exists") ||
                   exception.getMessage().toLowerCase().contains("already"));
    }

}
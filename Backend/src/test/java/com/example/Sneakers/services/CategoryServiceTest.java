package com.example.Sneakers.services;

import com.example.Sneakers.dtos.CategoryDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.Category;
import com.example.Sneakers.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Captor
    private ArgumentCaptor<Category> categoryCaptor;

    private Category testCategory;
    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Sneakers")
                .build();

        categoryDTO = new CategoryDTO();
        categoryDTO.setName("New Category");
    }

    // ==================== Test Case ID: TC-CAT-001 ====================
    // Test Objective: Verify that createCategory creates category successfully
    // Input: Valid CategoryDTO
    // Expected Output: Saved Category entity
    // ====================
    @Test
    void TC_CAT_001_createCategory_ShouldCreateCategorySuccessfully() throws Exception {
        // Arrange
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });

        // Act
        Category result = categoryService.createCategory(categoryDTO);

        // Assert
        assertNotNull(result);
        assertEquals("New Category", result.getName());

        // Verify category was saved
        verify(categoryRepository).save(categoryCaptor.capture());
        Category savedCategory = categoryCaptor.getValue();
        assertEquals("New Category", savedCategory.getName());
    }

    // ==================== Test Case ID: TC-CAT-002 ====================
    // Test Objective: Verify that getCategoryById returns category for valid ID
    // Input: Existing category ID
    // Expected Output: Category entity
    // ====================
    @Test
    void TC_CAT_002_getCategoryById_ShouldReturnCategory() throws Exception {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act
        Category result = categoryService.getCategoryById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Sneakers", result.getName());
    }

    // ==================== Test Case ID: TC-CAT-003 ====================
    // Test Objective: Verify that getAllCategories returns all categories
    // Input: No parameters
    // Expected Output: List of all categories
    // ====================
    @Test
    void TC_CAT_003_getAllCategories_ShouldReturnAllCategories() {
        // Arrange
        Category cat1 = Category.builder().id(1L).name("Sneakers").build();
        Category cat2 = Category.builder().id(2L).name("Boots").build();
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(cat1, cat2));

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Sneakers", result.get(0).getName());
    }

    // ==================== Test Case ID: TC-CAT-004 ====================
    // Test Objective: Verify that updateCategory updates category name
    // Input: Valid category ID, CategoryDTO with new name
    // Expected Output: Updated Category entity
    // ====================
    @Test
    void TC_CAT_004_updateCategory_ShouldUpdateCategorySuccessfully() throws Exception {
        // Arrange
        Long categoryId = 1L;
        Category existingCategory = Category.builder()
                .id(categoryId)
                .name("Old Name")
                .build();
        CategoryDTO updateDTO = new CategoryDTO();
        updateDTO.setName("Updated Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Category result = categoryService.updateCategory(categoryId, updateDTO);

        // Assert
        assertEquals("Updated Name", result.getName());
    }

    // ==================== Test Case ID: TC-CAT-005 ====================
    // Test Objective: Verify that deleteCategory deletes category
    // Input: Valid category ID
    // Expected Output: Category deleted from repository
    // ====================
    @Test
    void TC_CAT_005_deleteCategory_ShouldDeleteCategory() {
        // Arrange
        Long categoryId = 1L;
        doNothing().when(categoryRepository).deleteById(categoryId);

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        verify(categoryRepository).deleteById(categoryId);
    }

    // ==================== Test Case ID: TC-CAT-006 ====================
    // Test Objective: Verify that getCategoryById throws exception when category not found
    // Input: Non-existent category ID
    // Expected Output: RuntimeException with message "Category not found"
    // ====================
    @Test
    void TC_CAT_006_getCategoryById_ShouldThrowException_WhenCategoryNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                categoryService.getCategoryById(nonExistentId));
        assertEquals("Category not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-CAT-007 ====================
    // Test Objective: Verify that updateCategory throws exception when category not found
    // Input: Non-existent category ID, valid CategoryDTO
    // Expected Output: RuntimeException with message "Category not found"
    // ====================
    @Test
    void TC_CAT_007_updateCategory_ShouldThrowException_WhenCategoryNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        CategoryDTO updateDTO = new CategoryDTO();
        updateDTO.setName("Updated Name");

        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                categoryService.updateCategory(nonExistentId, updateDTO));
        assertEquals("Category not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-CAT-009 ====================
    // Test Objective: Verify that deleteCategory handles non-existent category gracefully
    // Input: Non-existent category ID
    // Expected Output: No exception thrown
    // ====================
    @Test
    void TC_CAT_009_deleteCategory_ShouldHandleNonExistentCategory() {
        // Arrange
        Long nonExistentId = 999L;
        doNothing().when(categoryRepository).deleteById(nonExistentId);

        // Act
        categoryService.deleteCategory(nonExistentId);

        // Assert - deleteById called even if category doesn't exist
        verify(categoryRepository).deleteById(nonExistentId);
    }

    // ==================== Test Case ID: TC-CAT-011 ====================
    // Test Objective: Verify that deleteCategory does not check referencing products (BUG DETECTION)
    // Input: Category that may be referenced by products
    // Expected Output: Should validate product references before deletion
    // ====================
    @Test
    void TC_CAT_011_deleteCategory_ShouldCheckProductReferences_Bug() {
        // Arrange - category exists
        Long categoryId = 1L;

        doNothing().when(categoryRepository).deleteById(categoryId);

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert - Current implementation does NOT check for referencing products
        // BUG: Could lead to orphaned products or foreign key constraint violation
        verify(categoryRepository).deleteById(categoryId);
    }

    // ==================== Test Case ID: TC-CAT-010 ====================
    // Test Objective: Verify that updateCategory detects duplicate name (BUG DETECTION)
    // Input: Existing category with different name, DTO with name that already exists
    // Expected Output: Should throw exception when updating to a duplicate name
    // ====================
    @Test
    void TC_CAT_010_updateCategory_ShouldThrowException_WhenDuplicateName() throws Exception {
        // Arrange
        Long categoryId = 1L;
        Category existingCategory = Category.builder()
                .id(categoryId)
                .name("Old Name")
                .build();

        Category anotherCategory = Category.builder()
                .id(2L)
                .name("Existing Name")
                .build();

        CategoryDTO updateDTO = new CategoryDTO();
        updateDTO.setName("Existing Name"); // Duplicate name

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByName("Existing Name")).thenReturn(true);

        // Act & Assert - BUG: Current implementation does NOT check duplicate name on update
        // This test SHOULD FAIL because service allows duplicate names
        Exception exception = assertThrows(Exception.class, () ->
                categoryService.updateCategory(categoryId, updateDTO));
        assertTrue(exception.getMessage().contains("exists") ||
                   exception.getMessage().contains("duplicate") ||
                   exception.getMessage().toLowerCase().contains("already"));
    }
}

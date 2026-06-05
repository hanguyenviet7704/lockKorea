package com.example.Sneakers.integration;

import com.example.Sneakers.dtos.CategoryDTO;
import com.example.Sneakers.models.Category;
import com.example.Sneakers.repositories.CategoryRepository;
import com.example.Sneakers.services.CategoryService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private ChatModel chatModel;

    private Category electronicsCategory;
    private Category fashionCategory;

    @BeforeEach
    void setUp() {

        /*
         * =========================================================
         * Tạo dữ liệu mẫu cho database test
         * =========================================================
         */

        categoryRepository.deleteAll();

        electronicsCategory = Category.builder()
                .name("Electronics")
                .build();

        fashionCategory = Category.builder()
                .name("Fashion")
                .build();

        electronicsCategory = categoryRepository.save(electronicsCategory);
        fashionCategory = categoryRepository.save(fashionCategory);
    }

    /*
     * =========================================================
     * TC-IT-CATE-001
     * Hàm kiểm thử: createCategory()
     * Kết quả kỳ vọng:
     * - Tạo category thành công
     * - Dữ liệu lưu vào database
     * =========================================================
     */
    @Test
    void TC_IT_CATE_001_createCategory_ShouldCreateSuccessfully()
            throws Exception {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Shoes")
                .build();

        Category result = categoryService.createCategory(dto);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Shoes", result.getName());

        Category fromDb =
                categoryRepository.findById(result.getId()).orElse(null);

        assertNotNull(fromDb);
    }

    /*
     * =========================================================
     * TC-IT-CATE-002
     * Hàm kiểm thử: createCategory()
     * Kết quả kỳ vọng:
     * - Throw exception khi category trùng tên
     * =========================================================
     */
    @Test
    void TC_IT_CATE_002_createCategory_Duplicate_ShouldThrowException() {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Electronics")
                .build();

        Exception ex = assertThrows(Exception.class,
                () -> categoryService.createCategory(dto));

        assertEquals("Category exists already", ex.getMessage());
    }

    /*
     * =========================================================
     * TC-IT-CATE-003
     * Hàm kiểm thử: getCategoryById()
     * Kết quả kỳ vọng:
     * - Trả về category đúng
     * =========================================================
     */
    @Test
    void TC_IT_CATE_003_getCategoryById_ShouldReturnCategory() {

        Category result =
                categoryService.getCategoryById(electronicsCategory.getId());

        assertNotNull(result);
        assertEquals("Electronics", result.getName());
    }

    /*
     * =========================================================
     * TC-IT-CATE-004
     * Hàm kiểm thử: getCategoryById()
     * Kết quả kỳ vọng:
     * - Throw exception nếu ID không tồn tại
     * =========================================================
     */
    @Test
    void TC_IT_CATE_004_getCategoryById_NotFound() {

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> categoryService.getCategoryById(99999L));

        assertEquals("Category not found", ex.getMessage());
    }

    /*
     * =========================================================
     * TC-IT-CATE-005
     * Hàm kiểm thử: getAllCategories()
     * Kết quả kỳ vọng:
     * - Trả về đúng số lượng category
     * =========================================================
     */
    @Test
    void TC_IT_CATE_005_getAllCategories_ShouldReturnAll() {

        List<Category> result = categoryService.getAllCategories();

        assertEquals(2, result.size());
    }

    /*
     * =========================================================
     * TC-IT-CATE-006
     * Hàm kiểm thử: getAllCategories()
     * Kết quả kỳ vọng:
     * - Trả về list rỗng khi DB không có dữ liệu
     * =========================================================
     */
    @Test
    void TC_IT_CATE_006_getAllCategories_WhenEmpty() {

        categoryRepository.deleteAll();

        List<Category> result = categoryService.getAllCategories();

        assertTrue(result.isEmpty());
    }

    /*
     * =========================================================
     * TC-IT-CATE-007
     * Hàm kiểm thử: updateCategory()
     * Kết quả kỳ vọng:
     * - Update category thành công
     * =========================================================
     */
    @Test
    void TC_IT_CATE_007_updateCategory_ShouldSuccess() {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Updated Electronics")
                .build();

        Category result =
                categoryService.updateCategory(
                        electronicsCategory.getId(),
                        dto
                );

        assertEquals("Updated Electronics", result.getName());
    }

    /*
     * =========================================================
     * TC-IT-CATE-008
     * Hàm kiểm thử: updateCategory()
     * Kết quả kỳ vọng:
     * - Throw exception nếu category không tồn tại
     * =========================================================
     */
    @Test
    void TC_IT_CATE_008_updateCategory_NotFound() {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Updated")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> categoryService.updateCategory(99999L, dto));

        assertEquals("Category not found", ex.getMessage());
    }

    /*
     * =========================================================
     * TC-IT-CATE-009
     * Hàm kiểm thử: deleteCategory()
     * Kết quả kỳ vọng:
     * - Xóa category thành công
     * =========================================================
     */
    @Test
    void TC_IT_CATE_009_deleteCategory_ShouldSuccess() {

        categoryService.deleteCategory(electronicsCategory.getId());

        boolean exists =
                categoryRepository.existsById(
                        electronicsCategory.getId()
                );

        assertFalse(exists);
    }

    /*
     * =========================================================
     * TC-IT-CATE-010
     * Hàm kiểm thử: deleteCategory()
     * Kết quả kỳ vọng:
     * - Không phát sinh exception khi xóa ID không tồn tại
     * =========================================================
     */
    @Test
    void TC_IT_CATE_010_deleteCategory_NotFound() {

        assertDoesNotThrow(
                () -> categoryService.deleteCategory(99999L)
        );
    }

    /*
     * =========================================================
     * TC-IT-CATE-011
     * Hàm kiểm thử: createCategory()
     * Kết quả kỳ vọng:
     * - ID được generate
     * =========================================================
     */
    @Test
    void TC_IT_CATE_011_createCategory_ShouldGenerateId()
            throws Exception {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Laptop")
                .build();

        Category result = categoryService.createCategory(dto);

        assertTrue(result.getId() > 0);
    }

    /*
     * =========================================================
     * TC-IT-CATE-012
     * Hàm kiểm thử: createCategory()
     * Kết quả kỳ vọng:
     * - Persist dữ liệu xuống DB
     * =========================================================
     */
    @Test
    void TC_IT_CATE_012_createCategory_ShouldPersistDatabase()
            throws Exception {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Accessories")
                .build();

        Category result = categoryService.createCategory(dto);

        Category fromDb =
                categoryRepository.findById(result.getId()).orElse(null);

        assertNotNull(fromDb);
    }

    /*
     * =========================================================
     * TC-IT-CATE-013
     * Hàm kiểm thử: getAllCategories()
     * Kết quả kỳ vọng:
     * - Danh sách chứa Electronics
     * =========================================================
     */
    @Test
    void TC_IT_CATE_013_getAllCategories_ShouldContainElectronics() {

        List<Category> result = categoryService.getAllCategories();

        assertTrue(
                result.stream()
                        .anyMatch(c -> c.getName().equals("Electronics"))
        );
    }

    /*
     * =========================================================
     * TC-IT-CATE-014
     * Hàm kiểm thử: updateCategory()
     * Kết quả kỳ vọng:
     * - Dữ liệu DB được update
     * =========================================================
     */
    @Test
    void TC_IT_CATE_014_updateCategory_ShouldUpdateDatabase() {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Gaming")
                .build();

        categoryService.updateCategory(
                electronicsCategory.getId(),
                dto
        );

        Category fromDb =
                categoryRepository.findById(
                        electronicsCategory.getId()
                ).orElse(null);

        assertEquals("Gaming", fromDb.getName());
    }

    /*
     * =========================================================
     * TC-IT-CATE-015
     * Hàm kiểm thử: deleteCategory()
     * Kết quả kỳ vọng:
     * - Tổng số category giảm sau khi xóa
     * =========================================================
     */
    @Test
    void TC_IT_CATE_015_deleteCategory_ShouldReduceCount() {

        long before = categoryRepository.count();

        categoryService.deleteCategory(electronicsCategory.getId());

        long after = categoryRepository.count();

        assertEquals(before - 1, after);
    }

    /*
     * =========================================================
     * TC-IT-CATE-016
     * Hàm kiểm thử: createCategory()
     * Kết quả kỳ vọng:
     * - Tổng số category tăng
     * =========================================================
     */
    @Test
    void TC_IT_CATE_016_createCategory_ShouldIncreaseCount()
            throws Exception {

        long before = categoryRepository.count();

        categoryService.createCategory(
                CategoryDTO.builder()
                        .name("Books")
                        .build()
        );

        long after = categoryRepository.count();

        assertEquals(before + 1, after);
    }

    /*
     * =========================================================
     * TC-IT-CATE-017
     * Hàm kiểm thử: updateCategory()
     * Kết quả kỳ vọng:
     * - ID category không thay đổi sau update
     * =========================================================
     */
    @Test
    void TC_IT_CATE_017_updateCategory_ShouldKeepSameId() {

        Long oldId = electronicsCategory.getId();

        CategoryDTO dto = CategoryDTO.builder()
                .name("Updated")
                .build();

        Category result =
                categoryService.updateCategory(oldId, dto);

        assertEquals(oldId, result.getId());
    }

    /*
     * =========================================================
     * TC-IT-CATE-018
     * Hàm kiểm thử: deleteCategory()
     * Kết quả kỳ vọng:
     * - Xóa toàn bộ category thành công
     * =========================================================
     */
    @Test
    void TC_IT_CATE_018_deleteAllCategories_ShouldSuccess() {

        categoryService.deleteCategory(electronicsCategory.getId());
        categoryService.deleteCategory(fashionCategory.getId());

        assertEquals(0, categoryRepository.count());
    }

    /*
     * =========================================================
     * TC-IT-CATE-019
     * Hàm kiểm thử: createCategory()
     * Kết quả kỳ vọng:
     * - Tạo category chứa ký tự đặc biệt thành công
     * =========================================================
     */
    @Test
    void TC_IT_CATE_019_createCategory_WithSpecialCharacters()
            throws Exception {

        CategoryDTO dto = CategoryDTO.builder()
                .name("Điện tử & Công nghệ")
                .build();

        Category result = categoryService.createCategory(dto);

        assertEquals("Điện tử & Công nghệ", result.getName());
    }

    /*
     * =========================================================
     * TC-IT-CATE-020
     * Hàm kiểm thử: updateCategory()
     * Kết quả kỳ vọng:
     * - Có thể update nhiều lần
     * =========================================================
     */
    @Test
    void TC_IT_CATE_020_updateCategory_MultipleTimes() {

        categoryService.updateCategory(
                electronicsCategory.getId(),
                CategoryDTO.builder()
                        .name("Step 1")
                        .build()
        );

        categoryService.updateCategory(
                electronicsCategory.getId(),
                CategoryDTO.builder()
                        .name("Step 2")
                        .build()
        );

        Category result =
                categoryRepository.findById(
                        electronicsCategory.getId()
                ).orElse(null);

        assertNotNull(result);
        assertEquals("Step 2", result.getName());
    }
}
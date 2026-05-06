package com.example.Sneakers.integration;

import com.example.Sneakers.dtos.ProductDTO;
import com.example.Sneakers.dtos.ProductImageDTO;
import com.example.Sneakers.models.Category;
import com.example.Sneakers.models.Product;
import com.example.Sneakers.models.ProductImage;
import com.example.Sneakers.repositories.CategoryRepository;
import com.example.Sneakers.repositories.OrderDetailRepository;
import com.example.Sneakers.repositories.ProductFeatureRepository;
import com.example.Sneakers.repositories.ProductImageRepository;
import com.example.Sneakers.repositories.ProductRepository;
import com.example.Sneakers.repositories.ReviewRepository;
import com.example.Sneakers.services.ProductService;
import com.example.Sneakers.services.ProductFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp cho ProductService.
 * <p>
 * Kiểm thử: Các thao tác CRUD, tìm kiếm, lọc, khoảng giá, lọc theo danh mục,
 * cập nhật ảnh đại diện, quản lý hình ảnh, quản lý số lượng, bán chạy nhất,
 * thống kê.
 * Sử dụng cơ sở dữ liệu in-memory H2 với các repository thực.
 * Mỗi bài test chạy trong một transaction và tự động rollback.
 * </p>
 */
import com.example.Sneakers.exceptions.InvalidParamException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductFeatureRepository productFeatureRepository;

    @MockBean
    private ProductFeatureService productFeatureService;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private dev.langchain4j.model.chat.ChatModel chatModel;

    private Category testCategory;
    private Product activeProduct;
    private Product outOfStockProduct;

    @BeforeEach
    void setUp() {
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // // Tạo danh mục mẫu để test
        testCategory = Category.builder()
                .name("KHóa Việt Nam")
                .build();
        testCategory = categoryRepository.save(testCategory);

        // Tạo các sản phẩm mẫu để test
        activeProduct = Product.builder()
                .name("Khóa Điện Tử Samsung")
                .price(200000L)
                .thumbnail("thumb1.jpg")
                .description("Khóa cửa vân tay cao cấp")
                .category(testCategory)
                .discount(10L)
                .quantity(50L)
                .inStock(true)
                .productImages(new ArrayList<>())
                .build();
        activeProduct = productRepository.save(activeProduct);

        outOfStockProduct = Product.builder()
                .name("Khóa Thông Minh Xiaomi")
                .price(300000L)
                .thumbnail("thumb2.jpg")
                .description("Khóa cửa wifi kết nối app")
                .category(testCategory)
                .discount(0L)
                .quantity(0L)
                .inStock(false)
                .productImages(new ArrayList<>())
                .build();
        outOfStockProduct = productRepository.save(outOfStockProduct);

        Product product3 = Product.builder()
                .name("Khóa Kính Kaadas")
                .price(150000L)
                .thumbnail("thumb3.jpg")
                .description("Khóa cửa kính cường lực")
                .category(testCategory)
                .discount(5L)
                .quantity(20L)
                .inStock(true)
                .productImages(new ArrayList<>())
                .build();
        productRepository.save(product3);

        Product product4 = Product.builder()
                .name("Khóa Khách Sạn Hafele")
                .price(120000L)
                .thumbnail("thumb4.jpg")
                .description("Hệ thống khóa thẻ từ khách sạn")
                .category(testCategory)
                .discount(0L)
                .quantity(10L)
                .inStock(true)
                .productImages(new ArrayList<>())
                .build();
        productRepository.save(product4);

        Product product5 = Product.builder()
                .name("Khóa Vân Tay Epic")
                .price(180000L)
                .thumbnail("thumb5.jpg")
                .description("Khóa cửa gỗ chống cháy")
                .category(testCategory)
                .discount(15L)
                .quantity(30L)
                .inStock(true)
                .productImages(new ArrayList<>())
                .build();
        productRepository.save(product5);
    }

    // ==================== Test Case ID: TC-IT-PROD-001 ====================
    // Mục tiêu kiểm thử: getProductById - phải trả về sản phẩm theo ID
    // Kết quả mong đợi: Sản phẩm với tên và chi tiết chính xác
    // ====================
    @Test
    void TC_IT_PROD_001_getProductById_ShouldReturnProduct() throws Exception {
        // Thực thi (Act)
        Product result = productService.getProductById(activeProduct.getId());

        // Kiểm tra kết quả (Assert)
        assertNotNull(result);
        assertEquals("Khóa Điện Tử Samsung", result.getName());
        assertEquals(200000L, result.getPrice());
        assertEquals(testCategory.getId(), result.getCategory().getId());
    }

    // ==================== Test Case ID: TC-IT-PROD-002 ====================
    // Mục tiêu kiểm thử: getProductById - phải ném ra ngoại lệ khi không tìm thấy
    // sản phẩm
    // Kết quả mong đợi: Ngoại lệ DataNotFoundException
    // ====================
    @Test
    void TC_IT_PROD_002_getProductById_WhenNotFound_ShouldThrowException() {
        // Thực thi (Act) & Assert
        assertThrows(Exception.class, () -> productService.getProductById(99999L));
    }

    // ==================== Test Case ID: TC-IT-PROD-003 ====================
    // Mục tiêu kiểm thử: getAllProducts - phải trả về tất cả sản phẩm có phân trang
    // Kết quả mong đợi: Trang chứa tất cả sản phẩm
    // ====================
    @Test
    void TC_IT_PROD_003_getAllProducts_ShouldReturnAllProducts() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("", null, null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }

    // ==================== Test Case ID: TC-IT-PROD-004 ====================
    // Mục tiêu kiểm thử: getAllProducts - phải lọc được theo từ khóa
    // Kết quả mong đợi: Chỉ các sản phẩm khớp từ khóa ở tên hoặc mô tả
    // ====================
    @Test
    void TC_IT_PROD_004_getAllProducts_WithKeyword_ShouldFilter() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("Samsung", null, null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(1, result.getTotalElements());
        assertEquals("Khóa Điện Tử Samsung", result.getContent().get(0).getName());
    }

    // ==================== Test Case ID: TC-IT-PROD-005 ====================
    // Mục tiêu kiểm thử: getAllProducts - phải lọc được theo danh mục
    // Kết quả mong đợi: Chỉ các sản phẩm trong danh mục được chỉ định
    // ====================
    @Test
    void TC_IT_PROD_005_getAllProducts_ByCategory_ShouldFilter() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("", testCategory.getId(), null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(5, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-PROD-006 ====================
    // Mục tiêu kiểm thử: getAllProducts - phải lọc được theo khoảng giá
    // Kết quả mong đợi: Chỉ các sản phẩm nằm trong khoảng giá
    // ====================
    @Test
    void TC_IT_PROD_006_getAllProducts_ByPriceRange_ShouldFilter() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("", null, 150000L, 250000L, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(3, result.getTotalElements());
        assertEquals("Khóa Kính Kaadas", result.getContent().get(0).getName());
    }

    // ==================== Test Case ID: TC-IT-PROD-007 ====================
    // Mục tiêu kiểm thử: getAllProducts - phải sắp xếp được theo đánh giá
    // Kết quả mong đợi: Sản phẩm được sắp xếp theo đánh giá trung bình (có xử lý
    // lỗi null)
    // ====================
    @Test
    void TC_IT_PROD_007_getAllProducts_SortByRating_ShouldSort() {
        // Thực thi (Act) - sort by rating (products have no reviews yet, so rating = 0)
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("rating").descending());
        var result = productService.getAllProducts("", null, null, null, pageRequest);

        // Kiểm tra kết quả (Assert) - should return products sorted by inStock status
        // then rating
        assertFalse(result.isEmpty());
        assertEquals(5, result.getContent().size());
        assertEquals(5, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-PROD-008 ====================
    // Mục tiêu kiểm thử: allProducts - phải trả về tất cả sản phẩm kèm hình ảnh
    // Kết quả mong đợi: Danh sách sản phẩm có mảng productImages được khởi tạo
    // ====================
    @Test
    void TC_IT_PROD_008_allProducts_ShouldReturnAllWithImages() {
        // Thực thi (Act)
        List<Product> result = productService.allProducts();

        // Kiểm tra kết quả (Assert)
        assertEquals(5, result.size());
        // Hình ảnh phải được lấy kèm (danh sách không null, có thể rỗng)
        assertNotNull(result.get(0).getProductImages());
    }

    // ==================== Test Case ID: TC-IT-PROD-009 ====================
    // Mục tiêu kiểm thử: createProduct - phải lưu được sản phẩm mới vào CSDL
    // Kết quả mong đợi: Sản phẩm được lưu với ID và thời gian tự động tạo
    // ====================
    @Test
    void TC_IT_PROD_009_createProduct_ShouldPersistToDatabase() throws Exception {
        // Chuẩn bị dữ liệu (Arrange)
        ProductDTO newProductDTO = ProductDTO.builder()
                .name("New Balance 574")
                .price(150000L)
                .thumbnail("new_thumb.jpg")
                .description("Classic sneakers")
                .categoryId(testCategory.getId())
                .discount(5L)
                .quantity(30L)
                .build();

        // Thực thi (Act)
        Product result = productService.createProduct(newProductDTO);

        // Kiểm tra kết quả (Assert) - return value
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("New Balance 574", result.getName());
        assertEquals(150000L, result.getPrice());

        // ✅ Kiểm tra DB: Xác nhận đã lưu
        Product fromDb = productRepository.findById(result.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals("New Balance 574", fromDb.getName());
        assertNotNull(fromDb.getCreatedAt());
    }

    // ==================== Test Case ID: TC-IT-PROD-009B ====================
    // Mục tiêu kiểm thử: createProduct - Backend CÓ CHẶN tạo sản phẩm trùng tên
    // không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ (Exception) khi cố tình tạo sản phẩm
    // trùng tên
    // ====================
    @Test
    void TC_IT_PROD_009B_createProduct_WithDuplicateName_ShouldThrowException() {
        ProductDTO duplicateDTO = ProductDTO.builder()
                .name("Khóa Điện Tử Samsung") // Tên đã tồn tại
                .price(100000L)
                .categoryId(testCategory.getId())
                .description("Mô tả hợp lệ") // ← Thêm field này
                .thumbnail("thumbnail.jpg") // ← Optional nhưng nên có
                .build();

        assertThrows(Exception.class, () -> productService.createProduct(duplicateDTO),
                "Backend đang bị lỗi: Cho phép tạo 2 sản phẩm trùng tên!");
    }

    // ==================== Test Case ID: TC-IT-PROD-009C ====================
    // Mục tiêu kiểm thử: createProduct - Backend CÓ KIỂM TRA danh mục tồn tại
    // không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ khi Category ID không tồn tại
    // ====================
    @Test
    void TC_IT_PROD_009C_createProduct_WithInvalidCategory_ShouldThrowException() {
        ProductDTO invalidCatDTO = ProductDTO.builder()
                .name("Sản phẩm lỗi danh mục")
                .price(100000L)
                .description("Mô tả sản phẩm hợp lệ") // Cần thiết: description NOT NULL
                .categoryId(999999L) // Danh mục ảo không tồn tại
                .build();
        ;
        // Thực thi & Kiểm tra
        assertThrows(Exception.class, () -> productService.createProduct(invalidCatDTO),
                "Backend đang bị lỗi: Không validate kiểm tra Category ID có tồn tại hay không trước khi lưu!");

    }

    // ==================== Test Case ID: TC-IT-PROD-009D ====================
    // Mục tiêu kiểm thử: createProduct - Backend CÓ CHẶN giá ÂM không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ InvalidParamException khi giá < 0
    // ====================
    @Test
    void TC_IT_PROD_009D_createProduct_WithNegativePrice_ShouldThrowException() {
        ProductDTO invalidPriceDTO = ProductDTO.builder()
                .name("Sản phẩm giá âm")
                .price(-50000L) // Cố tình truyền Giá âm
                .quantity(10L) // Số lượng hợp lệ
                .description("Mô tả sản phẩm hợp lệ") // Cần thiết: description NOT NULL
                .categoryId(testCategory.getId())
                .build();

        // Thực thi & Kiểm tra
        InvalidParamException exception = assertThrows(InvalidParamException.class, () -> {
            productService.createProduct(invalidPriceDTO);
        }, "Backend đang bị lỗi: Chấp nhận lưu sản phẩm với Giá bị ÂM!");

        assertTrue(exception.getMessage().toLowerCase().contains("giá"),
                "Lỗi ném ra không đúng trọng tâm (Kỳ vọng nhắc đến 'giá')");
    }

    // ==================== Test Case ID: TC-IT-PROD-009E ====================
    // Mục tiêu kiểm thử: createProduct - Backend CÓ CHẶN số lượng ÂM không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ InvalidParamException khi số lượng < 0
    // ====================
    @Test
    void TC_IT_PROD_009E_createProduct_WithNegativeQuantity_ShouldThrowException() {
        ProductDTO invalidQuantityDTO = ProductDTO.builder()
                .name("Sản phẩm số lượng âm")
                .price(150000L) // Giá hợp lệ
                .quantity(-10L) // Cố tình truyền Số lượng âm
                .description("Mô tả sản phẩm hợp lệ") // Cần thiết: description NOT NULL
                .categoryId(testCategory.getId())
                .build();

        // Thực thi & Kiểm tra
        InvalidParamException exception = assertThrows(InvalidParamException.class, () -> {
            productService.createProduct(invalidQuantityDTO);
        }, "Backend đang bị lỗi: Chấp nhận lưu sản phẩm với Số lượng bị ÂM!");

        assertTrue(exception.getMessage().toLowerCase().contains("số lượng"),
                "Lỗi ném ra không đúng trọng tâm (Kỳ vọng nhắc đến 'số lượng')");
    }

    // ==================== Test Case ID: TC-IT-PROD-009F ====================
    // Mục tiêu kiểm thử: createProduct - Backend CÓ CHẶN phần trăm giảm giá vô lý
    // không?
    // Kết quả mong đợi: Ném lỗi khi discount < 0 hoặc discount > 100
    // ====================
    @Test
    void TC_IT_PROD_009F_createProduct_WithInvalidDiscount_ShouldThrowException() {
        ProductDTO invalidDiscountDTO = ProductDTO.builder()
                .name("Sản phẩm discount ảo")
                .price(100000L)
                .quantity(10L)
                .discount(-10L) // Giảm giá 150% -> Bán lỗ
                .categoryId(testCategory.getId())
                .description("description not null")
                .build();

        // Thực thi & Kiểm tra
        InvalidParamException exception = assertThrows(InvalidParamException.class, () -> {
            productService.createProduct(invalidDiscountDTO);
        }, "Backend đang bị lỗi: Chấp nhận tạo sản phẩm mới với discount > 100% hoặc < 0%!");

        // Ép buộc Message lỗi phải chứa từ khóa chuẩn xác
        assertTrue(
                exception.getMessage().toLowerCase().contains("giảm giá")
                        || exception.getMessage().toLowerCase().contains("discount"),
                "Lỗi ném ra không đúng trọng tâm (Kỳ vọng nhắc đến 'giảm giá' hoặc 'discount')");
    }

    // ==================== Test Case ID: TC-IT-PROD-010 ====================
    // Mục tiêu kiểm thử: updateProduct - phải cập nhật được sản phẩm có sẵn
    // Kết quả mong đợi: Các thay đổi được lưu vào cơ sở dữ liệu
    // ====================
    @Test
    void TC_IT_PROD_010_updateProduct_ShouldUpdateInDatabase() throws Exception {
        // Chuẩn bị dữ liệu (Arrange)
        ProductDTO updateDTO = ProductDTO.builder()
                .name("Updated Nike Air Max")
                .price(220000L)
                .description("Updated description")
                .categoryId(testCategory.getId())
                .discount(15L)
                .quantity(60L)
                .build();

        // Thực thi (Act)
        Product result = productService.updateProduct(activeProduct.getId(), updateDTO);

        // Kiểm tra kết quả (Assert) - return value
        assertEquals("Updated Nike Air Max", result.getName());
        assertEquals(220000L, result.getPrice());

        // ✅ Kiểm tra trong CSDL (DB Check)
        Product fromDb = productRepository.findById(activeProduct.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals("Updated Nike Air Max", fromDb.getName());
        assertEquals(220000L, fromDb.getPrice());
        assertEquals("Updated description", fromDb.getDescription());
    }

    // ==================== Test Case ID: TC-IT-PROD-010B ====================
    // Mục tiêu kiểm thử: updateProduct - Backend CÓ CHẶN đổi tên trùng với SẢN PHẨM
    // KHÁC không?
    // Kết quả mong đợi: Ném lỗi khi cố tình đổi tên "Khóa Điện Tử Samsung" thành
    // "Khóa Thông Minh Xiaomi"
    // ====================
    @Test
    void TC_IT_PROD_010B_updateProduct_WithDuplicateNameOfAnotherProduct_ShouldThrowException() {
        ProductDTO updateDTO = ProductDTO.builder()
                .name("Khóa Thông Minh Xiaomi") // Tên của outOfStockProduct đã có sẵn
                .price(220000L)
                .description("Mô tả sản phẩm hợp lệ") // Cần thiết: description NOT NULL
                .categoryId(testCategory.getId())
                .build();

        // Thực thi & Kiểm tra
        InvalidParamException exception = assertThrows(InvalidParamException.class, () -> {
            productService.updateProduct(activeProduct.getId(), updateDTO);
        }, "Backend đang bị lỗi: Cho phép sửa tên sản phẩm trùng với một sản phẩm khác đang tồn tại!");

        assertTrue(
                exception.getMessage().toLowerCase().contains("tồn tại")
                        || exception.getMessage().toLowerCase().contains("trùng"),
                "Lỗi ném ra không đúng trọng tâm (Kỳ vọng báo 'tồn tại' hoặc 'trùng')");
    }

    // ==================== Test Case ID: TC-IT-PROD-010E ====================
    // Mục tiêu kiểm thử: updateProduct - Backend CÓ CHẶN số lượng ÂM khi
    // addQuantity = false?
    // Kết quả mong đợi: Ném lỗi khi cố tình set quantity = -50
    // ====================
    @Test
    void TC_IT_PROD_010E_updateProduct_ReplaceWithNegativeQuantity_ShouldThrowException() {
        ProductDTO updateDTO = ProductDTO.builder()
                .name("Khóa Điện Tử Samsung - Update")
                .price(200000L)
                .description("Mô tả sản phẩm hợp lệ") // Cần thiết: description NOT NULL
                .categoryId(testCategory.getId())
                .addQuantity(false) // Ghi đè số lượng
                .quantity(-50L) // Cố tình truyền số âm
                .build();

        // Thực thi & Kiểm tra (Bắt chính xác loại Exception và nội dung)
        InvalidParamException exception = assertThrows(InvalidParamException.class, () -> {
            productService.updateProduct(activeProduct.getId(), updateDTO);
        }, "Backend đang bị lỗi: Không hề ném ra InvalidParamException khi số lượng âm!");

        // Ép buộc Message lỗi phải chứa từ khóa chuẩn xác
        assertTrue(exception.getMessage().toLowerCase().contains("số lượng"),
                "Lỗi ném ra không đúng trọng tâm (Kỳ vọng nhắc đến 'số lượng')");
    }

    // ==================== Test Case ID: TC-IT-PROD-010F ====================
    // Mục tiêu kiểm thử: updateProduct - Backend CÓ CHẶN update discount vô lý
    // không?
    // Kết quả mong đợi: Ném lỗi khi discount < 0 hoặc discount > 100
    // ====================
    @Test
    void TC_IT_PROD_010F_updateProduct_WithInvalidDiscount_ShouldThrowException() {
        ProductDTO updateDTO = ProductDTO.builder()
                .name("Khóa Điện Tử Samsung - Update")
                .price(200000L)
                .description("Mô tả sản phẩm hợp lệ") // Cần thiết: description NOT NULL
                .categoryId(testCategory.getId())
                .discount(-15L) // Cố tình truyền discount vô lý
                .build();

        // Thực thi & Kiểm tra
        InvalidParamException exception = assertThrows(InvalidParamException.class, () -> {
            productService.updateProduct(activeProduct.getId(), updateDTO);
        }, "Backend đang bị lỗi: Không hề ném ra InvalidParamException khi discount âm!");

        // Ép buộc Message lỗi phải chứa từ khóa chuẩn xác
        assertTrue(
                exception.getMessage().toLowerCase().contains("giảm giá")
                        || exception.getMessage().toLowerCase().contains("discount"),
                "Lỗi ném ra không đúng trọng tâm (Kỳ vọng nhắc đến 'giảm giá' hoặc 'discount')");
    }

    // ==================== Test Case ID: TC-IT-PROD-010D ====================
    // Mục tiêu kiểm thử: updateProduct - Backend CÓ CHẶN update Giá thành số ÂM
    // không?
    // Kết quả mong đợi: Ném lỗi khi update price < 0
    // ====================
    @Test
    void TC_IT_PROD_010D_updateProduct_WithNegativePrice_ShouldThrowException() {
        ProductDTO updateDTO = ProductDTO.builder()
                .name("Tên hợp lệ 2")
                .price(-999000L) // Cố tình truyền Giá âm
                .categoryId(testCategory.getId())
                .description("Test lỗi giá âm")
                .build();

        // Thực thi & Kiểm tra (Bắt chính xác loại Exception và nội dung)
        InvalidParamException exception = assertThrows(InvalidParamException.class, () -> {
            productService.updateProduct(activeProduct.getId(), updateDTO);
        }, "Backend đang bị lỗi: Không hề ném ra InvalidParamException khi giá âm!");

        // Ép buộc Message lỗi phải chứa từ khóa chuẩn xác
        assertTrue(exception.getMessage().toLowerCase().contains("giá"),
                "Lỗi ném ra không đúng trọng tâm (Kỳ vọng nhắc đến 'giá')");
    }

    // ==================== Test Case ID: TC-IT-PROD-011 ====================
    // Mục tiêu kiểm thử: deleteProduct - phải xóa được sản phẩm khỏi cơ sở dữ liệu
    // Kết quả mong đợi: Sản phẩm không còn tồn tại
    // ====================
    @Test
    void TC_IT_PROD_011_deleteProduct_ShouldRemoveFromDatabase() {
        // Thực thi (Act)
        productService.deleteProduct(activeProduct.getId());

        // ✅ Kiểm tra trong CSDL (DB Check)
        assertTrue(productRepository.findById(activeProduct.getId()).isEmpty());
    }

    // ==================== Test Case ID: TC-IT-PROD-012 ====================
    // Mục tiêu kiểm thử: existsByName - phải trả về true nếu tên đã tồn tại
    // Kết quả mong đợi: true
    // ====================
    @Test
    void TC_IT_PROD_012_existsByName_WithExistingName_ShouldReturnTrue() {
        // Thực thi (Act)
        boolean result = productService.existsByName("Khóa Điện Tử Samsung");

        // Kiểm tra kết quả (Assert)
        assertTrue(result);
    }

    // ==================== Test Case ID: TC-IT-PROD-013 ====================
    // Mục tiêu kiểm thử: existsByName - phải trả về false nếu tên chưa tồn tại
    // Kết quả mong đợi: false
    // ====================
    @Test
    void TC_IT_PROD_013_existsByName_WithNonExistingName_ShouldReturnFalse() {
        // Thực thi (Act)
        boolean result = productService.existsByName("Non Existent Product");

        // Kiểm tra kết quả (Assert)
        assertFalse(result);
    }

    // ==================== Test Case ID: TC-IT-PROD-014 ====================
    // Mục tiêu kiểm thử: totalProducts - phải trả về tổng số lượng
    // Kết quả mong đợi: Đếm chính xác tổng số sản phẩm
    // ====================
    @Test
    void TC_IT_PROD_014_totalProducts_ShouldReturnCount() {
        // Thực thi (Act)
        long result = productService.totalProducts();

        // Kiểm tra kết quả (Assert)
        assertEquals(5, result);
    }

    // ==================== Test Case ID: TC-IT-PROD-015 ====================
    // Mục tiêu kiểm thử: findProductsByIds - phải lấy được nhiều sản phẩm theo danh
    // sách ID
    // Kết quả mong đợi: Danh sách các sản phẩm khớp với các ID
    // ====================
    @Test
    void TC_IT_PROD_015_findProductsByIds_ShouldReturnProducts() {
        // Chuẩn bị dữ liệu (Arrange)
        List<Long> ids = List.of(activeProduct.getId(), outOfStockProduct.getId());

        // Thực thi (Act)
        List<Product> result = productService.findProductsByIds(ids);

        // Kiểm tra kết quả (Assert)
        assertEquals(2, result.size());
    }

    // ==================== Test Case ID: TC-IT-PROD-016 ====================
    // Mục tiêu kiểm thử: getProductsByPrice - phải trả về sản phẩm trong khoảng giá
    // Kết quả mong đợi: ListProductResponse chứa sản phẩm trong khoảng min/max
    // price
    // ====================
    @Test
    void TC_IT_PROD_016_getProductsByPrice_ShouldFilterByPrice() {
        // Thực thi (Act)
        var result = productService.getProductsByPrice(150000L, 250000L);

        // Kiểm tra kết quả (Assert)
        assertEquals(3, result.getTotalProducts());
        assertEquals("Khóa Điện Tử Samsung", result.getProducts().get(0).getName());
    }

    // ==================== Test Case ID: TC-IT-PROD-017 ====================
    // Mục tiêu kiểm thử: getProductsByKeyword - phải tìm được sản phẩm theo từ khóa
    // Kết quả mong đợi: Các sản phẩm chứa từ khóa trong tên hoặc mô tả
    // ====================
    @Test
    void TC_IT_PROD_017_getProductsByKeyword_ShouldMatchKeyword() {
        // Thực thi (Act)
        var result = productService.getProductsByKeyword("cửa");

        // Kiểm tra kết quả (Assert) - both products have "cửa" in description
        assertEquals(4, result.getTotalProducts());
        assertEquals("Khóa Điện Tử Samsung", result.getProducts().get(0).getName());
    }

    // ==================== Test Case ID: TC-IT-PROD-018 ====================
    // Mục tiêu kiểm thử: getProductsByCategory - phải trả về sản phẩm thuộc danh
    // mục
    // Kết quả mong đợi: Tất cả sản phẩm thuộc danh mục được chỉ định
    // ====================
    @Test
    void TC_IT_PROD_018_getProductsByCategory_ShouldReturnCategoryProducts() {
        // Thực thi (Act)
        var result = productService.getProductsByCategory(testCategory.getId());

        // Kiểm tra kết quả (Assert)
        assertEquals(5, result.getTotalProducts());
    }

    // ==================== Test Case ID: TC-IT-PROD-019 ====================
    // Mục tiêu kiểm thử: getRelatedProducts - phải trả về các sản phẩm cùng danh
    // mục
    // Kết quả mong đợi: Tối đa 4 sản phẩm khác cùng danh mục, không bao gồm sản
    // phẩm hiện tại
    // ====================
    @Test
    void TC_IT_PROD_019_getRelatedProducts_ShouldReturnSameCategoryProducts() throws Exception {
        // Thực thi (Act)
        var result = productService.getRelatedProducts(activeProduct.getId());

        // Kiểm tra kết quả (Assert)
        assertEquals(4, result.getTotalProducts()); // Giới hạn hàm getRelatedProducts là 4 sản phẩm
        assertNotEquals(activeProduct.getId(), result.getProducts().get(0).getId());
    }

    // ==================== Test Case ID: TC-IT-PROD-020 ====================
    // Mục tiêu kiểm thử: getRelatedProducts - phải báo lỗi nếu sản phẩm không tồn
    // tại
    // Kết quả mong đợi: Ngoại lệ (Exception)
    // ====================
    @Test
    void TC_IT_PROD_020_getRelatedProducts_NonExistent_ShouldThrowException() {
        // Thực thi (Act) & Assert
        assertThrows(Exception.class, () -> productService.getRelatedProducts(99999L));
    }

    // ==================== Test Case ID: TC-IT-PROD-021 ====================
    // Mục tiêu kiểm thử: updateProductThumbnail - phải cập nhật được link ảnh đại
    // diện
    // Kết quả mong đợi: Thumbnail được cập nhật trong database
    // ====================
    @Test
    void TC_IT_PROD_021_updateProductThumbnail_ShouldPersistChange() throws Exception {
        // Chuẩn bị dữ liệu (Arrange)
        String newThumbnail = "updated_thumb.jpg";

        // Thực thi (Act)
        productService.updateProductThumbnail(activeProduct.getId(), newThumbnail);

        // Kiểm tra kết quả (Assert)
        Product fromDb = productRepository.findById(activeProduct.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(newThumbnail, fromDb.getThumbnail());
    }

    // ==================== Test Case ID: TC-IT-PROD-022 ====================
    // Mục tiêu kiểm thử: Tính toán inStock - phải là true khi số lượng > 0
    // Kết quả mong đợi: inStock = true cho các sản phẩm có số lượng > 0
    // ====================
    @Test
    void TC_IT_PROD_022_inStock_WhenQuantityPositive_ShouldBeTrue() {
        // Thực thi (Act)
        Product fromDb = productRepository.findById(activeProduct.getId()).orElse(null);

        // Kiểm tra kết quả (Assert)
        assertNotNull(fromDb);
        assertTrue(fromDb.getInStock());
    }

    // ==================== Test Case ID: TC-IT-PROD-023 ====================
    // Mục tiêu kiểm thử: Tính toán inStock - phải là false khi số lượng = 0
    // Kết quả mong đợi: inStock = false cho các sản phẩm hết hàng
    // ====================
    @Test
    void TC_IT_PROD_023_inStock_WhenQuantityZero_ShouldBeFalse() {
        // Thực thi (Act)
        Product fromDb = productRepository.findById(outOfStockProduct.getId()).orElse(null);

        // Kiểm tra kết quả (Assert)
        assertNotNull(fromDb);
        assertFalse(fromDb.getInStock());
    }

    // ==================== Test Case ID: TC-IT-PROD-024 ====================
    // Mục tiêu kiểm thử: getProductsByPrice với kết quả rỗng - phải trả về rỗng
    // Kết quả mong đợi: totalProducts = 0
    // ====================
    @Test
    void TC_IT_PROD_024_getProductsByPrice_NoMatch_ShouldReturnEmpty() {
        // Thực thi (Act)
        var result = productService.getProductsByPrice(500000L, 600000L);

        // Kiểm tra kết quả (Assert)
        assertEquals(0, result.getTotalProducts());
    }

    // ==================== Test Case ID: TC-IT-PROD-025 ====================
    // Mục tiêu kiểm thử: getProductsByKeyword không khớp - phải trả về rỗng
    // Kết quả mong đợi: totalProducts = 0
    // ====================
    @Test
    void TC_IT_PROD_025_getProductsByKeyword_NoMatch_ShouldReturnEmpty() {
        // Thực thi (Act)
        var result = productService.getProductsByKeyword("NonexistentKeyword");

        // Kiểm tra kết quả (Assert)
        assertEquals(0, result.getTotalProducts());
    }

    // ==================== Test Case ID: TC-IT-PROD-026 ====================
    // Mục tiêu kiểm thử: getProductsByCategory với kết quả rỗng - phải trả về rỗng
    // Kết quả mong đợi: totalProducts = 0
    // ====================
    @Test
    void TC_IT_PROD_026_getProductsByCategory_NonExistentCategory_ShouldReturnEmpty() {
        // Thực thi (Act)
        var result = productService.getProductsByCategory(99999L);

        // Kiểm tra kết quả (Assert)
        assertEquals(0, result.getTotalProducts());
    }

    // ==================== Test Case ID: TC-IT-PROD-027 ====================
    // Mục tiêu kiểm thử: sumTotalQuantity - phải cộng tổng số lượng tất cả sản phẩm
    // Kết quả mong đợi: Tổng số lượng tồn kho của tất cả sản phẩm
    // ====================
    @Test
    void TC_IT_PROD_027_sumTotalQuantity_ShouldSumAllQuantities() {
        // Thực thi (Act)
        Long total = productRepository.sumTotalQuantity();

        // Kiểm tra kết quả (Assert)
        assertNotNull(total);
        assertEquals(110L, total); // 50 + 0 + 20 + 10 + 30 = 110
    }

    // ==================== Test Case ID: TC-IT-PROD-028 ====================
    // Mục tiêu kiểm thử: findTopProductsByStock - phải trả về sản phẩm sắp xếp theo
    // số lượng
    // Kết quả mong đợi: Sản phẩm sắp xếp giảm dần theo số lượng
    // ====================
    @Test
    void TC_IT_PROD_028_findTopProductsByStock_ShouldReturnSorted() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Object[]> result = productRepository.findTopProductsByStock(pageRequest);

        // Kiểm tra kết quả (Assert)
        assertFalse(result.isEmpty());
        // Sản phẩm đầu tiên phải có số lượng cao nhất (50)
        Long firstProductId = (Long) result.get(0)[0];
        assertEquals(activeProduct.getId(), firstProductId);
    }

    // ==================== Test Case ID: TC-IT-PROD-029 ====================
    // Mục tiêu kiểm thử: getAllProducts chỉ có giá min - phải lọc chính xác
    // Kết quả mong đợi: Sản phẩm có giá >= minPrice
    // ====================
    @Test
    void TC_IT_PROD_029_getAllProducts_MinPriceOnly_ShouldFilter() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("", null, 250000L, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(1, result.getTotalElements());
        assertEquals("Khóa Thông Minh Xiaomi", result.getContent().get(0).getName());
    }

    // ==================== Test Case ID: TC-IT-PROD-030 ====================
    // Mục tiêu kiểm thử: getAllProducts chỉ có giá max - phải lọc chính xác
    // Kết quả mong đợi: Sản phẩm có giá <= maxPrice
    // ====================
    @Test
    void TC_IT_PROD_030_getAllProducts_MaxPriceOnly_ShouldFilter() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("", null, null, 250000L, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(4, result.getTotalElements());
        assertEquals("Khóa Điện Tử Samsung", result.getContent().get(0).getName());
    }

    // ==================== Test Case ID: TC-IT-PROD-031 ====================
    // Mục tiêu kiểm thử: getAllProducts không có kết quả khớp - phải trả về trang
    // rỗng
    // Kết quả mong đợi: Kết quả rỗng
    // ====================
    @Test
    void TC_IT_PROD_031_getAllProducts_NoMatch_ShouldReturnEmpty() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("NonExistent", null, null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(0, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-PROD-032 ====================
    // Mục tiêu kiểm thử: getRelatedProducts khi sản phẩm không có danh mục - phải
    // trả về rỗng
    // Kết quả mong đợi: Danh sách rỗng
    // ====================
    @Test
    void TC_IT_PROD_032_getRelatedProducts_NoCategory_ShouldReturnEmpty() throws Exception {
        // Chuẩn bị dữ liệu (Arrange) - create product without category
        Product noCategoryProduct = Product.builder()
                .name("No Category Product")
                .price(100000L)
                .description("Test")
                .quantity(5L)
                .build();
        noCategoryProduct = productRepository.save(noCategoryProduct);

        // Thực thi (Act)
        var result = productService.getRelatedProducts(noCategoryProduct.getId());

        // Kiểm tra kết quả (Assert)
        assertEquals(0, result.getTotalProducts());
    }

    // ==================== Test Case ID: TC-IT-PROD-033 ====================
    // Mục tiêu kiểm thử: Sản phẩm có giảm giá - phải lưu đúng giá trị giảm giá
    // Kết quả mong đợi: Trường giảm giá khớp với giá trị đã cài đặt
    // ====================
    @Test
    void TC_IT_PROD_033_productWithDiscount_ShouldHaveCorrectDiscount() {
        // Thực thi (Act)
        Product fromDb = productRepository.findById(activeProduct.getId()).orElse(null);

        // Kiểm tra kết quả (Assert)
        assertNotNull(fromDb);
        assertEquals(10L, fromDb.getDiscount());
    }

    // ==================== Test Case ID: TC-IT-PROD-034 ====================
    // Mục tiêu kiểm thử: Sản phẩm không có giảm giá - trường discount phải bằng 0
    // Kết quả mong đợi: Giảm giá là 0
    // ====================
    @Test
    void TC_IT_PROD_034_productWithoutDiscount_ShouldBeZero() {
        // Thực thi (Act)
        Product fromDb = productRepository.findById(outOfStockProduct.getId()).orElse(null);

        // Kiểm tra kết quả (Assert)
        assertNotNull(fromDb);
        assertEquals(0L, fromDb.getDiscount());
    }

    // ==================== Test Case ID: TC-IT-PROD-035 ====================
    // Mục tiêu kiểm thử: createProduct - phải liên kết đúng danh mục
    // Kết quả mong đợi: Sản phẩm đã lưu có đúng ID và tên danh mục
    // ====================
    @Test
    void TC_IT_PROD_035_createProduct_ShouldIncludeCategory() throws Exception {
        // Chuẩn bị dữ liệu (Arrange)
        ProductDTO newProductDTO = ProductDTO.builder()
                .name("Puma RS-X")
                .price(180000L)
                .thumbnail("puma.jpg")
                .description("Retro sports shoes")
                .categoryId(testCategory.getId())
                .discount(8L)
                .quantity(25L)
                .build();

        // Thực thi (Act)
        Product saved = productService.createProduct(newProductDTO);

        // Kiểm tra kết quả (Assert)
        assertNotNull(saved.getCategory());
        assertEquals(testCategory.getId(), saved.getCategory().getId());
        assertEquals("KHóa Việt Nam", saved.getCategory().getName());
    }

    // ==================== Test Case ID: TC-IT-PROD-036 ====================
    // Mục tiêu kiểm thử: searchProducts với categoryId=0 - phải bỏ qua bộ lọc danh
    // mục
    // Kết quả mong đợi: Tất cả sản phẩm bất kể thuộc danh mục nào
    // ====================
    @Test
    void TC_IT_PROD_036_searchProducts_CategoryIdZero_ShouldIgnoreFilter() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productRepository.searchProducts(0L, null, null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(5, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-PROD-037 ====================
    // Mục tiêu kiểm thử: Mô tả sản phẩm - phải lưu và lấy ra chính xác
    // Kết quả mong đợi: Mô tả khớp với đầu vào
    // ====================
    @Test
    void TC_IT_PROD_037_productDescription_ShouldStoreCorrectly() {
        // Thực thi (Act)
        Product fromDb = productRepository.findById(activeProduct.getId()).orElse(null);

        // Kiểm tra kết quả (Assert)
        assertNotNull(fromDb);
        assertEquals("Khóa cửa vân tay cao cấp", fromDb.getDescription());
    }

    // ==================== Test Case ID: TC-IT-PROD-038 ====================
    // Mục tiêu kiểm thử: Ảnh đại diện sản phẩm - phải cập nhật và lưu trữ được
    // Kết quả mong đợi: Link Thumbnail được cập nhật chính xác
    // ====================
    @Test
    void TC_IT_PROD_038_updateProductThumbnail_ShouldPersistChange() throws Exception {
        // Chuẩn bị dữ liệu (Arrange)
        String newThumbnail = "updated_thumb.jpg";

        // Thực thi (Act)
        productService.updateProductThumbnail(activeProduct.getId(), newThumbnail);

        // Kiểm tra kết quả (Assert)
        Product fromDb = productRepository.findById(activeProduct.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(newThumbnail, fromDb.getThumbnail());
    }

    // ==================== Test Case ID: TC-IT-PROD-039 ====================
    // Mục tiêu kiểm thử: getAllProducts với categoryId=0 - phải trả về tất cả
    // Kết quả mong đợi: Tất cả sản phẩm khi categoryId là 0
    // ====================
    @Test
    void TC_IT_PROD_039_getAllProducts_CategoryIdZero_ShouldReturnAll() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        var result = productService.getAllProducts("", 0L, null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(5, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-PROD-040 ====================
    // Mục tiêu kiểm thử: findTopProductsByStock với bảng rỗng - phải trả về rỗng
    // Kết quả mong đợi: Danh sách rỗng
    // ====================
    @Test
    void TC_IT_PROD_040_findTopProductsByStock_WhenNoProducts_ShouldReturnEmpty() {
        // Chuẩn bị dữ liệu (Arrange)
        productRepository.deleteAll();

        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Object[]> result = productRepository.findTopProductsByStock(pageRequest);

        // Kiểm tra kết quả (Assert)
        assertTrue(result.isEmpty());
    }

    // ==================== Test Case ID: TC-IT-PROD-041 ====================
    // Mục tiêu kiểm thử: getAllProducts có phân trang - phải giới hạn đúng kích
    // thước trang
    // Kết quả mong đợi: Trang tuân thủ giới hạn kích thước
    // ====================
    @Test
    void TC_IT_PROD_041_getAllProducts_Pagination_ShouldReturnCorrectSize() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(0, 1);
        var result = productService.getAllProducts("", null, null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(1, result.getContent().size());
        assertEquals(5L, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-PROD-042 ====================
    // Mục tiêu kiểm thử: getAllProducts phân trang ở trang 2 - phải trả về trang
    // thứ hai
    // Kết quả mong đợi: Trang thứ hai chứa các sản phẩm còn lại
    // ====================
    @Test
    void TC_IT_PROD_042_getAllProducts_PaginationPage2_ShouldReturnSecondPage() {
        // Thực thi (Act)
        PageRequest pageRequest = PageRequest.of(1, 1);
        var result = productService.getAllProducts("", null, null, null, pageRequest);

        // Kiểm tra kết quả (Assert)
        assertEquals(1, result.getContent().size());
        assertEquals(5L, result.getTotalElements());
    }

    // ==================== Test Case ID: TC-IT-PROD-043 ====================
    // Mục tiêu kiểm thử: getDetailProduct - phải lấy được sản phẩm kèm danh sách
    // ảnh
    // Kết quả mong đợi: Sản phẩm đã khởi tạo danh sách ảnh (productImages)
    // ====================
    @Test
    void TC_IT_PROD_043_getDetailProduct_ShouldFetchWithImages() {
        // Thực thi (Act)
        Optional<Product> result = productRepository.getDetailProduct(activeProduct.getId());

        // Kiểm tra kết quả (Assert)
        assertTrue(result.isPresent());
        assertNotNull(result.get().getProductImages());
    }

    // ==================== Test Case ID: TC-IT-PROD-044 ====================
    // Mục tiêu kiểm thử: findByIdWithImages - phải lấy được một sản phẩm kèm ảnh
    // Kết quả mong đợi: Sản phẩm với hình ảnh đã được tải
    // ====================
    @Test
    void TC_IT_PROD_044_findByIdWithImages_ShouldFetchWithImages() {
        // Thực thi (Act)
        Optional<Product> result = productRepository.findByIdWithImages(activeProduct.getId());

        // Kiểm tra kết quả (Assert)
        assertTrue(result.isPresent());
        assertNotNull(result.get().getProductImages());
    }

    // ==================== Test Case ID: TC-IT-PROD-045 ====================
    // Mục tiêu kiểm thử: createProductImage - phải thêm được ảnh vào sản phẩm
    // Kết quả mong đợi: ProductImage được lưu với liên kết sản phẩm chính xác
    // ====================
    @Test
    void TC_IT_PROD_045_createProductImage_ShouldAddImage() throws Exception {
        // Thực thi (Act)
        ProductImage newImage = ProductImage.builder()
                .product(activeProduct)
                .imageUrl("additional_image.jpg")
                .build();
        ProductImage savedImage = productImageRepository.save(newImage);

        // Kiểm tra kết quả (Assert)
        assertNotNull(savedImage);
        assertNotNull(savedImage.getId());
        assertEquals(activeProduct.getId(), savedImage.getProduct().getId());

        // ✅ Kiểm tra trong CSDL (DB Check)
        ProductImage fromDb = productImageRepository.findById(savedImage.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals("additional_image.jpg", fromDb.getImageUrl());
    }

    // ==================== Test Case ID: TC-IT-PROD-045B ====================
    // Mục tiêu kiểm thử: createProductImage - Backend CÓ CHẶN lưu ảnh với đường dẫn
    // rỗng/null không?
    // Kết quả mong đợi: Phải ném ra ngoại lệ nếu imageUrl bị trống
    // ====================
    @Test
    void TC_IT_PROD_045B_createProductImage_WithEmptyUrl_ShouldThrowException() {
        ProductImageDTO emptyUrlDTO = ProductImageDTO.builder()
                .productId(activeProduct.getId())
                .imageUrl("") // Đường dẫn rỗng
                .build();

        // Thực thi & Kiểm tra
        assertThrows(Exception.class, () -> productService.createProductImage(activeProduct.getId(), emptyUrlDTO),
                "Backend đang bị lỗi: Cho phép thêm mới Ảnh Sản Phẩm mà KHÔNG CÓ đường dẫn ảnh (imageUrl rỗng)!");
    }
}

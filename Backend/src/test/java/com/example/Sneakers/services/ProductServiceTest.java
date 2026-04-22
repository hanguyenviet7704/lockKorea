package com.example.Sneakers.services;

import com.example.Sneakers.ai.listeners.ProductEventListener.ProductDeleteEvent;
import com.example.Sneakers.ai.listeners.ProductEventListener.ProductSaveEvent;
import com.example.Sneakers.dtos.ProductDTO;
import com.example.Sneakers.dtos.ProductImageDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.exceptions.InvalidParamException;
import com.example.Sneakers.models.*;
import com.example.Sneakers.repositories.*;
import com.example.Sneakers.responses.ListProductResponse;
import com.example.Sneakers.responses.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductFeatureRepository productFeatureRepository;

    @Mock
    private ProductFeatureService productFeatureService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private Category testCategory;
    private Product testProduct;
    private ProductDTO productDTO;
    private ProductImage testImage;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Sneakers")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Sneakers")
                .price(100000L)
                .description("Test description")
                .category(testCategory)
                .discount(10L)
                .quantity(50L)
                .thumbnail("thumbnail.jpg")
                .build();

        productDTO = new ProductDTO();
        productDTO.setName("New Product");
        productDTO.setPrice(150000L);
        productDTO.setDescription("New description");
        productDTO.setCategoryId(1L);
        productDTO.setDiscount(15L);
        productDTO.setQuantity(30L);
        productDTO.setThumbnail("new-thumbnail.jpg");

        testImage = ProductImage.builder()
                .id(1L)
                .imageUrl("image1.jpg")
                .build();
    }

    // ==================== Test Case ID: TC-PROD-001 ====================
    // Test Objective: Verify that createProduct creates product with category successfully
    // Input: Valid ProductDTO with existing categoryId
    // Expected Output: Saved Product entity with all fields set
    // DB Check: INSERT INTO products
    // ====================
    @Test
    void TC_PROD_001_createProduct_ShouldCreateProductSuccessfully() throws Exception {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return product;
        });

        // Act
        Product result = productService.createProduct(productDTO);

        // Assert
        assertNotNull(result);
        assertEquals("New Product", result.getName());
        assertEquals(150000L, result.getPrice().longValue());
        assertEquals(testCategory, result.getCategory());
        assertEquals(15L, result.getDiscount().longValue());
        assertEquals(30L, result.getQuantity().longValue());

        // Verify product saved
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertEquals("New description", savedProduct.getDescription());

        // Verify event published for indexing
        verify(eventPublisher).publishEvent(any(ProductSaveEvent.class));
    }

    // ==================== Test Case ID: TC-PROD-002 ====================
    // Test Objective: Verify that createProduct throws exception when category not found
    // Input: ProductDTO with non-existent categoryId
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_PROD_002_createProduct_ShouldThrowException_WhenCategoryNotFound() throws Exception {
        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        productDTO.setCategoryId(999L);

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                productService.createProduct(productDTO));
        assertTrue(exception.getMessage().contains("Cannot find category"));
    }

    // ==================== Test Case ID: TC-PROD-003 ====================
    // Test Objective: Verify that getProductById returns product for valid ID
    // Input: Existing product ID
    // Expected Output: Product entity with details loaded (including images)
    // ====================
    @Test
    void TC_PROD_003_getProductById_ShouldReturnProduct() throws Exception {
        // Arrange
        Long productId = 1L;
        when(productRepository.getDetailProduct(productId)).thenReturn(Optional.of(testProduct));

        // Act
        Product result = productService.getProductById(productId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Sneakers", result.getName());
    }

    // ==================== Test Case ID: TC-PROD-004 ====================
    // Test Objective: Verify that getProductById throws exception for non-existent ID
    // Input: Non-existent product ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_PROD_004_getProductById_ShouldThrowException_WhenProductNotFound() throws Exception {
        // Arrange
        when(productRepository.getDetailProduct(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                productService.getProductById(999L));
        assertTrue(exception.getMessage().contains("Cannot find product"));
    }

    // ==================== Test Case ID: TC-PROD-005 ====================
    // Test Objective: Verify that getAllProducts with pagination returns ProductResponse page
    // Input: Keyword, categoryId, minPrice, maxPrice, PageRequest
    // Expected Output: Page<ProductResponse> with rating and sold quantity populated
    // ====================
    @Test
    void TC_PROD_005_getAllProducts_ShouldReturnPagedResults() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Product product1 = Product.builder().id(1L).name("Product 1").price(100000L).category(testCategory).build();
        Product product2 = Product.builder().id(2L).name("Product 2").price(200000L).category(testCategory).build();
        Page<Product> productPage = new PageImpl<>(List.of(product1, product2));

        Object[] ratingStats1 = new Object[]{4.5, 10L};
        Object[] ratingStats2 = new Object[]{4.0, 5L};

        when(productRepository.searchProducts(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(reviewRepository.getRatingStatsByProductIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(
                        new Object[]{1L, 4.5, 10L},
                        new Object[]{2L, 4.0, 5L}
                ));
        when(orderDetailRepository.getTotalSoldQuantityByProductIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(
                        new Object[]{1L, 50L},
                        new Object[]{2L, 30L}
                ));

        // Act
        Page<ProductResponse> result = productService.getAllProducts(null, null, null, null, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        ProductResponse response1 = result.getContent().get(0);
        assertEquals(1L, response1.getId().longValue());
        assertEquals(4.5, response1.getAverageRating());
        assertEquals(10L, response1.getTotalReviews().longValue());
        assertEquals(50L, response1.getSoldQuantity().longValue());
    }

    // ==================== Test Case ID: TC-PROD-006 ====================
    // Test Objective: Verify that getAllProducts handles sort by rating correctly
    // Input: Sort by rating ascending/descending
    // Expected Output: Products sorted by average rating
    // ====================
    @Test
    void TC_PROD_006_getAllProducts_ShouldSortByRatingCorrectly() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("rating"));
        Product product1 = Product.builder().id(1L).build();
        Product product2 = Product.builder().id(2L).build();

        Page<Long> productIdsPage = new PageImpl<>(Arrays.asList(2L, 1L));
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product2, product1));

        when(productRepository.findProductIdsSortedByRating(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productIdsPage);
        when(productRepository.findProductsByIds(Arrays.asList(2L, 1L)))
                .thenReturn(Arrays.asList(product2, product1));
        when(reviewRepository.getRatingStatsByProductIds(Arrays.asList(2L, 1L)))
                .thenReturn(Arrays.asList(
                        new Object[]{2L, 4.5, 10L},
                        new Object[]{1L, 4.0, 5L}
                ));
        when(orderDetailRepository.getTotalSoldQuantityByProductIds(Arrays.asList(2L, 1L)))
                .thenReturn(Arrays.asList(
                        new Object[]{2L, 50L},
                        new Object[]{1L, 30L}
                ));

        // Act
        Page<ProductResponse> result = productService.getAllProducts(null, null, null, null, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        // Verify products are in rating-sorted order (4.5 before 4.0)
        assertEquals(2L, result.getContent().get(0).getId().longValue());
        assertEquals(4.5, result.getContent().get(0).getAverageRating());
    }

    // ==================== Test Case ID: TC-PROD-007 ====================
    // Test Objective: Verify that updateProduct updates all fields correctly
    // Input: Valid product ID, ProductDTO with updates
    // Expected Output: Updated Product entity with new values
    // ====================
    @Test
    void TC_PROD_007_updateProduct_ShouldUpdateProductSuccessfully() throws Exception {
        // Arrange
        Long productId = 1L;
        Product existingProduct = Product.builder()
                .id(productId)
                .name("Old Name")
                .price(100000L)
                .category(testCategory)
                .quantity(50L)
                .build();

        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setPrice(150000L);
        updateDTO.setDescription("Updated description");
        updateDTO.setCategoryId(1L);
        updateDTO.setDiscount(20L);
        updateDTO.setQuantity(60L);
        updateDTO.setAddQuantity(false);

        when(productRepository.getDetailProduct(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productService.updateProduct(productId, updateDTO);

        // Assert
        assertEquals("Updated Name", result.getName());
        assertEquals(150000L, result.getPrice().longValue());
        assertEquals("Updated description", result.getDescription());
        assertEquals(20L, result.getDiscount().longValue());
        assertEquals(60L, result.getQuantity().longValue());
        assertEquals(testCategory, result.getCategory());

        // Verify event published for re-indexing
        verify(eventPublisher).publishEvent(any(ProductSaveEvent.class));
    }

    // ==================== Test Case ID: TC-PROD-008 ====================
    // Test Objective: Verify that updateProduct with addQuantity=true adds to existing quantity
    // Input: ProductDTO with addQuantity=true and quantity value
    // Expected Output: Existing quantity + new quantity
    // ====================
    @Test
    void TC_PROD_008_updateProduct_ShouldAddQuantityWhenFlagTrue() throws Exception {
        // Arrange
        Long productId = 1L;
        Product existingProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .category(testCategory)
                .quantity(50L)
                .build();

        ProductDTO addQuantityDTO = new ProductDTO();
        addQuantityDTO.setCategoryId(1L);
        addQuantityDTO.setQuantity(10L);
        addQuantityDTO.setAddQuantity(true);

        when(productRepository.getDetailProduct(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productService.updateProduct(productId, addQuantityDTO);

        // Assert
        assertEquals(60L, result.getQuantity().longValue());
    }

    // ==================== Test Case ID: TC-PROD-009 ====================
    // Test Objective: Verify that updateProduct throws exception when resulting quantity negative
    // Input: addQuantity=true with negative result (current 5, adding -10)
    // Expected Output: InvalidParamException with appropriate message
    // ====================
    @Test
    void TC_PROD_009_updateProduct_ShouldThrowException_WhenNegativeQuantity() throws Exception {
        // Arrange
        Long productId = 1L;
        Product existingProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .category(testCategory)
                .quantity(5L)
                .build();

        ProductDTO addQuantityDTO = new ProductDTO();
        addQuantityDTO.setCategoryId(1L);
        addQuantityDTO.setQuantity(-10L); // Will result in 5 + (-10) = -5
        addQuantityDTO.setAddQuantity(true);

        when(productRepository.getDetailProduct(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act & Assert
        assertThrows(InvalidParamException.class, () -> {
            productService.updateProduct(productId, addQuantityDTO);
        });
    }

    // ==================== Test Case ID: TC-PROD-010 ====================
    // Test Objective: Verify that deleteProduct soft deletes and publishes delete event
    // Input: Valid product ID
    // Expected Output: Product deleted, ProductDeleteEvent published
    // ====================
    @Test
    void TC_PROD_010_deleteProduct_ShouldDeleteProductAndPublishEvent() {
        // Arrange
        Long productId = 1L;
        Product product = Product.builder().id(productId).build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(any(Product.class));

        // Act
        productService.deleteProduct(productId);

        // Assert
        verify(productRepository).delete(product);
        verify(eventPublisher).publishEvent(any(ProductDeleteEvent.class));
    }

    // ==================== Test Case ID: TC-PROD-011 ====================
    // Test Objective: Verify that existsByName returns true for existing product name
    // Input: Product name that exists
    // Expected Output: true
    // ====================
    @Test
    void TC_PROD_011_existsByName_ShouldReturnTrue_WhenNameExists() {
        // Arrange
        when(productRepository.existsByName("Test Product")).thenReturn(true);

        // Act
        boolean exists = productService.existsByName("Test Product");

        // Assert
        assertTrue(exists);
    }

    // ==================== Test Case ID: TC-PROD-012 ====================
    // Test Objective: Verify that createProductImage creates image successfully
    // Input: Valid productId, ProductImageDTO
    // Expected Output: Saved ProductImage entity
    // DB Check: INSERT INTO product_images
    // ====================
    @Test
    void TC_PROD_012_createProductImage_ShouldCreateImageSuccessfully() throws Exception {
        // Arrange
        Long productId = 1L;
        ProductImageDTO imageDTO = new ProductImageDTO();
        imageDTO.setImageUrl("new-image.jpg");
        imageDTO.setProductId(productId);

        Product product = Product.builder().id(productId).build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductId(productId)).thenReturn(Collections.emptyList());
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> {
            ProductImage img = invocation.getArgument(0);
            img.setId(1L);
            return img;
        });

        // Act
        ProductImage result = productService.createProductImage(productId, imageDTO);

        // Assert
        assertNotNull(result);
        assertEquals("new-image.jpg", result.getImageUrl());
        assertEquals(product, result.getProduct());
    }

    // ==================== Test Case ID: TC-PROD-013 ====================
    // Test Objective: Verify that createProductImage throws exception when limit exceeded
    // Input: Product already has 6 images (max)
    // Expected Output: InvalidParamException "Number of images must be <= 6"
    // ====================
    @Test
    void TC_PROD_013_createProductImage_ShouldThrowException_WhenLimitExceeded() throws Exception {
        // Arrange
        Long productId = 1L;
        ProductImageDTO imageDTO = new ProductImageDTO();
        imageDTO.setImageUrl("new-image.jpg");
        imageDTO.setProductId(productId);

        Product product = Product.builder().id(productId).build();

        // Simulate 6 existing images (max limit)
        List<ProductImage> existingImages = Arrays.asList(
                createMockImage(1L), createMockImage(2L), createMockImage(3L),
                createMockImage(4L), createMockImage(5L), createMockImage(6L)
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductId(productId)).thenReturn(existingImages);

        // Act & Assert
        Exception exception = assertThrows(InvalidParamException.class, () ->
                productService.createProductImage(productId, imageDTO));
        assertTrue(exception.getMessage().contains("Number of images must be <= 6"));
    }

    private ProductImage createMockImage(Long id) {
        return ProductImage.builder().id(id).build();
    }

    // ==================== Test Case ID: TC-PROD-014 ====================
    // Test Objective: Verify that deleteProductImage deletes image by ID
    // Input: Valid image ID
    // Expected Output: Image deleted from repository
    // ====================
    @Test
    void TC_PROD_014_deleteProductImage_ShouldDeleteImage() throws Exception {
        // Arrange
        Long imageId = 1L;
        Product product = Product.builder().id(1L).build();
        ProductImage image = ProductImage.builder()
                .id(imageId)
                .product(product)
                .build();

        when(productImageRepository.findById(imageId)).thenReturn(Optional.of(image));
        doNothing().when(productImageRepository).deleteById(imageId);

        // Act
        productService.deleteProductImage(imageId);

        // Assert
        verify(productImageRepository).deleteById(imageId);
    }

    // ==================== Test Case ID: TC-PROD-015 ====================
    // Test Objective: Verify that deleteProductImage throws exception for orphaned image
    // Input: Image ID with no associated product
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_PROD_015_deleteProductImage_ShouldThrowException_WhenImageOrphaned() throws Exception {
        // Arrange
        Long imageId = 1L;
        ProductImage image = ProductImage.builder()
                .id(imageId)
                .product(null) // Orphaned
                .build();

        when(productImageRepository.findById(imageId)).thenReturn(Optional.of(image));

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                productService.deleteProductImage(imageId));
        assertTrue(exception.getMessage().contains("is not associated with any product"));
    }

    // ==================== Test Case ID: TC-PROD-016 ====================
    // Test Objective: Verify that getRelatedProducts returns products from same category
    // Input: Product ID, get up to 4 related products (excluding self)
    // Expected Output: List of up to 4 products from same category
    // ====================
    @Test
    void TC_PROD_016_getRelatedProducts_ShouldReturnProductsFromSameCategory() throws Exception {
        // Arrange
        Long productId = 1L;
        Category category = Category.builder().id(1L).build();
        Product targetProduct = Product.builder()
                .id(productId)
                .category(category)
                .build();

        Product related1 = Product.builder().id(2L).category(category).build();
        Product related2 = Product.builder().id(3L).category(category).build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(targetProduct));
        when(productRepository.getProductsByCategory(category.getId()))
                .thenReturn(Arrays.asList(targetProduct, related1, related2));
        when(reviewRepository.getAverageRatingByProductId(anyLong())).thenReturn(4.5);
        when(reviewRepository.countByProductId(anyLong())).thenReturn(10L);

        // Act
        ListProductResponse response = productService.getRelatedProducts(productId);

        // Assert
        assertEquals(2, response.getTotalProducts());
        assertTrue(response.getProducts().stream().noneMatch(p -> p.getId().equals(productId)));
    }

    // ==================== Test Case ID: TC-PROD-017 ====================
    // Test Objective: Verify that getRelatedProducts returns empty for product with no category
    // Input: Product with null category
    // Expected Output: Empty list
    // ====================
    @Test
    void TC_PROD_017_getRelatedProducts_ShouldReturnEmpty_WhenProductHasNoCategory() throws Exception {
        // Arrange
        Long productId = 1L;
        Product productWithNoCategory = Product.builder()
                .id(productId)
                .category(null)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(productWithNoCategory));

        // Act
        ListProductResponse response = productService.getRelatedProducts(productId);

        // Assert
        assertEquals(0, response.getTotalProducts());
    }

    // ==================== Test Case ID: TC-PROD-018 ====================
    // Test Objective: Verify that getProductsByPrice returns products filtered by price range
    // Input: minPrice, maxPrice
    // Expected Output: Products within price range with ratings and sold quantities
    // ====================
    @Test
    void TC_PROD_018_getProductsByPrice_ShouldReturnFilteredProducts() {
        // Arrange
        Long minPrice = 50000L;
        Long maxPrice = 200000L;
        Product product = Product.builder()
                .id(1L)
                .name("Mid-range Product")
                .price(100000L)
                .build();

        when(productRepository.getProductsByPrice(minPrice, maxPrice)).thenReturn(List.of(product));
        when(reviewRepository.getAverageRatingByProductId(1L)).thenReturn(4.2);
        when(reviewRepository.countByProductId(1L)).thenReturn(15L);
        when(orderDetailRepository.getTotalSoldQuantityByProductIds(Arrays.asList(1L)))
                .thenReturn(Collections.singletonList(new Object[]{1L, 100L}));

        // Act
        ListProductResponse response = productService.getProductsByPrice(minPrice, maxPrice);

        // Assert
        assertEquals(1, response.getTotalProducts());
        ProductResponse productResponse = response.getProducts().get(0);
        assertEquals(4.2, productResponse.getAverageRating());
        assertEquals(15L, productResponse.getTotalReviews().longValue());
        assertEquals(100L, productResponse.getSoldQuantity().longValue());
    }

    // ==================== Test Case ID: TC-PROD-019 ====================
    // Test Objective: Verify that getProductsByKeyword searches by name/description
    // Input: Keyword string
    // Expected Output: Products matching keyword with ratings and sold quantities
    // ====================
    @Test
    void TC_PROD_019_getProductsByKeyword_ShouldReturnMatchingProducts() {
        // Arrange
        String keyword = "running";
        Product product = Product.builder()
                .id(1L)
                .name("Running Sneakers")
                .build();

        when(productRepository.getProductsByKeyword(keyword)).thenReturn(List.of(product));
        when(reviewRepository.getAverageRatingByProductId(1L)).thenReturn(4.8);
        when(reviewRepository.countByProductId(1L)).thenReturn(25L);
        when(orderDetailRepository.getTotalSoldQuantityByProductIds(Arrays.asList(1L)))
                .thenReturn(Collections.singletonList(new Object[]{1L, 200L}));

        // Act
        ListProductResponse response = productService.getProductsByKeyword(keyword);

        // Assert
        assertEquals(1, response.getTotalProducts());
        assertEquals("Running Sneakers", response.getProducts().get(0).getName());
    }

    // ==================== Test Case ID: TC-PROD-020 ====================
    // Test Objective: Verify that findProductsByIds batch fetches products
    // Input: List of product IDs
    // Expected Output: List of matching Product entities
    // ====================
    @Test
    void TC_PROD_020_findProductsByIds_ShouldReturnProducts() {
        // Arrange
        List<Long> productIds = Arrays.asList(1L, 2L, 3L);
        List<Product> products = Arrays.asList(
                Product.builder().id(1L).build(),
                Product.builder().id(2L).build(),
                Product.builder().id(3L).build()
        );

        when(productRepository.findProductsByIds(productIds)).thenReturn(products);

        // Act
        List<Product> result = productService.findProductsByIds(productIds);

        // Assert
        assertEquals(3, result.size());
    }

    // ==================== Test Case ID: TC-PROD-021 ====================
    // Test Objective: Verify that allProducts returns all products with images
    // Input: No parameters
    // Expected Output: List of all products with eager-loaded images
    // ====================
    @Test
    void TC_PROD_021_allProducts_ShouldReturnAllProductsWithImages() {
        // Arrange
        Product product1 = Product.builder().id(1L).build();
        Product product2 = Product.builder().id(2L).build();

        when(productRepository.findAllWithImages()).thenReturn(Arrays.asList(product1, product2));

        // Act
        List<Product> result = productService.allProducts();

        // Assert
        assertEquals(2, result.size());
    }

    // ==================== Test Case ID: TC-PROD-022 ====================
    // Test Objective: Verify that totalProducts returns count of all products
    // Input: No parameters
    // Expected Output: Total product count
    // ====================
    @Test
    void TC_PROD_022_totalProducts_ShouldReturnTotalCount() {
        // Arrange
        when(productRepository.count()).thenReturn(150L);

        // Act
        long total = productService.totalProducts();

        // Assert
        assertEquals(150L, total);
    }

    // ==================== Test Case ID: TC-PROD-023 ====================
    // Test Objective: Verify that updateProduct throws exception when product not found
    // Input: Non-existent product ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_PROD_023_updateProduct_ShouldThrowException_WhenProductNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setPrice(150000L);
        updateDTO.setCategoryId(1L);
        when(productRepository.getDetailProduct(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                productService.updateProduct(nonExistentId, updateDTO));
        assertTrue(exception.getMessage().contains("Cannot find product"));
    }

    // ==================== Test Case ID: TC-PROD-024 ====================
    // Test Objective: Verify that deleteProduct throws exception when product not found
    // Input: Non-existent product ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_PROD_024_deleteProduct_ShouldHandleNonExistentProduct() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act - should not throw exception
        productService.deleteProduct(nonExistentId);

        // Assert - verify delete never called and no event published
        verify(productRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ==================== Test Case ID: TC-PROD-028 ====================
    // Test Objective: Verify that updateProduct handles null quantity (BUG DETECTION)
    // Input: Valid product ID, DTO with quantity = null (replace mode, addQuantity = false or null)
    // Expected Output: Should preserve existing quantity or throw validation error
    // ====================
    @Test
    void TC_PROD_028_updateProduct_ShouldHandleNullQuantity_Bug() throws Exception {
        // Arrange
        Long productId = 1L;
        Product existingProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .price(100000L)
                .quantity(50L) // Existing quantity
                .category(testCategory)
                .build();

        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setName("Updated Product");
        updateDTO.setPrice(150000L);
        updateDTO.setCategoryId(1L);
        updateDTO.setQuantity(null); // Null quantity - BUG: service sets quantity to null
        updateDTO.setAddQuantity(false); // Replace mode

        when(productRepository.getDetailProduct(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productService.updateProduct(productId, updateDTO);

        // Assert - BUG: Current implementation sets quantity to null, losing existing value
        // Expected: quantity should remain 50 or throw exception
        // This test will FAIL showing the bug
        assertNotNull(result.getQuantity());
        assertEquals(50L, result.getQuantity().longValue());
    }

    // ==================== Test Case ID: TC-PROD-029 ====================
    // Test Objective: Verify that getRelatedProducts throws exception when product not found
    // Input: Non-existent product ID
    // Expected Output: Generic Exception
    // ====================
    @Test
    void TC_PROD_027_createProductImage_ShouldThrowException_WhenProductNotFound() throws Exception {
        // Arrange
        Long nonExistentProductId = 999L;
        ProductImageDTO imageDTO = new ProductImageDTO();
        imageDTO.setImageUrl("image.jpg");
        imageDTO.setProductId(nonExistentProductId);
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                productService.createProductImage(nonExistentProductId, imageDTO));
        assertEquals("Cannot find category with id = " + nonExistentProductId, exception.getMessage());
    }

    // ==================== Test Case ID: TC-PROD-030 ====================
    // Test Objective: Verify that deleteProductImage throws exception when image not found
    // Input: Non-existent image ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_PROD_030_deleteProductImage_ShouldThrowException_WhenImageNotFound() throws Exception {
        // Arrange
        Long nonExistentImageId = 999L;
        when(productImageRepository.findById(nonExistentImageId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                productService.deleteProductImage(nonExistentImageId));
        assertTrue(exception.getMessage().contains("Cannot find product image"));
    }

    // ==================== Test Case ID: TC-PROD-029 ====================
    // Test Objective: Verify that getRelatedProducts throws exception when product not found
    // Input: Non-existent product ID
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_PROD_029_getRelatedProducts_ShouldThrowException_WhenProductNotFound() throws Exception {
        // Arrange
        Long nonExistentProductId = 999L;
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                productService.getRelatedProducts(nonExistentProductId));
        assertEquals("Cannot find product with id = " + nonExistentProductId, exception.getMessage());
    }

}

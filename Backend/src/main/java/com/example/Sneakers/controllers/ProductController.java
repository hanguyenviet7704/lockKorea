package com.example.Sneakers.controllers;

import com.example.Sneakers.components.LocalizationUtils;
import com.example.Sneakers.dtos.ProductDTO;
import com.example.Sneakers.dtos.ProductImageDTO;
import com.example.Sneakers.models.Product;
import com.example.Sneakers.models.ProductImage;
import com.example.Sneakers.repositories.ReviewRepository;
import com.example.Sneakers.repositories.OrderDetailRepository;
import com.example.Sneakers.responses.ListProductResponse;
import com.example.Sneakers.responses.ProductListResponse;
import com.example.Sneakers.responses.ProductResponse;
import com.example.Sneakers.responses.UploadProductResponse;
import com.example.Sneakers.services.IProductService;
import com.example.Sneakers.utils.MessageKeys;
import com.github.javafaker.Faker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("${api.prefix}/products")
@RestController
@RequiredArgsConstructor
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final IProductService productService;
    private final LocalizationUtils localizationUtils;
    private final ReviewRepository reviewRepository;
    private final OrderDetailRepository orderDetailRepository;

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> createProduct(
            @Valid @RequestBody ProductDTO productDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors().stream().map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(errorMessages);
            }
            Product newProduct = productService.createProduct(productDTO);
            return ResponseEntity.ok(UploadProductResponse
                    .builder()
                    .message("Create product successfully!")
                    .productId(newProduct.getId())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Upload ảnh
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    private String storeFile(MultipartFile file) throws IOException {
        if (!isImageFile(file) || file.getOriginalFilename() == null) {
            throw new IOException("Invalid image format");
        }
        // Lấy tên file
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        // Thêm UUID vào trước tên file để đảm bảo tên file là duy nhất
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        // Đường dẫn đến thư mục mà bạn muốn lưu file
        Path uploadDir = Paths.get("uploads");
        // Kiểm tra và tạo thư mục nếu nó không tồn tại
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        // Đường dẫn đầy đủ đến file
        Path destination = Paths.get(uploadDir.toString(), uniqueFileName);
        // Sao chép file vào thư mục đích
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return uniqueFileName;
    }

    @PostMapping(value = "uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> uploadImgages(
            @ModelAttribute("files") List<MultipartFile> files,
            @PathVariable("id") Long productId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            Product existingProduct = productService.getProductById(productId);
            files = files == null ? new ArrayList<MultipartFile>() : files;
            if (files.size() > ProductImage.MAXIMUM_IMAGES_PER_PRODUCT) {
                return ResponseEntity.badRequest().body(localizationUtils
                        .getLocalizedMessage(MessageKeys.UPLOAD_IMAGES_MAX_5));
            }
            List<ProductImage> productImages = new ArrayList<>();
            boolean isFirstImage = existingProduct.getThumbnail() == null || existingProduct.getThumbnail().isEmpty();

            for (MultipartFile file : files) {
                if (file.getSize() == 0) {
                    continue;
                }
                // Kiểm tra kích thước file và định dạng
                if (file.getSize() > 10 * 1024 * 1024) {
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                            .body(localizationUtils
                                    .getLocalizedMessage(MessageKeys.UPLOAD_IMAGES_FILE_LARGE));
                }
                // Kiểm tra định dạng file
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_IMAGES_FILE_MUST_BE_IMAGE));
                }
                // Lưu file và cập nhật thumnail trong DTO
                String fileName = storeFile(file);

                // Set the first image as thumbnail if product doesn't have one
                if (isFirstImage) {
                    existingProduct.setThumbnail(fileName);
                    productService.updateProductThumbnail(productId, fileName);
                    isFirstImage = false;
                }

                // Lưu vào đối tượng product trong database
                ProductImage productImage = productService.createProductImage(
                        existingProduct.getId(),
                        ProductImageDTO.builder()
                                .imageUrl(fileName)
                                .build());
                productImages.add(productImage);
            }
            return ResponseEntity.ok().body(productImages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/images/{imageName:.+}")
    public ResponseEntity<?> viewImage(@PathVariable String imageName) {
        try {
            // URL decode the image name
            String decodedImageName = URLDecoder.decode(imageName, StandardCharsets.UTF_8);
            
            // Remove any path prefixes like "image-uploads/" or "uploads/" if present
            String cleanImageName = decodedImageName;
            if (decodedImageName.contains("/")) {
                cleanImageName = decodedImageName.substring(decodedImageName.lastIndexOf("/") + 1);
            }
            
            // Get absolute path to Backend directory
            String userDir = System.getProperty("user.dir");
            Path currentDir = Paths.get(userDir).toAbsolutePath();
            Path backendRoot = currentDir;
            
            // Auto-detect Backend folder
            Path backendDir = currentDir.resolve("Backend");
            if (Files.exists(backendDir) && Files.isDirectory(backendDir)) {
                backendRoot = backendDir;
            } else if (currentDir.getFileName().toString().equals("Backend")) {
                backendRoot = currentDir;
            }
            
            logger.info("Loading image: original={}, clean={}, backendRoot={}, currentDir={}", 
                    decodedImageName, cleanImageName, backendRoot.toAbsolutePath(), currentDir);
            
            // Try multiple possible locations (order matters - try most common first)
            Path[] possiblePaths = {
                backendRoot.resolve("uploads").resolve(cleanImageName),
                backendRoot.resolve("image-uploads").resolve(cleanImageName),
                backendRoot.resolve("uploads").resolve(decodedImageName),
                backendRoot.resolve("image-uploads").resolve(decodedImageName),
                currentDir.resolve("uploads").resolve(cleanImageName), // Current dir uploads
                currentDir.resolve("image-uploads").resolve(cleanImageName),
                Paths.get("uploads").resolve(cleanImageName).toAbsolutePath(), // Relative to working dir
                Paths.get("image-uploads").resolve(cleanImageName).toAbsolutePath()
            };
            
            UrlResource resource = null;
            for (Path imagePath : possiblePaths) {
                try {
                    Path absolutePath = imagePath.toAbsolutePath();
                    logger.debug("Trying path: {}", absolutePath);
                    
                    if (Files.exists(absolutePath) && Files.isReadable(absolutePath)) {
                        resource = new UrlResource(absolutePath.toUri());
                        logger.info("Found image at: {}", absolutePath);
                        return ResponseEntity.ok()
                                .contentType(MediaType.IMAGE_JPEG)
                                .body(resource);
                    }
                } catch (Exception e) {
                    logger.debug("Path not accessible: {} - {}", imagePath, e.getMessage());
                    continue;
                }
            }
            
            // If no image found, return notfound.jpg from uploads
            Path notFoundPath = backendRoot.resolve("uploads").resolve("notfound.jpg");
            if (!Files.exists(notFoundPath)) {
                notFoundPath = Paths.get("uploads/notfound.jpg");
            }
            
            if (Files.exists(notFoundPath)) {
                logger.warn("Image not found: {}, returning notfound.jpg from {}", 
                        imageName, notFoundPath.toAbsolutePath());
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new UrlResource(notFoundPath.toAbsolutePath().toUri()));
            }
            
            logger.error("Image not found and notfound.jpg also missing: {}", imageName);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error loading image: {}", imageName, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("")
    public ResponseEntity<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0", name = "category_id") Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("id").ascending());
        logger.info(String.format("keyword =  %s, category_id = %d, page = %d, limit = %d",
                keyword, categoryId, page, limit));

        Page<ProductResponse> productPage = productService.getAllProducts(keyword, categoryId, null, null, pageRequest);

        int totalPages = productPage.getTotalPages();
        List<ProductResponse> products = productPage.getContent();
        long totalProduct = productService.totalProducts();
        return ResponseEntity.ok(ProductListResponse.builder()
                .products(products)
                .totalProducts(totalProduct)
                .totalPages(totalPages)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(
            @PathVariable("id") Long productId) {
        try {
            Product existingProduct = productService.getProductById(productId);
            ProductResponse response = ProductResponse.fromProduct(existingProduct);
            // Add rating stats
            Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
            Long totalReviews = reviewRepository.countByProductId(productId);
            response.setAverageRating(avgRating != null ? avgRating : 0.0);
            response.setTotalReviews(totalReviews);
            // Add sold quantity
            Long soldQuantity = orderDetailRepository.getTotalSoldQuantityByProductId(productId);
            response.setSoldQuantity(soldQuantity != null ? soldQuantity : 0L);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductByCategory(
            @PathVariable("categoryId") Long categoryId) {
        try {
            ListProductResponse listProductResponse = productService.getProductsByCategory(categoryId);
            return ResponseEntity.ok(listProductResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/related/{productId}")
    public ResponseEntity<?> getRelatedProducts(
            @PathVariable("productId") Long productId) {
        try {
            ListProductResponse listProductResponse = productService.getRelatedProducts(productId);
            return ResponseEntity.ok(listProductResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> allProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0", name = "category_id") Long categoryId,
            @RequestParam(name = "min_price", required = false) Long minPrice,
            @RequestParam(name = "max_price", required = false) Long maxPrice) {
        try {
            // Parse sort param (format: "field,direction")
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            String sortDirection = sortParams.length > 1 ? sortParams[1] : "desc";
            
            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            
            // Prioritize in-stock products, then apply user sort
            Sort sortBy = Sort.by(Sort.Direction.DESC, "inStock")
                    .and(Sort.by(direction, sortField));

            // Sử dụng pagination thay vì load tất cả
            PageRequest pageRequest = PageRequest.of(page, limit, sortBy);
            Page<ProductResponse> productPage = productService.getAllProducts(keyword, categoryId, minPrice, maxPrice, pageRequest);

            return ResponseEntity.ok(ProductListResponse.builder()
                    .products(productPage.getContent())
                    .totalProducts(productPage.getTotalElements()) // Use totalElements from page for filtered results
                    .totalPages(productPage.getTotalPages())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/price")
    public ResponseEntity<?> getProductsByPrice(
            @RequestParam(name = "min_price", defaultValue = "0") Long minPrice,
            @RequestParam(name = "max_price", defaultValue = "50000000") Long maxPrice) {
        try {
            ListProductResponse listProductResponse = productService.getProductsByPrice(minPrice, maxPrice);
            return ResponseEntity.ok(listProductResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> getProductsByKeyword(
            @RequestParam("keyword") String keyword) {
        try {
            ListProductResponse listProductResponse = productService.getProductsByKeyword(keyword);
            return ResponseEntity.ok(listProductResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/by-ids")
    public ResponseEntity<?> getProductsByIds(@RequestParam("ids") String ids) {
        // eg: 1,3,5,7
        try {
            // Tách chuỗi ids thành một mảng các số nguyên
            List<Long> productIds = Arrays.stream(ids.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            List<Product> products = productService.findProductsByIds(productIds);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDTO productDTO,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            Product updatedProduct = productService.updateProduct(id, productDTO);
            return ResponseEntity.ok(ProductResponse.fromProduct(updatedProduct));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok("Product with id = " + id + " deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/images/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> deleteProductImage(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            productService.deleteProductImage(id);
            return ResponseEntity.ok("Product image with id = " + id + " deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // @PostMapping("generateFakeProducts")
    // private ResponseEntity<String> genereateFakeProducts(){
    // Faker faker = new Faker();
    // for(int i = 0; i < 1_000_000; i++){
    // String productName = faker.commerce().productName();
    // if(productService.existsByName(productName)){
    // continue;
    // }
    // ProductDTO productDTO = ProductDTO.builder()
    // .name(productName)
    // .price((float)faker.number().numberBetween(10, 90_000_000))
    // .thumbnail("")
    // .description(faker.lorem().sentence())
    // .categoryId((long)faker.number().numberBetween(1,5))
    // .build();
    // try {
    // productService.createProduct(productDTO);
    // } catch (Exception e) {
    // return ResponseEntity.badRequest().body(e.getMessage());
    // }
    // }
    // return ResponseEntity.ok("Fake products created successfully");
    // }
}
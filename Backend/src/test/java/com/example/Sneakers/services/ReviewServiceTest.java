package com.example.Sneakers.services;

import com.example.Sneakers.dtos.ReviewDTO;
import com.example.Sneakers.dtos.ReviewReplyDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.models.Product;
import com.example.Sneakers.models.Review;
import com.example.Sneakers.models.User;
import com.example.Sneakers.repositories.ProductRepository;
import com.example.Sneakers.repositories.ReviewRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.responses.ReviewResponse;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Captor
    private ArgumentCaptor<Review> reviewCaptor;

    private User testUser;
    private Product testProduct;
    private ReviewDTO reviewDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("0123456789")
                .fullName("Test User")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Sneakers")
                .build();

        reviewDTO = new ReviewDTO();
        reviewDTO.setProductId(1L);
        reviewDTO.setRating(5);
        reviewDTO.setComment("Excellent quality!");
    }

    // ==================== Test Case ID: TC-REV-001 ====================
    // Test Objective: Verify that createReview creates review successfully
    // Input: Valid ReviewDTO, existing user and product
    // Expected Output: Saved Review entity with user and product associations
    // ====================
    @Test
    void TC_REV_001_createReview_ShouldCreateReviewSuccessfully() throws Exception {
        // Arrange
        Long userId = 1L;
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(1L);
            review.setCreatedAt(LocalDateTime.now());
            return review;
        });

        // Act
        ReviewResponse response = reviewService.createReview(reviewDTO, userId);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Excellent quality!", response.getComment());

        // Verify review was saved with correct associations
        verify(reviewRepository).save(reviewCaptor.capture());
        Review savedReview = reviewCaptor.getValue();
        assertEquals(testUser, savedReview.getUser());
        assertEquals(testProduct, savedReview.getProduct());
    }

    // ==================== Test Case ID: TC-REV-002 ====================
    // Test Objective: Verify that getReviewsByProduct returns all reviews for a product
    // Input: Product ID
    // Expected Output: List of ReviewResponse objects
    // ====================
    @Test
    void TC_REV_002_getReviewsByProduct_ShouldReturnAllReviews() {
        // Arrange
        Long productId = 1L;
        Review review1 = Review.builder()
                .id(1L)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Great!")
                .build();
        Review review2 = Review.builder()
                .id(2L)
                .product(testProduct)
                .user(testUser)
                .rating(4)
                .comment("Good")
                .build();
        when(reviewRepository.findByProductIdWithUser(productId)).thenReturn(Arrays.asList(review1, review2));

        // Act
        List<ReviewResponse> result = reviewService.getReviewsByProductId(productId);

        // Assert
        assertEquals(2, result.size());
        assertEquals(5, result.get(0).getRating());
        assertEquals("Great!", result.get(0).getComment());
    }

    // ==================== Test Case ID: TC-REV-003 ====================
    // Test Objective: Verify that getProductRatingStats returns correct statistics
    // Input: Product ID
    // Expected Output: Map with averageRating and totalReviews
    // ====================
    @Test
    void TC_REV_003_getProductRatingStats_ShouldReturnStatistics() {
        // Arrange
        Long productId = 1L;
        when(reviewRepository.getAverageRatingByProductId(productId)).thenReturn(4.5);
        when(reviewRepository.countByProductId(productId)).thenReturn(10L);

        // Act
        Map<String, Object> stats = reviewService.getProductRatingStats(productId);

        // Assert
        assertEquals(4.5, stats.get("averageRating"));
        assertEquals(10L, stats.get("totalReviews"));
    }

    // ==================== Test Case ID: TC-REV-004 ====================
    // Test Objective: Verify that getReviewById returns review for valid ID
    // Input: Review ID
    // Expected Output: ReviewResponse
    // ====================
    @Test
    void TC_REV_004_getReviewById_ShouldReturnReview() throws Exception {
        // Arrange
        Long reviewId = 1L;
        Review review = Review.builder()
                .id(reviewId)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Great!")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // Act
        ReviewResponse response = reviewService.getReviewById(reviewId);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Great!", response.getComment());
    }

    // ==================== Test Case ID: TC-REV-005 ====================
    // Test Objective: Verify that deleteReview deletes review successfully
    // Input: Review ID owned by user
    // Expected Output: Review deleted
    // ====================
    @Test
    void TC_REV_005_deleteReview_ShouldDeleteReview() throws Exception {
        // Arrange
        Long reviewId = 1L;
        Long userId = 1L;
        Review review = Review.builder()
                .id(reviewId)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Great!")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        doNothing().when(reviewRepository).delete(review);

        // Act
        reviewService.deleteReview(reviewId, userId);

        // Assert
        verify(reviewRepository).delete(review);
    }

    // ==================== Test Case ID: TC-REV-006 ====================
    // Test Objective: Verify that getReviewById throws exception when review not found
    // Input: Non-existent review ID
    // Expected Output: DataNotFoundException with message "Review not found"
    // ====================
    @Test
    void TC_REV_006_getReviewById_ShouldThrowException_WhenReviewNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(reviewRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                reviewService.getReviewById(nonExistentId));
        assertEquals("Review not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-REV-007 ====================
    // Test Objective: Verify that createReview throws exception when product not found
    // Input: ReviewDTO with non-existent product ID
    // Expected Output: DataNotFoundException with message "Product not found"
    // ====================
    @Test
    void TC_REV_007_createReview_ShouldThrowException_WhenProductNotFound() throws Exception {
        // Arrange
        Long userId = 1L;
        Long nonExistentProductId = 999L;
        reviewDTO.setProductId(nonExistentProductId);
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                reviewService.createReview(reviewDTO, userId));
        assertEquals("Product not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-REV-008 ====================
    // Test Objective: Verify that createReview throws exception when user not found
    // Input: ReviewDTO with non-existent user ID
    // Expected Output: DataNotFoundException with message "User not found"
    // ====================
    @Test
    void TC_REV_008_createReview_ShouldThrowException_WhenUserNotFound() throws Exception {
        // Arrange
        Long nonExistentUserId = 999L;
        reviewDTO.setProductId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                reviewService.createReview(reviewDTO, nonExistentUserId));
        assertEquals("User not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-REV-009 ====================
    // Test Objective: Verify that deleteReview throws exception when review not found
    // Input: Non-existent review ID
    // Expected Output: DataNotFoundException with message "Review not found"
    // ====================
    @Test
    void TC_REV_009_deleteReview_ShouldThrowException_WhenReviewNotFound() throws Exception {
        // Arrange
        Long nonExistentReviewId = 999L;
        Long userId = 1L;
        when(reviewRepository.findById(nonExistentReviewId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                reviewService.deleteReview(nonExistentReviewId, userId));
        assertEquals("Review not found", exception.getMessage());
    }

    // ==================== Test Case ID: TC-REV-010 ====================
    // Test Objective: Verify that deleteReview throws exception when user is not the owner
    // Input: Review ID owned by different user
    // Expected Output: RuntimeException with Vietnamese message "Bạn không có quyền xóa đánh giá này"
    // ====================
    @Test
    void TC_REV_010_deleteReview_ShouldThrowException_WhenUserIsNotOwner() throws Exception {
        // Arrange
        Long reviewId = 1L;
        Long ownerUserId = 1L;
        Long differentUserId = 2L;
        User differentUser = User.builder()
                .id(differentUserId)
                .phoneNumber("0987654321")
                .fullName("Different User")
                .build();
        User ownerUser = User.builder()
                .id(ownerUserId)
                .build();
        Review review = Review.builder()
                .id(reviewId)
                .product(testProduct)
                .user(ownerUser) // owner is ownerUser with id 1L
                .rating(5)
                .comment("Great!")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                reviewService.deleteReview(reviewId, differentUserId));
        assertEquals("Bạn không có quyền xóa đánh giá này", exception.getMessage());
    }

    // ==================== Test Case ID: TC-REV-011 ====================
    // Test Objective: Verify that createReview handles rating as provided (no validation in service)
    // Input: ReviewDTO with rating (any integer, service doesn't validate range)
    // Expected Output: Review is created with given rating
    // Note: Service layer does not validate rating range; assumes DTO validation at controller
    // ====================
    // Removed: Invalid rating test - service doesn't validate rating range

    // ==================== Test Case ID: TC-REV-012 ====================
    // Test Objective: Verify that createReview handles comment as provided (no validation in service)
    // Input: ReviewDTO with null comment
    // Expected Output: Review is created with null comment (if allowed by DB)
    // Note: Service layer does not validate comment; assumes DTO validation at controller
    // ====================
    // Removed: Null comment test - service doesn't validate comment

    // ==================== Test Case ID: TC-REV-013 ====================
    // Test Objective: Verify that replyToReview throws exception when staff has no role (BUG DETECTION)
    // Input: Staff user with null role
    // Expected Output: Should throw exception, not NullPointerException
    // ====================
    @Test
    void TC_REV_013_replyToReview_ShouldThrowException_WhenStaffHasNoRole_Bug() throws Exception {
        // Arrange
        Long reviewId = 1L;
        Long staffId = 1L;
        ReviewReplyDTO replyDTO = new ReviewReplyDTO();
        replyDTO.setReviewId(reviewId);
        replyDTO.setReply("Staff reply");

        Review review = Review.builder()
                .id(reviewId)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Great!")
                .build();

        User staffWithoutRole = User.builder()
                .id(staffId)
                .phoneNumber("0123456789")
                .fullName("Staff User")
                .role(null) // BUG: null role causes NPE
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffWithoutRole));

        // Act & Assert - Should throw RuntimeException, not NullPointerException
        Exception exception = assertThrows(Exception.class, () ->
                reviewService.replyToReview(replyDTO, staffId));
        assertFalse(exception instanceof NullPointerException);
        assertTrue(exception.getMessage().contains("quyền") || exception.getMessage().contains("STAFF") ||
                   exception.getMessage().contains("ADMIN"));
    }

}

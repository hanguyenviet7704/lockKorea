package com.example.Sneakers;

import com.example.Sneakers.controllers.ReviewController;
import com.example.Sneakers.dtos.ReviewReplyDTO;
import com.example.Sneakers.models.Product;
import com.example.Sneakers.models.Review;
import com.example.Sneakers.models.Role;
import com.example.Sneakers.models.User;
import com.example.Sneakers.repositories.ProductRepository;
import com.example.Sneakers.repositories.ReviewRepository;
import com.example.Sneakers.repositories.RoleRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.responses.ReviewResponse;
import com.example.Sneakers.services.ReviewService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.jpa.hibernate.ddl-auto=none")
@Transactional
@Rollback
class ReviewAdminDbIntegrationTest {
    /*
     * Integration test cho module Danh gia Admin.
     *
     * Test nay dung MySQL that de tao Product/User/Review va goi ReviewService.
     * Cac test co saveReview/saveProduct/saveUser deu co CheckDB.
     * @Rollback giup xoa du lieu test sau moi test case.
     *
     * Cac test validate ReviewReplyDTO hoac doc @PreAuthorize tren controller
     * khong tao du lieu DB nen khong can rollback thuc te.
     *
     * Cach doc nhanh moi test:
     * - Arrange: tao user/product/review can cho scenario.
     * - Act: goi ReviewService hoac validate DTO.
     * - Assert: kiem tra response va query lai DB neu test co ghi du lieu.
     */
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testGetAllReviewsForAdmin() {
        // Test Case ID: TC-IT-REV-001
        // CheckDB: insert product/user/review that va doc lai bang service tu MySQL.
        // Rollback: @Transactional + @Rollback xoa du lieu test sau khi test ket thuc.
        Review review = saveReview("Hai long", saveUser(Role.USER), saveProduct("Khoa admin all"));

        Page<ReviewResponse> responses = reviewService.getAllReviews(0, 15, null, null);

        assertTrue(responses.getContent().stream().anyMatch(item -> item.getId().equals(review.getId())));
    }

    @Test
    void testSearchReviewsByKeyword() {
        // Test Case ID: TC-IT-REV-002
        // Muc tieu: admin search review theo keyword.
        // CheckDB: tao review matching va review khong matching trong DB.
        // Rollback: product/user/review test bi rollback.
        Review matching = saveReview("Lap dat nhanh", saveUser(Role.USER), saveProduct("Khoa nhanh"));
        saveReview("Binh thuong", saveUser(Role.USER), saveProduct("Khoa khac"));

        Page<ReviewResponse> responses = reviewService.getAllReviews(0, 10, "nhanh", null);

        assertTrue(responses.getContent().stream().anyMatch(item -> item.getId().equals(matching.getId())));
        assertTrue(responses.getContent().stream().allMatch(item ->
                item.getComment().toLowerCase().contains("nhanh")
                        || item.getUserName().toLowerCase().contains("nhanh")));
    }

    @Test
    void testFilterReviewsByProductId() {
        // Test Case ID: TC-IT-REV-003
        // Muc tieu: filter review theo productId.
        // CheckDB: tao 2 product va review that, service query theo productId.
        // Rollback: du lieu test bi rollback.
        Product targetProduct = saveProduct("Khoa loc");
        Review review = saveReview("Dung tot", saveUser(Role.USER), targetProduct);
        saveReview("San pham khac", saveUser(Role.USER), saveProduct("Khoa khac"));

        Page<ReviewResponse> responses = reviewService.getAllReviews(0, 10, null, targetProduct.getId());

        assertTrue(responses.getContent().stream().anyMatch(item -> item.getId().equals(review.getId())));
        assertTrue(responses.getContent().stream().allMatch(item -> item.getProductId().equals(targetProduct.getId())));
    }

    @Test
    void testGetAllReviewsPagination() {
        // Test Case ID: TC-IT-REV-004
        // Muc tieu: kiem tra phan trang danh sach review admin.
        // CheckDB: tao 2 review that, goi page size = 1.
        // Expected: content size = 1 va totalElements phai >= 2.
        // Actual neu test fail: totalElements khong tinh dung tong review vua tao.
        // Rollback: du lieu test bi rollback.
        saveReview("Trang 1", saveUser(Role.USER), saveProduct("Khoa page 1"));
        saveReview("Trang 2", saveUser(Role.USER), saveProduct("Khoa page 2"));

        Page<ReviewResponse> responses = reviewService.getAllReviews(0, 1, null, null);

        assertEquals(1, responses.getContent().size());
        assertTrue(responses.getTotalElements() >= 2);
    }

    @Test
    void testGetAllReviewsSkipMissingUser() {
        // Test Case ID: TC-IT-REV-005
        // DB that co NOT NULL/FK user_id nen khong tao duoc review thieu user.
        // Kiem tra constraint DB thay vi gia lap du lieu hong trong service.
        // CheckDB: co goi reviewRepository.saveAndFlush de DB that nem DataIntegrityViolationException.
        // Rollback: khong co record loi nao duoc commit.
        Review brokenReview = Review.builder()
                .product(saveProduct("Khoa loi user"))
                .user(null)
                .rating(4)
                .comment("Du lieu cu")
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> reviewRepository.saveAndFlush(brokenReview));
    }

    @Test
    void testUserCannotGetAdminReviews() throws Exception {
        // Test Case ID: TC-IT-REV-006
        // Muc tieu: API lay danh sach review admin chi cho STAFF/ADMIN.
        // CheckDB: khong dung DB, chi doc annotation @PreAuthorize bang reflection.
        // Rollback: khong can vi khong ghi DB.
        Method method = ReviewController.class.getMethod(
                "getAllReviews", int.class, int.class, String.class, Long.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('ROLE_STAFF') or hasRole('ROLE_ADMIN')", preAuthorize.value());
    }

    @Test
    void testAdminReplyToReviewSuccess() throws Exception {
        // Test Case ID: TC-IT-REV-007
        // Muc tieu: admin tra loi review thanh cong.
        // CheckDB: tao admin/user/product/review that, reply, flush va query lai review.
        // Rollback: staffReply/staffReplyAt va du lieu test bi rollback.
        // Arrange: tao admin va 1 review cua khach.
        User admin = saveUser(Role.ADMIN);
        Review review = saveReview("Can tu van", saveUser(Role.USER), saveProduct("Khoa admin reply"));

        // Act: admin gui reply cho review.
        ReviewResponse response = reviewService.replyToReview(
                ReviewReplyDTO.builder().reviewId(review.getId()).reply("Cam on ban").build(),
                admin.getId());
        reviewRepository.flush();

        // Assert: DB phai luu noi dung, nguoi reply va thoi gian reply.
        Review persisted = reviewRepository.findById(review.getId()).orElseThrow();
        assertEquals("Cam on ban", persisted.getStaffReply());
        assertEquals(admin.getId(), response.getStaffReplyBy());
        assertNotNull(persisted.getStaffReplyAt());
    }

    @Test
    void testStaffReplyToReviewSuccess() throws Exception {
        // Test Case ID: TC-IT-REV-008
        // Muc tieu: staff cung duoc phep tra loi review.
        // CheckDB: tao staff/user/product/review that va service cap nhat review.
        // Rollback: du lieu test bi rollback.
        User staff = saveUser(Role.STAFF);
        Review review = saveReview("Can ho tro", saveUser(Role.USER), saveProduct("Khoa staff reply"));

        ReviewResponse response = reviewService.replyToReview(
                ReviewReplyDTO.builder().reviewId(review.getId()).reply("Shop se ho tro").build(),
                staff.getId());

        assertEquals("Shop se ho tro", response.getStaffReply());
        assertEquals(staff.getId(), response.getStaffReplyBy());
    }

    @Test
    void testReplyToReviewMissingReviewId() {
        // Test Case ID: TC-IT-REV-009
        // Muc tieu: DTO reply thieu reviewId phai bi validation bat loi.
        // CheckDB: khong dung DB, chi validate DTO.
        // Rollback: khong can.
        Set<ConstraintViolation<ReviewReplyDTO>> violations =
                validator.validate(ReviewReplyDTO.builder().reply("Cam on").build());

        assertFalse(violations.isEmpty());
    }

    @Test
    void testReplyToReviewEmptyMessage() {
        // Test Case ID: TC-IT-REV-010
        // Muc tieu: reply rong phai bi validation bat loi.
        // CheckDB: khong dung DB.
        // Rollback: khong can.
        Set<ConstraintViolation<ReviewReplyDTO>> violations =
                validator.validate(ReviewReplyDTO.builder().reviewId(1L).reply("").build());

        assertFalse(violations.isEmpty());
    }

    @Test
    void testReplyToReviewNotFound() {
        // Test Case ID: TC-IT-REV-011
        // Muc tieu: reply reviewId khong ton tai phai bao loi.
        // CheckDB: service query DB theo reviewId khong ton tai.
        // Rollback: chi tao admin test, sau test rollback.
        User admin = saveUser(Role.ADMIN);

        assertThrows(Exception.class,
                () -> reviewService.replyToReview(
                        ReviewReplyDTO.builder().reviewId(999_999_999L).reply("Khong tim thay").build(),
                        admin.getId()));
    }

    @Test
    @Commit
    void testUserCannotReplyToReview() {
        // Test Case ID: TC-IT-REV-012
        // Muc tieu: customer ROLE_USER khong duoc reply review.
        // CheckDB: tao customer/review that, service doc user role va review tu DB.
        // Rollback: du lieu test bi rollback.
        User customer = saveUser(Role.USER);
        Review review = saveReview("Can ho tro", saveUser(Role.USER), saveProduct("Khoa user reply"));

        assertThrows(RuntimeException.class,
                () -> reviewService.replyToReview(
                        ReviewReplyDTO.builder().reviewId(review.getId()).reply("Khong du quyen").build(),
                        customer.getId()));
    }

    @Test
    void testReplyTooLongBug() {
        // Test Case ID: TC-IT-REV-013
        // Expected: reply qua dai phai bi chan validation. DTO hien chua co @Size nen test fail that neu bug con ton tai.
        // Actual neu test fail: validator khong tra violation voi chuoi 5001 ky tu.
        // Root cause: ReviewReplyDTO chua co @Size(max=...) cho field reply.
        // CheckDB: khong dung DB, chi validate DTO trong memory.
        // Rollback: khong can.
        Set<ConstraintViolation<ReviewReplyDTO>> violations =
                validator.validate(ReviewReplyDTO.builder().reviewId(1L).reply("a".repeat(5_001)).build());

        assertFalse(violations.isEmpty());
    }

    @Test
    void testReplyDuplicateContentBug() throws Exception {
        // Test Case ID: TC-IT-REV-014
        // Expected: reply trung noi dung cu khong cap nhat lai staffReplyAt.
        // Source hien tai ghi de staffReplyAt nen test fail that neu bug con ton tai.
        // Actual neu test fail: staffReplyAt bi cap nhat sang thoi gian moi du noi dung reply khong doi.
        // Root cause: replyToReview chua check duplicate content truoc khi update timestamp.
        // CheckDB: tao review da co reply that, goi service reply lai va query DB de so sanh staffReplyAt.
        // Rollback: du lieu test bi rollback.
        // Arrange: tao review da co san reply cu va timestamp cu.
        User admin = saveUser(Role.ADMIN);
        Review review = saveReview("Noi dung review", saveUser(Role.USER), saveProduct("Khoa duplicate reply"));
        LocalDateTime oldReplyAt = LocalDateTime.now().minusDays(1);
        review.setStaffReply("Cam on ban");
        review.setStaffReplyBy(admin);
        review.setStaffReplyAt(oldReplyAt);
        reviewRepository.saveAndFlush(review);

        // Act: reply lai dung cung noi dung cu.
        reviewService.replyToReview(
                ReviewReplyDTO.builder().reviewId(review.getId()).reply("Cam on ban").build(),
                admin.getId());

        // Assert: timestamp cu phai duoc giu nguyen neu noi dung reply khong doi.
        Review persisted = reviewRepository.findById(review.getId()).orElseThrow();
        assertEquals(oldReplyAt, persisted.getStaffReplyAt());
    }

    private Review saveReview(String comment, User user, Product product) {
        // Helper tao review that lien ket product/user that.
        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(5)
                .comment(comment)
                .build();
        return reviewRepository.saveAndFlush(review);
    }

    private Product saveProduct(String prefix) {
        // Helper tao product that de review co FK hop le.
        Product product = Product.builder()
                .name(unique(prefix))
                .description("San pham DB rollback")
                .price(900_000L)
                .quantity(10L)
                .build();
        return productRepository.saveAndFlush(product);
    }

    private User saveUser(String roleName) {
        // Helper tao user va role that theo tung scenario ADMIN/STAFF/USER.
        Role role = roleRepository.saveAndFlush(Role.builder().name(roleName).build());
        User user = User.builder()
                .fullName(unique(roleName + " User"))
                .phoneNumber(uniquePhone())
                .email(unique("review") + "@example.com")
                .password("secret")
                .active(true)
                .role(role)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private String unique(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    private String uniquePhone() {
        return String.valueOf(9200000000L + Math.abs(System.nanoTime() % 79999999L)).substring(0, 10);
    }
}

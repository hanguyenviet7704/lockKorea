package com.example.Sneakers;

import com.example.Sneakers.components.JwtTokenUtils;
import com.example.Sneakers.dtos.AdminReturnActionDTO;
import com.example.Sneakers.dtos.ReturnRequestDTO;
import com.example.Sneakers.exceptions.PermissionDenyException;
import com.example.Sneakers.models.Order;
import com.example.Sneakers.models.OrderStatus;
import com.example.Sneakers.models.ReturnRequest;
import com.example.Sneakers.models.Role;
import com.example.Sneakers.models.User;
import com.example.Sneakers.repositories.OrderRepository;
import com.example.Sneakers.repositories.ReturnRequestRepository;
import com.example.Sneakers.repositories.RoleRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.services.ReturnService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.jpa.hibernate.ddl-auto=none")
@Transactional
@Rollback
class ReturnDbIntegrationTest {
    /*
     * Integration test cho module Doi tra.
     *
     * Cac service/repository o day chay voi Spring context that va MySQL that.
     * Cac test tao User/Order/ReturnRequest se ghi vao DB trong transaction test.
     * @Rollback giup tra DB ve trang thai ban dau sau moi test case.
     *
     * Cac test chi validate DTO bang Validator thi khong ghi DB.
     * Vi vay nhung case do khong can rollback thuc te, nhung van nam trong class
     * @Transactional de giu format thong nhat voi cac integration test khac.
     *
     * Cach doc nhanh moi test:
     * - Arrange: tao user/order/return request can cho scenario.
     * - Act: goi ReturnService voi data vua tao.
     * - Assert: kiem tra ket qua tra ve va query lai DB neu test co ghi du lieu.
     */
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Autowired
    private ReturnService returnService;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Test
    void testCreateReturnRequestSuccess() throws Exception {
        // Test Case ID: TC-IT-RET-001
        // CheckDB: tao return_requests that va doc lai bang repository trong MySQL.
        // Rollback: @Transactional + @Rollback xoa order/user/return_request sau test.
        User customer = saveUser(Role.USER);
        Order order = saveOrder(customer, OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(5));

        ReturnRequest result = returnService.createReturnRequest(
                new ReturnRequestDTO(order.getId(), "San pham bi loi can doi tra"),
                bearerToken(customer));
        returnRequestRepository.flush();

        ReturnRequest persisted = returnRequestRepository.findById(result.getId()).orElseThrow();
        assertEquals("PENDING", persisted.getStatus());
        assertEquals(order.getId(), persisted.getOrder().getId());
        assertEquals(500_000L, persisted.getRefundAmount().longValue());
    }

    @Test
    void testCreateReturnRequestMissingOrderId() {
        // Test Case ID: TC-IT-RET-002
        // Muc tieu: DTO thieu orderId phai bi validation bat loi.
        // CheckDB: khong dung DB, chi validate object DTO trong memory.
        // Rollback: khong can vi khong insert/update/delete DB.
        Set<ConstraintViolation<ReturnRequestDTO>> violations =
                validator.validate(new ReturnRequestDTO(null, "San pham bi loi can doi tra"));

        assertFalse(violations.isEmpty());
    }

    @Test
    void testCreateReturnRequestReasonTooShort() {
        // Test Case ID: TC-IT-RET-003
        // Muc tieu: ly do doi tra qua ngan phai bi validation bat loi.
        // CheckDB: khong dung DB.
        // Rollback: khong can vi khong co thay doi du lieu.
        Set<ConstraintViolation<ReturnRequestDTO>> violations =
                validator.validate(new ReturnRequestDTO(1L, "loi"));

        assertFalse(violations.isEmpty());
    }

    @Test
    void testCreateReturnRequestOtherUserOrder() throws Exception {
        // Test Case ID: TC-IT-RET-004
        // Muc tieu: user khac khong duoc tao yeu cau doi tra cho don hang khong phai cua minh.
        // CheckDB: tao 2 user va 1 order that; service doc order/user tu DB de check quyen.
        // Rollback: user/order test bi rollback sau test.
        User currentUser = saveUser(Role.USER);
        User owner = saveUser(Role.USER);
        Order order = saveOrder(owner, OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(3));

        assertThrows(PermissionDenyException.class,
                () -> returnService.createReturnRequest(
                        new ReturnRequestDTO(order.getId(), "San pham bi loi can doi tra"),
                        bearerToken(currentUser)));
    }

    @Test
    void testCreateReturnRequestDuplicate() throws Exception {
        // Test Case ID: TC-IT-RET-005
        // Muc tieu: mot order da co return request thi khong duoc tao trung.
        // CheckDB: tao order va return_request that roi goi service tao lai.
        // Rollback: order/return_request test bi rollback.
        User customer = saveUser(Role.USER);
        Order order = saveOrder(customer, OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(3));
        saveReturnRequest(order, "PENDING");

        assertThrows(Exception.class,
                () -> returnService.createReturnRequest(
                        new ReturnRequestDTO(order.getId(), "San pham bi loi can doi tra"),
                        bearerToken(customer)));
    }

    @Test
    void testCreateReturnRequestInvalidOrderStatus() throws Exception {
        // Test Case ID: TC-IT-RET-006
        // Muc tieu: order chua DELIVERED thi khong duoc doi tra.
        // CheckDB: tao order PROCESSING that de service kiem tra business rule.
        // Rollback: order/user test bi rollback.
        User customer = saveUser(Role.USER);
        Order order = saveOrder(customer, OrderStatus.PROCESSING, "Cash", LocalDateTime.now().minusDays(1));

        assertThrows(Exception.class,
                () -> returnService.createReturnRequest(
                        new ReturnRequestDTO(order.getId(), "San pham bi loi can doi tra"),
                        bearerToken(customer)));
    }

    @Test
    void testCreateReturnRequestExpiredPeriod() throws Exception {
        // Test Case ID: TC-IT-RET-007
        // Muc tieu: qua thoi han doi tra thi service phai tu choi.
        // CheckDB: tao order delivered cach 40 ngay trong DB.
        // Rollback: order/user test bi rollback.
        User customer = saveUser(Role.USER);
        Order order = saveOrder(customer, OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(40));

        assertThrows(Exception.class,
                () -> returnService.createReturnRequest(
                        new ReturnRequestDTO(order.getId(), "San pham bi loi can doi tra"),
                        bearerToken(customer)));
    }

    @Test
    void testGetMyReturnRequests() throws Exception {
        // Test Case ID: TC-IT-RET-008
        // Muc tieu: user chi lay duoc danh sach return request cua minh.
        // CheckDB: tao return_request that va query lai qua service.
        // Rollback: du lieu test bi rollback.
        User customer = saveUser(Role.USER);
        Order order = saveOrder(customer, OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4));
        ReturnRequest request = saveReturnRequest(order, "PENDING");

        List<ReturnRequest> result = returnService.getMyReturnRequests(bearerToken(customer));

        assertTrue(result.stream().anyMatch(item -> item.getId().equals(request.getId())));
    }

    @Test
    void testCreateReturnRequestWithoutTokenBug() {
        // Test Case ID: TC-IT-RET-009
        // Expected: khong token phai tra Unauthorized/PermissionDenyException.
        // Source hien tai substring token rong gay StringIndexOutOfBoundsException nen test fail that neu bug con ton tai.
        // Actual neu test fail: throw StringIndexOutOfBoundsException thay vi PermissionDenyException.
        // Root cause: ReturnService.getUserIdFromToken substring(7) ma chua check token rong/khong co Bearer.
        // CheckDB: khong can DB vi loi xay ra truoc khi service query du lieu.
        // Rollback: khong can vi khong ghi DB.
        assertThrows(PermissionDenyException.class,
                () -> returnService.createReturnRequest(
                        new ReturnRequestDTO(1L, "San pham bi loi can doi tra"), ""));
    }

    @Test
    void testGetAllReturnRequestsForAdmin() {
        // Test Case ID: TC-IT-RET-010
        // Muc tieu: admin lay duoc toan bo return request.
        // CheckDB: tao return_request that va service lay danh sach tu DB.
        // Rollback: du lieu test bi rollback.
        User customer = saveUser(Role.USER);
        Order order = saveOrder(customer, OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4));
        ReturnRequest request = saveReturnRequest(order, "PENDING");

        List<ReturnRequest> result = returnService.getAllReturnRequestsForAdmin();

        assertTrue(result.stream().anyMatch(item -> item.getId().equals(request.getId())));
    }

    @Test
    void testApproveReturnRequestSuccess() throws Exception {
        // Test Case ID: TC-IT-RET-011
        // Muc tieu: admin approve request PENDING thi request thanh AWAITING_REFUND.
        // CheckDB: tao return_request PENDING that, approve, flush va query lai order.
        // Rollback: thay doi status request/order bi rollback.
        ReturnRequest request = saveReturnRequest(
                saveOrder(saveUser(Role.USER), OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4)),
                "PENDING");

        ReturnRequest result = returnService.approveReturnRequest(request.getId(), adminAction("Dong y doi tra"));
        returnRequestRepository.flush();
        orderRepository.flush();

        assertEquals("AWAITING_REFUND", result.getStatus());
        assertEquals("awaiting_refund", orderRepository.findById(result.getOrder().getId()).orElseThrow().getStatus());
    }

    @Test
    void testRejectReturnRequestSuccess() throws Exception {
        // Test Case ID: TC-IT-RET-012
        // Muc tieu: admin reject request PENDING thi status request thanh REJECTED.
        // CheckDB: tao return_request PENDING that va doc lai order sau khi reject.
        // Rollback: du lieu test bi rollback.
        ReturnRequest request = saveReturnRequest(
                saveOrder(saveUser(Role.USER), OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4)),
                "PENDING");

        ReturnRequest result = returnService.rejectReturnRequest(request.getId(), adminAction("Khong du dieu kien"));
        returnRequestRepository.flush();
        orderRepository.flush();

        assertEquals("REJECTED", result.getStatus());
        assertEquals("delivered", orderRepository.findById(result.getOrder().getId()).orElseThrow().getStatus());
    }

    @Test
    void testCompleteRefundSuccess() throws Exception {
        // Test Case ID: TC-IT-RET-013
        // Muc tieu: hoan tien request AWAITING_REFUND thanh cong thi request thanh REFUNDED.
        // CheckDB: tao request that, complete refund va query lai order status.
        // Rollback: thay doi request/order bi rollback.
        ReturnRequest request = saveReturnRequest(
                saveOrder(saveUser(Role.USER), OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4)),
                "AWAITING_REFUND");

        ReturnRequest result = returnService.completeRefund(request.getId(), adminAction("Da chuyen khoan"));

        assertEquals("REFUNDED", result.getStatus());
        assertEquals("canceled", orderRepository.findById(result.getOrder().getId()).orElseThrow().getStatus());
    }

    @Test
    void testApproveReturnRequestNotPending() {
        // Test Case ID: TC-IT-RET-014
        // Muc tieu: request khong o trang thai PENDING thi khong duoc approve lai.
        // CheckDB: tao request REFUNDED that de check business rule.
        // Rollback: request/order/user test bi rollback.
        ReturnRequest request = saveReturnRequest(
                saveOrder(saveUser(Role.USER), OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4)),
                "REFUNDED");

        assertThrows(Exception.class,
                () -> returnService.approveReturnRequest(request.getId(), adminAction("Duyet lai")));
    }

    @Test
    void testCompleteRefundInvalidStatus() {
        // Test Case ID: TC-IT-RET-015
        // Muc tieu: request chua AWAITING_REFUND thi khong duoc complete refund.
        // CheckDB: tao request PENDING that.
        // Rollback: du lieu test bi rollback.
        ReturnRequest request = saveReturnRequest(
                saveOrder(saveUser(Role.USER), OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4)),
                "PENDING");

        assertThrows(Exception.class,
                () -> returnService.completeRefund(request.getId(), adminAction("Da chuyen khoan")));
    }

    @Test
    void testAdminActionMissingNotes() {
        // Test Case ID: TC-IT-RET-016
        // Muc tieu: adminNotes rong phai bi validation bat loi.
        // CheckDB: khong dung DB, chi validate DTO.
        // Rollback: khong can vi khong ghi DB.
        Set<ConstraintViolation<AdminReturnActionDTO>> violations = validator.validate(adminAction(""));

        assertFalse(violations.isEmpty());
    }

    @Test
    void testRejectRefundedRequestBug() {
        // Test Case ID: TC-IT-RET-017
        // Expected: request REFUNDED khong duoc reject lai. Source hien tai van reject nen test fail that neu bug con ton tai.
        // Actual neu test fail: service cho phep doi REFUNDED -> REJECTED.
        // Root cause: rejectReturnRequest chua chan trang thai da hoan tien.
        // CheckDB: tao request REFUNDED that de service cap nhat/kiem tra tren DB.
        // Rollback: request/order/user test bi rollback.
        ReturnRequest request = saveReturnRequest(
                saveOrder(saveUser(Role.USER), OrderStatus.DELIVERED, "Cash", LocalDateTime.now().minusDays(4)),
                "REFUNDED");

        assertThrows(Exception.class,
                () -> returnService.rejectReturnRequest(request.getId(), adminAction("Reject lai")));
    }

    private ReturnRequest saveReturnRequest(Order order, String status) {
        // Helper tao return_request that va set status theo tung kich ban.
        // saveAndFlush giup DB co du lieu ngay de service/repository doc lai.
        ReturnRequest request = ReturnRequest.builder()
                .order(order)
                .reason("San pham bi loi can doi tra")
                .refundAmount(java.math.BigDecimal.valueOf(order.getTotalMoney()))
                .build();
        ReturnRequest saved = returnRequestRepository.saveAndFlush(request);
        saved.setStatus(status);
        return returnRequestRepository.saveAndFlush(saved);
    }

    private Order saveOrder(User user, String status, String paymentMethod, LocalDateTime orderDate) {
        // Helper tao order that trong DB, lien ket voi user that.
        Order order = Order.builder()
                .user(user)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address("Ha Noi")
                .orderDate(orderDate)
                .status(status)
                .totalMoney(500_000L)
                .paymentMethod(paymentMethod)
                .shippingMethod("Nhanh")
                .active(true)
                .build();
        return orderRepository.saveAndFlush(order);
    }

    private User saveUser(String roleName) {
        // Helper tao user va role that, tranh phu thuoc vao account co san trong DB.
        Role role = roleRepository.saveAndFlush(Role.builder().name(roleName).build());
        User user = User.builder()
                .fullName("Return DB User")
                .phoneNumber(uniquePhone())
                .email(unique("return") + "@example.com")
                .password("secret")
                .active(true)
                .role(role)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private String bearerToken(User user) throws Exception {
        // Tao JWT that bang JwtTokenUtils de service doc userId nhu flow backend that.
        return "Bearer " + jwtTokenUtils.generateToken(user);
    }

    private AdminReturnActionDTO adminAction(String notes) {
        // Tao DTO hanh dong cua admin cho approve/reject/refund.
        AdminReturnActionDTO dto = new AdminReturnActionDTO();
        dto.setAdminNotes(notes);
        return dto;
    }

    private String unique(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    private String uniquePhone() {
        return String.valueOf(9100000000L + Math.abs(System.nanoTime() % 89999999L)).substring(0, 10);
    }
}

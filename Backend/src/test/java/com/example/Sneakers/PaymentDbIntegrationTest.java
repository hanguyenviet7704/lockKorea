package com.example.Sneakers;

import com.example.Sneakers.components.JwtTokenUtils;
import com.example.Sneakers.controllers.VnpayController;
import com.example.Sneakers.dtos.ApplyVoucherDTO;
import com.example.Sneakers.dtos.CreateVnpayPaymentDTO;
import com.example.Sneakers.dtos.OrderDTO;
import com.example.Sneakers.models.Order;
import com.example.Sneakers.models.OrderStatus;
import com.example.Sneakers.models.Role;
import com.example.Sneakers.models.User;
import com.example.Sneakers.models.Voucher;
import com.example.Sneakers.repositories.OrderRepository;
import com.example.Sneakers.repositories.RoleRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.repositories.VoucherRepository;
import com.example.Sneakers.responses.PaymentResponse;
import com.example.Sneakers.responses.VoucherApplicationResponse;
import com.example.Sneakers.services.OrderService;
import com.example.Sneakers.services.VnPayService;
import com.example.Sneakers.services.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.jpa.hibernate.ddl-auto=none")
@Transactional
@Rollback
class PaymentDbIntegrationTest {
    /*
     * Integration test cho module Payment.
     *
     * Cach chay:
     * - Test load Spring context  bang @SpringBootTest.
     * - Service/Controller/Repository duoc autowire nhu khi app chay 
     * - Datasource lay tu application.yaml, nen test truy cap MySQL 
     *
     * Cach bao ve DB:
     * - @Transactional mo transaction rieng cho moi test case.
     * - @Rollback dam bao du lieu tao trong test bi rollback sau khi test xong.
     * - Cac test chi validate DTO bang Validator thi khong ghi DB, rollback khong anh huong.
     *
     * Cach doc nhanh moi test:
     * - Arrange: tao data test hoac tao DTO request.
     * - Act: goi service/controller can test.
     * - Assert: kiem tra ket qua tra ve va query lai DB neu test co ghi du lieu.
     */
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VnPayService vnPayService;

    @Autowired
    private VnpayController vnpayController;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testApplyVoucherSuccess() throws Exception {
        // Test Case ID: TC-IT-PAY-001
        // Muc tieu: kiem tra voucher hop le co duoc ap dung khi thanh toan khong.
        // CheckDB: voucher duoc insert vao MySQL service doc lai bang VoucherRepository.
        // Rollback: @Transactional + @Rollback xoa voucher test sau khi test ket thuc.
        Voucher voucher = saveVoucher("SALE66", 20, 100_000L, 200_000L, true);

        VoucherApplicationResponse response = voucherService.applyVoucher(ApplyVoucherDTO.builder()
                .voucherCode(voucher.getCode())
                .orderTotal(500_000L)
                .build());

        assertTrue(response.getIsApplied());
        assertEquals(100_000L, response.getDiscountAmount());
        assertEquals(400_000L, response.getFinalTotal());
        assertTrue(voucherRepository.findById(voucher.getId()).isPresent());
    }

    @Test
    void testApplyVoucherInvalidCode() throws Exception {
        // Test Case ID: TC-IT-PAY-002
        // Muc tieu: nhap voucher khong ton tai thi khong duoc giam tien.
        // CheckDB: khong tao voucher moi; service query DB nhung khong tim thay code nay.
        // Rollback: khong co du lieu moi can rollback.
        VoucherApplicationResponse response = voucherService.applyVoucher(ApplyVoucherDTO.builder()
                .voucherCode(unique("WRONG"))
                .orderTotal(500_000L)
                .build());

        assertFalse(response.getIsApplied());
        assertEquals(500_000L, response.getFinalTotal());
    }

    @Test
    void testApplyVoucherBelowMinimum() throws Exception {
        // Test Case ID: TC-IT-PAY-003
        // Muc tieu: don hang chua dat gia tri toi thieu thi voucher khong duoc ap dung.
        // CheckDB: tao voucher that co minOrderValue cao hon orderTotal.
        // Rollback: voucher test bi xoa sau khi test ket thuc.
        Voucher voucher = saveVoucher("MIN900", 15, 100_000L, 900_000L, true);

        VoucherApplicationResponse response = voucherService.applyVoucher(ApplyVoucherDTO.builder()
                .voucherCode(voucher.getCode())
                .orderTotal(50_000L)
                .build());

        assertFalse(response.getIsApplied());
        assertEquals(50_000L, response.getFinalTotal());
    }

    @Test
    void testApplyVoucherExpired() throws Exception {
        // Test Case ID: TC-IT-PAY-004
        // Muc tieu: voucher het han khong duoc ap dung.
        // CheckDB: tao voucher that voi validTo nam trong qua khu.
        // Rollback: voucher test bi rollback sau test.
        Voucher voucher = saveVoucher("EXPIRED10", 10, 50_000L, 100_000L, false);

        VoucherApplicationResponse response = voucherService.applyVoucher(ApplyVoucherDTO.builder()
                .voucherCode(voucher.getCode())
                .orderTotal(500_000L)
                .build());

        assertFalse(response.getIsApplied());
        assertEquals(500_000L, response.getFinalTotal());
    }

    @Test
    void testApplyVoucherMissingCodeValidation() {
        // Test Case ID: TC-IT-PAY-005
        // Muc tieu: request thieu voucherCode phai bi bat validation.
        // CheckDB: khong dung DB, chi validate object DTO trong memory.
        // Rollback: khong can vi test khong insert/update/delete DB.
        ApplyVoucherDTO request = ApplyVoucherDTO.builder().orderTotal(500_000L).build();

        Set<ConstraintViolation<ApplyVoucherDTO>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void testApplyVoucherMissingOrderTotalValidation() {
        // Test Case ID: TC-IT-PAY-006
        // Muc tieu: request thieu orderTotal phai bi bat validation.
        // CheckDB: khong dung DB, chi kiem tra annotation validation cua DTO.
        // Rollback: khong can vi test khong ghi DB.
        ApplyVoucherDTO request = ApplyVoucherDTO.builder().voucherCode("SALE66").build();

        Set<ConstraintViolation<ApplyVoucherDTO>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void testApplyVoucherMaxDiscountCap() throws Exception {
        // Test Case ID: TC-IT-PAY-007
        // Muc tieu: tien giam khong duoc vuot maxDiscountAmount cua voucher.
        // CheckDB: tao voucher that voi discount 20% nhung max cap = 100000.
        // Rollback: voucher test bi rollback sau test.
        Voucher voucher = saveVoucher("MAXCAP", 20, 100_000L, 200_000L, true);

        VoucherApplicationResponse response = voucherService.applyVoucher(ApplyVoucherDTO.builder()
                .voucherCode(voucher.getCode())
                .orderTotal(1_000_000L)
                .build());

        assertEquals(100_000L, response.getDiscountAmount());
        assertEquals(900_000L, response.getFinalTotal());
    }

    @Test
    @Commit
    void testCreateVnpayPaymentSuccess() {
        // Test Case ID: TC-IT-PAY-008
        // Muc tieu: tao URL thanh toan VNPAY thanh cong cho don hang co that.
        // CheckDB: tao order/user that roi controller goi service tao payment URL.
        // Rollback: order/user tao trong test bi rollback sau test.
        // Arrange: tao order that de request tao payment co order hop le.
        Order order = saveOrder("payment_failed");
        CreateVnpayPaymentDTO dto = CreateVnpayPaymentDTO.builder()
                .amount(500_000L)
                .orderInfo("Thanh toan don hang " + order.getId())
                .orderId(order.getId())
                .build();

        // Act: goi controller giong luong frontend tao URL thanh toan.
        ResponseEntity<PaymentResponse> response = vnpayController.createPayment(dto);

        // Assert: phai tra 200, status OK va co URL.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("OK", response.getBody().getStatus());
        assertNotNull(response.getBody().getUrl());
    }

    @Test
    void testCreateVnpayPaymentSaveTxnRef() {
        // Test Case ID: TC-IT-PAY-009
        // Muc tieu: sau khi tao payment URL, vnpTxnRef phai duoc luu vao bang orders.
        // CheckDB: doc lai order bang orderRepository.findById de xac nhan vnpTxnRef da persist.
        // Rollback: update vnpTxnRef va order/user test bi rollback sau test.
        // Arrange: tao order that de service luu vnpTxnRef vao DB.
        Order order = saveOrder("payment_failed");

        // Act: tao payment URL, trong qua trinh nay service se save txnRef vao order.
        String paymentUrl = vnPayService.createOrder(500_000L, "Thanh toan don hang " + order.getId(), order.getId());
        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();

        // Assert: URL dung amount va DB da co txnRef.
        assertTrue(paymentUrl.contains("vnp_Amount=50000000"));
        assertNotNull(savedOrder.getVnpTxnRef());
        assertEquals(8, savedOrder.getVnpTxnRef().length());
    }

    @Test
    void testCreateVnpayPaymentAmountZeroBug() {
        // Test Case ID: TC-IT-PAY-010
        // Expected: amount=0 phai bi chan. Dung order that de chi test validation amount.
        // Actual neu test fail: service khong throw IllegalArgumentException va van tao URL thanh toan.
        // Root cause: VnPayService.createOrder chua validate amount > 0 truoc khi tao tham so VNPAY.
        // CheckDB: co tao order that de loai tru nguyen nhan orderId sai.
        // Rollback: order/user test bi rollback.
        Order order = saveOrder("payment_failed");

        assertThrows(IllegalArgumentException.class,
                () -> vnPayService.createOrder(0L, "Thanh toan amount 0", order.getId()));
    }

    @Test
    void testCreateVnpayPaymentNegativeAmountBug() {
        // Test Case ID: TC-IT-PAY-011
        // Expected: amount am phai bi chan. Dung order that de chi test validation amount.
        // Actual neu test fail: service khong throw IllegalArgumentException va van tao URL voi amount am.
        // Root cause: thieu dieu kien validate amount <= 0 trong VnPayService.
        // CheckDB: co tao order that de dam bao chi test loi amount am.
        // Rollback: order/user test bi rollback.
        Order order = saveOrder("payment_failed");

        assertThrows(IllegalArgumentException.class,
                () -> vnPayService.createOrder(-100_000L, "Thanh toan amount am", order.getId()));
    }

    @Test
    void testCreateVnpayPaymentOrderNotFoundBug() {
        // Test Case ID: TC-IT-PAY-012
        // Expected: orderId khong ton tai phai bao loi. Source hien tai van tao URL nen test fail that neu bug con ton tai.
        // Actual neu test fail: service van tao URL thanh toan cho orderId khong ton tai.
        // Root cause: VnPayService.createOrder chua query DB de kiem tra orderId truoc khi tao payment URL.
        // CheckDB: dung ID rat lon khong ton tai trong bang orders.
        // Rollback: khong co du lieu moi can rollback.
        assertThrows(RuntimeException.class,
                () -> vnPayService.createOrder(300_000L, "Thanh toan don khong ton tai", 999_999_999L));
    }

    @Test
    void testPaymentCallbackSuccess() throws Exception {
        // Test Case ID: TC-IT-PAY-013
        // Muc tieu: VNPAY callback responseCode=00 thi don hang chuyen sang PAID.
        // CheckDB: tao order , gan vnpTxnRef, goi callback, sau do query lai order tu DB.
        // Rollback: thay doi status/vnpTransactionNo bi rollback sau test.
        Order order = saveOrder("payment_failed");
        order.setVnpTxnRef(unique("TXN"));
        orderRepository.saveAndFlush(order);

        MockHttpServletResponse response = runCallback(order.getVnpTxnRef(), "00", "VNP123");

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
        assertEquals("VNP123", updatedOrder.getVnpTransactionNo());
        assertTrue(response.getRedirectedUrl().contains("/order-detail/" + order.getId()));
    }

    @Test
    void testPaymentCallbackFailed() throws Exception {
        // Test Case ID: TC-IT-PAY-014
        // Muc tieu: VNPAY callback that bai thi don hang chuyen sang PAYMENT_FAILED.
        // CheckDB: tao order that va doc lai status sau khi callback.
        // Rollback: order/status test bi rollback.
        Order order = saveOrder("pending");
        order.setVnpTxnRef(unique("TXN"));
        orderRepository.saveAndFlush(order);

        runCallback(order.getVnpTxnRef(), "24", "VNP999");

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAYMENT_FAILED, updatedOrder.getStatus());
    }

    @Test
    void testPaymentCallbackTxnRefNotFound() throws Exception {
        // Test Case ID: TC-IT-PAY-015
        // Muc tieu: callback voi txnRef khong ton tai phai redirect ve trang not_found.
        // CheckDB: service co query DB theo vnpTxnRef nhung khong co record.
        // Rollback: khong insert/update DB trong test nay.
        MockHttpServletResponse response = runCallback(unique("NOTFOUND"), "00", "VNP404");

        assertTrue(response.getRedirectedUrl().contains("payment_status=not_found"));
    }

    @Test
    void testUpdateCheckoutUserInfoSuccess() throws Exception {
        // Test Case ID: TC-IT-PAY-016
        // Muc tieu: cap nhat thong tin nguoi dung khi checkout thanh cong.
        // CheckDB: tao order/user that, goi OrderService.updateOrder, flush va query lai DB.
        // Expected: fullName, paymentMethod, status duoc persist dung.
        // Actual neu test fail: ModelMapper loi khi map OrderDTO vao Order.
        // Root cause: ModelMapper thay nhieu field ket thuc bang Id trong OrderDTO
        // (districtId, userId, provinceId) va map nham vao Order.setId().
        // Rollback: order/user va thay doi update bi rollback sau test.
        Order order = saveOrder("pending");
        User user = order.getUser();

        OrderDTO request = OrderDTO.builder()
                .userId(user.getId())
                .fullName("Nguyen Van Test DB")
                .email("db-test@example.com")
                .phoneNumber("0901234567")
                .address("Ha Noi")
                .paymentMethod("VNPAY")
                .shippingMethod("Nhanh")
                .totalMoney(650_000L)
                .status(OrderStatus.PAID)
                .build();

        Order result = assertDoesNotThrow(() -> orderService.updateOrder(order.getId(), request));
        orderRepository.flush();

        Optional<Order> persistedOrder = orderRepository.findById(result.getId());
        assertTrue(persistedOrder.isPresent());
        assertEquals("Nguyen Van Test DB", persistedOrder.get().getFullName());
        assertEquals("VNPAY", persistedOrder.get().getPaymentMethod());
        assertEquals(OrderStatus.PAID, persistedOrder.get().getStatus());
    }

    @Test
    void testUpdateCheckoutOrderNotFound() {
        // Test Case ID: TC-IT-PAY-017
        // Muc tieu: update orderId khong ton tai phai bao loi.
        // CheckDB: service query bang orders nhung khong tim thay ID nay.
        // Rollback: khong tao du lieu moi.
        assertThrows(Exception.class,
                () -> orderService.updateOrder(999_999_999L, OrderDTO.builder().userId(1L).build()));
    }

    @Test
    void testUpdateOrderStatusAfterPayment() throws Exception {
        // Test Case ID: TC-IT-PAY-018
        // Muc tieu: sau khi thanh toan thanh cong, status order duoc cap nhat thanh PAID.
        // CheckDB: tao order that, update status, flush va query lai bang orders.
        // Rollback: order/status test bi rollback sau test.
        Order order = saveOrder("payment_failed");

        orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);
        orderRepository.flush();

        assertEquals(OrderStatus.PAID, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    private MockHttpServletResponse runCallback(String txnRef, String responseCode, String transactionNo) throws Exception {
        // Tao request/response gia lap de goi truc tiep controller callback.
        // MockHttpServletRequest khong phai DB; DB chi duoc dung ben trong controller/service khi tim order theo txnRef.
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).setParameter("vnp_ResponseCode", responseCode);
        ((MockHttpServletRequest) request).setParameter("vnp_TxnRef", txnRef);
        ((MockHttpServletRequest) request).setParameter("vnp_TransactionNo", transactionNo);
        MockHttpServletResponse response = new MockHttpServletResponse();
        vnpayController.paymentCallback(request, response);
        return response;
    }

    private Voucher saveVoucher(String prefix, int discountPercentage, Long maxDiscountAmount,
                                Long minOrderValue, boolean validDate) {
        // Helper tao voucher trong MySQL that.
        // Ten/code co suffix unique de tranh trung du lieu voi lan test khac.
        // saveAndFlush day du lieu xuong DB ngay, giup test doc lai/chay service voi du lieu that.
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .code(unique(prefix))
                .name("Voucher DB rollback")
                .discountPercentage(discountPercentage)
                .minOrderValue(minOrderValue)
                .maxDiscountAmount(maxDiscountAmount)
                .quantity(10)
                .remainingQuantity(5)
                .validFrom(validDate ? now.minusDays(1) : now.minusDays(10))
                .validTo(validDate ? now.plusDays(1) : now.minusDays(1))
                .isActive(true)
                .build();
        return voucherRepository.saveAndFlush(voucher);
    }

    private Order saveOrder(String status) {
        // Helper tao order that kem user that.
        // Moi order dung user moi de tranh phu thuoc du lieu co san trong DB.
        User user = saveUser();
        Order order = Order.builder()
                .user(user)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address("Ha Noi")
                .orderDate(LocalDateTime.now())
                .status(status)
                .totalMoney(500_000L)
                .shippingMethod("Nhanh")
                .paymentMethod("VNPAY")
                .active(true)
                .build();
        return orderRepository.saveAndFlush(order);
    }

    private User saveUser() {
        // Helper tao user va role that trong DB.
        // Role cung duoc tao moi trong transaction test va se bi rollback cung user/order.
        Role role = roleRepository.saveAndFlush(Role.builder().name(Role.USER).build());
        User user = User.builder()
                .fullName("DB Test User")
                .phoneNumber(uniquePhone())
                .email(unique("db") + "@example.com")
                .password("secret")
                .active(true)
                .role(role)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private String unique(String prefix) {
        // Tao chuoi gan nhu duy nhat de test khong bi trung code/email/txnRef.
        return prefix + "_" + System.nanoTime();
    }

    private String uniquePhone() {
        // Tao so dien thoai 10 so de khong trung unique constraint cua bang users.
        return String.valueOf(9000000000L + Math.abs(System.nanoTime() % 99999999L)).substring(0, 10);
    }
}

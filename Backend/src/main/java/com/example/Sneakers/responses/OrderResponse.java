package com.example.Sneakers.responses;

import com.example.Sneakers.models.Order;
import com.example.Sneakers.models.OrderDetail;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("fullname")
    private String fullname;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("email")
    private String email;

    @JsonProperty("address")
    private String address;

    @JsonProperty("note")
    private String note;

    @JsonProperty("order_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderDate;

    @JsonProperty("status")
    private String status;

    @JsonProperty("total_money")
    private Long totalMoney;

    @JsonProperty("shipping_method")
    private String shippingMethod;

    @JsonProperty("shipping_date")
    private LocalDate shippingDate;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("voucher")
    private VoucherInfo voucher;

    @JsonProperty("discount_amount")
    private Long discountAmount;

    @JsonProperty("order_details")
    private List<OrderDetailResponse> orderDetails;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("total_products")
    private int totalProducts;

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("carrier")
    private String carrier;

    @JsonProperty("tracking_info")
    private Object trackingInfo;

    @JsonProperty("district_id")
    private Integer districtId;

    @JsonProperty("ward_code")
    private String wardCode;

    @JsonProperty("assigned_staff")
    private StaffInfo assignedStaff;

    @JsonProperty("payment_intent_id")
    private String paymentIntentId;

    @JsonProperty("vnp_txn_ref")
    private String vnpTxnRef;

    @JsonProperty("vnp_transaction_no")
    private String vnpTransactionNo;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VoucherInfo {
        private String code;
        private String name;
        @JsonProperty("discount_percentage")
        private Integer discountPercentage;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StaffInfo {
        private Long id;
        private String fullname;
        @JsonProperty("phone_number")
        private String phoneNumber;
    }

    public static OrderResponse fromOrder(Order order) {
        List<OrderDetail> orderDetails = order.getOrderDetails();
        if (orderDetails == null) {
            orderDetails = new java.util.ArrayList<>();
        }

        List<OrderDetailResponse> orderDetailResponses = orderDetails.stream()
                .filter(od -> od.getProduct() != null)
                .map(OrderDetailResponse::fromOrderDetail)
                .collect(Collectors.toList());

        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .fullname(order.getFullName())
                .phoneNumber(order.getPhoneNumber())
                .email(order.getEmail())
                .address(order.getAddress())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalMoney(order.getTotalMoney())
                .shippingMethod(order.getShippingMethod())
                .shippingDate(order.getShippingDate())
                .paymentMethod(order.getPaymentMethod())
                .discountAmount(order.getDiscountAmount())
                .orderDetails(orderDetailResponses)
                .trackingNumber(order.getTrackingNumber())
                .carrier(order.getCarrier())
                .districtId(order.getDistrictId())
                .wardCode(order.getWardCode())
                .paymentIntentId(order.getPaymentIntentId())
                .vnpTxnRef(order.getVnpTxnRef())
                .vnpTransactionNo(order.getVnpTransactionNo());

        // Map voucher information if present
        if (order.getVoucher() != null) {
            VoucherInfo voucherInfo = VoucherInfo.builder()
                    .code(order.getVoucher().getCode())
                    .name(order.getVoucher().getName())
                    .discountPercentage(order.getVoucher().getDiscountPercentage())
                    .build();
            builder.voucher(voucherInfo);
        }

        // Map assigned staff information if present
        if (order.getAssignedStaff() != null) {
            StaffInfo staffInfo = StaffInfo.builder()
                    .id(order.getAssignedStaff().getId())
                    .fullname(order.getAssignedStaff().getFullName())
                    .phoneNumber(order.getAssignedStaff().getPhoneNumber())
                    .build();
            builder.assignedStaff(staffInfo);
        }

        OrderResponse orderResponse = builder.build();

        // Set product name and total products if orderDetails not empty
        if (!orderDetails.isEmpty()) {
            String productNames = orderDetails.stream()
                    .filter(od -> od.getProduct() != null)
                    .map(od -> od.getProduct().getName())
                    .collect(Collectors.joining(", "));
            orderResponse.setProductName(productNames);

            long totalProducts = orderDetails.stream()
                    .mapToLong(OrderDetail::getNumberOfProducts)
                    .sum();
            orderResponse.setTotalProducts((int) totalProducts);
        } else {
            orderResponse.setProductName("N/A");
            orderResponse.setTotalProducts(0);
        }

        return orderResponse;
    }
}
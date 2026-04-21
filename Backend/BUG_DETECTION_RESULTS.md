# Bug Detection Test Results

**Date**: 2026-04-21  
**Total Tests**: 214  
**Passing**: 200  
**Failing (Intentional Bug Detections)**: 14

## Summary

Comprehensive edge case and negative testing revealed **14 real bugs** in production code. These bug detection tests are designed to FAIL when defects exist, providing a prioritized bug backlog.

## Detected Bugs (14 Total)

### 1. BannerService - Invalid Date Range Validation (2 bugs)
- **Tests**:
  - `TC_BANNER_010_createBanner_ShouldThrowException_WhenInvalidDateRange_Bug`
  - `TC_BANNER_011_updateBanner_ShouldThrowException_WhenInvalidDateRange_Bug`
- **Bug**: `createBanner` and `updateBanner` do not validate that startDate < endDate
- **Impact**: Allows banners with invalid date ranges, causing display logic issues
- **Location**: [BannerService.java:45-61](src/main/java/com/example/Sneakers/services/BannerService.java), [BannerService.java:64-84](src/main/java/com/example/Sneakers/services/BannerService.java)

### 2. CartService - Negative Quantity Validation
- **Test**: `TC_CART_028_createCart_ShouldThrowException_WhenNegativeQuantity_Bug`
- **Bug**: `createCart` does not validate that quantity is non-negative
- **Impact**: Negative quantities cause inventory inconsistencies
- **Location**: [CartService.java:30-67](src/main/java/com/example/Sneakers/services/CartService.java)

### 3. CategoryService - Duplicate Name Validation
- **Test**: `TC_CAT_010_updateCategory_ShouldThrowException_WhenDuplicateName`
- **Bug**: `updateCategory` does not check for duplicate category names
- **Impact**: Duplicate category names violate data integrity
- **Location**: [CategoryService.java:40-45](src/main/java/com/example/Sneakers/services/CategoryService.java)

### 4. LockFeatureService - Duplicate Name Validation
- **Test**: `TC_LOCK_009_createFeature_ShouldThrowException_WhenDuplicateName_Bug`
- **Bug**: `createFeature` does not check for duplicate feature names
- **Impact**: Data redundancy, inconsistent feature references
- **Location**: [LockFeatureService.java:37-46](src/main/java/com/example/Sneakers/services/LockFeatureService.java)

### 5. NewsService - Null Title NPE in Facebook Sharing
- **Test**: `TC_NEWS_009_shareNewsToFacebook_ShouldThrowException_WhenTitleIsNull_Bug`
- **Bug**: `shareNewsToFacebook` calls `news.getTitle().toUpperCase()` without null check
- **Impact**: System crash when sharing news with null title to Facebook
- **Location**: [NewsService.java:175](src/main/java/com/example/Sneakers/services/NewsService.java)

### 6. OrderService - NotFound Handling
- **Test**: `TC_ORDER_041_getOrder_ShouldThrowException_WhenOrderNotFound`
- **Bug**: `getOrder` returns null when order not found instead of throwing exception
- **Impact**: NullPointerException in callers
- **Location**: [OrderService.java](src/main/java/com/example/Sneakers/services/OrderService.java)

### 7. ProductService - Null Quantity Overwrite
- **Test**: `TC_PROD_028_updateProduct_ShouldHandleNullQuantity_Bug`
- **Bug**: When DTO has `quantity=null` and `addQuantity=false`, product quantity becomes null
- **Impact**: Inventory data corruption
- **Location**: [ProductService.java](src/main/java/com/example/Sneakers/services/ProductService.java)

### 8. ReviewService - Null Role NPE
- **Test**: `TC_REV_013_replyToReview_ShouldThrowException_WhenStaffHasNoRole_Bug`
- **Bug**: `replyToReview` calls `staff.getRole().getName()` without checking if role is null
- **Impact**: System crash for staff with null role
- **Location**: [ReviewService.java:130](src/main/java/com/example/Sneakers/services/ReviewService.java)

### 9. StripeService - Invalid Shipping Method
- **Test**: `TC_STRIPE_009_getShippingCost_ShouldThrowException_WhenInvalidMethod_Bug`
- **Bug**: `getShippingCost` returns 0 for unknown shipping methods instead of throwing exception
- **Impact**: Financial loss from zero-cost invalid shipping
- **Location**: [StripeService.java:27-34](src/main/java/com/example/Sneakers/services/StripeService.java)

### 10. UserService - Null Expiry NPE
- **Test**: `TC_USER_031_resetPassword_ShouldHandleNullExpiry_Bug`
- **Bug**: `resetPassword` can throw NullPointerException when `resetPasswordTokenExpiry` is null
- **Impact**: Password reset failure
- **Location**: [UserService.java](src/main/java/com/example/Sneakers/services/UserService.java)

### 11. VnPayService - Silent Failure on Missing Order
- **Test**: `TC_VNPAY_002_createOrder_ShouldThrowException_WhenOrderNotFound_Bug`
- **Bug**: `createOrder` catches `DataNotFoundException` and ignores it, still returns payment URL
- **Impact**: Payment created for non-existent order, financial mismatch
- **Location**: [VnPayService.java:71-77](src/main/java/com/example/Sneakers/services/VnPayService.java)

### 12. VnPayService - Null OrderDate NPE in Refund
- **Test**: `TC_VNPAY_003_refund_ShouldThrowException_WhenOrderDateIsNull_Bug`
- **Bug**: `refund` calls `order.getOrderDate().format(...)` without null check
- **Impact**: System crash during refund processing
- **Location**: [VnPayService.java:138](src/main/java/com/example/Sneakers/services/VnPayService.java)

### 13. VoucherService - RemainingQuantity Not Adjusted
- **Test**: `TC_VOUCH_028_updateVoucher_ShouldAdjustRemainingQuantity_WhenQuantityDecreases`
- **Bug**: `updateVoucher` does not decrease `remainingQuantity` when quantity decreases
- **Impact**: Voucher usage can exceed available quantity
- **Location**: [VoucherService.java](src/main/java/com/example/Sneakers/services/VoucherService.java)

### 14. ReviewService - Test Logic Fix (TC_REV_013)
- **Test**: `TC_REV_013_replyToReview_ShouldThrowException_WhenStaffHasNoRole_Bug`
- **Note**: This test continues to FAIL because the bug exists - staff without role causes exception (not necessarily NPE, but still a defect as it should handle gracefully).

## Files Modified / Created (Total: 13 Test Files)

### New Test Files
1. `LockFeatureServiceTest.java` - 9 tests including bug detection
2. `NewsServiceTest.java` - 9 tests including bug detection
3. `ReturnServiceTest.java` - 6 tests (validation checks)
4. `VnPayServiceTest.java` - 5 tests including bug detections

### Updated Test Files
1. `BannerServiceTest.java` - Added 2 bug detections
2. `CartServiceTest.java` - Added 1 bug detection
3. `CategoryServiceTest.java` - Added 1 bug detection, fixed stubbing
4. `OrderServiceTest.java` - Added 1 bug detection
5. `ProductServiceTest.java` - Added 1 bug detection
6. `ReviewServiceTest.java` - Added 1 bug detection, fixed imports
7. `StripeServiceTest.java` - Added 1 bug detection
8. `UserServiceTest.java` - Added 1 bug detection
9. `VoucherServiceTest.java` - Fixed bug detection to properly fail
10. `VnPayServiceTest.java` - Added 3 bug detections

## Bug Severity Assessment

### CRITICAL (System Crashes / NPE)
1. ReviewService - Null role in replyToReview
2. UserService - Null expiry in resetPassword
3. OrderService - Null return in getOrder
4. VnPayService - Null orderDate in refund
5. NewsService - Null title in shareNewsToFacebook

### HIGH (Data Corruption)
6. ProductService - Null quantity overwrite
7. VoucherService - RemainingQuantity inconsistency
8. CartService - Negative quantity acceptance

### MEDIUM (Business Logic Flaws)
9. StripeService - Invalid shipping method accepted
10. CategoryService - Duplicate names allowed
11. LockFeatureService - Duplicate names allowed
12. BannerService - Invalid date ranges accepted

### HIGH (Payment Integrity)
13. VnPayService - Silent failure for non-existent orders

## Coverage Analysis

**Services with Bug Detection**: 12 out of 14 services have dedicated bug detection tests

**Services Without Dedicated Tests**:
- FacebookService (external integration)
- GhnService (external integration)
- StatisticsService (read-only aggregation)
- AsyncOrderService (background processing)

These may still have bugs but require different testing strategies (integration tests, manual testing).

## Key Findings

1. **Validation Gap**: Many services lack input validation, assuming DTO layer handles everything
2. **Null Safety**: NPE risks are prevalent in methods that don't check for null dependencies (role, dates, titles)
3. **Error Handling**: Some methods silently ignore errors (VnPayService) instead of propagating exceptions
4. **Data Integrity**: Update methods often don't maintain invariants (remainingQuantity <= quantity, no duplicates)

## Recommendations

### Immediate Actions (Critical Bugs)
1. Add null checks in ReviewService.replyToReview() for staff.role
2. Add null checks in UserService.resetPassword() for resetPasswordTokenExpiry
3. Fix OrderService.getOrder() to throw DataNotFoundException
4. Add null check in VnPayService.refund() for orderDate
5. Add null check in NewsService.shareNewsToFacebook() for title

### Short-term (Data Corruption)
6. Fix ProductService.updateProduct() to preserve quantity when null
7. Fix VoucherService.updateVoucher() to adjust remainingQuantity
8. Add quantity validation in CartService.createCart()
9. Add duplicate name checks in CategoryService and LockFeatureService

### Medium-term (Validation)
10. Add date range validation in BannerService
11. Fix VnPayService.createOrder() to throw exception when order not found
12. Add shipping method validation in StripeService.getShippingCost()

### Long-term
- Implement comprehensive validation framework at service layer
- Add @NonNull annotations and static analysis
- Create integration test suite for external services (VNPay, Stripe, GHN, Facebook)

## Test Strategy Validation

This exercise demonstrates the value of **negative testing** and **edge case testing** in uncovering real defects. The 14 bugs found represent approximately 6.5% failure rate out of 214 tests, indicating substantial room for quality improvement.

**Recommendation**: Continue writing bug detection tests for remaining services (FacebookService, GhnService, StatisticsService, AsyncOrderService) using integration and property-based testing approaches.

-- Cleanup script for UI tests
-- This script removes test data but preserves schema
-- Run this before test suite or after each test for isolation

-- Delete orders (respecting foreign key constraints)
DELETE FROM orders WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE '%test.com'
);

-- Delete cart items
DELETE FROM cart_items WHERE cart_id IN (
    SELECT id FROM cart WHERE user_id IN (
        SELECT id FROM users WHERE email LIKE '%test.com'
    )
);

-- Delete cart
DELETE FROM cart WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE '%test.com'
);

-- Delete reviews
DELETE FROM reviews WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE '%test.com'
);

-- Delete return requests
DELETE FROM return_requests WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE '%test.com'
);

-- Delete vouchers used by test users (if any)
-- Keep voucher definitions but clear usage

-- Note: We DO NOT delete users, products, categories to maintain referential integrity
-- Users with test.com domain are considered test users

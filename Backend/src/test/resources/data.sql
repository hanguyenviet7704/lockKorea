-- Insert test roles
INSERT INTO roles (id, name) VALUES (1, 'USER');
INSERT INTO roles (id, name) VALUES (2, 'ADMIN');
INSERT INTO roles (id, name) VALUES (3, 'STAFF');

-- Insert test user with BCrypt encoded password for "password123"
INSERT INTO users (id, phone_number, password, fullname, email, role_id, active)
VALUES (1, '0123456789', '$2a$10$N.zmdr9k7UOCG3aZHWeDGuWDzH7IEEzd4gNV.f8CoE3FPMI8SGzQK', 'Test User', 'test@example.com', 1, true);

-- Insert test category
INSERT INTO categories (id, name, description) VALUES (1, 'Sneakers', 'Test category');

-- Insert test product
INSERT INTO products (id, name, description, price, quantity, category_id, sold_quantity, rating)
VALUES (1, 'Test Sneakers', 'Test product description', 100000, 50, 1, 10, 4.5);

-- Insert test voucher
INSERT INTO vouchers (id, code, name, discount_percentage, min_order_value, max_discount_amount, remaining_quantity, valid_from, valid_to, is_active)
VALUES (1, 'TEST10', 'Test Voucher', 10, 100000, 50000, 100, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, true);

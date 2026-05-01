-- Seed data for project reset (Admin only)
-- Use this script to clear all data and start with a fresh Admin account.
-- Default credentials: admin / password

SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1) USER MICROSERVICE (DB: userservice)
-- =========================================================
USE userservice;

DELETE FROM restaurants;
DELETE FROM email_verification_otp;
DELETE FROM outbox_events;
DELETE FROM users;

-- Insert Admin Account
INSERT INTO users (
    id, username, email, password, role, approved, active, email_verified,
    full_name, phone, address
) VALUES (
    1,
    'admin',
    'admin@example.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGFJZ3.Dym3eKxK1J46.', -- Password: password
    'ADMIN',
    1,
    1,
    1,
    'System Administrator',
    '0900000001',
    'System Base'
);

-- =========================================================
-- 2) PRODUCT MICROSERVICE (DB: productmicroservice)
-- =========================================================
USE productmicroservice;

DELETE FROM stock_deduction_records;
DELETE FROM products;

-- =========================================================
-- 3) ORDER MICROSERVICE (DB: orderservice)
-- =========================================================
USE orderservice;

DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM user_addresses;
DELETE FROM idempotency_keys;
DELETE FROM outbox_events;

-- =========================================================
-- 4) DRONE MICROSERVICE (DB: droneservice)
-- =========================================================
USE droneservice;

DELETE FROM drone_missions;
DELETE FROM drones;
DELETE FROM outbox_events;

-- =========================================================
-- 5) PAYMENT MICROSERVICE (DB: paymentservice)
-- =========================================================
USE paymentservice;

DELETE FROM payments;
DELETE FROM idempotency_keys;

-- =========================================================
-- 6) NOTIFICATION MICROSERVICE (DB: notificationservice)
-- =========================================================
USE notificationservice;

DELETE FROM email_notifications;

SET FOREIGN_KEY_CHECKS = 1;

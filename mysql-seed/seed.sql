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
    id, username, email, password, role, approved, active,
    email_verified, email_undeliverable, bounce_count, pending_email, last_bounce_at,
    full_name, phone, address, avatar,
    restaurant_name, restaurant_address, restaurant_image, opening_hours
) VALUES
    -- Admin account (id = 1)
    (
        1,
        'admin',
        'admin@example.com',
        'CHANGE_ME_BCRYPT_ADMIN',     -- BCrypt hash for admin password
        'ADMIN',
        1,
        1,
        1, -- email_verified
        0, -- email_undeliverable
        0, -- bounce_count
        NULL, -- pending_email
        NULL, -- last_bounce_at
        'System Administrator',
        '0900000001',
        '123 Admin Street, Hanoi',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL
    ),

    -- Merchant 1 (id = 2)
    (
        2,
        'merchant1',
        'merchant1@example.com',
        'CHANGE_ME_BCRYPT_MERCHANT1', -- BCrypt hash for merchant1 password
        'MERCHANT',
        1,
        1,
        1, -- email_verified
        0, -- email_undeliverable
        0, -- bounce_count
        NULL, -- pending_email
        NULL, -- last_bounce_at
        'Merchant One',
        '0900000002',
        '456 Merchant Road, Hanoi',
        NULL,
        'Merchant 1 Restaurant',
        '456 Merchant Road, Hanoi',
        'https://example.com/images/merchant1.png',
        '08:00-22:00'
    ),

    -- Merchant 2 (id = 3)
    (
        3,
        'merchant2',
        'merchant2@example.com',
        'CHANGE_ME_BCRYPT_MERCHANT2', -- BCrypt hash for merchant2 password
        'MERCHANT',
        1,
        1,
        1, -- email_verified
        0, -- email_undeliverable
        0, -- bounce_count
        NULL, -- pending_email
        NULL, -- last_bounce_at
        'Merchant Two',
        '0900000003',
        '789 Merchant Avenue, Hanoi',
        NULL,
        'Merchant 2 Restaurant',
        '789 Merchant Avenue, Hanoi',
        'https://example.com/images/merchant2.png',
        '09:00-23:00'
    ),

    -- Normal User 1 (id = 4)
    (
        4,
        'user1',
        'user1@example.com',
        'CHANGE_ME_BCRYPT_USER1',     -- BCrypt hash for user1 password
        'USER',
        1,
        1,
        1, -- email_verified
        0, -- email_undeliverable
        0, -- bounce_count
        NULL, -- pending_email
        NULL, -- last_bounce_at
        'User One',
        '0900000004',
        '10 User Street, Hanoi',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL
    ),

    -- Normal User 2 (id = 5)
    (
        5,
        'user2',
        'user2@example.com',
        'CHANGE_ME_BCRYPT_USER2',     -- BCrypt hash for user2 password
        'USER',
        1,
        1,
        1, -- email_verified
        0, -- email_undeliverable
        0, -- bounce_count
        NULL, -- pending_email
        NULL, -- last_bounce_at
        'User Two',
        '0900000005',
        '20 User Street, Hanoi',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL
    ),

    -- Normal User 3 (id = 6)
    (
        6,
        'user3',
        'user3@example.com',
        'CHANGE_ME_BCRYPT_USER3',     -- BCrypt hash for user3 password
        'USER',
        1,
        1,
        1, -- email_verified
        0, -- email_undeliverable
        0, -- bounce_count
        NULL, -- pending_email
        NULL, -- last_bounce_at
        'User Three',
        '0900000006',
        '30 User Street, Hanoi',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL
    );


-- 1.2 Restaurants (1 per merchant)
-- Table columns from RestaurantJpaEntity / migration:
-- id, merchant_id, name, description, address, city, district,
-- latitude, longitude, image, phone, email, opening_hours,
-- active, approved, category, delivery_fee, estimated_delivery_time,
-- rating, review_count, created_at, updated_at

INSERT INTO restaurants (
    merchant_id, name, description, address, city, district,
    latitude, longitude, image, phone, email, opening_hours,
    active, approved, category, delivery_fee, estimated_delivery_time,
    rating, review_count, created_at, updated_at
) VALUES
    (
        2,
        'Merchant 1 Restaurant',
        'Fast food restaurant for merchant 1',
        '456 Merchant Road, Hanoi',
        'Hanoi',
        'Dong Da',
        21.027763, 105.834160,
        'https://example.com/images/merchant1_restaurant.png',
        '0900000002',
        'merchant1@example.com',
        '{"monday":"08:00-22:00","tuesday":"08:00-22:00"}',
        1,
        1,
        'FOOD',
        15000.00,
        30,
        4.5,
        10,
        NOW(),
        NOW()
    ),
    (
        3,
        'Merchant 2 Restaurant',
        'Fast food restaurant for merchant 2',
        '789 Merchant Avenue, Hanoi',
        'Hanoi',
        'Cau Giay',
        21.028763, 105.835160,
        'https://example.com/images/merchant2_restaurant.png',
        '0900000003',
        'merchant2@example.com',
        '{"monday":"09:00-23:00","tuesday":"09:00-23:00"}',
        1,
        1,
        'FOOD',
        20000.00,
        35,
        4.2,
        5,
        NOW(),
        NOW()
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

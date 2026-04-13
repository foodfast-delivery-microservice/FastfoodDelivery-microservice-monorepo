-- Seed data for local development
-- NOTE: Replace the placeholder password hashes with real BCrypt hashes
-- that match your desired plaintext passwords (e.g. Admin@123, Merchant@123, User@123).
-- You can generate a BCrypt hash using any online tool or a small Spring Boot snippet
-- with new BCryptPasswordEncoder().encode("yourPassword").

-- =========================================================
-- 1) USERS + RESTAURANTS (DB: userservice)
--    - 1 ADMIN
--    - 2 MERCHANT
--    - 3 USER
--    - 1 RESTAURANT per MERCHANT
-- =========================================================

USE userservice;

-- Clear existing demo data (optional, for idempotent local runs)
DELETE FROM restaurants WHERE merchant_id IN (2, 3);
DELETE FROM users WHERE id IN (1, 2, 3, 4, 5, 6);

-- 1.1 Users
-- Columns inferred from UserJpaEntity + migrations:
-- id, username, email, password, role, approved, active,
-- full_name, phone, address, avatar,
-- restaurant_name, restaurant_address, restaurant_image, opening_hours

INSERT INTO users (
    id, username, email, password, role, approved, active,
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
-- 2) PRODUCTS (DB: productmicroservice)
--    - Each merchant owns 5 products (total 10)
-- =========================================================

USE productmicroservice;

-- Optional cleanup of demo data
DELETE FROM products WHERE merchant_id IN (2, 3);

-- Table columns from ProductJpaEntity:
-- id, name, description, price, stock, category, merchant_id, active, image_url

-- Explicitly set id values to avoid "Field 'id' doesn't have a default value"
INSERT INTO products (
    id, name, description, price, stock, category, merchant_id, active, image_url
) VALUES
    -- ===== Merchant 1: 5 products =====
    (1, 'M1 Burger Classic', 'Classic beef burger from Merchant 1', 50000.00, 100, 'FOOD', 2, 1, 'https://example.com/images/m1_burger_classic.png'),
    (2, 'M1 Cheese Burger', 'Cheese burger from Merchant 1', 55000.00, 80, 'FOOD', 2, 1, 'https://example.com/images/m1_cheese_burger.png'),
    (3, 'M1 Fried Chicken', 'Fried chicken combo from Merchant 1', 70000.00, 60, 'FOOD', 2, 1, 'https://example.com/images/m1_fried_chicken.png'),
    (4, 'M1 French Fries', 'French fries medium size', 30000.00, 120, 'FOOD', 2, 1, 'https://example.com/images/m1_fries.png'),
    (5, 'M1 Cola Drink', 'Cola drink 330ml', 15000.00, 200, 'DRINK', 2, 1, 'https://example.com/images/m1_cola.png'),

    -- ===== Merchant 2: 5 products =====
    (6, 'M2 Chicken Burger', 'Chicken burger from Merchant 2', 52000.00, 90, 'FOOD', 3, 1, 'https://example.com/images/m2_chicken_burger.png'),
    (7, 'M2 Double Beef Burger', 'Double beef burger from Merchant 2', 75000.00, 70, 'FOOD', 3, 1, 'https://example.com/images/m2_double_beef_burger.png'),
    (8, 'M2 Spicy Wings', 'Spicy chicken wings', 65000.00, 50, 'FOOD', 3, 1, 'https://example.com/images/m2_spicy_wings.png'),
    (9, 'M2 Onion Rings', 'Crispy onion rings', 32000.00, 110, 'FOOD', 3, 1, 'https://example.com/images/m2_onion_rings.png'),
    (10, 'M2 Lemon Tea', 'Iced lemon tea 500ml', 18000.00, 180, 'DRINK', 3, 1, 'https://example.com/images/m2_lemon_tea.png');


-- =========================================================
-- 3) DRONES (DB: droneservice)
--    Một số drone IDLE sẵn sàng giao hàng
--    Bảng: drones (id, serial_number, model, battery_level, state,
--          current_latitude, current_longitude, base_latitude, base_longitude, weight_capacity)
-- =========================================================

USE droneservice;

-- Xóa dữ liệu seed cũ nếu có (id 1–8 dùng cho seed)
DELETE FROM drone_missions WHERE drone_id IN (1, 2, 3, 4, 5, 6, 7, 8);
DELETE FROM drones WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8);

INSERT INTO drones (
    id, serial_number, model, battery_level, state,
    current_latitude, current_longitude, base_latitude, base_longitude, weight_capacity
) VALUES
    (1, 'DRONE-001', 'Delivery Pro v1', 100, 'IDLE', 21.027763, 105.834160, 21.027763, 105.834160, 2.5),
    (2, 'DRONE-002', 'Delivery Pro v1', 98, 'IDLE', 21.027763, 105.834160, 21.027763, 105.834160, 2.5),
    (3, 'DRONE-003', 'Delivery Pro v1', 95, 'IDLE', 21.028000, 105.835000, 21.028000, 105.835000, 2.5),
    (4, 'DRONE-004', 'Delivery Pro v2', 100, 'IDLE', 21.028000, 105.835000, 21.028000, 105.835000, 3.0),
    (5, 'DRONE-005', 'Delivery Pro v2', 97, 'IDLE', 21.026500, 105.832000, 21.026500, 105.832000, 3.0),
    (6, 'DRONE-006', 'Delivery Pro v2', 92, 'IDLE', 21.026500, 105.832000, 21.026500, 105.832000, 3.0),
    (7, 'DRONE-007', 'Delivery Pro v1', 99, 'IDLE', 21.027200, 105.833500, 21.027200, 105.833500, 2.5),
    (8, 'DRONE-008', 'Delivery Pro v1', 96, 'IDLE', 21.027200, 105.833500, 21.027200, 105.833500, 2.5);


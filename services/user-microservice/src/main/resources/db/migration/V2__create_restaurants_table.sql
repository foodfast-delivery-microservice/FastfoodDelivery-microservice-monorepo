CREATE TABLE IF NOT EXISTS restaurants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    district VARCHAR(100),
    image VARCHAR(512),
    phone VARCHAR(50),
    email VARCHAR(150),
    opening_hours TEXT,
    active TINYINT(1) NOT NULL DEFAULT 1,
    approved TINYINT(1) NOT NULL DEFAULT 0,
    category VARCHAR(50),
    delivery_fee DECIMAL(10,2),
    estimated_delivery_time INT,
    rating DOUBLE,
    review_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_restaurants_users FOREIGN KEY (merchant_id) REFERENCES users(id)
);













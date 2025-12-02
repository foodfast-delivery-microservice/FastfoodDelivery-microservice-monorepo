CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    street VARCHAR(255) NOT NULL,
    province_code VARCHAR(20) NOT NULL,
    province_name VARCHAR(100) NOT NULL,
    commune_code VARCHAR(20) NOT NULL,
    commune_name VARCHAR(100) NOT NULL,
    district_name VARCHAR(100),
    full_address VARCHAR(400) NOT NULL,
    note VARCHAR(255),
    lat DECIMAL(10,7),
    lng DECIMAL(10,7),
    source VARCHAR(32) NOT NULL DEFAULT 'GEOCODE_ONLY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_addresses_user_id ON user_addresses (user_id);
CREATE INDEX idx_user_addresses_commune_code ON user_addresses (commune_code);



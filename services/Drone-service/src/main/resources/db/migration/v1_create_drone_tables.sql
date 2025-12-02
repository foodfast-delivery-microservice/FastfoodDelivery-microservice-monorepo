CREATE TABLE IF NOT EXISTS drones
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    serial_number
    VARCHAR
(
    50
) NOT NULL UNIQUE,
    model VARCHAR
(
    100
) NOT NULL,
    battery_level INT NOT NULL DEFAULT 100,
    state VARCHAR
(
    20
) NOT NULL DEFAULT 'IDLE',
    current_latitude DOUBLE NOT NULL,
    current_longitude DOUBLE NOT NULL,
    base_latitude DOUBLE NOT NULL,
    base_longitude DOUBLE NOT NULL,
    weight_capacity DOUBLE NOT NULL DEFAULT 3.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK
(
    battery_level
    >=
    0
    AND
    battery_level
    <=
    100
),
    CHECK
(
    current_latitude
    >=
    -
    90
    AND
    current_latitude
    <=
    90
),
    CHECK
(
    current_longitude
    >=
    -
    180
    AND
    current_longitude
    <=
    180
)
    );
CREATE TABLE IF NOT EXISTS drone_missions
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    drone_id
    BIGINT
    NOT
    NULL,
    order_id
    BIGINT
    NOT
    NULL,

    pickup_latitude
    DOUBLE
    NOT
    NULL,
    pickup_longitude
    DOUBLE
    NOT
    NULL,
    delivery_latitude
    DOUBLE
    NOT
    NULL,
    delivery_longitude
    DOUBLE
    NOT
    NULL,

    status
    VARCHAR
(
    20
) NOT NULL DEFAULT 'ASSIGNED',
    distance_km DOUBLE,
    estimated_duration_minutes INT,

    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY
(
    drone_id
) REFERENCES drones
(
    id
) ON DELETE CASCADE,
    INDEX idx_order_id
(
    order_id
),
    INDEX idx_status
(
    status
)
    );

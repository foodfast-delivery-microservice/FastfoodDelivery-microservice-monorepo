CREATE TABLE webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sg_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    user_id BIGINT,
    notification_id BIGINT,
    event_data TEXT,
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sg_event_id (sg_event_id),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

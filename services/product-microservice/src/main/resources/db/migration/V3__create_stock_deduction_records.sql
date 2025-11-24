CREATE TABLE stock_deduction_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT UNIQUE NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_stock_deduction_order_id (order_id)
);



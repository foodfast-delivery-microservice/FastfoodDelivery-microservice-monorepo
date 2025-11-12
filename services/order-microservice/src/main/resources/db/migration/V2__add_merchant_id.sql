-- Repair script: Check if columns exist and complete migration if needed

-- Check and add merchant_id to orders if not exists
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'orders' 
    AND COLUMN_NAME = 'merchant_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE orders ADD COLUMN merchant_id BIGINT NULL',
    'SELECT "Column merchant_id already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add merchant_id to order_items if not exists
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'order_items' 
    AND COLUMN_NAME = 'merchant_id'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE order_items ADD COLUMN merchant_id BIGINT NULL',
    'SELECT "Column merchant_id already exists in order_items" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Update existing orders: set merchant_id to 0 (system/admin orders)
UPDATE orders
SET merchant_id = 0
WHERE merchant_id IS NULL;

-- Update existing order_items: set merchant_id from their order
UPDATE order_items oi
JOIN orders o ON oi.order_id = o.id
SET oi.merchant_id = o.merchant_id
WHERE oi.merchant_id IS NULL;

-- Make merchant_id NOT NULL after backfilling (only if column is nullable)
SET @col_nullable = (
    SELECT IS_NULLABLE 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'orders' 
    AND COLUMN_NAME = 'merchant_id'
);

SET @sql = IF(@col_nullable = 'YES', 
    'ALTER TABLE orders MODIFY COLUMN merchant_id BIGINT NOT NULL',
    'SELECT "Column merchant_id already NOT NULL in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_nullable = (
    SELECT IS_NULLABLE 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'order_items' 
    AND COLUMN_NAME = 'merchant_id'
);

SET @sql = IF(@col_nullable = 'YES', 
    'ALTER TABLE order_items MODIFY COLUMN merchant_id BIGINT NOT NULL',
    'SELECT "Column merchant_id already NOT NULL in order_items" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create indexes if they don't exist
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'orders' 
    AND INDEX_NAME = 'idx_orders_merchant_id'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_orders_merchant_id ON orders (merchant_id)',
    'SELECT "Index idx_orders_merchant_id already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'order_items' 
    AND INDEX_NAME = 'idx_order_items_merchant_id'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_order_items_merchant_id ON order_items (merchant_id)',
    'SELECT "Index idx_order_items_merchant_id already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


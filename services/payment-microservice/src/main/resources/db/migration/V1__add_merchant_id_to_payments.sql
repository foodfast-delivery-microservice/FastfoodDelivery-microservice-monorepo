-- Add merchant_id column to payments table
-- Check if column exists before adding
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'payments'
    AND COLUMN_NAME = 'merchant_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE payments ADD COLUMN merchant_id BIGINT NULL',
    'SELECT "Column merchant_id already exists in payments" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill merchant_id for existing payments (set to 0 for system/admin orders)
UPDATE payments
SET merchant_id = 0
WHERE merchant_id IS NULL;

-- Make merchant_id NOT NULL after backfilling
SET @col_nullable = (
    SELECT IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'payments'
    AND COLUMN_NAME = 'merchant_id'
);

SET @sql2 = IF(@col_nullable = 'YES',
    'ALTER TABLE payments MODIFY COLUMN merchant_id BIGINT NOT NULL',
    'SELECT "Column merchant_id is already NOT NULL" AS message'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Create index for merchant_id if it doesn't exist
SET @idx_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'payments'
    AND INDEX_NAME = 'idx_payments_merchant_id'
);

SET @sql3 = IF(@idx_exists = 0,
    'CREATE INDEX idx_payments_merchant_id ON payments (merchant_id)',
    'SELECT "Index idx_payments_merchant_id already exists" AS message'
);
PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- Create composite index for merchant_id and status if it doesn't exist
SET @idx_exists2 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'payments'
    AND INDEX_NAME = 'idx_payments_merchant_status'
);

SET @sql4 = IF(@idx_exists2 = 0,
    'CREATE INDEX idx_payments_merchant_status ON payments (merchant_id, status)',
    'SELECT "Index idx_payments_merchant_status already exists" AS message'
);
PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;


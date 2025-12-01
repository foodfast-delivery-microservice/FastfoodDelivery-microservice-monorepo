-- Add administrative address fields and GPS coordinates to orders table if they don't exist

-- province_code
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'province_code'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN province_code VARCHAR(20) NULL',
    'SELECT "Column province_code already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- province_name
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'province_name'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN province_name VARCHAR(100) NULL',
    'SELECT "Column province_name already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- commune_code
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'commune_code'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN commune_code VARCHAR(20) NULL',
    'SELECT "Column commune_code already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- commune_name
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'commune_name'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN commune_name VARCHAR(100) NULL',
    'SELECT "Column commune_name already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- normalized_district_name
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'normalized_district_name'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN normalized_district_name VARCHAR(100) NULL',
    'SELECT "Column normalized_district_name already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- lat
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'lat'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN lat DECIMAL(10,7) NULL',
    'SELECT "Column lat already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- lng
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'lng'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE orders ADD COLUMN lng DECIMAL(10,7) NULL',
    'SELECT "Column lng already exists in orders" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;




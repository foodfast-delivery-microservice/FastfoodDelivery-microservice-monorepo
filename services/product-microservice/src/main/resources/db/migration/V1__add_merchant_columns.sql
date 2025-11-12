ALTER TABLE products
    ADD COLUMN merchant_id BIGINT NULL;

UPDATE products
SET merchant_id = 0
WHERE merchant_id IS NULL;

ALTER TABLE products
    MODIFY COLUMN merchant_id BIGINT NOT NULL;

CREATE INDEX idx_products_merchant ON products (merchant_id);

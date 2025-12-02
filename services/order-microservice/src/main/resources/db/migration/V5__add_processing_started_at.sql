-- Add processing_started_at column to orders table
-- This field tracks when the restaurant starts processing the order (PAID -> PROCESSING transition)
-- Used for SLA reporting and statistics

ALTER TABLE orders
ADD COLUMN processing_started_at TIMESTAMP NULL;

-- Add index for better query performance when filtering by processing_started_at
CREATE INDEX idx_orders_processing_started_at ON orders (processing_started_at);

-- Add comment to document the field
ALTER TABLE orders 
MODIFY COLUMN processing_started_at TIMESTAMP NULL 
COMMENT 'Timestamp when order status changed to PROCESSING (restaurant started preparing food)';


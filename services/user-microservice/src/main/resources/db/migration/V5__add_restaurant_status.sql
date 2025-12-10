-- Add status column to restaurants table for Saga pattern deletion workflow
ALTER TABLE restaurants
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Add index for status queries
CREATE INDEX idx_restaurants_status ON restaurants(status);

-- Add comment for documentation
ALTER TABLE restaurants MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' 
COMMENT 'Restaurant status: ACTIVE, DELETE_PENDING, DELETED';

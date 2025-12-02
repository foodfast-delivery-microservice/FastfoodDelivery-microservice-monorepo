-- Add latitude and longitude columns to restaurants table
-- These coordinates are used for drone pickup location calculation

ALTER TABLE restaurants
ADD COLUMN latitude DECIMAL(10, 7) NULL COMMENT 'Restaurant latitude coordinate for GPS location',
ADD COLUMN longitude DECIMAL(10, 7) NULL COMMENT 'Restaurant longitude coordinate for GPS location';

-- Add index for location-based queries
CREATE INDEX idx_restaurants_location ON restaurants(latitude, longitude);


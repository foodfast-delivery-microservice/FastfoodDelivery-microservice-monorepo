INSERT INTO restaurants (
    merchant_id,
    name,
    description,
    address,
    city,
    district,
    image,
    phone,
    email,
    opening_hours,
    active,
    approved,
    category,
    created_at,
    updated_at
)
SELECT
    id AS merchant_id,
    COALESCE(restaurant_name, CONCAT('Merchant #', id)) AS name,
    NULL AS description,
    COALESCE(restaurant_address, address, '') AS address,
    NULL AS city,
    NULL AS district,
    restaurant_image AS image,
    phone,
    email,
    opening_hours,
    active,
    approved,
    NULL AS category,
    NOW() AS created_at,
    NOW() AS updated_at
FROM users
WHERE role = 'MERCHANT'
  AND restaurant_name IS NOT NULL
  AND restaurant_name <> ''
  AND id NOT IN (SELECT merchant_id FROM restaurants);













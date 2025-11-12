# Product Service – Merchant Support

## Data Model
- Each product stores `merchant_id` (non-null). Ownership is enforced in service logic.
- Column `approved` has been removed; merchant approval is handled entirely by the user service.
- Flyway migration `V2__drop_product_approved.sql` drops `approved` for existing databases.

## API Behaviour
- `POST /api/v1/products`: merchant ownership derived from JWT `userId`. Admins must provide `merchantId`.
- `PUT /api/v1/products/{id}` and `DELETE /api/v1/products/{id}`: merchants can manage only their products; admins manage all.
- `GET /api/v1/products/merchants/me`: returns products for the authenticated merchant. Admins may query any merchant via `?merchantId=`.
- Public catalogue endpoints (`GET /api/v1/products`, `/api/v1/products/{category}`) return products with `active = true`.

## Validation Changes
- `/api/v1/products/validate` now treats inactive products as invalid; there is no additional approval status check.

## Notes
- Ensure user service issues tokens only for approved merchants; product service assumes authenticated merchants are already vetted.


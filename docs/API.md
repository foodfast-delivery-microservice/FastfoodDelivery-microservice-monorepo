# API Documentation

## 📋 Table of Contents
1. [API Overview](#api-overview)
2. [Authentication](#authentication)
3. [User Service APIs](#user-service-apis)
4. [Product Service APIs](#product-service-apis)
5. [Order Service APIs](#order-service-apis)
6. [Payment Service APIs](#payment-service-apis)
7. [Error Handling](#error-handling)

## API Overview

### Base URLs
- **API Gateway (Production)**: `http://localhost:8080/api/v1`
- **Direct Service Access (Development)**: 
  - User Service: `http://localhost:8081/api/v1`
  - Product Service: `http://localhost:8082/api/v1`
  - Order Service: `http://localhost:8083/api/v1`
  - Payment Service: `http://localhost:8084/api/v1`

### Common Headers
```http
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

### HTTP Status Codes
- `200 OK`: Successful request
- `201 Created`: Resource created successfully
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource conflict (e.g., duplicate)
- `500 Internal Server Error`: Server error

## Authentication

### Register User
Create a new user account.

**Endpoint**: `POST /auth/register`

**Request Body**:
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecureP@ss123",
  "role": "CUSTOMER"
}
```

**Roles**: `CUSTOMER`, `MERCHANT`, `ADMIN`

**Response** (201 Created):
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "approved": true
}
```

**Notes**:
- Merchants require admin approval (`approved: false` initially)
- Customers are auto-approved
- Password must be at least 6 characters

### Login
Authenticate and receive JWT tokens.

**Endpoint**: `POST /auth/login`

**Request Body**:
```json
{
  "username": "john_doe",
  "password": "SecureP@ss123"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 90000,
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "CUSTOMER"
  }
}
```

**Token Expiration**:
- Access Token: 25 hours
- Refresh Token: 30 days

### Refresh Token
Get a new access token using refresh token.

**Endpoint**: `POST /auth/refresh`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 90000
}
```

## User Service APIs

### Get Current User
Get authenticated user's profile.

**Endpoint**: `GET /users/me`  
**Auth**: Required  

**Response** (200 OK):
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "approved": true,
  "createdAt": "2024-11-26T10:00:00Z"
}
```

### Get User by ID
Get user information by ID (Admin only).

**Endpoint**: `GET /users/{id}`  
**Auth**: Admin  

**Response** (200 OK):
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "approved": true
}
```

### List All Users
Get paginated list of users (Admin only).

**Endpoint**: `GET /users`  
**Auth**: Admin  
**Query Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `role` (optional): Filter by role

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "role": "CUSTOMER",
      "approved": true
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "page": 0,
  "size": 20
}
```

### Approve Merchant
Approve a merchant account (Admin only).

**Endpoint**: `PUT /users/{id}/approve`  
**Auth**: Admin  

**Response** (200 OK):
```json
{
  "id": 2,
  "username": "pizza_shop",
  "email": "pizza@example.com",
  "role": "MERCHANT",
  "approved": true
}
```

## Product Service APIs

### Create Product
Create a new product (Merchant/Admin).

**Endpoint**: `POST /products`  
**Auth**: Merchant or Admin  

**Request Body**:
```json
{
  "name": "Deluxe Burger",
  "description": "Premium beef burger with cheese",
  "price": 59.99,
  "stock": 100,
  "category": "BURGERS",
  "active": true
}
```

**Categories**: `BURGERS`, `PIZZA`, `CHICKEN`, `DRINKS`, `DESSERTS`, `SIDES`

**Response** (201 Created):
```json
{
  "id": 1,
  "name": "Deluxe Burger",
  "description": "Premium beef burger with cheese",
  "price": 59.99,
  "stock": 100,
  "category": "BURGERS",
  "merchantId": 2,
  "active": true,
  "createdAt": "2024-11-26T10:00:00Z"
}
```

**Notes**:
- Merchant's products automatically assigned their merchantId
- Admin can specify merchantId in request

### List Products
Get all active products (Public - no auth required).

**Endpoint**: `GET /products`  
**Query Parameters**:
- `category` (optional): Filter by category
- `merchantId` (optional): Filter by merchant
- `active` (optional): Filter by active status
- `search` (optional): Search by name
- `page` (optional): Page number
- `size` (optional): Page size

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "name": "Deluxe Burger",
      "description": "Premium beef burger",
      "price": 59.99,
      "stock": 100,
      "category": "BURGERS",
      "merchantId": 2,
      "active": true
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```

### Get Product by ID
Get single product details.

**Endpoint**: `GET /products/{id}`  

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Deluxe Burger",
  "description": "Premium beef burger with cheese",
  "price": 59.99,
  "stock": 100,
  "category": "BURGERS",
  "merchantId": 2,
  "active": true,
  "createdAt": "2024-11-26T10:00:00Z",
  "updatedAt": "2024-11-26T11:00:00Z"
}
```

### Get Products by Category
Get products in a specific category.

**Endpoint**: `GET /products/category/{category}`  

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Deluxe Burger",
    "price": 59.99,
    "category": "BURGERS"
  },
  {
    "id": 2,
    "name": "Classic Burger",
    "price": 39.99,
    "category": "BURGERS"
  }
]
```

### Get My Products (Merchant)
Get products owned by authenticated merchant.

**Endpoint**: `GET /products/merchants/me`  
**Auth**: Merchant  

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Deluxe Burger",
    "price": 59.99,
    "stock": 100,
    "active": true
  }
]
```

### Update Product
Update product details (Merchant can update own products, Admin can update any).

**Endpoint**: `PUT /products/{id}`  
**Auth**: Merchant or Admin  

**Request Body**:
```json
{
  "name": "Premium Deluxe Burger",
  "description": "Updated description",
  "price": 69.99,
  "stock": 150,
  "active": true
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Premium Deluxe Burger",
  "description": "Updated description",
  "price": 69.99,
  "stock": 150,
  "category": "BURGERS",
  "merchantId": 2,
  "active": true
}
```

### Delete Product
Delete a product (Soft delete).

**Endpoint**: `DELETE /products/{id}`  
**Auth**: Merchant or Admin  

**Response** (204 No Content)

### Validate Products
Internal API to validate products for order creation.

**Endpoint**: `POST /products/validate`  
**Auth**: Required (Service-to-service)  

**Request Body**:
```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

**Response** (200 OK):
```json
{
  "valid": true,
  "products": [
    {
      "id": 1,
      "name": "Deluxe Burger",
      "price": 59.99,
      "merchantId": 2,
      "available": true
    }
  ]
}
```

## Order Service APIs

### Create Order
Create a new order.

**Endpoint**: `POST /orders`  
**Auth**: Customer  

**Request Body**:
```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ],
  "receiverName": "John Doe",
  "receiverPhone": "0123456789",
  "addressLine1": "123 Main Street",
  "ward": "Ward 1",
  "district": "District 1",
  "city": "Ho Chi Minh City",
  "note": "Please call before delivery",
  "idempotencyKey": "unique-request-id-123"
}
```

**Response** (201 Created):
```json
{
  "id": 100,
  "orderCode": "ORD-2024-100",
  "userId": 1,
  "merchantId": 2,
  "status": "PENDING",
  "currency": "VND",
  "subtotal": 119.98,
  "discount": 0.00,
  "shippingFee": 15.00,
  "grandTotal": 134.98,
  "items": [
    {
      "id": 201,
      "productId": 1,
      "productName": "Deluxe Burger",
      "quantity": 2,
      "unitPrice": 59.99,
      "lineTotal": 119.98
    }
  ],
  "receiverName": "John Doe",
  "receiverPhone": "0123456789",
  "addressLine1": "123 Main Street",
  "ward": "Ward 1",
  "district": "District 1",
  "city": "Ho Chi Minh City",
  "createdAt": "2024-11-26T14:30:00Z"
}
```

**Order Statuses**:
- `PENDING`: Order created, waiting for confirmation
- `CONFIRMED`: Merchant confirmed the order
- `PAID`: Payment successful
- `PREPARING`: Merchant preparing the order
- `SHIPPING`: Order on the way
- `DELIVERED`: Order delivered
- `CANCELLED`: Order cancelled
- `REFUNDED`: Order refunded

### Get Order by ID
Get order details.

**Endpoint**: `GET /orders/{id}`  
**Auth**: Required (Customer sees own orders, Merchant sees their orders, Admin sees all)  

**Response** (200 OK):
```json
{
  "id": 100,
  "orderCode": "ORD-2024-100",
  "userId": 1,
  "merchantId": 2,
  "status": "CONFIRMED",
  "grandTotal": 134.98,
  "items": [...],
  "deliveryInfo": {...}
}
```

### List My Orders (Customer)
Get orders for authenticated customer.

**Endpoint**: `GET /orders/my-orders`  
**Auth**: Customer  
**Query Parameters**:
- `status` (optional): Filter by status
- `page` (optional): Page number
- `size` (optional): Page size

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 100,
      "orderCode": "ORD-2024-100",
      "status": "DELIVERED",
      "grandTotal": 134.98,
      "createdAt": "2024-11-26T14:30:00Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 3
}
```

### List Merchant Orders
Get orders for authenticated merchant.

**Endpoint**: `GET /orders/merchant/me`  
**Auth**: Merchant  
**Query Parameters**:
- `status` (optional): Filter by status
- `startDate` (optional): Filter from date (ISO 8601)
- `endDate` (optional): Filter to date
- `page`, `size`: Pagination

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 100,
      "orderCode": "ORD-2024-100",
      "userId": 1,
      "status": "PAID",
      "grandTotal": 134.98,
      "createdAt": "2024-11-26T14:30:00Z"
    }
  ]
}
```

### Update Order Status
Update order status (Merchant or Admin).

**Endpoint**: `PUT /orders/{id}/status`  
**Auth**: Merchant or Admin  

**Request Body**:
```json
{
  "status": "CONFIRMED"
}
```

**Valid Transitions**:
- `PENDING → CONFIRMED` or `CANCELLED`
- `CONFIRMED → PREPARING` or `CANCELLED`
- `PREPARING → SHIPPING`
- `SHIPPING → DELIVERED`
- `DELIVERED → REFUNDED` (Admin only)

**Response** (200 OK):
```json
{
  "id": 100,
  "status": "CONFIRMED",
  "updatedAt": "2024-11-26T15:00:00Z"
}
```

### Request Refund
Request refund for delivered order.

**Endpoint**: `POST /orders/{id}/refund`  
**Auth**: Customer, Merchant, or Admin  

**Request Body**:
```json
{
  "reason": "Product quality issue",
  "refundAmount": 134.98
}
```

**Response** (200 OK):
```json
{
  "orderId": 100,
  "status": "REFUNDED",
  "refundAmount": 134.98,
  "message": "Refund request processed successfully"
}
```

### Get Order Statistics (Admin)
Get system-wide order statistics.

**Endpoint**: `GET /orders/admin/statistics`  
**Auth**: Admin  
**Query Parameters**:
- `startDate`, `endDate`: Date range

**Response** (200 OK):
```json
{
  "totalOrders": 1250,
  "totalRevenue": 125000.50,
  "byStatus": {
    "PENDING": 15,
    "CONFIRMED": 30,
    "PAID": 25,
    "DELIVERED": 1150,
    "CANCELLED": 20,
    "REFUNDED": 10
  },
  "averageOrderValue": 100.00,
  "period": {
    "start": "2024-11-01T00:00:00Z",
    "end": "2024-11-26T23:59:59Z"
  }
}
```

### Get User Statistics
Get user's order statistics.

**Endpoint**: `GET /orders/user/statistics`  
**Auth**: Customer  

**Response** (200 OK):
```json
{
  "totalOrders": 25,
  "totalSpent": 2500.00,
  "completedOrders": 23,
  "cancelledOrders": 2,
  "averageOrderValue": 100.00
}
```

## Payment Service APIs

### Process Payment
Process payment for an order.

**Endpoint**: `POST /payments`  
**Auth**: Customer  

**Request Body**:
```json
{
  "orderId": 100,
  "amount": 134.98,
  "paymentMethod": "CREDIT_CARD"
}
```

**Payment Methods**: `CREDIT_CARD`, `DEBIT_CARD`, `E_WALLET`, `CASH`

**Response** (200 OK):
```json
{
  "id": 500,
  "orderId": 100,
  "userId": 1,
  "amount": 134.98,
  "currency": "VND",
  "status": "SUCCESS",
  "paymentMethod": "CREDIT_CARD",
  "transactionId": "TXN-20241126-500",
  "paidAt": "2024-11-26T15:30:00Z"
}
```

**Payment Statuses**:
- `PENDING`: Payment created, waiting for processing
- `SUCCESS`: Payment successful
- `FAILED`: Payment failed
- `REFUNDED`: Payment refunded

### Get Payment by Order ID
Get payment information for an order.

**Endpoint**: `GET /payments/order/{orderId}`  
**Auth**: Required (Customer sees own, Merchant sees their orders, Admin sees all)  

**Response** (200 OK):
```json
{
  "id": 500,
  "orderId": 100,
  "amount": 134.98,
  "status": "SUCCESS",
  "paymentMethod": "CREDIT_CARD",
  "paidAt": "2024-11-26T15:30:00Z"
}
```

### List My Payments (Customer)
Get payment history for authenticated customer.

**Endpoint**: `GET /payments/my-payments`  
**Auth**: Customer  
**Query Parameters**:
- `status` (optional)
- `page`, `size`

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 500,
      "orderId": 100,
      "amount": 134.98,
      "status": "SUCCESS",
      "paidAt": "2024-11-26T15:30:00Z"
    }
  ]
}
```

### List Merchant Payments
Get payment history for merchant's orders.

**Endpoint**: `GET /payments/merchant/me`  
**Auth**: Merchant  

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 500,
      "orderId": 100,
      "amount": 134.98,
      "status": "SUCCESS",
      "merchantId": 2,
      "paidAt": "2024-11-26T15:30:00Z"
    }
  ],
  "totalRevenue": 12500.00,
  "totalTransactions": 125
}
```

## Error Handling

### Error Response Format
```json
{
  "timestamp": "2024-11-26T15:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: Product stock is insufficient",
  "path": "/api/v1/orders",
  "errors": [
    {
      "field": "items[0].quantity",
      "message": "Requested quantity exceeds available stock"
    }
  ]
}
```

### Common Error Messages

#### Authentication Errors
- `401: Invalid or expired token`
- `403: Insufficient permissions for this operation`

#### Validation Errors
- `400: Invalid request data`
- `400: Product stock is insufficient`
- `400: All products must belong to the same merchant`
- `400: Invalid order status transition`

#### Resource Errors
- `404: Order not found`
- `404: Product not found`
- `404: User not found`
- `409: Username already exists`
- `409: Email already exists`

#### Business Logic Errors
- `400: Merchant not approved`
- `400: Order must be in DELIVERED status for refund`
- `400: Payment amount does not match order total`

---

**For more detailed API specifications, please refer to the [PRD.md](PRD.md) document.**

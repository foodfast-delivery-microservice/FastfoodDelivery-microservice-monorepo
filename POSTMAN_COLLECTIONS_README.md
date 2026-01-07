# Postman Collections - FastFood Delivery Microservices

Đã tạo Postman collections cho tất cả các microservices trong hệ thống.

## 📦 Danh sách Collections

### 1. **Drone Service** (13 endpoints)
**File**: `services/Drone-service/Drone-Service-Postman-Collection.json`
**Port**: 8084
- 📂 Drone Management (6)
  - Create Drone
  - Get All Drones
  - Get Drone by ID
  - Get Drones by State
  - Update Drone Battery
  - Update Drone State
- 📂 Mission Tracking (6)
  - Get All Missions
  - Get Mission by ID
  - Get Mission by Order ID
  - Get Missions by Drone ID
  - Get Mission Tracking
  - Get Tracking by Order ID
- 📂 Drone Assignment (1)
  - Assign Drone to Order

### 2. **User Service** (17 endpoints)
**File**: `services/user-microservice/User-Service-Postman-Collection.json`
**Port**: 8081
- 📂 Authentication (2)
  - Register
  - Login
- 📂 User Management (8)
  - Create User
  - Get All Users
  - Get User by ID
  - Get Current User (Me)
  - Update User
  - Change Password
  - Validate User
  - Delete User
- 📂 Restaurant Management (6)
  - List Restaurants
  - Get Restaurant by ID
  - Get Restaurant by Merchant ID
  - Get My Restaurant
  - Update My Restaurant
  - Update Restaurant Status
- 📂 Internal APIs (1)
  - Validate User (Internal)

### 3. **Order Service** (19 endpoints)
**File**: `services/order-microservice/Order-Service-Postman-Collection.json`
**Port**: 8082
- 📂 Order Management (7)
  - Create Order
  - Get My Orders
  - Get Order Detail
  - Get Order List (Admin)
  - Update Order Status
  - Search Orders
  - Get User Statistics
- 📂 Merchant Orders (3)
  - Get My Merchant Orders
  - Get My Merchant Order Detail
  - Update My Merchant Order Status
- 📂 User Addresses (7)
  - Create Address
  - Get My Addresses
  - Update Address Location
  - Driver Adjust Location
  - Get Provinces
  - Get Communes by Province
  - Get Address Metrics
- 📂 Admin Dashboard (1)
  - Get System KPIs
- 📂 Internal APIs (1)
  - Get Order Detail (Internal)

### 4. **Product Service** (11 endpoints)
**File**: `services/product-microservice/Product-Service-Postman-Collection.json`
**Port**: 8083
- 📂 Product Management (10)
  - Create Product
  - Get All Products
  - Get Product by ID
  - Get Products by Category
  - Get Products by Merchant (Public)
  - Get My Merchant Products
  - Update Product
  - Delete Product
  - Validate Products
  - Ping
- 📂 File Upload (1)
  - Upload Image

### 5. **Payment Service** (5 endpoints)
**File**: `services/payment-microservice/Payment-Service-Postman-Collection.json`
**Port**: 8085
- 📂 Payment Processing (2)
  - Process Payment
  - Get Payment by Order ID
- 📂 Merchant Payments (3)
  - Get My Merchant Payments
  - Get My Merchant Payment Statistics
  - Get Merchant Statistics (Admin)

## 📊 Tổng quan

- **Tổng số collections**: 5
- **Tổng số endpoints**: 65
- **Services không có REST API**: 
  - Gateway Service (routing only)
  - Registry Service (service discovery only)

## 🚀 Hướng dẫn Import vào Postman

### Cách 1: Import từng collection
1. Mở Postman
2. Click **Import** (góc trên bên trái)
3. Chọn tab **File**
4. Kéo thả hoặc chọn file JSON collection
5. Click **Import**

### Cách 2: Import tất cả cùng lúc
1. Mở Postman
2. Click **Import**
3. Chọn tất cả 5 file JSON
4. Click **Import**

## 🔑 Environment Variables

Mỗi collection đã có sẵn variables:
- `base_url`: URL của service
- `jwt_token`: JWT token sau khi login
- Các ID liên quan: `user_id`, `order_id`, `product_id`, v.v.

### Cách setup JWT token:
1. Gọi endpoint **Login** trong User Service
2. Copy `accessToken` từ response
3. Set vào biến `jwt_token` trong collection

## 📝 Lưu ý

- Tất cả endpoints cần authentication đều dùng header: `Authorization: Bearer {{jwt_token}}`
- Các endpoint Internal không cần authentication
- Testing nên bắt đầu từ User Service để lấy JWT token trước

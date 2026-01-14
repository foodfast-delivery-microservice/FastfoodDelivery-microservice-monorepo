# Hướng dẫn sử dụng API Services

## Tổng quan

Frontend hiện đã được cấu hình để gọi **trực tiếp từng microservice** với port riêng biệt trong môi trường development.

## Service Ports

| Service | Port | Base URL |
|---------|------|----------|
| User Service | 8081 | http://localhost:8081/api/v1 |
| Product Service | 8082 | http://localhost:8082/api/v1 |
| Order Service | 8083 | http://localhost:8083/api/v1 |
| Payment Service | 8084 | http://localhost:8084/api/v1 |
| Drone Service | 8085 | http://localhost:8085/api/v1 |
| Gateway (Demo) | 8080 | http://localhost:8080/api/v1 |

## Cách sử dụng

### 1. Import axios instances

```javascript
import { userHttp, productHttp, orderHttp, paymentHttp, droneHttp } from '../../services/http'
```

### 2. Sử dụng instance tương ứng

```javascript
// Gọi User Service (Authentication, Users, Restaurants)
const userData = await userHttp.get('/users')
const restaurants = await userHttp.get('/restaurants')

// Gọi Product Service
const products = await productHttp.get('/products')

// Gọi Order Service
const orders = await orderHttp.get('/orders')

// Gọi Payment Service
const payments = await paymentHttp.get('/payments')

// Gọi Drone Service
const drones = await droneHttp.get('/drones')
```

### 3. Default http instance

Instance mặc định `http` sẽ gọi đến **User Service (8081)** để backward compatible:

```javascript
import http from '../../services/http'
const data = await http.get('/users') // → http://localhost:8081/api/v1/users
```

## Ví dụ thực tế

### Orders.jsx (đã cập nhật)

```javascript
import { orderHttp, userHttp } from "../../services/http";

// Fetch orders từ Order Service (8083)
const res = await orderHttp.get("/orders", { params: { size: 100, page: 0 } });

// Fetch restaurants từ User Service (8081)
const res = await userHttp.get("/restaurants", { params: { size: 100, page: 0 } });
```

### droneApi.js (đã cập nhật)

```javascript
import { droneHttp as http } from './http'

// Tất cả calls đều đi đến Drone Service (8085)
const { data } = await http.get('/drones')
```

## Khi nào sử dụng Gateway?

Khi demo hoặc production, đổi sang sử dụng `gatewayHttp`:

```javascript
import { gatewayHttp } from '../../services/http'
const data = await gatewayHttp.get('/orders') // → http://localhost:8080/api/v1/orders
```

Hoặc sửa trong `apiConfig.js` để tất cả services đều point về Gateway:

```javascript
const API_SERVICES = {
  USER: 'http://localhost:8080/api/v1',
  PRODUCT: 'http://localhost:8080/api/v1',
  ORDER: 'http://localhost:8080/api/v1',
  PAYMENT: 'http://localhost:8080/api/v1',
  DRONE: 'http://localhost:8080/api/v1',
  GATEWAY: 'http://localhost:8080/api/v1',
}
```

## Interceptors

Tất cả axios instances đều có:
- **Request interceptor**: Tự động thêm JWT token từ localStorage
- **Response interceptor**: Xử lý 401 Unauthorized và redirect về login

## Files đã thay đổi

- ✅ `src/services/apiConfig.js` - Cấu hình ports cho tất cả services
- ✅ `src/services/http.js` - Tạo axios instances riêng cho từng service
- ✅ `src/admin/pages/Orders.jsx` - Sử dụng `orderHttp` và `userHttp`
- ✅ `src/services/droneApi.js` - Sử dụng `droneHttp`

# Hướng Dẫn Deploy Hệ Thống FastFood Delivery

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Môi Trường Development (Local)](#môi-trường-development-local)
3. [Môi Trường Staging (Docker Compose)](#môi-trường-staging-docker-compose)
4. [Môi Trường Production (Cloud)](#môi-trường-production-cloud)
5. [Kiểm Tra và Xác Thực](#kiểm-tra-và-xác-thực)
6. [Troubleshooting](#troubleshooting)

---

## Tổng Quan

Hệ thống FastFood Delivery hỗ trợ 3 môi trường triển khai:

| Môi Trường | Mục Đích | Công Cụ | Độ Phức Tạp |
|------------|----------|---------|-------------|
| **Development** | Phát triển và test local | Maven, Node.js | ⭐⭐ |
| **Staging** | Test tích hợp trước production | Docker Compose | ⭐⭐⭐ |
| **Production** | Chạy thực tế | Railway, Render, Fly.io | ⭐⭐⭐⭐ |

---

## Môi Trường Development (Local)

### Mục Đích
- Phát triển và debug code
- Test các tính năng mới
- Chạy trên máy local không cần Docker

### Yêu Cầu
- **Java**: JDK 17+
- **Maven**: 3.8+
- **Node.js**: 18+
- **MySQL**: 8.0+ (hoặc Docker cho MySQL)
- **RabbitMQ**: 3.13+ (hoặc Docker cho RabbitMQ)

### Bước 1: Chuẩn Bị Infrastructure

#### 1.1. Khởi động MySQL bằng Docker
```bash
docker run -d \
  --name mysql-dev \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=userservice \
  -v mysql-dev-data:/var/lib/mysql \
  mysql:8.0
```

#### 1.2. Tạo các Database cần thiết
```bash
docker exec -i mysql-dev mysql -uroot -p1234 <<EOF
CREATE DATABASE IF NOT EXISTS productmicroservice;
CREATE DATABASE IF NOT EXISTS orderservice;
CREATE DATABASE IF NOT EXISTS paymentservice;
CREATE DATABASE IF NOT EXISTS droneservice;
EOF
```

#### 1.3. Khởi động RabbitMQ bằng Docker
```bash
docker run -d \
  --name rabbitmq-dev \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3.13-management
```

**Kiểm tra RabbitMQ**: http://localhost:15672 (guest/guest)

### Bước 2: Khởi Động Các Microservices

#### 2.1. Khởi động Registry Service (Eureka)
```bash
cd services/registry-service
mvn clean spring-boot:run
```

**Đợi Eureka khởi động hoàn tất** (kiểm tra: http://localhost:8761)

#### 2.2. Khởi động API Gateway
Mở terminal mới:
```bash
cd services/gateway-service
mvn clean spring-boot:run
```

#### 2.3. Khởi động User Service
Mở terminal mới:
```bash
cd services/user-microservice
mvn clean spring-boot:run
```

#### 2.4. Khởi động Product Service
Mở terminal mới:
```bash
cd services/product-microservice
mvn clean spring-boot:run
```

#### 2.5. Khởi động Order Service
Mở terminal mới:
```bash
cd services/order-microservice
mvn clean spring-boot:run
```

#### 2.6. Khởi động Payment Service
Mở terminal mới:
```bash
cd services/payment-microservice
mvn clean spring-boot:run
```

#### 2.7. Khởi động Drone Service (nếu có)
Mở terminal mới:
```bash
cd services/Drone-service
mvn clean spring-boot:run
```

### Bước 3: Khởi Động Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend sẽ chạy tại: **http://localhost:5173**

### Cấu Hình Environment Variables (Nếu Cần)

Tạo file `.env` hoặc export các biến môi trường:

```bash
# MySQL
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/userservice
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=1234

# RabbitMQ
export SPRING_RABBITMQ_HOST=localhost
export SPRING_RABBITMQ_PORT=5672
export SPRING_RABBITMQ_USERNAME=guest
export SPRING_RABBITMQ_PASSWORD=guest

# Eureka
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/

# JWT
export APP_JWT_SECRETKEY=g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ==
```

### Kiểm Tra Development Environment

```bash
# Eureka Dashboard
curl http://localhost:8761

# Gateway Health
curl http://localhost:8080/actuator/health

# User Service Health
curl http://localhost:8081/actuator/health

# Product Service Health
curl http://localhost:8082/actuator/health

# Order Service Health
curl http://localhost:8083/actuator/health

# Payment Service Health
curl http://localhost:8084/actuator/health
```

### Dừng Services

```bash
# Dừng tất cả containers
docker stop mysql-dev rabbitmq-dev

# Xóa containers (nếu cần)
docker rm mysql-dev rabbitmq-dev

# Xóa volumes (nếu cần reset data)
docker volume rm mysql-dev-data
```

---

## Môi Trường Staging (Docker Compose)

### Mục Đích
- Test tích hợp toàn bộ hệ thống
- Mô phỏng môi trường production
- Dễ dàng reset và test lại

### Yêu Cầu
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **RAM**: Tối thiểu 8GB
- **Disk**: Tối thiểu 20GB

### Bước 1: Clone Repository

```bash
git clone <repository-url>
cd FastfoodDelivery-microservice-monorepo
```

### Bước 2: Cấu Hình Environment Variables (Tùy Chọn)

Tạo file `.env` trong thư mục gốc:

```env
# MySQL Configuration
MYSQL_ROOT_PASSWORD=your_secure_password_here
MYSQL_HOST=mysql-db

# JWT Secret Key
APP_JWT_SECRETKEY=g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ==

# RabbitMQ Configuration
SPRING_RABBITMQ_HOST=rabbitmq-broker
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# Eureka Configuration
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://registry-service:8761/eureka/
```

**Lưu ý**: Nếu không tạo file `.env`, hệ thống sẽ sử dụng giá trị mặc định trong `docker-compose.yml`.

### Bước 3: Khởi Động Tất Cả Services

```bash
# Build và khởi động tất cả services
docker-compose up -d

# Hoặc build lại nếu có thay đổi code
docker-compose up -d --build
```

Lệnh này sẽ:
- Pull các Docker images cần thiết
- Build các microservices từ Dockerfiles
- Khởi động tất cả services theo thứ tự đúng
- Tạo network và volumes cần thiết

### Bước 4: Kiểm Tra Trạng Thái

```bash
# Xem trạng thái tất cả containers
docker-compose ps

# Kết quả mong đợi: Tất cả services hiển thị "Up"
# NAME                   STATUS
# registry-service       Up
# mysql                  Up (healthy)
# rabbitmq-broker        Up
# gateway-service        Up
# user-service           Up
# product-service        Up
# order-service          Up
# payment-service        Up
# drone-service          Up
# frontend               Up
```

### Bước 5: Xem Logs

```bash
# Xem logs của tất cả services
docker-compose logs -f

# Xem logs của service cụ thể
docker-compose logs -f user-service
docker-compose logs -f order-service

# Xem 100 dòng log cuối cùng
docker-compose logs --tail=100 payment-service
```

### Bước 6: Truy Cập Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | - |
| **API Gateway** | http://localhost:8080 | - |
| **Eureka Dashboard** | http://localhost:8761 | - |
| **RabbitMQ Management** | http://localhost:15672 | guest/guest |
| **MySQL** | localhost:3306 | root/1234 |

### Bước 7: Khởi Tạo Dữ Liệu Mẫu (Tùy Chọn)

```bash
# Chạy script SQL để tạo dữ liệu mẫu
docker exec -i mysql mysql -uroot -p1234 < scripts/sample-data.sql
```

### Dừng và Dọn Dẹp

```bash
# Dừng tất cả services (giữ lại volumes)
docker-compose stop

# Dừng và xóa containers (giữ lại volumes)
docker-compose down

# Dừng và xóa tất cả bao gồm volumes (RESET HOÀN TOÀN)
docker-compose down -v
```

### Restart Service Cụ Thể

```bash
# Restart một service
docker-compose restart user-service

# Rebuild và restart một service
docker-compose up -d --build user-service
```

---

## Môi Trường Production (Cloud)

### Mục Đích
- Chạy hệ thống thực tế cho người dùng
- High availability và scalability
- Sử dụng các nền tảng cloud miễn phí

### Kiến Trúc Production

```
┌─────────────────────────────────────────────────────────┐
│                    Production Architecture               │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Railway (Registry + Gateway)                            │
│  ├── registry-service:8761                              │
│  └── gateway-service:8080                               │
│                                                          │
│  Render (User + Product Services)                        │
│  ├── user-service:8081                                  │
│  └── product-service:8082                               │
│                                                          │
│  Fly.io (Order + Payment Services)                      │
│  ├── order-service:8083                                 │
│  └── payment-service:8084                               │
│                                                          │
│  CloudAMQP (RabbitMQ)                                   │
│  └── Message Broker                                     │
│                                                          │
│  Railway MySQL (Database)                                │
│  └── Multi-database instance                            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Yêu Cầu Tài Khoản

1. **Railway**: https://railway.app (Registry, Gateway, MySQL)
2. **Render**: https://render.com (User, Product services)
3. **Fly.io**: https://fly.io (Order, Payment services)
4. **CloudAMQP**: https://cloudamqp.com (RabbitMQ)

### Phần 1: Setup Infrastructure

#### 1.1. Tạo MySQL Database trên Railway

1. Đăng nhập Railway: https://railway.app
2. Tạo project mới
3. Thêm MySQL service
4. Lưu lại thông tin kết nối:
   - `MYSQL_HOST`
   - `MYSQL_PORT`
   - `MYSQL_USER`
   - `MYSQL_PASSWORD`

5. Tạo các databases:
```sql
CREATE DATABASE IF NOT EXISTS userservice;
CREATE DATABASE IF NOT EXISTS productmicroservice;
CREATE DATABASE IF NOT EXISTS orderservice;
CREATE DATABASE IF NOT EXISTS paymentservice;
CREATE DATABASE IF NOT EXISTS droneservice;
```

#### 1.2. Tạo CloudAMQP Instance

1. Đăng nhập CloudAMQP: https://cloudamqp.com
2. Tạo instance mới (chọn plan miễn phí)
3. Lưu lại connection string: `amqp://user:pass@host.cloudamqp.com/vhost`

### Phần 2: Deploy Registry Service (Railway)

#### 2.1. Tạo Service trên Railway

1. Trong Railway project, click **"New"** → **"GitHub Repo"**
2. Chọn repository và branch
3. Chọn service: `services/registry-service`
4. Railway sẽ tự động detect và build

#### 2.2. Cấu Hình Environment Variables

Trong Railway dashboard, thêm các biến môi trường:

```env
SPRING_APPLICATION_NAME=registry-service
SERVER_PORT=8761
EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
EUREKA_CLIENT_FETCH_REGISTRY=false
```

#### 2.3. Cấu Hình Public Domain

1. Vào tab **"Settings"** → **"Generate Domain"**
2. Lưu lại domain: `https://registry-service-production.up.railway.app`
3. Đảm bảo service đã deploy thành công

### Phần 3: Deploy Gateway Service (Railway)

#### 3.1. Tạo Service

1. Trong cùng Railway project, thêm service mới
2. Chọn: `services/gateway-service`

#### 3.2. Cấu Hình Environment Variables

```env
SPRING_APPLICATION_NAME=gateway-service
SERVER_PORT=8080
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=https://registry-service-production.up.railway.app/eureka/
EUREKA_INSTANCE_HOSTNAME=gateway-service-production.up.railway.app
EUREKA_INSTANCE_PREFER_IP=false
EUREKA_NON_SECURE_PORT_ENABLED=false
EUREKA_SECURE_PORT_ENABLED=true
APP_JWT_SECRETKEY=g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ==
```

#### 3.3. Generate Domain

1. Generate domain: `https://gateway-service-production.up.railway.app`
2. Deploy và kiểm tra

### Phần 4: Deploy User Service (Render)

#### 4.1. Tạo Web Service trên Render

1. Đăng nhập Render: https://render.com
2. Click **"New"** → **"Web Service"**
3. Connect GitHub repository
4. Cấu hình:
   - **Name**: `user-service`
   - **Environment**: `Docker`
   - **Root Directory**: `services/user-microservice`
   - **Dockerfile Path**: `Dockerfile`

#### 4.2. Cấu Hình Environment Variables

```env
SPRING_APPLICATION_NAME=user-service
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:mysql://MYSQL_HOST:MYSQL_PORT/userservice
SPRING_DATASOURCE_USERNAME=MYSQL_USER
SPRING_DATASOURCE_PASSWORD=MYSQL_PASSWORD
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=https://registry-service-production.up.railway.app/eureka/
EUREKA_INSTANCE_HOSTNAME=user-service.onrender.com
EUREKA_INSTANCE_PREFER_IP=false
EUREKA_NON_SECURE_PORT_ENABLED=false
EUREKA_SECURE_PORT_ENABLED=true
SPRING_RABBITMQ_ADDRESSES=amqp://user:pass@host.cloudamqp.com/vhost
APP_JWT_SECRETKEY=g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ==
```

**Lưu ý**: Thay thế `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD` bằng giá trị thực từ Railway MySQL.

#### 4.3. Deploy

1. Click **"Create Web Service"**
2. Render sẽ tự động build và deploy
3. Lưu lại URL: `https://user-service.onrender.com`

### Phần 5: Deploy Product Service (Render)

Làm tương tự User Service:

#### 5.1. Tạo Web Service

- **Name**: `product-service`
- **Root Directory**: `services/product-microservice`

#### 5.2. Environment Variables

```env
SPRING_APPLICATION_NAME=product-service
SERVER_PORT=8082
SPRING_DATASOURCE_URL=jdbc:mysql://MYSQL_HOST:MYSQL_PORT/productmicroservice
SPRING_DATASOURCE_USERNAME=MYSQL_USER
SPRING_DATASOURCE_PASSWORD=MYSQL_PASSWORD
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=https://registry-service-production.up.railway.app/eureka/
EUREKA_INSTANCE_HOSTNAME=product-service.onrender.com
EUREKA_INSTANCE_PREFER_IP=false
EUREKA_NON_SECURE_PORT_ENABLED=false
EUREKA_SECURE_PORT_ENABLED=true
SPRING_RABBITMQ_ADDRESSES=amqp://user:pass@host.cloudamqp.com/vhost
APP_JWT_SECRETKEY=g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ==
```

### Phần 6: Deploy Order Service (Fly.io)

#### 6.1. Cài Đặt Fly CLI

**Windows (PowerShell)**:
```powershell
iwr https://fly.io/install.ps1 -useb | iex
```

**Mac/Linux**:
```bash
curl -L https://fly.io/install.sh | sh
```

#### 6.2. Login

```bash
fly auth login
```

#### 6.3. Deploy Order Service

```bash
cd services/order-microservice

# Khởi tạo Fly app (không deploy ngay)
fly launch --no-deploy

# Thiết lập secrets (environment variables)
fly secrets set \
  SPRING_APPLICATION_NAME="order-service" \
  SERVER_PORT="8083" \
  SPRING_DATASOURCE_URL="jdbc:mysql://MYSQL_HOST:MYSQL_PORT/orderservice" \
  SPRING_DATASOURCE_USERNAME="MYSQL_USER" \
  SPRING_DATASOURCE_PASSWORD="MYSQL_PASSWORD" \
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="https://registry-service-production.up.railway.app/eureka/" \
  EUREKA_INSTANCE_HOSTNAME="order-service-yourname.fly.dev" \
  EUREKA_INSTANCE_PREFER_IP="false" \
  EUREKA_NON_SECURE_PORT_ENABLED="false" \
  EUREKA_SECURE_PORT_ENABLED="true" \
  SPRING_RABBITMQ_ADDRESSES="amqp://user:pass@host.cloudamqp.com/vhost" \
  APP_JWT_SECRETKEY="g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ=="

# Deploy
fly deploy
```

#### 6.4. Kiểm Tra

```bash
# Xem status
fly status

# Xem logs
fly logs

# Xem URL
fly info
```

### Phần 7: Deploy Payment Service (Fly.io)

Làm tương tự Order Service:

```bash
cd services/payment-microservice

fly launch --no-deploy

fly secrets set \
  SPRING_APPLICATION_NAME="payment-service" \
  SERVER_PORT="8084" \
  SPRING_DATASOURCE_URL="jdbc:mysql://MYSQL_HOST:MYSQL_PORT/paymentservice" \
  SPRING_DATASOURCE_USERNAME="MYSQL_USER" \
  SPRING_DATASOURCE_PASSWORD="MYSQL_PASSWORD" \
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="https://registry-service-production.up.railway.app/eureka/" \
  EUREKA_INSTANCE_HOSTNAME="payment-service-yourname.fly.dev" \
  EUREKA_INSTANCE_PREFER_IP="false" \
  EUREKA_NON_SECURE_PORT_ENABLED="false" \
  EUREKA_SECURE_PORT_ENABLED="true" \
  SPRING_RABBITMQ_ADDRESSES="amqp://user:pass@host.cloudamqp.com/vhost" \
  APP_JWT_SECRETKEY="g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ=="

fly deploy
```

### Phần 8: Deploy Frontend (Tùy Chọn)

#### 8.1. Build Frontend

```bash
cd frontend
npm install
npm run build
```

#### 8.2. Deploy lên Render Static Site

1. Tạo **Static Site** trên Render
2. Connect repository
3. Cấu hình:
   - **Build Command**: `cd frontend && npm install && npm run build`
   - **Publish Directory**: `frontend/dist`
4. Thêm environment variable:
   ```
   VITE_API_URL=https://gateway-service-production.up.railway.app
   ```

### Lệnh Fly.io Hữu Ích

```bash
# Xem status của app
fly status

# Xem logs
fly logs

# SSH vào container
fly ssh console

# Scale app
fly scale count 1

# List tất cả apps
fly apps list

# Xem thông tin app
fly info
```

---

## Kiểm Tra và Xác Thực

### Health Checks

#### Development/Staging
```bash
# Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Product Service
curl http://localhost:8082/actuator/health

# Order Service
curl http://localhost:8083/actuator/health

# Payment Service
curl http://localhost:8084/actuator/health
```

#### Production
```bash
# Gateway
curl https://gateway-service-production.up.railway.app/actuator/health

# User Service
curl https://user-service.onrender.com/actuator/health

# Product Service
curl https://product-service.onrender.com/actuator/health

# Order Service
curl https://order-service-yourname.fly.dev/actuator/health

# Payment Service
curl https://payment-service-yourname.fly.dev/actuator/health
```

### Kiểm Tra Eureka Dashboard

- **Development**: http://localhost:8761
- **Staging**: http://localhost:8761
- **Production**: https://registry-service-production.up.railway.app

Tất cả services phải hiển thị status **UP** trong Eureka dashboard.

### Test API Endpoints

#### Register User
```bash
curl -X POST https://gateway-service-production.up.railway.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "role": "CUSTOMER"
  }'
```

#### Login
```bash
curl -X POST https://gateway-service-production.up.railway.app/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

#### Get Products (với token)
```bash
curl -X GET https://gateway-service-production.up.railway.app/api/v1/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Checklist Deployment

- [ ] Tất cả services đã deploy thành công
- [ ] Tất cả services hiển thị trong Eureka dashboard
- [ ] Health checks trả về status "UP"
- [ ] Database connections hoạt động
- [ ] RabbitMQ connections hoạt động
- [ ] API Gateway routing đúng
- [ ] JWT authentication hoạt động
- [ ] Frontend kết nối được với backend
- [ ] Test các API endpoints chính

---

## Troubleshooting

### Vấn Đề 1: Services Không Đăng Ký Với Eureka

**Triệu chứng**: Services không hiển thị trong Eureka dashboard

**Giải pháp**:
```bash
# Kiểm tra Eureka URL đúng chưa
echo $EUREKA_CLIENT_SERVICEURL_DEFAULTZONE

# Kiểm tra network connectivity
docker exec user-service curl http://registry-service:8761/eureka/apps

# Kiểm tra environment variables
docker exec user-service env | grep EUREKA
```

### Vấn Đề 2: Lỗi Kết Nối Database

**Triệu chứng**: `CommunicationsException` hoặc connection refused

**Giải pháp**:
```bash
# Kiểm tra MySQL đã khởi động chưa
docker-compose logs mysql

# Kiểm tra MySQL health
docker exec mysql mysqladmin ping -h localhost -uroot -p1234

# Kiểm tra databases đã tạo chưa
docker exec mysql mysql -uroot -p1234 -e "SHOW DATABASES;"
```

### Vấn Đề 3: Lỗi Kết Nối RabbitMQ

**Triệu chứng**: Services không kết nối được RabbitMQ

**Giải pháp**:
```bash
# Kiểm tra RabbitMQ đang chạy
docker-compose logs rabbitmq-broker

# Kiểm tra connections
curl -u guest:guest http://localhost:15672/api/connections

# Kiểm tra queues
curl -u guest:guest http://localhost:15672/api/queues
```

### Vấn Đề 4: Port Đã Được Sử Dụng

**Triệu chứng**: `bind: address already in use`

**Giải pháp**:
```bash
# Windows - Tìm process đang dùng port
netstat -ano | findstr :8080

# Linux/Mac - Tìm process
lsof -i :8080

# Kill process hoặc đổi port trong docker-compose.yml
```

### Vấn Đề 5: Out of Memory

**Triệu chứng**: Container bị crash với OOM error

**Giải pháp**:
Thêm resource limits vào `docker-compose.yml`:
```yaml
services:
  order-service:
    environment:
      JAVA_OPTS: -Xmx512m -Xms256m
    deploy:
      resources:
        limits:
          memory: 1G
```

### Vấn Đề 6: Frontend Không Kết Nối Backend

**Triệu chứng**: CORS errors hoặc network errors

**Giải pháp**:
1. Kiểm tra Gateway đang chạy
2. Kiểm tra API URL trong frontend config
3. Kiểm tra CORS settings trong Gateway
4. Kiểm tra firewall/network rules

### Vấn Đề 7: Production - Service Không Start

**Triệu chứng**: Service crash ngay sau khi deploy

**Giải pháp**:
```bash
# Xem logs chi tiết
fly logs -a order-service

# Kiểm tra environment variables
fly secrets list -a order-service

# Kiểm tra health
fly status -a order-service
```

### Vấn Đề 8: Production - Database Connection Timeout

**Triệu chứng**: Services không kết nối được MySQL trên Railway

**Giải pháp**:
1. Kiểm tra MySQL đã public chưa
2. Kiểm tra firewall rules
3. Kiểm tra connection string đúng format
4. Kiểm tra credentials

---

## Tài Liệu Tham Khảo

- [DEPLOYMENT.md](DEPLOYMENT.md) - Tài liệu deployment chi tiết (tiếng Anh)
- [DEPLOYMENT_QUICK_REFERENCE.md](../DEPLOYMENT_QUICK_REFERENCE.md) - Quick reference cho production
- [README.md](../README.md) - Tài liệu tổng quan hệ thống

---

## Liên Hệ Hỗ Trợ

Nếu gặp vấn đề trong quá trình deploy, vui lòng:
1. Kiểm tra logs của service
2. Kiểm tra Eureka dashboard
3. Xem lại các bước trong tài liệu này
4. Tạo issue trên repository

---

**Chúc bạn deploy thành công! 🚀**


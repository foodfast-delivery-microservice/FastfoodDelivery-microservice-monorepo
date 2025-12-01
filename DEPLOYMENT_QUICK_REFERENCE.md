# Quick Reference - Free Deployment Commands

## 📦 Platforms URLs

- **Railway**: https://railway.app
- **Render**: https://render.com  
- **Fly.io**: https://fly.io
- **CloudAMQP**: https://cloudamqp.com

---

## 🔑 Environment Variables Template

### Railway (Registry & Gateway)

**Registry Service:**
```env
SPRING_APPLICATION_NAME=registry-service
SERVER_PORT=8761
EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
EUREKA_CLIENT_FETCH_REGISTRY=false
```

**Gateway Service:**
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

### Render (User & Product)

**User Service:**
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

**Product Service:**
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

---

## 🚀 Fly.io Commands

### Install CLI (Windows)
```powershell
iwr https://fly.io/install.ps1 -useb | iex
```

### Login
```bash
fly auth login
```

### Deploy Order Service
```bash
cd "d:\WORKSPACE\PROJECT JAVA\FastfoodDelivery-microservice-monorepo\services\order-microservice"

fly launch --no-deploy

fly secrets set \
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

fly deploy
```

### Deploy Payment Service
```bash
cd "d:\WORKSPACE\PROJECT JAVA\FastfoodDelivery-microservice-monorepo\services\payment-microservice"

fly launch --no-deploy

fly secrets set \
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

### Useful Fly Commands
```bash
fly status              # Check app status
fly logs               # View logs
fly ssh console        # SSH into container
fly scale count 1      # Scale to 1 instance
fly apps list          # List all apps
```

---

## 🗄️ MySQL Setup (Railway)

```sql
CREATE DATABASE IF NOT EXISTS userservice;
CREATE DATABASE IF NOT EXISTS productmicroservice;
CREATE DATABASE IF NOT EXISTS orderservice;
CREATE DATABASE IF NOT EXISTS paymentservice;

SHOW DATABASES;
```

---

## ✅ Testing Commands

### Health Checks
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

### API Tests
```bash
# Register User
curl -X POST https://gateway-service-production.up.railway.app/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password123"}'

# Login
curl -X POST https://gateway-service-production.up.railway.app/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

---

## 📋 Deployment Checklist

- [ ] Push code to GitHub
- [ ] Create Railway project + MySQL
- [ ] Create CloudAMQP instance
- [ ] Deploy Registry Service (Railway)
- [ ] Deploy Gateway Service (Railway)
- [ ] Deploy User Service (Render)
- [ ] Deploy Product Service (Render)
- [ ] Deploy Order Service (Fly.io)
- [ ] Deploy Payment Service (Fly.io)
- [ ] Verify all services in Eureka Dashboard
- [ ] Test API endpoints

---

## 🔗 Important URLs to Save

```
Eureka Dashboard: https://registry-service-production.up.railway.app
API Gateway: https://gateway-service-production.up.railway.app
User Service: https://user-service.onrender.com
Product Service: https://product-service.onrender.com
Order Service: https://order-service-yourname.fly.dev
Payment Service: https://payment-service-yourname.fly.dev
```

---

## 💰 Cost: $0/month ✅

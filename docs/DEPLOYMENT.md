# Deployment Guide

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Docker Deployment](#docker-deployment)
3. [Local Development Deployment](#local-development-deployment)
4. [Production Deployment](#production-deployment)
5. [Configuration](#configuration)
6. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- **Docker**: Version 20.10+
- **Docker Compose**: Version 2.0+
- **Java**: JDK 17+ (for local development)
- **Maven**: 3.8+ (for building from source)
- **Node.js**: 18+ (for frontend development)
- **Git**: For version control

### System Requirements
- **CPU**: 4 cores minimum (8 cores recommended)
- **RAM**: 8GB minimum (16GB recommended)
- **Disk**: 20GB free space
- **Network**: Stable internet connection for downloading images

## Docker Deployment

### Production Deployment with Docker Compose

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd FastfoodDelivery-microservice-monorepo
```

#### 2. Configure Environment Variables (Optional)
Create a `.env` file in the root directory:

```env
# MySQL Configuration
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_HOST=mysql-db

# JWT Secret Key (Generate a new one for production!)
APP_JWT_SECRETKEY=your_base64_encoded_secret_key

# RabbitMQ Configuration
SPRING_RABBITMQ_HOST=rabbitmq-broker
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# Eureka Configuration
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://registry-service:8761/eureka/
```

#### 3. Start All Services
```bash
docker-compose up -d
```

This command will:
- Pull required Docker images
- Build microservice images from Dockerfiles
- Start all services in the correct order
- Create necessary networks and volumes

#### 4. Verify Deployment
```bash
# Check all containers are running
docker-compose ps

# Expected output: All services should show "Up" status
# NAME                   STATUS
# registry-service       Up
# mysql                  Up (healthy)
# rabbitmq-broker        Up
# gateway-service        Up
# user-service           Up
# product-service        Up
# order-service          Up
# payment-service        Up
# frontend               Up
```

#### 5. Access Services
- **Frontend Application**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Eureka Dashboard**: http://localhost:8761
- **RabbitMQ Management UI**: http://localhost:15672 (guest/guest)

#### 6. Initialize Sample Data (Optional)
```bash
# Execute SQL scripts for sample data
docker exec -i mysql mysql -uroot -p1234 < scripts/sample-data.sql
```

### View Logs
```bash
# View all service logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f user-service
docker-compose logs -f order-service

# View last 100 lines
docker-compose logs --tail=100 payment-service
```

### Stop Services
```bash
# Stop all services (preserves volumes)
docker-compose stop

# Stop and remove containers (preserves volumes)
docker-compose down

# Stop and remove everything including volumes
docker-compose down -v
```

## Local Development Deployment

### Infrastructure Setup

#### 1. Start MySQL Database
```bash
docker run -d \
  --name mysql-dev \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=userservice \
  -v mysql-dev-data:/var/lib/mysql \
  mysql:8.0
```

Create additional databases:
```bash
docker exec -i mysql-dev mysql -uroot -p1234 <<EOF
CREATE DATABASE IF NOT EXISTS productmicroservice;
CREATE DATABASE IF NOT EXISTS orderservice;
CREATE DATABASE IF NOT EXISTS paymentservice;
EOF
```

#### 2. Start RabbitMQ
```bash
docker run -d \
  --name rabbitmq-dev \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3.13-management
```

### Running Microservices Locally

#### 1. Start Registry Service (Eureka)
```bash
cd services/registry-service
mvn clean spring-boot:run
```
Wait for Eureka to start (check http://localhost:8761)

#### 2. Start API Gateway
```bash
cd services/gateway-service
mvn clean spring-boot:run
```

#### 3. Start User Service
```bash
cd services/user-microservice
mvn clean spring-boot:run
```

#### 4. Start Product Service
```bash
cd services/product-microservice
mvn clean spring-boot:run
```

#### 5. Start Order Service
```bash
cd services/order-microservice

# Note: Check application.properties
# Ensure RabbitMQ host is set to localhost if not using Docker
# spring.rabbitmq.host=localhost

mvn clean spring-boot:run
```

#### 6. Start Payment Service
```bash
cd services/payment-microservice
mvn clean spring-boot:run
```

#### 7. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at http://localhost:5173 (Vite default port)

### Development Tips

#### Hot Reload for Backend
Use Spring Boot DevTools for automatic restart:
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

#### Hot Reload for Frontend
Vite automatically provides Hot Module Replacement (HMR). Just save your files and see changes instantly.

## Production Deployment

### Best Practices for Production

#### 1. Security Hardening

**Generate Strong JWT Secret**
```bash
# Generate a secure random secret key
openssl rand -base64 64
```

**Update docker-compose.yml or use environment variables**
```yaml
environment:
  APP_JWT_SECRETKEY: <your-generated-secret>
```

**Change Default Passwords**
- MySQL root password
- RabbitMQ credentials

#### 2. Database Configuration

**Use Separate MySQL Instances** (recommended for production)
```yaml
# Instead of single MySQL container, use managed MySQL service
# Example: AWS RDS, Google Cloud SQL, Azure Database for MySQL
```

**Enable SSL/TLS for MySQL connections**
```properties
spring.datasource.url=jdbc:mysql://host:3306/db?useSSL=true&requireSSL=true
```

#### 3. Resource Limits

Add resource limits to docker-compose.yml:
```yaml
services:
  user-service:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

#### 4. Logging Configuration

**Centralized Logging** (ELK Stack, Splunk, CloudWatch)
```properties
# application.properties for production
logging.level.root=INFO
logging.level.com.example=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

#### 5. Health Checks & Monitoring

**Configure Health Checks**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

**Monitoring Tools**
- Prometheus + Grafana for metrics
- ELK Stack for log aggregation
- Jaeger or Zipkin for distributed tracing

#### 6. Reverse Proxy

Use Nginx as reverse proxy:
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://frontend:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/ {
        proxy_pass http://gateway-service:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### 7. SSL/TLS Configuration

Use Let's Encrypt with Certbot:
```bash
# Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d yourdomain.com
```

## Configuration

### Environment-Specific Configuration

#### Development
```properties
# application-dev.properties
spring.jpa.show-sql=true
logging.level.root=DEBUG
```

#### Staging
```properties
# application-staging.properties
spring.jpa.show-sql=false
logging.level.root=INFO
```

#### Production
```properties
# application-prod.properties
spring.jpa.show-sql=false
logging.level.root=WARN
spring.datasource.hikari.maximum-pool-size=50
```

### Database Migration

**Using Flyway for Production**
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.jpa.hibernate.ddl-auto=validate
```

Migration scripts:
```sql
-- V1__initial_schema.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'MERCHANT', 'ADMIN') NOT NULL,
    approved BOOLEAN DEFAULT FALSE
);
```

## Troubleshooting

### Common Issues

#### 1. Services Not Registering with Eureka
**Symptom**: Services show as DOWN in Eureka dashboard

**Solution**:
```bash
# Check service can reach Eureka
docker exec user-service curl http://registry-service:8761/eureka/apps

# Verify network connectivity
docker network inspect microservice-net

# Check environment variables
docker exec user-service env | grep EUREKA
```

#### 2. Database Connection Errors
**Symptom**: `CommunicationsException` or connection refused

**Solution**:
```bash
# Wait for MySQL to be fully started
docker-compose logs mysql

# Check MySQL health
docker exec mysql mysqladmin ping -h localhost -uroot -p1234

# Verify database exists
docker exec mysql mysql -uroot -p1234 -e "SHOW DATABASES;"
```

#### 3. RabbitMQ Connection Issues
**Symptom**: Services can't connect to RabbitMQ

**Solution**:
```bash
# Check RabbitMQ is running
docker-compose logs rabbitmq-broker

# Verify RabbitMQ connections
curl -u guest:guest http://localhost:15672/api/connections

# Check queue status
curl -u guest:guest http://localhost:15672/api/queues
```

#### 4. Port Already in Use
**Symptom**: `bind: address already in use`

**Solution**:
```bash
# Find process using the port
netstat -tulpn | grep :8080  # Linux
netstat -ano | findstr :8080  # Windows

# Kill the process or change port in docker-compose.yml
```

#### 5. Out of Memory Errors
**Symptom**: Container crashes with OOM error

**Solution**:
```yaml
# Increase memory limit in docker-compose.yml
services:
  order-service:
    environment:
      JAVA_OPTS: -Xmx512m -Xms256m
    deploy:
      resources:
        limits:
          memory: 1G
```

#### 6. Frontend Can't Connect to Backend
**Symptom**: CORS errors or network errors

**Solution**:
```bash
# Verify Gateway is running
curl http://localhost:8080/actuator/health

# Check API Gateway routing
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/users/me

# Enable CORS in Gateway if needed
```

### Useful Commands

```bash
# Restart a specific service
docker-compose restart user-service

# Rebuild and restart a service
docker-compose up -d --build user-service

# View service resource usage
docker stats

# Clean up unused resources
docker system prune -a

# Backup MySQL database
docker exec mysql mysqldump -uroot -p1234 --all-databases > backup.sql

# Restore MySQL database
docker exec -i mysql mysql -uroot -p1234 < backup.sql
```

## Scaling

### Horizontal Scaling

Scale services based on load:
```bash
# Scale order service to 3 instances
docker-compose up -d --scale order-service=3

# Scale payment service to 2 instances
docker-compose up -d --scale payment-service=2
```

**Note**: Ensure services are stateless and use load balancing (Eureka + Gateway handle this automatically)

### Kubernetes Deployment

For production at scale, consider Kubernetes:
```yaml
# order-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: order-service:latest
        ports:
        - containerPort: 8083
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:mysql://mysql:3306/orderservice
```

---

**For questions or issues, please check the [main README](../README.md) or open an issue.**

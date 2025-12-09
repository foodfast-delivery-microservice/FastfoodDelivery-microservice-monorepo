# ========================================
# Script Chuẩn Bị Deploy Lên Render
# ========================================
# Mục đích: Tự động hóa việc chuẩn bị code để deploy lên Render free tier
# Tác giả: Antigravity AI
# Ngày: 2025-12-02

Write-Host "🚀 Bắt đầu chuẩn bị deploy lên Render..." -ForegroundColor Green
Write-Host ""

# 1. Kiểm tra Git
Write-Host "📋 Bước 1: Kiểm tra Git status..." -ForegroundColor Cyan
git status

Write-Host ""
$continue = Read-Host "Bạn có muốn tạo branch mới 'deploy/render-free-tier'? (y/n)"
if ($continue -eq 'y') {
    git checkout -b deploy/render-free-tier
    Write-Host "✅ Đã tạo branch mới" -ForegroundColor Green
} else {
    Write-Host "⏭️ Bỏ qua tạo branch" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "📋 Bước 2: Thêm PostgreSQL driver vào pom.xml..." -ForegroundColor Cyan

# Danh sách services cần update
$services = @(
    "services\user-microservice",
    "services\product-microservice",
    "services\order-microservice",
    "services\gateway-service"
)

$postgresqlDependency = @"

        <!-- PostgreSQL Driver for Render deployment -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
"@

foreach ($service in $services) {
    $pomPath = "$service\pom.xml"
    if (Test-Path $pomPath) {
        $content = Get-Content $pomPath -Raw
        
        # Kiểm tra xem đã có PostgreSQL dependency chưa
        if ($content -notmatch "org.postgresql") {
            Write-Host "  ➕ Thêm PostgreSQL vào $service..." -ForegroundColor Yellow
            
            # Tìm vị trí </dependencies> cuối cùng và thêm vào trước đó
            $content = $content -replace '(\s*)</dependencies>', "$postgresqlDependency`$1</dependencies>"
            Set-Content -Path $pomPath -Value $content
            
            Write-Host "  ✅ Đã thêm PostgreSQL vào $service" -ForegroundColor Green
        } else {
            Write-Host "  ⏭️ $service đã có PostgreSQL dependency" -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "📋 Bước 3: Tạo application-prod.properties cho các services..." -ForegroundColor Cyan

# Template cho application-prod.properties
$prodPropertiesTemplate = @"
# ========================================
# Production Configuration for Render
# ========================================

# Server
server.port=`${PORT:8081}

# Database - Render PostgreSQL
spring.datasource.url=`${DATABASE_URL}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update

# Connection Pool - Optimized for free tier
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000

# JWT Secret
app.jwt.secretKey=`${APP_JWT_SECRETKEY}

# Disable Eureka for Render deployment
eureka.client.enabled=false
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

# Actuator - Only expose essential endpoints
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized

# Logging
logging.level.root=INFO
logging.level.org.springframework.web=INFO
logging.level.org.hibernate=WARN
"@

# Tạo application-prod.properties cho từng service
$serviceConfigs = @{
    "services\user-microservice" = 8081
    "services\product-microservice" = 8082
    "services\order-microservice" = 8083
    "services\gateway-service" = 8080
}

foreach ($service in $serviceConfigs.Keys) {
    $port = $serviceConfigs[$service]
    $prodPropsPath = "$service\src\main\resources\application-prod.properties"
    
    # Tạo thư mục nếu chưa có
    $resourcesDir = "$service\src\main\resources"
    if (-not (Test-Path $resourcesDir)) {
        New-Item -ItemType Directory -Path $resourcesDir -Force | Out-Null
    }
    
    # Tạo file với port tương ứng
    $content = $prodPropertiesTemplate -replace 'PORT:8081', "PORT:$port"
    
    # Thêm config đặc biệt cho order-service
    if ($service -eq "services\order-microservice") {
        $content += @"

# Service URLs for inter-service communication
app.services.user-service.url=`${USER_SERVICE_URL:https://user-service.onrender.com}
app.services.product-service.url=`${PRODUCT_SERVICE_URL:https://product-service.onrender.com}
"@
    }
    
    # Thêm config đặc biệt cho gateway-service
    if ($service -eq "services\gateway-service") {
        $content += @"

# Service URLs (hardcoded - no Eureka)
app.services.user-service.url=`${USER_SERVICE_URL:https://user-service.onrender.com}
app.services.product-service.url=`${PRODUCT_SERVICE_URL:https://product-service.onrender.com}
app.services.order-service.url=`${ORDER_SERVICE_URL:https://order-service.onrender.com}

# Gateway Routes
spring.cloud.gateway.routes[0].id=user-service
spring.cloud.gateway.routes[0].uri=`${app.services.user-service.url}
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/v1/users/**,/api/v1/auth/**

spring.cloud.gateway.routes[1].id=product-service
spring.cloud.gateway.routes[1].uri=`${app.services.product-service.url}
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/v1/products/**,/api/v1/categories/**

spring.cloud.gateway.routes[2].id=order-service
spring.cloud.gateway.routes[2].uri=`${app.services.order-service.url}
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/v1/orders/**
"@
    }
    
    Set-Content -Path $prodPropsPath -Value $content
    Write-Host "  ✅ Đã tạo $prodPropsPath" -ForegroundColor Green
}

Write-Host ""
Write-Host "📋 Bước 4: Tối ưu Dockerfile cho các services..." -ForegroundColor Cyan

$dockerfileTemplate = @"
# Multi-stage build for optimized image size
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage - minimal image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Reduce memory footprint for Render free tier (512MB RAM)
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (will be overridden by Render's PORT env var)
EXPOSE PORT_PLACEHOLDER

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:PORT_PLACEHOLDER/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java `$JAVA_OPTS -jar app.jar"]
"@

foreach ($service in $serviceConfigs.Keys) {
    $port = $serviceConfigs[$service]
    $dockerfilePath = "$service\Dockerfile"
    
    # Thay PORT_PLACEHOLDER bằng port thực tế
    $content = $dockerfileTemplate -replace 'PORT_PLACEHOLDER', $port
    
    Set-Content -Path $dockerfilePath -Value $content
    Write-Host "  ✅ Đã tạo/cập nhật $dockerfilePath" -ForegroundColor Green
}

Write-Host ""
Write-Host "📋 Bước 5: Tạo .dockerignore để tối ưu build..." -ForegroundColor Cyan

$dockerignoreContent = @"
# Git
.git
.gitignore

# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties

# IDE
.idea/
.vscode/
*.iml
*.ipr
*.iws

# OS
.DS_Store
Thumbs.db

# Logs
*.log

# Documentation
*.md
docs/

# Tests
src/test/
"@

foreach ($service in $serviceConfigs.Keys) {
    $dockerignorePath = "$service\.dockerignore"
    Set-Content -Path $dockerignorePath -Value $dockerignoreContent
    Write-Host "  ✅ Đã tạo $dockerignorePath" -ForegroundColor Green
}

Write-Host ""
Write-Host "📋 Bước 6: Tạo file hướng dẫn deployment..." -ForegroundColor Cyan

$deploymentGuide = @"
# 🚀 Render Deployment Guide

## Các file đã được chuẩn bị:

✅ PostgreSQL driver đã được thêm vào pom.xml
✅ application-prod.properties đã được tạo cho mỗi service
✅ Dockerfile đã được tối ưu cho Render free tier (512MB RAM)
✅ .dockerignore đã được tạo để giảm build time

## Bước tiếp theo:

### 1. Commit và push code

``````bash
git add .
git commit -m "feat: prepare for Render deployment"
git push origin deploy/render-free-tier
``````

### 2. Tạo tài khoản Render

- Truy cập: https://render.com
- Sign up với GitHub account
- Authorize Render truy cập repository

### 3. Tạo PostgreSQL Database

1. Dashboard → New → PostgreSQL
2. Name: fastfood-db
3. Region: Singapore
4. Plan: Free
5. Lưu lại **Internal Database URL**

### 4. Deploy các services

Thứ tự deploy:
1. User Service (port 8081)
2. Product Service (port 8082)
3. Order Service (port 8083)
4. Gateway Service (port 8080)

Cho mỗi service:
- Root Directory: services/[service-name]
- Runtime: Docker
- Plan: Free
- Environment Variables:
  - PORT=[8081/8082/8083/8080]
  - SPRING_PROFILES_ACTIVE=prod
  - DATABASE_URL=[Internal Database URL]
  - APP_JWT_SECRETKEY=g5rhnoLF2O4S/p5wPKY9ojbK3X2g6ifB6cG9lkaLUg9quMDtO1PSI4J6biyJHZ5uAnyTbuTyaWRpHy+BADU7NQ==

Cho Order Service, thêm:
  - USER_SERVICE_URL=https://user-service-xxxx.onrender.com
  - PRODUCT_SERVICE_URL=https://product-service-xxxx.onrender.com

Cho Gateway Service, thêm:
  - USER_SERVICE_URL=https://user-service-xxxx.onrender.com
  - PRODUCT_SERVICE_URL=https://product-service-xxxx.onrender.com
  - ORDER_SERVICE_URL=https://order-service-xxxx.onrender.com

### 5. Test deployment

``````bash
# Health check
curl https://gateway-service-xxxx.onrender.com/actuator/health

# Register user
curl -X POST https://gateway-service-xxxx.onrender.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password123","role":"CUSTOMER"}'
``````

## Troubleshooting

### Service bị crash (Out of Memory)
→ Kiểm tra logs, có thể cần giảm JAVA_OPTS trong Dockerfile

### Database connection failed
→ Đảm bảo dùng Internal Database URL (không phải External)

### Build timeout
→ Kiểm tra .dockerignore, đảm bảo không build thư mục target/

## Chi phí: $0/tháng ✅

---

Xem thêm chi tiết tại: 
- deployment_guide_for_freshers.md
- render_deployment_checklist.md
- deployment_cost_comparison.md
"@

Set-Content -Path "RENDER_DEPLOYMENT_GUIDE.md" -Value $deploymentGuide
Write-Host "  ✅ Đã tạo RENDER_DEPLOYMENT_GUIDE.md" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ HOÀN TẤT CHUẨN BỊ!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Các file đã được tạo/cập nhật:" -ForegroundColor Cyan
Write-Host "  - pom.xml (PostgreSQL dependency)" -ForegroundColor White
Write-Host "  - application-prod.properties (mỗi service)" -ForegroundColor White
Write-Host "  - Dockerfile (tối ưu cho Render)" -ForegroundColor White
Write-Host "  - .dockerignore (giảm build time)" -ForegroundColor White
Write-Host "  - RENDER_DEPLOYMENT_GUIDE.md" -ForegroundColor White
Write-Host ""
Write-Host "🚀 Bước tiếp theo:" -ForegroundColor Cyan
Write-Host "  1. Review các thay đổi: git status" -ForegroundColor White
Write-Host "  2. Commit code: git add . && git commit -m 'feat: prepare for Render deployment'" -ForegroundColor White
Write-Host "  3. Push lên GitHub: git push origin deploy/render-free-tier" -ForegroundColor White
Write-Host "  4. Đọc RENDER_DEPLOYMENT_GUIDE.md để deploy" -ForegroundColor White
Write-Host ""
Write-Host "📚 Tài liệu tham khảo:" -ForegroundColor Cyan
Write-Host "  - deployment_guide_for_freshers.md (trong .gemini/antigravity/brain/...)" -ForegroundColor White
Write-Host "  - render_deployment_checklist.md" -ForegroundColor White
Write-Host "  - deployment_cost_comparison.md" -ForegroundColor White
Write-Host ""
Write-Host "Good luck! 🎉" -ForegroundColor Green

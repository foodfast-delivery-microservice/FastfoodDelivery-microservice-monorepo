# 🚀 GitHub Actions Workflows

## 📋 Workflows Overview

Dự án này có **3 workflows** được thiết kế tối ưu cho microservices:

### 1️⃣ **ci.yml** - Continuous Integration
**Trigger**: Push/PR vào `main`, `develop`, `feature/**`, `bugfix/**`

**Features**:
- ✅ Build 7 services song song (matrix strategy)
- ✅ Smart change detection - chỉ build services có thay đổi
- ✅ Run unit tests
- ✅ Build frontend React
- ✅ Upload test results & JAR artifacts
- ✅ Code quality check (PR only)
- ✅ Build summary với status table

**Time**: ~5-8 phút

---

### 2️⃣ **pr-check.yml** - Quick PR Validation
**Trigger**: Pull Request vào `main`, `develop`

**Features**:
- ✅ Lightweight & fast
- ✅ Chỉ build services có thay đổi
- ✅ Skip tests để nhanh hơn
- ✅ PR summary

**Time**: ~2-3 phút

**Use case**: Quick feedback cho PR, full CI sẽ chạy sau khi merge

---

### 3️⃣ **cd.yml** - Continuous Deployment
**Trigger**: 
- Push vào `main`
- Manual trigger (workflow_dispatch)

**Features**:
- ✅ Build tất cả services
- ✅ Verify JAR files
- ✅ Deploy to Render (khi setup)
- ✅ Deployment summary
- ✅ Manual deployment với environment selection

**Time**: ~10-15 phút

---

## 🎯 Workflow Strategy

```
Feature Branch
     │
     ├─ Push → pr-check.yml (Quick validation)
     │
     ├─ Create PR → ci.yml (Full CI)
     │
     ├─ Merge to develop → ci.yml
     │
     └─ Merge to main → ci.yml + cd.yml (Deploy)
```

---

## 🔧 Services Included

Workflows tự động detect và build các services sau:

1. **user-microservice** (Port 8081)
2. **product-microservice** (Port 8082)
3. **order-microservice** (Port 8083)
4. **payment-microservice** (Port 8084)
5. **drone-microservice** (Port 8085)
6. **gateway-service** (Port 8080)
7. **registry-service** (Port 8761)

Plus:
- **frontend** (React app)

---

## 📊 Change Detection

Workflows sử dụng **smart change detection**:

```yaml
# Chỉ build services có thay đổi
if: steps.check-changes.outputs.changed == 'true'
```

**Ví dụ**:
- Sửa code trong `user-microservice` → Chỉ build user-microservice
- Sửa `README.md` → Không build gì cả
- First push/PR → Build tất cả

**Lợi ích**:
- ⚡ Tiết kiệm thời gian build
- 💰 Tiết kiệm GitHub Actions minutes
- 🚀 Faster feedback

---

## 🚀 Quick Start

### Step 1: Commit workflows
```bash
git add .github/
git commit -m "ci: add GitHub Actions workflows"
git push origin develop
```

### Step 2: Create PR
```bash
# Create PR from develop to main
# → pr-check.yml sẽ chạy (quick validation)
# → ci.yml sẽ chạy (full CI)
```

### Step 3: Merge to main
```bash
# Merge PR
# → ci.yml chạy
# → cd.yml chạy (deployment)
```

---

## 📈 Workflow Comparison

| Feature | ci.yml | pr-check.yml | cd.yml |
|---------|--------|--------------|--------|
| **Trigger** | Push/PR | PR only | Push to main |
| **Build** | ✅ All | ✅ Changed only | ✅ All |
| **Tests** | ✅ Yes | ❌ No | ❌ No |
| **Frontend** | ✅ Yes | ❌ No | ❌ No |
| **Deploy** | ❌ No | ❌ No | ✅ Yes |
| **Time** | 5-8 min | 2-3 min | 10-15 min |
| **Purpose** | Full CI | Quick check | Deployment |

---

## 🎨 Customization

### Thêm service mới

Edit `.github/workflows/ci.yml`:

```yaml
strategy:
  matrix:
    service:
      - user-microservice
      - product-microservice
      # ... existing services
      - your-new-service  # Add here
```

### Thay đổi Java version

Edit tất cả workflows:

```yaml
- name: ☕ Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '21'  # Change to 21
```

### Enable deployment

Edit `.github/workflows/cd.yml`:

Uncomment deployment code và setup secrets:
```yaml
# Uncomment and configure:
curl -X POST "https://api.render.com/v1/services/${{ secrets.RENDER_SERVICE_ID }}/deploys" \
  -H "Authorization: Bearer ${{ secrets.RENDER_API_KEY }}"
```

---

## 🔐 GitHub Secrets (Optional)

Nếu muốn enable auto-deployment, setup secrets sau:

```
RENDER_API_KEY              # Render API key
RENDER_USER_SERVICE_ID      # Service ID for user-microservice
RENDER_PRODUCT_SERVICE_ID   # Service ID for product-microservice
RENDER_ORDER_SERVICE_ID     # Service ID for order-microservice
RENDER_GATEWAY_SERVICE_ID   # Service ID for gateway-service
```

**Cách lấy**:
1. Render Dashboard → Account Settings → API Keys
2. Render Dashboard → Service → Copy service ID từ URL

---

## 📊 Viewing Results

### GitHub Actions Tab
1. Repository → **Actions** tab
2. Click vào workflow run
3. Xem logs và summary

### Build Summary
Mỗi workflow tạo summary với:
- ✅ Build status cho từng service
- 📊 Test results
- 📦 Artifacts
- 🔗 Links

### Artifacts
Download artifacts:
```bash
# Via GitHub CLI
gh run download <run-id>

# Via UI
Actions → Run → Artifacts section
```

---

## 🔧 Troubleshooting

### ❌ Build fail - "pom.xml not found"

**Fix**: Kiểm tra `working-directory`:
```yaml
working-directory: ./services/user-microservice
```

### ❌ Change detection không hoạt động

**Fix**: Đảm bảo có `fetch-depth: 0`:
```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0
```

### ❌ Tests fail

**Fix**: Chạy local trước:
```bash
cd services/user-microservice
mvn test
```

---

## 💡 Best Practices

### 1. Branch Protection
Setup cho `main` branch:
- ✅ Require status checks to pass
- ✅ Require PR before merge
- ✅ Require review

### 2. Commit Messages
Follow conventional commits:
```
feat: add new API endpoint
fix: resolve null pointer exception
ci: update workflow
docs: update README
```

### 3. PR Strategy
- Create small, focused PRs
- Wait for `pr-check.yml` to pass
- Review `ci.yml` results before merge

---

## 📚 Additional Resources

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Maven CI/CD Best Practices](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Microservices CI/CD Patterns](https://microservices.io/patterns/deployment/continuous-deployment.html)

---

## ✅ Checklist

- [ ] Workflows committed to repository
- [ ] First CI run successful
- [ ] PR check working
- [ ] Branch protection enabled
- [ ] Team understands workflow strategy
- [ ] (Optional) Deployment secrets configured

---

**Happy CI/CD! 🎉**

Last updated: 2025-12-02

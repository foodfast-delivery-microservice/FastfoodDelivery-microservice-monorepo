# ⚡ CI/CD Quick Reference

## 📋 3 Workflows

```
ci.yml          → Full CI (Build + Test)
pr-check.yml    → Quick PR validation  
cd.yml          → Deployment
```

## 🎯 When They Run

```
Feature branch push    → pr-check.yml (2-3 min)
Create PR             → ci.yml (5-8 min)
Merge to develop      → ci.yml
Merge to main         → ci.yml + cd.yml
```

## 🚀 Quick Start

```bash
# 1. Commit workflows
git add .github/
git commit -m "ci: add workflows"
git push origin develop

# 2. Create PR
# → Workflows auto-run

# 3. Check results
# GitHub → Actions tab
```

## 📊 Services (7 total)

```
✅ user-microservice
✅ product-microservice  
✅ order-microservice
✅ payment-microservice
✅ Drone-service
✅ gateway-service
✅ registry-service
✅ frontend (React)
```

## 💡 Key Features

**Smart Change Detection**
- Chỉ build services có thay đổi
- Tiết kiệm thời gian & costs

**Matrix Build**
- Build 7 services song song
- Fast feedback

**Artifacts**
- JAR files (7 days)
- Test results (7 days)

## 🔧 Common Tasks

### View workflow status
```bash
gh workflow list
gh run list
```

### Download artifacts
```bash
gh run download <run-id>
```

### Trigger manual deploy
```
Actions → cd.yml → Run workflow
```

## ✅ Checklist

- [ ] Workflows committed
- [ ] First run successful
- [ ] PR check working
- [ ] Team notified

---

**Full docs**: `.github/README.md`

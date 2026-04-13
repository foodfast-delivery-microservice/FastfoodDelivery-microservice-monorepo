# ⚡ Quick Start: Test Email với Gmail Thật

## 🎯 3 Bước Đơn Giản

### Bước 1: Tạo Gmail App Password (5 phút)

1. Vào: https://myaccount.google.com/apppasswords
2. Chọn "Mail" → "Other" → Nhập tên: "Notification Service"
3. Click "Generate"
4. **Copy App Password** (16 ký tự)

---

### Bước 2: Cấu Hình (2 phút)

**Mở file:** `services/notification-service/src/main/resources/application.properties`

**Thay đổi 2 dòng này:**

```properties
spring.mail.username=your-email@gmail.com        # ← Thay bằng email của bạn
spring.mail.password=your-app-password           # ← Thay bằng App Password vừa copy
```

**Ví dụ:**

```properties
spring.mail.username=nguyenvana@gmail.com
spring.mail.password=abcd efgh ijkl mnop
```

---

### Bước 3: Chạy và Test (1 phút)

```bash
# 1. Build
cd services/notification-service
mvn clean install

# 2. Run
mvn spring-boot:run
```

**Test trên Postman:**

- **URL:** `POST http://localhost:8086/api/v1/test/email/success`
- **Body:**
  ```json
  {
    "orderId": 12345,
    "paymentId": 67890,
    "userId": 1,
    "email": "your-email@gmail.com",
    "amount": 150000,
    "transactionId": "TXN_TEST_123"
  }
  ```

**Kiểm tra inbox Gmail của bạn!** 📧

---

## ⚠️ Lưu Ý Quan Trọng

1. **PHẢI bật 2-Step Verification** trước khi tạo App Password
2. **Dùng App Password**, không dùng password thường
3. Email có thể vào **Spam** lần đầu - hãy kiểm tra cả Spam folder

---

**Xem hướng dẫn chi tiết:** `GMAIL_SETUP_GUIDE.md`

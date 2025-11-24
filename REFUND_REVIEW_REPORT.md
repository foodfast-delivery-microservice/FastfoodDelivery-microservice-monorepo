# BÁO CÁO REVIEW CHỨC NĂNG HOÀN TIỀN (REFUND)

## 📋 TỔNG QUAN

Chức năng hoàn tiền hiện tại đã có một số thành phần cơ bản nhưng **chưa hoàn thiện** và còn thiếu nhiều phần quan trọng để hoạt động đầy đủ.

---

## ✅ CÁC PHẦN ĐÃ CÓ

### 1. **Payment Service - Xử lý refund**
- ✅ `ProcessRefundUseCase`: Xử lý logic refund payment
- ✅ `OrderRefundRequestListener`: Listener nhận event từ order service
- ✅ `Payment.refund()`: Domain logic validate và update status
- ✅ `PaymentRefundedEvent`: Event phản hồi về order service
- ✅ Idempotency check: Tránh duplicate refund request

### 2. **Order Service - Status Management**
- ✅ `OrderStatus.REFUNDED`: Enum status đã có
- ✅ `UpdateOrderStatusUseCase`: Có logic chuyển status sang REFUNDED
- ✅ Status transition validation: Chỉ cho phép DELIVERED → REFUNDED

---

## ❌ CÁC VẤN ĐỀ VÀ THIẾU SÓT

### 🔴 **CRITICAL - Thiếu hoàn toàn**

#### 1. **Thiếu API Endpoint để trigger refund**
- ❌ Không có REST API endpoint để user/admin/merchant yêu cầu refund
- ❌ Không có use case để tạo refund request từ order service
- **Impact**: Không thể trigger refund từ bên ngoài

**Cần thêm:**
```java
// OrderManagementController.java
@PostMapping("/{orderId}/refund")
public ResponseEntity<RefundResponse> requestRefund(...)
```

#### 2. **Thiếu publish OrderRefundRequestEvent từ Order Service**
- ❌ Khi order status chuyển sang REFUNDED, không publish event đến payment service
- ❌ `UpdateOrderStatusUseCase.updateOrderStatus()` chỉ set status, không tạo refund event
- **Impact**: Payment service không nhận được yêu cầu refund

**Cần thêm trong `UpdateOrderStatusUseCase`:**
```java
case REFUNDED:
    order.setStatus(OrderStatus.REFUNDED);
    // TODO: Publish OrderRefundRequestEvent to payment service
    break;
```

#### 3. **Thiếu listener cho PaymentRefundedEvent trong Order Service**
- ❌ `PaymentEventListener` chỉ có `handlePaymentSuccess` và `handlePaymentFailed`
- ❌ Không có `handlePaymentRefunded` để nhận xác nhận từ payment service
- **Impact**: Order service không biết payment đã được refund thành công

**Cần thêm:**
```java
@RabbitListener(queues = RabbitMQConfig.PAYMENT_REFUNDED_QUEUE)
public void handlePaymentRefunded(PaymentRefundedEvent event)
```

#### 4. **Thiếu cấu hình RabbitMQ cho refund events**
- ❌ Order service không có queue/binding cho `payment.refunded`
- ❌ Order service không có routing key cho `order.refund.request`
- **Impact**: Events không thể được route đúng

---

### 🟡 **HIGH PRIORITY - Bug và Logic Issues**

#### 5. **Bug: OutboxEventRelay của Payment Service - Event type mismatch**
**File:** `services/payment-microservice/src/main/java/com/example/payment/infrastructure/messaging/OutboxEventRelay.java`

**Vấn đề:**
```java
// Line 47: Check "PAYMENT_REFUND"
} else if ("PAYMENT_REFUND".equals(event.getType())) {
    routingKey = RabbitMQConfig.PAYMENT_REFUNDED_ROUTING_KEY;

// Nhưng ProcessRefundUseCase tạo event với type "PAYMENT_REFUNDED" (line 49)
.type("PAYMENT_REFUNDED")
```

**Fix:**
```java
} else if ("PAYMENT_REFUNDED".equals(event.getType())) {
```

#### 6. **OutboxEventRelay của Order Service - Chỉ publish ORDER_CREATED**
**File:** `services/order-microservice/src/main/java/com/example/order_service/infrastructure/messaging/OutboxEventRelay.java`

**Vấn đề:**
- Chỉ có logic publish `ORDER_CREATED_ROUTING_KEY`
- Không có logic để publish `order.refund.request` event

**Cần thêm:**
```java
String routingKey;
if ("OrderCreated".equals(event.getType())) {
    routingKey = ORDER_CREATED_ROUTING_KEY;
} else if ("OrderRefundRequest".equals(event.getType())) {
    routingKey = "order.refund.request";
} else {
    log.warn("Unknown event type: {}", event.getType());
    continue;
}
```

#### 7. **Payment.refund() không lưu refundAmount**
**File:** `services/payment-microservice/src/main/java/com/example/payment/domain/model/Payment.java`

**Vấn đề:**
- Method `refund(BigDecimal refundAmount)` nhận parameter nhưng không lưu vào database
- Chỉ update status, không track số tiền đã refund
- **Impact**: Không thể audit/query refund amount sau này

**Cần thêm field:**
```java
@Column(name = "refund_amount", precision = 12, scale = 2)
private BigDecimal refundAmount;
```

#### 8. **Thiếu validation: Order phải có payment trước khi refund**
- ❌ `UpdateOrderStatusUseCase` không check order đã có payment chưa
- ❌ Không validate paymentId trước khi tạo refund event
- **Impact**: Có thể tạo refund request cho order chưa có payment

---

### 🟠 **MEDIUM PRIORITY - Improvements**

#### 9. **Exception handling không đầy đủ**
- `ProcessRefundUseCase.execute()` throw `RuntimeException` generic
- Nên có custom exceptions: `PaymentNotFoundException`, `InvalidRefundAmountException`

#### 10. **Thiếu refund reason validation**
- `OrderRefundRequestEvent.reason` không có validation
- Nên có max length và required check

#### 11. **Thiếu refund amount validation trong Order Service**
- Khi tạo refund request, không validate refundAmount <= order.grandTotal
- Nên validate trước khi publish event

#### 12. **Thiếu audit logging**
- Không log ai đã request refund (userId, merchantId, adminId)
- Không log refund reason và amount

---

## 🔄 LUỒNG HOẠT ĐỘNG HIỆN TẠI (INCOMPLETE)

```
1. ❌ User/Admin gọi API refund → KHÔNG CÓ
2. ❌ Order Service tạo OrderRefundRequestEvent → KHÔNG CÓ
3. ❌ Publish event đến payment service → KHÔNG CÓ
4. ✅ Payment Service nhận event (có listener)
5. ✅ ProcessRefundUseCase xử lý refund
6. ✅ Payment status → REFUNDED
7. ✅ Tạo PaymentRefundedEvent vào outbox
8. ✅ OutboxEventRelay publish event (có bug event type)
9. ❌ Order Service nhận PaymentRefundedEvent → KHÔNG CÓ LISTENER
10. ❌ Update order status → REFUNDED → KHÔNG TỰ ĐỘNG
```

---

## 📝 KHUYẾN NGHỊ SỬA CHỮA

### **Phase 1: Fix Critical Bugs**
1. Fix event type mismatch trong `OutboxEventRelay` (Payment Service)
2. Thêm logic publish refund event trong `OutboxEventRelay` (Order Service)
3. Thêm listener `PaymentRefundedEvent` trong Order Service
4. Thêm RabbitMQ config cho refund events

### **Phase 2: Implement Missing Features**
5. Tạo `RequestRefundUseCase` trong Order Service
6. Tạo API endpoint `/api/v1/orders/{orderId}/refund`
7. Thêm logic publish `OrderRefundRequestEvent` khi status → REFUNDED
8. Thêm validation: order có payment, refundAmount <= grandTotal

### **Phase 3: Enhancements**
9. Thêm `refundAmount` field vào Payment entity
10. Cải thiện exception handling
11. Thêm audit logging
12. Thêm refund reason validation

---

## 🧪 TESTING RECOMMENDATIONS

### Cần test các scenarios:
1. ✅ Refund order đã DELIVERED → thành công
2. ❌ Refund order chưa có payment → phải reject
3. ❌ Refund order đã REFUNDED → phải reject (idempotency)
4. ❌ Refund amount > payment amount → phải reject
5. ❌ Duplicate refund request → phải handle idempotent
6. ❌ Payment service down → order service phải retry
7. ❌ Order service down → payment service phải retry

---

## 📊 TỔNG KẾT

| Category | Status | Count |
|----------|--------|-------|
| ✅ Đã có | Working | 4 |
| 🔴 Critical Missing | Blocking | 4 |
| 🟡 High Priority Bugs | Needs Fix | 4 |
| 🟠 Medium Priority | Improvements | 4 |

**Kết luận:** Chức năng hoàn tiền hiện tại **chưa thể hoạt động** do thiếu các thành phần quan trọng. Cần implement thêm ít nhất 4 phần critical và fix 4 bugs để có thể sử dụng được.

---

*Review date: $(date)*
*Reviewer: AI Code Review*


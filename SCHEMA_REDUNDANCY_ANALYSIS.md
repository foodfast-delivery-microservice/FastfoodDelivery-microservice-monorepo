# Đánh Giá Schema - Các Field Thừa

## Tổng Quan
Phân tích schema database để xác định các field có thể thừa hoặc trùng lặp.

---

## 1. Bảng `orders` - Các Field Địa Chỉ

### ⚠️ **Các Field Có Thể Thừa:**

#### 1.1. `city` vs `province_name`
- **Vấn đề**: Trong bối cảnh Việt Nam, "city" thường trùng với "province" (ví dụ: "Thành phố Hồ Chí Minh" = tỉnh/thành phố)
- **Hiện tại**: 
  - `city` (VARCHAR(100), NOT NULL) - text người dùng nhập
  - `province_name` (VARCHAR(100), NULL) - đã chuẩn hóa từ AddressKit
- **Đánh giá**: 
  - `city` được dùng trong `getFullAddress()` để hiển thị
  - `province_name` dùng cho analytics/joining với hệ thống khác
  - **Kết luận**: KHÔNG thừa hoàn toàn, nhưng có thể hợp nhất nếu chỉ cần một nguồn dữ liệu

#### 1.2. `ward` vs `commune_name`
- **Vấn đề**: "Ward" (phường/xã) thường trùng với "commune" trong hệ thống hành chính VN
- **Hiện tại**:
  - `ward` (VARCHAR(100), NOT NULL) - text người dùng nhập
  - `commune_name` (VARCHAR(100), NULL) - đã chuẩn hóa từ AddressKit
- **Đánh giá**: 
  - `ward` được dùng trong `getFullAddress()` để hiển thị
  - `commune_name` dùng cho analytics/joining
  - **Kết luận**: Tương tự `city`/`province_name`, không thừa hoàn toàn nhưng có thể hợp nhất

#### 1.3. `district` vs `normalized_district_name` ⚠️ **THỪA - NÊN XÓA**
- **Vấn đề**: Cả hai đều đại diện cho quận/huyện
- **Hiện tại**:
  - `district` (VARCHAR(100), NOT NULL) - text người dùng nhập
  - `normalized_district_name` (VARCHAR(100), NULL) - đã chuẩn hóa từ AddressKit
- **Đánh giá**: 
  - `district` được dùng trong `getFullAddress()` để hiển thị
  - `normalized_district_name` chỉ được lưu nhưng **KHÔNG THẤY được query/join/analytics trong code**
  - Comment trong code nói "for backward compatibility with legacy data" nhưng không thấy sử dụng
  - **Kết luận**: **NÊN XÓA** `normalized_district_name` - field này không được sử dụng

---

## 2. Bảng `order_items` - Denormalization

### ⚠️ **Field Có Thể Thừa:**

#### 2.1. `merchant_id` 
- **Vấn đề**: `merchant_id` đã có trong bảng `orders`, có thể lấy qua JOIN
- **Hiện tại**: 
  - `merchant_id` (BIGINT, NOT NULL, INDEX) trong `order_items`
  - `merchant_id` (BIGINT, NOT NULL, INDEX) trong `orders`
- **Đánh giá**: 
  - Đây là **denormalization có chủ đích** để tối ưu performance
  - Cho phép query order_items theo merchant mà không cần JOIN với orders
  - Migration script đã backfill từ orders → order_items
  - **Kết luận**: **KHÔNG THỪA** - đây là pattern hợp lệ để tối ưu query

---

## 3. Bảng `user_addresses` - Computed Field

### ⚠️ **Field Có Thể Thừa:**

#### 3.1. `full_address`
- **Vấn đề**: Field này có thể được tính toán từ các field khác (`street`, `commune_name`, `district_name`, `province_name`)
- **Hiện tại**: 
  - `full_address` (VARCHAR(400), NOT NULL)
  - Các field thành phần: `street`, `commune_name`, `district_name`, `province_name`
- **Đánh giá**: 
  - Có thể được tính toán động từ các field khác
  - Tuy nhiên, có thể hữu ích cho:
    - Full-text search/indexing
    - Tránh tính toán mỗi lần query
    - Lưu version đã được user/driver điều chỉnh
  - **Kết luận**: **CÓ THỂ THỪA** nếu không dùng cho search/indexing, nhưng nên giữ nếu có lợi cho performance

---

## 4. Bảng `idempotency_keys` - Foreign Key

### ✅ **Không Thừa:**
- `order_id`: Cần thiết để link với order, hữu ích cho queries và cleanup

---

## 5. Bảng `outbox_events` - Event Sourcing

### ✅ **Không Thừa:**
- Tất cả các field đều cần thiết cho pattern Outbox

---

## 6. Bảng `flyway_schema_history`

### ✅ **Không Thừa:**
- Đây là bảng system của Flyway, không nên chỉnh sửa

---

## Tóm Tắt & Khuyến Nghị

### 🔴 **Field Nên Xóa Ngay:**
1. **`orders.normalized_district_name`** ⚠️ **THỪA**
   - Trùng với `district`, không thấy được sử dụng trong queries/joins/analytics
   - Chỉ được lưu nhưng không bao giờ được đọc
   - Có thể xóa an toàn

### 🟡 **Field Cần Đánh Giá Thêm:**
1. **`user_addresses.full_address`** - Nếu không dùng cho full-text search, có thể tính toán động
2. **`orders.province_name`, `orders.commune_name`, `orders.province_code`, `orders.commune_code`**
   - Được đánh dấu "for analytics/joining with other systems" nhưng **KHÔNG THẤY được sử dụng trong code**
   - Chỉ được lưu khi tạo order, không thấy queries/joins nào sử dụng
   - **Cần xác nhận**: Có hệ thống analytics/reporting nào đọc các field này không?
   - Nếu không có, có thể xóa để giảm storage
3. **`orders.city` vs `orders.province_name`** - Có thể hợp nhất nếu chỉ cần một nguồn dữ liệu
4. **`orders.ward` vs `orders.commune_name`** - Tương tự, có thể hợp nhất

### 🟢 **Field Không Thừa (Denormalization Hợp Lệ):**
1. **`order_items.merchant_id`** - Denormalization có chủ đích để tối ưu performance

---

## Hành Động Đề Xuất

### Ngay Lập Tức:
1. ✅ **Xóa `orders.normalized_district_name`** - Field này không được sử dụng
2. ✅ **Kiểm tra và xóa các field normalized không dùng:**
   - `orders.province_name` (nếu không có analytics/reporting dùng)
   - `orders.commune_name` (nếu không có analytics/reporting dùng)
   - `orders.province_code` (nếu không có analytics/reporting dùng)
   - `orders.commune_code` (nếu không có analytics/reporting dùng)
3. ✅ Kiểm tra xem `user_addresses.full_address` có được dùng cho full-text search không

### Dài Hạn:
1. 🔄 Xem xét hợp nhất `city`/`province_name` và `ward`/`commune_name` nếu không cần cả hai nguồn dữ liệu
2. 🔄 Đánh giá lại việc denormalize `merchant_id` trong `order_items` - có thực sự cần thiết không?

---

## Lưu Ý Khi Xóa Field

⚠️ **Trước khi xóa bất kỳ field nào:**
1. Kiểm tra tất cả queries/reports/analytics có dùng field đó không
2. Kiểm tra các service khác có đọc field đó qua API không
3. Tạo migration script để backup dữ liệu trước khi xóa
4. Xóa theo thứ tự: code → migration → database


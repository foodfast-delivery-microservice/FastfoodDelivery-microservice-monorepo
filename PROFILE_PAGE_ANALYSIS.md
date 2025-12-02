# Phân Tích Trang Profile - http://localhost:5173/profile

## Tổng Quan

Trang Profile là trang quản lý thông tin cá nhân và địa chỉ giao hàng của người dùng. Trang này được truy cập qua route `/profile` và bao gồm 2 phần chính:
1. **Thông tin cá nhân** - Quản lý thông tin tài khoản
2. **Quản lý địa chỉ** - Quản lý địa chỉ giao hàng với bản đồ tương tác

---

## Cấu Trúc Component

### 1. Profile.jsx (Component Chính)

**Location**: `frontend/src/components/Profile.jsx`

**Chức năng**:
- Hiển thị và chỉnh sửa thông tin cá nhân của người dùng
- Quản lý state cho user data và saving status
- Tích hợp component `AddressManager` để quản lý địa chỉ

**State Management**:
```javascript
- userData: Thông tin người dùng (username, email, role)
- saving: Trạng thái đang lưu dữ liệu
```

**API Calls**:
- `getProfile()` - Lấy thông tin profile từ `/users/me`
- `updateProfile({ email })` - Cập nhật email qua `PATCH /users/me`

**UI Components**:
- Form hiển thị:
  - Tên đăng nhập (disabled, read-only)
  - Email (editable)
  - Vai trò (disabled, read-only)
- Button "Lưu thay đổi" để cập nhật email

---

### 2. AddressManager.jsx (Quản Lý Địa Chỉ)

**Location**: `frontend/src/components/AddressManager.jsx`

**Chức năng**:
- Tạo địa chỉ giao hàng mới
- Hiển thị danh sách địa chỉ đã lưu
- Chỉnh sửa vị trí địa chỉ trên bản đồ (drag marker)
- Tích hợp bản đồ Leaflet với OpenStreetMap

**State Management**:
```javascript
- provinces: Danh sách tỉnh/thành
- communes: Danh sách phường/xã (theo tỉnh đã chọn)
- form: Form tạo địa chỉ mới
  - street: Địa chỉ chi tiết
  - provinceCode: Mã tỉnh/thành
  - communeCode: Mã phường/xã
  - note: Ghi chú (chung cư, block...)
- addresses: Danh sách địa chỉ đã lưu
- selectedAddressId: ID địa chỉ đang được chọn
- saving: Trạng thái đang lưu
- loadingList: Trạng thái đang tải danh sách
```

**API Calls**:
- `getProvinces()` - `GET /addresses/provinces`
- `getCommunes(provinceCode)` - `GET /addresses/provinces/{provinceCode}/communes`
- `getUserAddresses()` - `GET /addresses`
- `createUserAddress(payload)` - `POST /addresses`
- `updateAddressLocation(addressId, payload)` - `PATCH /addresses/{addressId}/location`

**Tính năng**:
1. **Tạo địa chỉ mới**:
   - Nhập địa chỉ chi tiết (street)
   - Chọn Tỉnh/Thành từ dropdown
   - Chọn Phường/Xã từ dropdown (phụ thuộc vào tỉnh đã chọn)
   - Thêm ghi chú (tùy chọn)
   - Lưu địa chỉ → tự động geocode và hiển thị trên bản đồ

2. **Quản lý địa chỉ đã lưu**:
   - Hiển thị danh sách địa chỉ với full address
   - Hiển thị nguồn địa chỉ:
     - `GEOCODE_ONLY`: Chỉ geocode tự động
     - `GEOCODE_USER_ADJUST`: Người dùng đã chỉnh sửa
     - `GEOCODE_DRIVER_ADJUST`: Shipper đã chỉnh sửa
   - Click vào địa chỉ để xem trên bản đồ

3. **Chỉnh sửa vị trí trên bản đồ**:
   - Marker có thể kéo thả (draggable)
   - Khi kéo marker, tự động cập nhật lat/lng
   - Cập nhật source thành `GEOCODE_USER_ADJUST`

**Map Integration**:
- Sử dụng `react-leaflet` và `leaflet`
- Tile layer: OpenStreetMap
- Default position: `{ lat: 10.776389, lng: 106.700806 }` (Hồ Chí Minh)
- Zoom level: 16
- Marker có thể kéo thả

---

## API Endpoints

### User Profile APIs

#### GET /api/v1/users/me
**Mô tả**: Lấy thông tin profile của user hiện tại  
**Auth**: Required (JWT)  
**Response**:
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "approved": true
}
```

#### PATCH /api/v1/users/me
**Mô tả**: Cập nhật thông tin profile (hiện tại chỉ hỗ trợ email)  
**Auth**: Required (JWT)  
**Request Body**:
```json
{
  "email": "newemail@example.com"
}
```

### Address Management APIs

#### GET /api/v1/addresses
**Mô tả**: Lấy danh sách địa chỉ của user hiện tại  
**Auth**: Required (JWT)  
**Response**:
```json
[
  {
    "id": 1,
    "street": "Số 4 Đường 30",
    "provinceCode": "79",
    "provinceName": "Thành phố Hồ Chí Minh",
    "communeCode": "27694",
    "communeName": "Phường 4",
    "districtName": "Quận 3",
    "fullAddress": "Số 4 Đường 30, Phường 4, Quận 3, Thành phố Hồ Chí Minh",
    "lat": 10.776389,
    "lng": 106.700806,
    "source": "GEOCODE_ONLY",
    "note": "Chung cư A, block B"
  }
]
```

#### POST /api/v1/addresses
**Mô tả**: Tạo địa chỉ mới  
**Auth**: Required (JWT)  
**Request Body**:
```json
{
  "street": "Số 4 Đường 30",
  "provinceCode": "79",
  "communeCode": "27694",
  "note": "Chung cư A, block B"
}
```

#### PATCH /api/v1/addresses/{addressId}/location
**Mô tả**: Cập nhật vị trí (lat/lng) của địa chỉ  
**Auth**: Required (JWT)  
**Request Body**:
```json
{
  "lat": 10.776389,
  "lng": 106.700806,
  "source": "GEOCODE_USER_ADJUST"
}
```

#### GET /api/v1/addresses/provinces
**Mô tả**: Lấy danh sách tỉnh/thành  
**Auth**: Required (JWT)  
**Response**:
```json
[
  {
    "code": "79",
    "name": "Thành phố Hồ Chí Minh"
  }
]
```

#### GET /api/v1/addresses/provinces/{provinceCode}/communes
**Mô tả**: Lấy danh sách phường/xã theo tỉnh  
**Auth**: Required (JWT)  
**Response**:
```json
[
  {
    "code": "27694",
    "name": "Phường 4",
    "districtName": "Quận 3"
  }
]
```

---

## Styling

### Profile.css
- Background: `#fff7f0` (màu cam nhạt)
- Card: White background với shadow và border radius
- Button: Gradient từ `#ff7a00` đến `#ff5400`
- Responsive design

### AddressManager.css
- Layout: Flexbox với 2 cột (form và map)
- Form card và Map card: White background với shadow
- Address list: Scrollable với max-height 200px
- Active address: Highlight với border màu xanh
- Responsive: Chuyển sang 1 cột khi màn hình < 900px

---

## Routing

**Location**: `frontend/src/App.jsx`

```jsx
<Route path="profile" element={
  <ProtectedRoute>
    <Profile />
  </ProtectedRoute>
} />
```

- Route: `/profile`
- Protected: Yêu cầu authentication (JWT)
- Component: `Profile`

---

## Flow Hoạt Động

### 1. Load Trang Profile
1. Component `Profile` mount
2. Gọi `getProfile()` để lấy thông tin user
3. Hiển thị thông tin: username, email, role
4. Component `AddressManager` mount
5. Gọi `getProvinces()` và `getUserAddresses()` song song
6. Load communes của tỉnh đầu tiên
7. Hiển thị danh sách địa chỉ và bản đồ

### 2. Cập Nhật Email
1. User chỉnh sửa email trong input
2. Click "Lưu thay đổi"
3. Gọi `updateProfile({ email })`
4. Cập nhật state `userData`
5. Hiển thị message success

### 3. Tạo Địa Chỉ Mới
1. User nhập địa chỉ chi tiết
2. Chọn Tỉnh/Thành → Tự động load communes
3. Chọn Phường/Xã
4. Nhập ghi chú (tùy chọn)
5. Click "Lưu địa chỉ"
6. Gọi `createUserAddress(payload)`
7. Backend geocode địa chỉ → trả về lat/lng
8. Thêm địa chỉ vào danh sách
9. Tự động chọn địa chỉ mới và hiển thị trên bản đồ

### 4. Chỉnh Sửa Vị Trí Địa Chỉ
1. User chọn địa chỉ từ danh sách
2. Bản đồ hiển thị marker tại vị trí hiện tại
3. User kéo marker đến vị trí mới
4. Tự động gọi `updateAddressLocation()` với lat/lng mới
5. Cập nhật source thành `GEOCODE_USER_ADJUST`
6. Cập nhật state `addresses`

---

## Dependencies

### Frontend Libraries
- `react` - UI framework
- `react-leaflet` - Map component
- `leaflet` - Map library
- `antd` - UI components (message notifications)

### Backend Services
- **User Service**: Quản lý user profile (`/users/me`)
- **Order Service**: Quản lý địa chỉ (`/addresses/*`)

---

## Tính Năng Nổi Bật

1. ✅ **Quản lý địa chỉ với bản đồ tương tác**
   - Geocode tự động khi tạo địa chỉ
   - Cho phép chỉnh sửa vị trí bằng cách kéo marker
   - Hiển thị nguồn địa chỉ (geocode/user/driver adjust)

2. ✅ **Dropdown phụ thuộc**
   - Communes tự động load khi chọn province
   - UX tốt với loading states

3. ✅ **Validation**
   - Kiểm tra đầy đủ thông tin trước khi lưu
   - Error handling với message notifications

4. ✅ **Responsive Design**
   - Layout tự động điều chỉnh trên mobile
   - Card-based design dễ đọc

---

## Cải Tiến Có Thể Thực Hiện

1. ⚠️ **Thiếu tính năng xóa địa chỉ**
   - Hiện tại chỉ có tạo và chỉnh sửa
   - Nên thêm nút xóa địa chỉ

2. ⚠️ **Thiếu tính năng đặt địa chỉ mặc định**
   - Nên có option để đánh dấu địa chỉ mặc định
   - Hiển thị badge "Mặc định" trên địa chỉ

3. ⚠️ **Thiếu validation cho email**
   - Nên validate format email trước khi lưu
   - Hiển thị error message nếu email không hợp lệ

4. ⚠️ **Thiếu tính năng chỉnh sửa địa chỉ**
   - Hiện tại chỉ có thể chỉnh sửa vị trí (lat/lng)
   - Nên cho phép chỉnh sửa street, note, etc.

5. ⚠️ **Thiếu loading skeleton**
   - Nên có skeleton loading thay vì chỉ text "Đang tải..."

6. ⚠️ **Thiếu error boundary**
   - Nên có error boundary để handle lỗi gracefully

---

## Security Considerations

1. ✅ **Authentication Required**
   - Tất cả API endpoints đều yêu cầu JWT
   - User chỉ có thể xem/sửa địa chỉ của chính mình

2. ✅ **Input Validation**
   - Backend validate input trước khi lưu
   - Frontend có basic validation

3. ⚠️ **CORS & XSS**
   - Nên kiểm tra CORS configuration
   - Sanitize user input để tránh XSS

---

## Testing Suggestions

1. **Unit Tests**:
   - Test form validation
   - Test state management
   - Test API calls (mock)

2. **Integration Tests**:
   - Test flow tạo địa chỉ mới
   - Test flow chỉnh sửa vị trí
   - Test dropdown phụ thuộc

3. **E2E Tests**:
   - Test complete user journey
   - Test với nhiều địa chỉ
   - Test responsive trên mobile

---

## Kết Luận

Trang Profile là một trang quan trọng trong ứng dụng, cung cấp:
- ✅ Quản lý thông tin cá nhân cơ bản
- ✅ Quản lý địa chỉ giao hàng với bản đồ tương tác
- ✅ UX tốt với validation và error handling

Tuy nhiên, còn thiếu một số tính năng như xóa địa chỉ, đặt mặc định, và chỉnh sửa địa chỉ chi tiết. Nên bổ sung các tính năng này để hoàn thiện hơn.


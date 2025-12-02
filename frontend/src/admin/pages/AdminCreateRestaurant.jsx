import React, { useState } from "react";
import { Form, Input, Button, message, Card, Row, Col, Divider, InputNumber, Select } from "antd";
import http from "../../services/http";
import "./AdminCreateRestaurant.css";

export default function AdminCreateRestaurant() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const logApi = (label, payload) => {
    if (process.env.NODE_ENV === "production") return;
    // eslint-disable-next-line no-console
    console.log(`[CreateRestaurant] ${label}`, payload);
  };

  // Geocoding địa chỉ → lat/lng
  const geocodeAddress = async (address) => {
    if (!address) {
      logApi("Geocoding skipped", "No address provided");
      return null;
    }
    
    try {
      // Add delay to respect Nominatim usage policy (max 1 request per second)
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const searchQuery = `${address}, Vietnam`;
      const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(
        searchQuery
      )}&format=json&limit=1&countrycodes=vn&addressdetails=1`;
      
      logApi("Geocoding request", { url, address: searchQuery });

      const response = await fetch(url, {
        method: 'GET',
        mode: 'cors',
        headers: {
          'User-Agent': 'FastFoodDelivery-Admin/1.0', // Required by Nominatim
          'Accept': 'application/json',
        }
      });
      
      logApi("Geocoding response status", { 
        status: response.status, 
        statusText: response.statusText,
        ok: response.ok 
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        logApi("Geocoding HTTP error", { 
          status: response.status, 
          statusText: response.statusText,
          body: errorText
        });
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      logApi("Geocoding response data", { 
        dataLength: Array.isArray(data) ? data.length : 'not array',
        data: data 
      });

      if (!data || !Array.isArray(data) || data.length === 0) {
        logApi("Geocoding failed", "No results found", { data });
        return null;
      }

      const result = data[0];
      logApi("Geocoding first result", result);
      
      const lat = parseFloat(result.lat);
      const lng = parseFloat(result.lon);

      if (isNaN(lat) || isNaN(lng)) {
        logApi("Geocoding failed", "Invalid coordinates", { 
          lat: result.lat, 
          lng: result.lon,
          parsedLat: lat,
          parsedLng: lng
        });
        return null;
      }

      const coords = { lat, lng };
      logApi("Geocoding success", coords);
      return coords;
    } catch (err) {
      // Check if it's a CORS error
      if (err.message && err.message.includes('CORS')) {
        logApi("Geocoding CORS error", {
          message: "CORS blocked - may need backend proxy",
          error: err.message
        });
      } else {
        logApi("Geocoding error", {
          message: err.message,
          name: err.name,
          stack: err.stack,
          address
        });
      }
      return null;
    }
  };

  const handleSubmit = async (values) => {
    try {
      setLoading(true);
      logApi("Form values", values);

      // Validate required fields (Ant Design Form đã validate, nhưng double check để chắc chắn)
      if (!values.restaurantName || !values.username || !values.email || !values.password) {
        message.error("Vui lòng nhập đầy đủ thông tin bắt buộc!");
        setLoading(false);
        return;
      }

      // Geocode address to get coordinates
      let coords = null;
      if (values.restaurantAddress) {
        try {
          message.loading({ content: "Đang tìm tọa độ địa chỉ...", key: "geocoding" });
          logApi("Starting geocoding", { address: values.restaurantAddress });
          
          coords = await geocodeAddress(values.restaurantAddress);
          
          logApi("Geocoding result", { coords, hasLat: !!coords?.lat, hasLng: !!coords?.lng });
          
          if (coords && coords.lat != null && coords.lng != null && !isNaN(coords.lat) && !isNaN(coords.lng)) {
            message.success({ 
              content: `✅ Đã tìm thấy tọa độ: ${coords.lat.toFixed(6)}, ${coords.lng.toFixed(6)}`, 
              key: "geocoding", 
              duration: 3 
            });
            logApi("Geocoded coordinates", coords);
          } else {
            message.warning({ 
              content: "⚠️ Không tìm thấy tọa độ. Nhà hàng sẽ được tạo không có tọa độ.", 
              key: "geocoding",
              duration: 4
            });
            logApi("Geocoding returned null or invalid", { 
              coords, 
              address: values.restaurantAddress,
              latValid: coords?.lat != null && !isNaN(coords.lat),
              lngValid: coords?.lng != null && !isNaN(coords.lng)
            });
            coords = null; // Ensure it's null if invalid
          }
        } catch (geocodeError) {
          logApi("Geocoding exception", {
            error: geocodeError,
            message: geocodeError?.message,
            stack: geocodeError?.stack
          });
          message.error({ 
            content: "❌ Lỗi khi tìm tọa độ. Tiếp tục tạo nhà hàng không có tọa độ.", 
            key: "geocoding",
            duration: 3
          });
          coords = null; // Ensure it's null on error
        }
      } else {
        logApi("Geocoding skipped", "No restaurantAddress provided");
      }

      // Prepare request data
      const requestData = {
        username: values.username,
        password: values.password,
        email: values.email,
        role: "MERCHANT", // Must be MERCHANT to create restaurant
        approved: true, // Admin can approve immediately
        // Profile fields
        fullName: values.fullName || values.restaurantName,
        phone: values.phone || "",
        address: values.restaurantAddress || "",
        avatar: values.restaurantImage || "",
        // Merchant/Restaurant fields
        restaurantName: values.restaurantName,
        restaurantAddress: values.restaurantAddress || "",
        restaurantImage: values.restaurantImage || "",
        openingHours: values.openingHours || "",
        // Restaurant additional fields
        restaurantDescription: values.description || "",
        restaurantCity: values.city || "",
        restaurantDistrict: values.district || "",
        restaurantCategory: values.category || "",
        restaurantLatitude: coords?.lat || null,
        restaurantLongitude: coords?.lng || null,
        restaurantDeliveryFee: values.deliveryFee || null,
        restaurantEstimatedDeliveryTime: values.estimatedDeliveryTime || null,
      };

      logApi("POST /users request", requestData);

      // Create user with MERCHANT role
      const response = await http.post("/users", requestData);
      logApi("POST /users response", response?.data);

      const userData = response?.data?.data || response?.data;
      
      if (userData) {
        message.success(
          `✅ Tạo nhà hàng thành công! Username: ${userData.username || values.username}`
        );
        
        // Reset form
        form.resetFields();
      } else {
        message.warning("Tạo user thành công nhưng không nhận được response data");
      }
    } catch (error) {
      const errorMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.message ||
        "Không thể tạo nhà hàng";
      
      logApi("Error creating restaurant", {
        message: errorMessage,
        status: error?.response?.status,
        data: error?.response?.data,
      });

      message.error(`❌ ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="acr-container">
      <Card>
        <h2 className="acr-title">🏪 Tạo Nhà Hàng Mới</h2>
        <p style={{ color: "#666", marginBottom: 24 }}>
          Tạo tài khoản merchant mới và nhà hàng sẽ được tạo tự động
        </p>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          onFinishFailed={(errorInfo) => {
            logApi("Form validation failed", errorInfo);
            message.error("Vui lòng kiểm tra lại các trường bắt buộc!");
          }}
          autoComplete="off"
        >
          <Divider orientation="left">Thông tin nhà hàng</Divider>

          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item
                label="Tên nhà hàng"
                name="restaurantName"
                rules={[{ required: true, message: "Vui lòng nhập tên nhà hàng!" }]}
              >
                <Input placeholder="Nhập tên nhà hàng" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                label="Địa chỉ"
                name="restaurantAddress"
                rules={[{ required: true, message: "Vui lòng nhập địa chỉ!" }]}
              >
                <Input placeholder="Nhập địa chỉ nhà hàng" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item label="Mô tả" name="description">
                <Input.TextArea
                  rows={3}
                  placeholder="Mô tả về nhà hàng..."
                />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item label="Hình ảnh (URL)" name="restaurantImage">
                <Input placeholder="https://example.com/image.jpg" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={8}>
              <Form.Item label="Thành phố" name="city">
                <Input placeholder="VD: TP. Hồ Chí Minh" />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item label="Quận/Huyện" name="district">
                <Input placeholder="VD: Quận 1" />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item label="Danh mục" name="category">
                <Select placeholder="Chọn danh mục" allowClear>
                  <Select.Option value="FOOD">Đồ ăn</Select.Option>
                  <Select.Option value="DRINK">Đồ uống</Select.Option>
                  <Select.Option value="BOTH">Cả hai</Select.Option>
                  <Select.Option value="OTHER">Khác</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={8}>
              <Form.Item label="Giờ mở cửa" name="openingHours">
                <Input placeholder="VD: 08:00 - 22:00" />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item label="Phí giao hàng (₫)" name="deliveryFee">
                <InputNumber
                  style={{ width: "100%" }}
                  placeholder="VD: 20000"
                  min={0}
                  formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={(value) => value.replace(/\$\s?|(,*)/g, '')}
                />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item label="Thời gian giao hàng ước tính (phút)" name="estimatedDeliveryTime">
                <InputNumber
                  style={{ width: "100%" }}
                  placeholder="VD: 30"
                  min={0}
                  max={300}
                />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">Thông tin tài khoản đăng nhập</Divider>

          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item
                label="Username"
                name="username"
                rules={[
                  { required: true, message: "Vui lòng nhập username!" },
                  { min: 3, message: "Username phải có ít nhất 3 ký tự!" },
                ]}
              >
                <Input placeholder="Nhập username" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item
                label="Email"
                name="email"
                rules={[
                  { required: true, message: "Vui lòng nhập email!" },
                  { type: "email", message: "Email không hợp lệ!" },
                ]}
              >
                <Input type="email" placeholder="email@example.com" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item
                label="Mật khẩu"
                name="password"
                rules={[
                  { required: true, message: "Vui lòng nhập mật khẩu!" },
                  { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự!" },
                ]}
              >
                <Input.Password placeholder="Nhập mật khẩu" />
              </Form.Item>
            </Col>

            <Col xs={24} md={12}>
              <Form.Item label="Số điện thoại" name="phone">
                <Input placeholder="Nhập số điện thoại" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item label="Tên đầy đủ" name="fullName">
            <Input placeholder="Tên người quản lý nhà hàng" />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              disabled={loading}
              size="large"
              className="acr-btn"
            >
              {loading ? "Đang tạo..." : "Tạo Nhà Hàng"}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}

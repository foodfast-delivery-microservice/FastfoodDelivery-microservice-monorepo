import React, { useState } from "react";
import { Form, Input, Button, message, Card, Row, Col, Divider } from "antd";
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

  // Geocoding địa chỉ → lat/lng (optional, có thể để backend xử lý)
  const geocodeAddress = async (address) => {
    if (!address) return null;
    try {
      const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(
        address + ", Vietnam"
      )}&format=json&limit=1&countrycodes=vn`;

      const response = await fetch(url);
      const data = await response.json();

      if (data.length === 0) return null;

      return {
        lat: parseFloat(data[0].lat),
        lng: parseFloat(data[0].lon),
      };
    } catch (err) {
      logApi("Geocoding error", err);
      return null;
    }
  };

  const handleSubmit = async (values) => {
    try {
      setLoading(true);
      logApi("Form values", values);

      // Validate required fields
      if (!values.restaurantName || !values.username || !values.email || !values.password) {
        message.error("Vui lòng nhập đầy đủ thông tin bắt buộc!");
        return;
      }

      // Geocode address (optional)
      let coords = null;
      if (values.restaurantAddress) {
        coords = await geocodeAddress(values.restaurantAddress);
        logApi("Geocoded coordinates", coords);
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

          <Form.Item label="Giờ mở cửa" name="openingHours">
            <Input placeholder="VD: 08:00 - 22:00" />
          </Form.Item>

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

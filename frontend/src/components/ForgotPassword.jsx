import React, { useState } from "react";
import { Form, Input, Button, message, Card, Typography } from "antd";
import { MailOutlined } from "@ant-design/icons";
import axios from "axios";
import { useNavigate, Link } from "react-router-dom";

const { Title, Text } = Typography;

const ForgotPassword = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      await axios.post("http://localhost:8080/api/v1/auth/forgot-password", {
        email: values.email,
      });
      message.success("Nếu email tồn tại, một liên kết đặt lại mật khẩu đã được gửi!");
      navigate("/login");
    } catch (error) {
      console.error(error);
      message.error("Có lỗi xảy ra, vui lòng thử lại sau.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 'calc(100vh - 150px)' }}>
      <Card style={{ width: 400, boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3} style={{ color: "#ea580c" }}>Quên Mật Khẩu</Title>
          <Text type="secondary">Nhập email của bạn để nhận liên kết đặt lại mật khẩu</Text>
        </div>

        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: "Vui lòng nhập email!" },
              { type: "email", message: "Email không hợp lệ!" },
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="Ví dụ: user@example.com" size="large" />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
              style={{ backgroundColor: "#ea580c", borderColor: "#ea580c" }}
            >
              Gửi Liên Kết
            </Button>
          </Form.Item>

          <div style={{ textAlign: "center", marginTop: 16 }}>
            <Text type="secondary">Nhớ mật khẩu? </Text>
            <Link to="/login" style={{ color: "#ea580c", fontWeight: "bold" }}>
              Đăng nhập ngay
            </Link>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default ForgotPassword;

import React, { useState, useEffect } from "react";
import { Form, Input, Button, message, Card, Typography } from "antd";
import { LockOutlined } from "@ant-design/icons";
import axios from "axios";
import { useNavigate, useSearchParams } from "react-router-dom";

const { Title, Text } = Typography;

const ResetPassword = () => {
  const [loading, setLoading] = useState(false);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");

  useEffect(() => {
    if (!token) {
      message.error("Liên kết không hợp lệ hoặc đã hết hạn!");
      navigate("/login");
    }
  }, [token, navigate]);

  const onFinish = async (values) => {
    if (values.password !== values.confirmPassword) {
      message.error("Mật khẩu xác nhận không khớp!");
      return;
    }

    setLoading(true);
    try {
      await axios.post("http://localhost:8089/api/v1/auth/reset-password", {
        token,
        newPassword: values.password,
      });
      message.success("Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
      navigate("/login");
    } catch (error) {
      console.error(error);
      const errorMsg = error.response?.data?.message || "Liên kết hết hạn hoặc không hợp lệ.";
      message.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  if (!token) return null;

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 'calc(100vh - 150px)' }}>
      <Card style={{ width: 400, boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3} style={{ color: "#ea580c" }}>Đặt Lại Mật Khẩu</Title>
          <Text type="secondary">Nhập mật khẩu mới cho tài khoản của bạn</Text>
        </div>

        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="password"
            label="Mật khẩu mới"
            rules={[
              { required: true, message: "Vui lòng nhập mật khẩu mới!" },
              { min: 6, message: "Mật khẩu phải dài ít nhất 6 ký tự!" },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Mật khẩu mới" size="large" />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label="Xác nhận mật khẩu"
            dependencies={['password']}
            rules={[
              { required: true, message: "Vui lòng xác nhận mật khẩu!" },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không khớp!'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Xác nhận mật khẩu" size="large" />
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
              Cập Nhật Mật Khẩu
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default ResetPassword;

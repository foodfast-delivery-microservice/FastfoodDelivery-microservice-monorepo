import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import "./Register.css";
import { message } from "antd";
import { register } from "../services/auth";

function Register() {
  const [username, setUsername] = useState("");
  // có thể tách fullName riêng nếu muốn, hiện tại dùng username là tên hiển thị đăng nhập
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [phonenumber, setPhonenumber] = useState("");
  const [address, setAddress] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async () => {
    setError("");

    if (!username.trim() || !email.trim() || !password.trim()) {
      setError("Vui lòng nhập tên đăng nhập, email và mật khẩu.");
      return;
    }

    setLoading(true);
    try {
      await register({
        username: username.trim(),
        email: email.trim(),
        password: password.trim(),
        // map thêm các field profile mà backend hỗ trợ
        phone: phonenumber.trim() || null,
        address: address.trim() || null,
        fullName: username.trim(), // nếu sau này có input Họ tên riêng, hãy đổi sang state fullName
        // Backend expects roles matching User.UserRole enum: USER or MERCHANT (ADMIN is forbidden)
        // For normal customers, we should use USER
        role: "USER",
      });

      message.success("Đăng ký thành công! Vui lòng đăng nhập.", 2);
      navigate("/login");
    } catch (err) {
      console.error("Register Error:", err);
      if (err?.response?.status === 409) {
        setError("Tên đăng nhập đã tồn tại.");
      } else {
        setError("❌ Đã có lỗi xảy ra khi đăng ký.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-container">
      <div className="register-box">
        <h2>Đăng ký tài khoản</h2>

        <div className="field-group">
          <label>
            Tên đăng nhập <span className="required">*</span>
          </label>
          <input
            type="text"
            placeholder="Ví dụ: meowlover_99"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </div>

        <div className="field-group">
          <label>
            Email <span className="required">*</span>
          </label>
          <input
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>

        <div className="field-group">
          <label>Số điện thoại</label>
          <input
            type="text"
            placeholder="Số điện thoại liên hệ giao hàng"
            value={phonenumber}
            onChange={(e) => setPhonenumber(e.target.value)}
          />
        </div>

        <div className="field-group">
          <label>
            Mật khẩu <span className="required">*</span>
          </label>
          <input
            type="password"
            placeholder="Tối thiểu 6 ký tự"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        <div className="field-group">
          <label>Địa chỉ giao hàng</label>
          <input
            type="text"
            placeholder="Ví dụ: 273 An Dương Vương, Q.5, TP.HCM"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
          />
        </div>

        {error && <p className="register-error">{error}</p>}

        <button className="register-btn" onClick={handleRegister} disabled={loading}>
          {loading ? "Đang xử lý..." : "Tạo tài khoản"}
        </button>

        <p className="to-login">
          Đã có tài khoản?
          <Link to="/login">Đăng nhập</Link>
        </p>
      </div>
    </div>
  );
}

export default Register;




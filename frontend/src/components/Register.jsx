import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import "./Register.css";
import { message } from "antd";
import { register } from "../services/auth";

function Register() {
  const [username, setUsername] = useState("");
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
        role: "CUSTOMER",
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
    <div className="register-container-simple">
      <h2>Đăng ký tài khoản</h2>

      <input type="text" placeholder="Tên đăng nhập" value={username} onChange={e => setUsername(e.target.value)} />
      <input type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} />
      <input type="text" placeholder="Số điện thoại" value={phonenumber} onChange={e => setPhonenumber(e.target.value)} />
      <input type="password" placeholder="Mật khẩu" value={password} onChange={e => setPassword(e.target.value)} />
      <input type="text" placeholder="Địa chỉ" value={address} onChange={e => setAddress(e.target.value)} />

      {error && <p className="error-message">{error}</p>}

      <button onClick={handleRegister} disabled={loading}>
        {loading ? "Đang xử lý..." : "Đăng ký"}
      </button>

      <p>
        Đã có tài khoản?{" "}
        <Link to="/login" style={{ textDecoration: "none", color: "#d2191a" }}>
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}

export default Register;
import React, { useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import "./Login.css";
import { useAuth } from "../context/AuthContext";
import { message } from "antd";
import { login, getProfile } from "../services/auth";

function Login() {
  const [phonenumber, setPhonenumber] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { setSession } = useAuth();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    if (!phonenumber || !password) {
      setError("Vui lòng nhập đầy đủ số điện thoại và mật khẩu.");
      setIsLoading(false);
      return;
    }

    try {
      // 1. Gọi API login
      const loginResponse = await login({ username: phonenumber, password });

      // 2. Lưu token vào localStorage (thông qua AuthContext hoặc trực tiếp nếu cần)
      // loginResponse là ApiResponse { data: { accessToken... } }
      // Cần lấy loginResponse.data để lưu
      const session = loginResponse.data || loginResponse;

      // Tạm thời lưu token để getProfile hoạt động ngay lập tức
      localStorage.setItem("app_session", JSON.stringify(session));

      // 3. Lấy thông tin user
      const profileResponse = await getProfile();
      const userProfile = profileResponse?.data || profileResponse;

      // 4. Cập nhật AuthContext
      // Kết hợp token và thông tin user
      const sessionData = { ...session, ...userProfile };
      setSession(sessionData);

      // 5. Merge cart (giữ nguyên logic cũ nhưng điều chỉnh key nếu cần)
      try {
        const guestKey = "cart_guest";
        const userKey = `cart_${userProfile.id}`; // Dùng ID thay vì phonenumber cho chuẩn

        const guestCart = JSON.parse(localStorage.getItem(guestKey) || "[]");
        const userCart = JSON.parse(localStorage.getItem(userKey) || "[]");

        if (guestCart.length > 0) {
          console.log("🧩 Merge guest cart vào user cart...");
          const merged = [...userCart];
          guestCart.forEach((g) => {
            const exist = merged.find((i) => i.id === g.id);
            if (exist) exist.quantity += g.quantity || 1;
            else merged.push(g);
          });

          localStorage.setItem(userKey, JSON.stringify(merged));
          localStorage.removeItem(guestKey);
        }
      } catch (err) {
        console.error("⚠️ Lỗi merge cart:", err);
      }

      // 6. Điều hướng
      const role = userProfile.role ? userProfile.role.toLowerCase() : "customer";
      switch (role) {
        case "admin":
          navigate("/admin");
          break;
        case "merchant":
        case "restaurant":
          navigate("/merchant"); // Hoặc trang merchant dashboard
          break;
        default:
          navigate("/");
          break;
      }

      message.success(`Chào mừng, ${userProfile.fullName || userProfile.username || "người dùng"} 👋`, 2);
    } catch (err) {
      console.error("Login Error:", err);
      const errorBody = err.response?.data;
      const errorCode = errorBody?.errorCode || errorBody?.code;
      let msg = errorBody?.message || errorBody?.error || "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.";

      if (errorCode === "EMAIL_NOT_VERIFIED") {
        const unverifiedEmail = errorBody?.data?.email;
        if (unverifiedEmail) {
          message.warning("Email chưa được xác thực. Đang chuyển hướng đến trang xác thực...", 2);
          navigate(`/verify-email?email=${encodeURIComponent(unverifiedEmail)}&type=SIGNUP`);
          return;
        }
        msg = "Email của bạn chưa được xác thực. Vui lòng kiểm tra hộp thư để lấy mã OTP và xác thực tài khoản trước khi đăng nhập.";
      }

      if (!err.response) {
        msg = "Không thể kết nối server. Kiểm tra Gateway (localhost:8080) đã chạy chưa hoặc lỗi mạng.";
      }
      setError(msg);
      message.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <h2>Đăng Nhập</h2>
        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label htmlFor="phonenumber">Tên đăng nhập / Số điện thoại</label>
            <input
              id="phonenumber"
              type="text"
              placeholder="Nhập tên đăng nhập hoặc SĐT"
              value={phonenumber}
              onChange={(e) => setPhonenumber(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Mật khẩu</label>
            <input
              id="password"
              type="password"
              placeholder="Nhập mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && <p className="error-message">{error}</p>}

          <button type="submit" className="login-btn" disabled={isLoading}>
            {isLoading ? "Đang đăng nhập..." : "Đăng nhập"}
          </button>
        </form>

        <p className="register-link">
          Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
        </p>
      </div>
    </div>
  );
}

export default Login;

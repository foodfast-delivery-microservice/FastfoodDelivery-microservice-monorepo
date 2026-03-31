import React, { useMemo, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import "./Register.css";
import { message } from "antd";
import { register, verifyEmailOtp, resendEmailOtp } from "../services/auth";

function Register() {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [phonenumber, setPhonenumber] = useState("");
  const [address, setAddress] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState("form"); // form | otp
  const [registeredEmail, setRegisteredEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [otpLoading, setOtpLoading] = useState(false);
  const navigate = useNavigate();

  const passwordStrength = useMemo(() => {
    if (!password) return "";
    if (password.length < 6) return "Yếu";
    if (password.length < 10) return "Trung bình";
    return "Mạnh";
  }, [password]);

  const handleSubmit = async (e) => {
    e.preventDefault();
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
        role: "USER",
        fullName: username.trim(),
        phone: phonenumber.trim() || null,
        address: address.trim() || null,
      });

      setRegisteredEmail(email.trim());
      setStep("otp");
      message.success(
        "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã OTP và nhập vào bên dưới để xác thực.",
        3
      );
    } catch (err) {
      console.error("Register Error:", err);
      const status = err?.response?.status;
      if (status === 409) {
        setError(err?.response?.data?.message || "Tên đăng nhập hoặc email đã tồn tại.");
      } else {
        setError("❌ Đã có lỗi xảy ra khi đăng ký. Vui lòng thử lại sau.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    if (!otp.trim()) {
      setError("Vui lòng nhập mã OTP.");
      return;
    }
    setError("");
    setOtpLoading(true);
    try {
      await verifyEmailOtp({ email: registeredEmail, otp: otp.trim(), type: "SIGNUP" });
      message.success("Xác thực email thành công! Bây giờ bạn có thể đăng nhập.", 3);
      navigate("/login");
    } catch (err) {
      console.error("Verify OTP Error:", err);
      const body = err?.response?.data;
      const code = body?.errorCode || body?.code;
      let msg = body?.message || "Xác thực OTP thất bại. Vui lòng thử lại.";

      if (code === "OTP_EXPIRED") msg = "Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.";
      if (code === "OTP_TOO_MANY_ATTEMPTS") msg = "Bạn đã nhập sai quá số lần cho phép. Vui lòng thử lại sau.";
      if (code === "OTP_INVALID") msg = "Mã OTP không đúng. Vui lòng kiểm tra lại.";

      setError(msg);
      message.error(msg);
    } finally {
      setOtpLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!registeredEmail) return;
    setOtpLoading(true);
    setError("");
    try {
      await resendEmailOtp({ email: registeredEmail, type: "SIGNUP" });
      message.success("Đã gửi lại mã OTP. Vui lòng kiểm tra email của bạn.", 3);
    } catch (err) {
      console.error("Resend OTP Error:", err);
      const body = err?.response?.data;
      const code = body?.errorCode || body?.code;
      let msg = body?.message || "Không thể gửi lại OTP. Vui lòng thử lại sau.";
      if (code === "OTP_RESEND_LIMIT_EXCEEDED") {
        msg = "Bạn đã vượt quá số lần gửi lại OTP trong ngày. Vui lòng thử lại vào ngày mai.";
      }
      setError(msg);
      message.error(msg);
    } finally {
      setOtpLoading(false);
    }
  };

  return (
    <div className="register-page">
      <div className="register-card">
        <h2 className="register-title">
          {step === "form" ? "Đăng ký tài khoản" : "Xác thực email"}
        </h2>
        <p className="register-subtitle">
          {step === "form"
            ? "Tạo tài khoản để đặt món nhanh hơn và quản lý đơn hàng của bạn."
            : `Nhập mã OTP đã được gửi đến email ${registeredEmail}.`}
        </p>

        {step === "form" && (
          <form className="register-form" onSubmit={handleSubmit}>
          <div className="register-row">
            <div className="register-field">
              <label htmlFor="username">Tên đăng nhập *</label>
              <input
                id="username"
                type="text"
                placeholder="Nhập tên đăng nhập"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <div className="register-field">
              <label htmlFor="email">Email *</label>
              <input
                id="email"
                type="email"
                placeholder="example@gmail.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="register-row">
            <div className="register-field">
              <label htmlFor="phone">Số điện thoại</label>
              <input
                id="phone"
                type="tel"
                placeholder="Nhập số điện thoại"
                value={phonenumber}
                onChange={(e) => setPhonenumber(e.target.value)}
              />
            </div>

            <div className="register-field">
              <label htmlFor="password">Mật khẩu *</label>
              <input
                id="password"
                type="password"
                placeholder="Tối thiểu 6 ký tự"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              {passwordStrength && (
                <span className={`password-strength strength-${passwordStrength.toLowerCase()}`}>
                  Độ mạnh mật khẩu: {passwordStrength}
                </span>
              )}
            </div>
          </div>

          <div className="register-field">
            <label htmlFor="address">Địa chỉ giao hàng</label>
            <input
              id="address"
              type="text"
              placeholder="Ví dụ: 273 An Dương Vương, Q.5, TP.HCM"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
            />
          </div>

          <p className="register-info">
            Sau khi đăng ký, chúng tôi sẽ gửi <strong>mã OTP</strong> về email để bạn xác thực tài khoản.
          </p>

          {error && <p className="error-message">{error}</p>}

          <button type="submit" className="register-submit-btn" disabled={loading}>
            {loading ? "Đang xử lý..." : "Đăng ký"}
          </button>
        </form>
        )}

        {step === "otp" && (
          <form className="register-form" onSubmit={handleVerifyOtp}>
            <div className="register-field">
              <label htmlFor="otp">Mã OTP</label>
              <input
                id="otp"
                type="text"
                placeholder="Nhập mã OTP gồm 6 chữ số"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
              />
            </div>

            <p className="register-info">
              Mã OTP có hiệu lực trong 5 phút. Nếu bạn chưa nhận được, hãy bấm{" "}
              <button
                type="button"
                className="register-link-button"
                onClick={handleResendOtp}
                disabled={otpLoading}
              >
                gửi lại OTP
              </button>
              .
            </p>

            {error && <p className="error-message">{error}</p>}

            <button type="submit" className="register-submit-btn" disabled={otpLoading}>
              {otpLoading ? "Đang xác thực..." : "Xác thực email"}
            </button>
          </form>
        )}

        <p className="register-footer-text">
          Đã có tài khoản?{" "}
          <Link to="/login" className="register-login-link">
            Đăng nhập
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Register;
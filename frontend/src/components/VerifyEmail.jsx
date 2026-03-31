import React, { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import "./VerifyEmail.css";
import { message } from "antd";
import { verifyEmailOtp, resendEmailOtp } from "../services/auth";

const RESEND_COOLDOWN_SECONDS = 60;

function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const emailFromQuery = searchParams.get("email") || "";
  const typeFromQuery = searchParams.get("type") || "SIGNUP";

  const [email, setEmail] = useState(emailFromQuery);
  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [verified, setVerified] = useState(false);

  const otpInputRef = useRef(null);

  // Focus OTP input on mount
  useEffect(() => {
    if (otpInputRef.current) {
      otpInputRef.current.focus();
    }
  }, []);

  // Resend countdown timer
  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const mapErrorCode = useCallback((code, fallbackMsg) => {
    const errorMap = {
      OTP_EXPIRED: "Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.",
      OTP_TOO_MANY_ATTEMPTS: "Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu gửi lại OTP.",
      OTP_INVALID: "Mã OTP không đúng. Vui lòng kiểm tra lại.",
      OTP_RESEND_LIMIT_EXCEEDED: "Bạn đã vượt quá số lần gửi lại OTP trong ngày. Vui lòng thử lại vào ngày mai.",
    };
    return errorMap[code] || fallbackMsg || "Đã có lỗi xảy ra. Vui lòng thử lại.";
  }, []);

  const handleVerify = async (e) => {
    e.preventDefault();

    if (!email.trim()) {
      setError("Vui lòng nhập email.");
      return;
    }
    if (!otp.trim()) {
      setError("Vui lòng nhập mã OTP.");
      return;
    }
    if (otp.trim().length !== 6) {
      setError("Mã OTP phải gồm 6 chữ số.");
      return;
    }

    setError("");
    setLoading(true);

    try {
      await verifyEmailOtp({
        email: email.trim(),
        otp: otp.trim(),
        type: typeFromQuery,
      });
      setVerified(true);
      message.success("Xác thực email thành công! Đang chuyển hướng đến trang đăng nhập...", 3);
      setTimeout(() => navigate("/login"), 2000);
    } catch (err) {
      const body = err?.response?.data;
      const code = body?.errorCode || body?.code;
      const msg = mapErrorCode(code, body?.message);
      setError(msg);
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!email.trim()) {
      setError("Vui lòng nhập email trước khi gửi lại OTP.");
      return;
    }
    if (resendCooldown > 0) return;

    setError("");
    setLoading(true);

    try {
      await resendEmailOtp({
        email: email.trim(),
        type: typeFromQuery,
      });
      setResendCooldown(RESEND_COOLDOWN_SECONDS);
      setOtp("");
      message.success("Đã gửi lại mã OTP. Vui lòng kiểm tra email của bạn.", 3);
    } catch (err) {
      const body = err?.response?.data;
      const code = body?.errorCode || body?.code;
      const msg = mapErrorCode(code, body?.message);
      setError(msg);
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  // Handle OTP input - only allow digits, max 6
  const handleOtpChange = (e) => {
    const value = e.target.value.replace(/\D/g, "").slice(0, 6);
    setOtp(value);
  };

  if (verified) {
    return (
      <div className="verify-email-page">
        <div className="verify-email-card">
          <div className="verify-email-success">
            <span className="verify-email-success-icon">✅</span>
            <p className="verify-email-success-text">Email đã được xác thực thành công!</p>
            <p className="verify-email-success-redirect">
              Đang chuyển hướng đến trang đăng nhập...
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="verify-email-page">
      <div className="verify-email-card">
        <div className="verify-email-icon">📧</div>
        <h2 className="verify-email-title">Xác thực email</h2>
        <p className="verify-email-subtitle">
          {email ? (
            <>
              Nhập mã OTP 6 chữ số đã được gửi đến{" "}
              <strong>{email}</strong>
            </>
          ) : (
            "Nhập email và mã OTP để xác thực tài khoản của bạn."
          )}
        </p>

        <form className="verify-email-form" onSubmit={handleVerify}>
          {!emailFromQuery && (
            <div className="verify-email-field">
              <label htmlFor="verify-email-input">Email</label>
              <input
                id="verify-email-input"
                type="email"
                className="otp-single-input"
                placeholder="example@gmail.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                style={{ letterSpacing: "normal", fontSize: "0.98rem", fontWeight: "normal" }}
                required
              />
            </div>
          )}

          <div className="verify-email-field">
            <label htmlFor="verify-otp-input">Mã OTP</label>
            <input
              ref={otpInputRef}
              id="verify-otp-input"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              className="otp-single-input"
              placeholder="000000"
              value={otp}
              onChange={handleOtpChange}
              maxLength={6}
            />
          </div>

          <p className="verify-email-info">
            Mã OTP có hiệu lực trong 10 phút. Nếu bạn chưa nhận được, hãy bấm{" "}
            {resendCooldown > 0 ? (
              <span className="verify-email-countdown">
                gửi lại sau {resendCooldown}s
              </span>
            ) : (
              <button
                type="button"
                className="verify-email-resend-btn"
                onClick={handleResend}
                disabled={loading}
              >
                gửi lại OTP
              </button>
            )}
            .
          </p>

          {error && <p className="verify-email-error">{error}</p>}

          <button
            type="submit"
            className="verify-email-submit-btn"
            disabled={loading || otp.length !== 6}
          >
            {loading ? "Đang xác thực..." : "Xác thực email"}
          </button>
        </form>

        <p className="verify-email-footer">
          Đã xác thực?{" "}
          <Link to="/login">Đăng nhập</Link>
        </p>
      </div>
    </div>
  );
}

export default VerifyEmail;

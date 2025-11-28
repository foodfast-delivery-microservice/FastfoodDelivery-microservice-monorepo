import React, { createContext, useContext, useEffect, useState } from "react";
import { message } from "antd";
import { getProfile } from "../services/auth";

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Hàm helper để set session và update state
  const setSession = (sessionData) => {
    if (sessionData) {
      localStorage.setItem("app_session", JSON.stringify(sessionData));
      setCurrentUser(sessionData);
    } else {
      localStorage.removeItem("app_session");
      setCurrentUser(null);
    }
  };

  useEffect(() => {
    const checkUser = async () => {
      console.log("🟡 [Auth] Bắt đầu kiểm tra user...");
      try {
        // 1. Lấy session từ localStorage
        const stored = localStorage.getItem("app_session");

        if (stored) {
          const parsedSession = JSON.parse(stored);
          // parsedSession chứa { accessToken, ...userProfile }

          // 2. Set tạm vào state để hiển thị UI ngay
          setCurrentUser(parsedSession);
          console.log("📦 [Auth] Có session local:", parsedSession.username || parsedSession.email);

          // 3. Gọi API lấy thông tin mới nhất (verify token luôn)
          try {
            const profileResponse = await getProfile();
            const userProfile = profileResponse?.data || profileResponse;

            // Merge thông tin mới nhất vào session
            const updatedSession = { ...parsedSession, ...userProfile };

            // Nếu user bị ban
            if (userProfile.status === "banned" || userProfile.active === false) {
              message.error("🚫 Tài khoản bị chặn hoặc chưa kích hoạt!");
              setSession(null); // Logout
              setTimeout(() => (window.location.href = "/login"), 2000);
              return;
            }

            setSession(updatedSession);
            console.log("🔥 [Auth] Đã cập nhật user từ Backend");
          } catch (apiErr) {
            console.error("⚠️ [Auth] Token hết hạn hoặc lỗi API:", apiErr);
            // Nếu lỗi 401 thì logout
            if (apiErr.response && apiErr.response.status === 401) {
              setSession(null);
            }
          }
        } else {
          console.log("⚪ [Auth] Không có session trong localStorage.");
          setCurrentUser(null);
        }
      } catch (err) {
        console.error("🔥 [Auth] Lỗi kiểm tra user:", err);
        setSession(null);
      } finally {
        console.log("🟢 [Auth] Hoàn tất khởi tạo AuthContext");
        setLoading(false);
      }
    };
    checkUser();
  }, []);

  const logout = () => {
    console.log("🚪 [Auth] Đăng xuất");
    setSession(null);
    window.location.href = "/login";
  };

  return (
    <AuthContext.Provider value={{ currentUser, setCurrentUser, setSession, logout, loading }}>
      {loading ? <p>⏳ Đang xác thực người dùng...</p> : children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

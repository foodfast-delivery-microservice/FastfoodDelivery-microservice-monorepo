import axios from "axios";

// 1. Cấu hình baseURL trỏ đúng về Backend Spring Boot (Port 8080)
// Lưu ý: Kiểm tra xem backend của bạn có prefix "/api/v1" không nhé
export const api = axios.create({
  baseURL: "http://localhost:8080/api/v1", 
  headers: {
    "Content-Type": "application/json",
  },
});

// 2. Cấu hình Interceptor (Cái này quan trọng nhất để sửa lỗi 403)
// Nó hoạt động như một "người gác cổng": Trước khi request bay đi, nó sẽ tự động nhét Token vào túi.
api.interceptors.request.use(
  (config) => {
    // BƯỚC QUAN TRỌNG: Kiểm tra xem lúc Login bạn lưu token tên là gì?
    // Thường là 'accessToken', 'token', hoặc 'auth_token'. 
    // Hãy mở F12 -> Application -> Local Storage để xem tên chính xác.
    const token = localStorage.getItem("accessToken"); 

    if (token) {
      // Gắn token vào Header theo chuẩn JWT
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
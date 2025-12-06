import axios from 'axios'

const http = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: Tự động thêm JWT token vào header
http.interceptors.request.use(
  (config) => {
    try {
      const session = localStorage.getItem('app_session')
      if (session) {
        const parsedSession = JSON.parse(session)
        if (parsedSession.accessToken) {
          config.headers.Authorization = `Bearer ${parsedSession.accessToken}`
          console.log('🔑 [HTTP] Đã thêm token vào request:', config.url)
        } else {
          console.warn('⚠️ [HTTP] Không tìm thấy accessToken trong session cho request:', config.url)
        }
      } else {
        console.warn('⚠️ [HTTP] Không có session trong localStorage cho request:', config.url)
      }
      // Don't override Content-Type for multipart/form-data (file uploads)
      if (config.data instanceof FormData) {
        delete config.headers['Content-Type']
      }
    } catch (error) {
      console.error('❌ [HTTP] Lỗi đọc session từ localStorage:', error)
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor: Xử lý lỗi 401 (Unauthorized) và 403 (Forbidden)
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token hết hạn hoặc không hợp lệ
      console.error('❌ [HTTP] 401 Unauthorized - Token không hợp lệ hoặc hết hạn')
      localStorage.removeItem('app_session')
      // Có thể redirect đến trang login nếu cần
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else if (error.response?.status === 403) {
      // 403 Forbidden - Không có quyền truy cập
      console.error('❌ [HTTP] 403 Forbidden - Không có quyền truy cập:', error.config?.url)
      const session = localStorage.getItem('app_session')
      if (session) {
        try {
          const parsedSession = JSON.parse(session)
          console.log('🔍 [HTTP] Session hiện tại:', {
            hasToken: !!parsedSession.accessToken,
            username: parsedSession.username || parsedSession.email,
            role: parsedSession.role
          })
        } catch (e) {
          console.error('❌ [HTTP] Lỗi parse session:', e)
        }
      }
    }
    return Promise.reject(error)
  }
)

export default http

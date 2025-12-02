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
        }
      }
      // Don't override Content-Type for multipart/form-data (file uploads)
      if (config.data instanceof FormData) {
        delete config.headers['Content-Type']
      }
    } catch (error) {
      console.error('Error reading session from localStorage:', error)
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor: Xử lý lỗi 401 (Unauthorized)
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token hết hạn hoặc không hợp lệ
      localStorage.removeItem('app_session')
      // Có thể redirect đến trang login nếu cần
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default http

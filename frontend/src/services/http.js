import axios from 'axios'
import API_SERVICES from './apiConfig'

// ===== Tạo axios instance cho từng service =====

// User Service (Authentication, Users, Merchants, Restaurants)
export const userHttp = axios.create({
  baseURL: API_SERVICES.USER,
  headers: { 'Content-Type': 'application/json' },
})

// Product Service (Products, Categories)
export const productHttp = axios.create({
  baseURL: API_SERVICES.PRODUCT,
  headers: { 'Content-Type': 'application/json' },
})

// Order Service (Orders)
export const orderHttp = axios.create({
  baseURL: API_SERVICES.ORDER,
  headers: { 'Content-Type': 'application/json' },
})

// Payment Service (Payments)
export const paymentHttp = axios.create({
  baseURL: API_SERVICES.PAYMENT,
  headers: { 'Content-Type': 'application/json' },
})

// Drone Service (Drones, Deliveries)
export const droneHttp = axios.create({
  baseURL: API_SERVICES.DRONE,
  headers: { 'Content-Type': 'application/json' },
})

// Gateway (fallback hoặc dùng khi demo)
export const gatewayHttp = axios.create({
  baseURL: API_SERVICES.GATEWAY,
  headers: { 'Content-Type': 'application/json' },
})

// Default http instance (sử dụng User Service làm mặc định)
const http = userHttp

// ===== Shared Interceptors =====

const instances = [userHttp, productHttp, orderHttp, paymentHttp, droneHttp, gatewayHttp]

// Request interceptor: Tự động thêm JWT token vào header
instances.forEach(instance => {
  instance.interceptors.request.use(
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
  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        // Token hết hạn hoặc không hợp lệ
        localStorage.removeItem('app_session')
        // Redirect đến trang login nếu cần
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
      return Promise.reject(error)
    }
  )
})

export default http

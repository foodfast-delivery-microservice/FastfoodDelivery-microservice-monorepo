import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

const http = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const stored =
    localStorage.getItem('app_session') ??
    localStorage.getItem('user') ??
    localStorage.getItem('session')

  if (stored) {
    try {
      const parsed = JSON.parse(stored)
      const token = parsed?.accessToken ?? parsed?.token
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch {
      // ignore malformed session
    }
  }
  return config
})

export default http


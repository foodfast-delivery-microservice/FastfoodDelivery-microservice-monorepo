import http from './http'

// Helper để unwrap ApiResponse
const unwrapData = (responseData) => {
  // Nếu là ApiResponse wrapper: { status, message, data: T }
  if (responseData?.data !== undefined && responseData?.status !== undefined) {
    return responseData.data
  }
  // Nếu trả về trực tiếp
  return responseData
}

export const login = async (credentials) => {
  const { data } = await http.post('/auth/login', credentials)
  return unwrapData(data)
}

export const register = async (payload) => {
  const { data } = await http.post('/auth/register', payload)
  return unwrapData(data)
}

export const getProfile = async () => {
  const { data } = await http.get('/users/me')
  return unwrapData(data)
}

export const updateProfile = async (partial) => {
  const { data } = await http.patch('/users/me', partial)
  return unwrapData(data)
}








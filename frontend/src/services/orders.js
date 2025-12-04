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

const unwrapPage = (payload) => {
  const unwrapped = unwrapData(payload)
  if (!unwrapped) return []
  if (Array.isArray(unwrapped)) return unwrapped
  if (Array.isArray(unwrapped.content)) return unwrapped.content
  return []
}

export const createOrder = async (payload) => {
  const { data } = await http.post('/orders', payload)
  return unwrapData(data)
}

export const getOrderById = async (orderId) => {
  const { data } = await http.get(`/orders/${orderId}`)
  return unwrapData(data)
}

export const listMyOrders = async (params = {}) => {
  const { data } = await http.get('/orders/my-orders', { params })
  return unwrapPage(data)
}

export const listMerchantOrders = async (params = {}) => {
  const { data } = await http.get('/orders/merchants/me', { params })
  return unwrapPage(data)
}

export const listAllOrders = async (params = {}) => {
  const { data } = await http.get('/orders', { params })
  return unwrapData(data)
}

export const updateOrderStatus = async (orderId, status) => {
  const { data } = await http.put(`/orders/${orderId}/status`, { status })
  return unwrapData(data)
}


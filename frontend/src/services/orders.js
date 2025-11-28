import http from './http'

const unwrapPage = (payload) => {
  if (!payload) return []
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload.content)) return payload.content
  return []
}

export const createOrder = async (payload) => {
  const { data } = await http.post('/orders', payload)
  return data
}

export const getOrderById = async (orderId) => {
  const { data } = await http.get(`/orders/${orderId}`)
  return data
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
  return data
}

export const updateOrderStatus = async (orderId, status) => {
  const { data } = await http.put(`/orders/${orderId}/status`, { status })
  return data
}

export const requestRefund = async (orderId, payload) => {
  const { data } = await http.post(`/orders/${orderId}/refund`, payload)
  return data
}



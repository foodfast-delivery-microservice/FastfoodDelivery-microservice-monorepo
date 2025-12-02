import http from './http'

// Helper để unwrap ApiResponse
const unwrapData = (responseData) => {
  if (responseData?.data !== undefined && responseData?.status !== undefined) {
    return responseData.data
  }
  return responseData
}

export const getUsers = async (params = {}) => {
  const { data } = await http.get('/users', { params })
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

export const updateUserStatus = async (userId, active) => {
  const { data } = await http.patch(`/users/${userId}`, { active })
  return unwrapData(data)
}

export const deleteUser = async (userId) => {
  await http.delete(`/users/${userId}`)
}

export const getOrders = async (params = {}) => {
  const { data } = await http.get('/orders', { params })
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

export const getOrderDetails = async (orderId) => {
  const { data } = await http.get(`/orders/${orderId}`)
  return unwrapData(data)
}

export const getMerchantStatistics = async (merchantId, params = {}) => {
  const { data } = await http.get(`/payments/merchants/${merchantId}/statistics`, {
    params,
  })
  return unwrapData(data)
}

export const getKpis = async (params = {}) => {
  const { data } = await http.get('/admin/dashboard/kpis', { params })
  return unwrapData(data)
}








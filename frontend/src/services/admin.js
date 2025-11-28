import http from './http'

export const getUsers = async (params = {}) => {
  const { data } = await http.get('/users', { params })
  return data
}

export const updateUserStatus = async (userId, active) => {
  const { data } = await http.patch(`/users/${userId}`, { active })
  return data
}

export const deleteUser = async (userId) => {
  await http.delete(`/users/${userId}`)
}

export const getOrders = async (params = {}) => {
  const { data } = await http.get('/orders', { params })
  return data
}

export const getOrderDetails = async (orderId) => {
  const { data } = await http.get(`/orders/${orderId}`)
  return data
}

export const getMerchantStatistics = async (merchantId, params = {}) => {
  const { data } = await http.get(`/payments/merchants/${merchantId}/statistics`, {
    params,
  })
  return data
}

export const getKpis = async (params = {}) => {
  const { data } = await http.get('/admin/dashboard/kpis', { params })
  return data
}



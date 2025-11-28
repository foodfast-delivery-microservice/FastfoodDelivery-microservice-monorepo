import http from './http'

export const getMerchantStats = async (params = {}) => {
  const { fromDate, toDate } = params
  const { data } = await http.get('/payments/merchants/me/statistics', {
    params: { fromDate, toDate },
  })
  return data
}

export const getMerchantOrders = async (params = {}) => {
  const { data } = await http.get('/orders/merchants/me', { params })
  return data
}

export const getMerchantProducts = async (params = {}) => {
  const { data } = await http.get('/products/merchants/me', {
    params: { includeInactive: true, ...params },
  })
  return data
}

export const createMerchantProduct = async (payload) => {
  const { data } = await http.post('/products', payload)
  return data
}

export const updateMerchantProduct = async (productId, payload) => {
  const { data } = await http.put(`/products/${productId}`, payload)
  return data
}

export const deleteMerchantProduct = async (productId) => {
  await http.delete(`/products/${productId}`)
}



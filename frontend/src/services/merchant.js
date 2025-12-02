import http from './http'

// Helper để unwrap ApiResponse
const unwrapData = (responseData) => {
  if (responseData?.data !== undefined && responseData?.status !== undefined) {
    return responseData.data
  }
  return responseData
}

export const getMerchantStats = async (params = {}) => {
  const { fromDate, toDate } = params
  const { data } = await http.get('/payments/merchants/me/statistics', {
    params: { fromDate, toDate },
  })
  return unwrapData(data)
}

export const getMerchantOrders = async (params = {}) => {
  const { data } = await http.get('/orders/merchants/me', { params })
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

export const getMerchantProducts = async (params = {}) => {
  const { data } = await http.get('/products/merchants/me', {
    params: { includeInactive: true, ...params },
  })
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

export const createMerchantProduct = async (payload) => {
  const { data } = await http.post('/products', payload)
  return unwrapData(data)
}

export const updateMerchantProduct = async (productId, payload) => {
  const { data } = await http.put(`/products/${productId}`, payload)
  return unwrapData(data)
}

export const deleteMerchantProduct = async (productId) => {
  await http.delete(`/products/${productId}`)
}








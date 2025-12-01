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

export const fetchProducts = async (params = {}) => {
  const { data } = await http.get('/products', { params })
  const unwrapped = unwrapData(data)
  // Backend có thể trả về PageResponse hoặc array trực tiếp
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  // Nếu là PageResponse, trả về content
  return unwrapped?.content || []
}

export const fetchProductById = async (id) => {
  const { data } = await http.get(`/products/${id}`)
  return unwrapData(data)
}

export const fetchProductsByCategory = async (category) => {
  const { data } = await http.get(`/products/${category}`)
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

/**
 * Public endpoint: Get products by merchantId (for guests)
 * @param {number} merchantId - The merchant ID
 * @returns {Promise<Array>} List of active products
 */
export const fetchProductsByMerchantId = async (merchantId) => {
  const { data } = await http.get(`/products/merchants/${merchantId}`)
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

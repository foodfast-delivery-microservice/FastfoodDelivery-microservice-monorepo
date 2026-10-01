import { productHttp as http } from './http'
import { demoProducts } from '../data/demoData'

const IS_DEMO = import.meta.env.VITE_DEMO_MODE === 'true'

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
  if (IS_DEMO) return demoProducts

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
  if (IS_DEMO) return demoProducts.find((product) => product.id === id) || null

  const { data } = await http.get(`/products/${id}`)
  return unwrapData(data)
}

export const fetchProductsByCategory = async (category) => {
  if (IS_DEMO) {
    return demoProducts.filter((product) => product.category.toLowerCase() === category.toLowerCase())
  }

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
  if (IS_DEMO) return demoProducts.filter((product) => product.merchantId === merchantId)

  const { data } = await http.get(`/products/merchants/${merchantId}`)
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

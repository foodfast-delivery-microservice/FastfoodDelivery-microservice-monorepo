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

export const fetchRestaurants = async (params = {}) => {
  const { data } = await http.get('/restaurants', { params })
  const unwrapped = unwrapData(data)
  // Backend trả về Page<RestaurantResponse>
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  // Nếu là Page object
  return unwrapped?.content || []
}

export const fetchRestaurantById = async (id) => {
  const { data } = await http.get(`/restaurants/${id}`)
  return unwrapData(data)
}

// Lấy restaurant theo merchantId (dùng cho trang chi tiết món)
export const fetchRestaurantByMerchantId = async (merchantId) => {
  if (!merchantId) return null
  const { data } = await http.get(`/restaurants/merchants/${merchantId}`)
  return unwrapData(data)
}

export const fetchRestaurantMenu = async (restaurantId, params = {}) => {
  // Lấy menu từ products service với merchantId
  // Dùng endpoint public /products/merchants/{merchantId} để guest có thể xem
  
  // Lấy merchantId từ restaurant
  const restaurant = await fetchRestaurantById(restaurantId)
  const merchantId = restaurant?.merchantId
  
  if (!merchantId) {
    console.warn(`Restaurant ${restaurantId} không có merchantId`)
    return []
  }
  
  // Dùng endpoint public mới: /products/merchants/{merchantId}
  // Endpoint này chỉ trả về active products (phù hợp cho guest)
  const { data } = await http.get(`/products/merchants/${merchantId}`, { params })
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

import http from './http'
import { fetchProducts } from './products'

// Helper to enhance restaurant data (add fallback image if missing)
const enhanceRestaurant = (restaurant) => {
  return {
    ...restaurant,
    // Use restaurantImage if available, otherwise avatar, otherwise fallback
    img: restaurant.restaurantImage || restaurant.avatar || '/Images/Logo.png',
    name: restaurant.restaurantName || restaurant.fullName || restaurant.username || 'Nhà hàng',
    address: restaurant.restaurantAddress || restaurant.address || 'Chưa cập nhật địa chỉ',
    openingHours: restaurant.openingHours || '8:00 - 22:00',
    rating: 4.5, // Mock rating for now
    distance: '2.5km', // Mock distance
    deliveryTime: '15-20 min', // Mock time
  }
}

export const fetchRestaurants = async () => {
  try {
    const { data } = await http.get('/users/restaurants')
    // API returns ApiResponse<List<CreateUserResponse>>
    // data.data is the list
    const list = data?.data || data || []
    return list.map(enhanceRestaurant)
  } catch (error) {
    console.error('Error fetching restaurants:', error)
    return []
  }
}

export const fetchRestaurantById = async (id) => {
  try {
    const { data } = await http.get(`/users/${id}`)
    // API returns ApiResponse<CreateUserResponse>
    const restaurant = data?.data || data
    return enhanceRestaurant(restaurant)
  } catch (error) {
    console.error('Error fetching restaurant detail:', error)
    return null
  }
}

export const fetchRestaurantMenu = async (merchantId) => {
  // Reuse product service to fetch products for this merchant
  // Backend must support filtering by merchantId in GET /products
  // If not, we might need to filter client-side or add backend support
  return await fetchProducts({ merchantId })
}

export const listRestaurants = fetchRestaurants

export const appendLocalRestaurant = async (restaurant) => {
  console.warn('appendLocalRestaurant: Not implemented via API yet', restaurant)
}

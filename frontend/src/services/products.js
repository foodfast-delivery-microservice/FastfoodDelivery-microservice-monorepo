import http from './http'
import sharedDb from '../../shared/db.json'

const sharedProducts = sharedDb.products ?? []
const sharedRestaurants = sharedDb.restaurants ?? []

const toRestaurantName = (merchantId) => {
  if (!merchantId) return 'Đối tác'
  const fallback = sharedRestaurants.find(
    (r) => String(r.id) === String(merchantId) || String(r.id) === String(merchantId).padStart(2, '0'),
  )
  return fallback?.name ?? `Merchant #${merchantId}`
}

const enhanceProduct = (product) => {
  const fallback = sharedProducts.find(
    (p) => String(p.id) === String(product.id) || Number(p.id) === Number(product.id),
  )

  return {
    id: product.id,
    name: product.name ?? fallback?.name ?? 'Sản phẩm',
    description: product.description ?? fallback?.description ?? '',
    price: Number(product.price ?? fallback?.price ?? 0),
    stock: product.stock ?? fallback?.stock ?? 0,
    category: product.category ?? fallback?.category ?? 'OTHER',
    active: product.active ?? fallback?.isAvailable ?? true,
    merchantId: product.merchantId ?? fallback?.restaurantId ?? null,
    restaurantName: fallback?.restaurant ?? toRestaurantName(product.merchantId),
    img: product.imageUrl ?? fallback?.img ?? '/Images/Logo.png',
    rating: fallback?.rating ?? 4.7,
    reviews: fallback?.reviews ?? 0,
    discount: fallback?.discount ?? 0,
    ingredients: fallback?.ingredients ?? [],
  }
}

const unwrapPage = (payload) => {
  if (!payload) return []
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload.data)) return payload.data
  if (Array.isArray(payload.content)) return payload.content
  return []
}

export const fetchProducts = async (params = {}) => {
  const { data } = await http.get('/products', {
    params: { page: 0, size: 100, ...params },
  })
  return unwrapPage(data).map(enhanceProduct)
}

export const fetchProductById = async (id) => {
  const { data } = await http.get(`/products/${id}`)
  return enhanceProduct(data?.data || data)
}

export const fetchMerchantProducts = async (params = {}) => {
  const { data } = await http.get('/products/merchants/me', { params })
  return unwrapPage(data).map(enhanceProduct)
}

export const createProduct = async (payload) => {
  const { data } = await http.post('/products', payload)
  return enhanceProduct(data?.data || data)
}

export const updateProduct = async (productId, payload) => {
  const { data } = await http.put(`/products/${productId}`, payload)
  return enhanceProduct(data?.data || data)
}

export const deleteProduct = async (productId) => {
  await http.delete(`/products/${productId}`)
}


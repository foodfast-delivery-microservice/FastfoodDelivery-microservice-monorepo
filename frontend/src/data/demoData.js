import { assetUrl } from '../utils/assetUrl'

// Public showcase data only. Keep private user, order, and account fixtures out
// of the static bundle because everything in a GitHub Pages build is public.
export const demoRestaurants = [
  {
    id: 'demo-r1', merchantId: 'demo-m1', name: 'Meowchick Quận 1',
    address: 'Quận 1, TP. Hồ Chí Minh', imageUrl: assetUrl('Images/Hambur.jpg'),
    description: 'Gà rán giòn, burger bò phô mai và món ăn nhanh cho bữa trưa.',
    category: 'FOOD', rating: 4.8, deliveryTime: '25–35 phút', distance: '1.2 km', openingHours: '10:00–22:00',
  },
  {
    id: 'demo-r2', merchantId: 'demo-m2', name: 'Taco House Bình Thạnh',
    address: 'Bình Thạnh, TP. Hồ Chí Minh', imageUrl: assetUrl('Images/tacos.jpg'),
    description: 'Tacos, burrito và sốt Mexico được làm mới mỗi ngày.',
    category: 'FOOD', rating: 4.9, deliveryTime: '30–40 phút', distance: '2.4 km', openingHours: '10:00–21:30',
  },
  {
    id: 'demo-r3', merchantId: 'demo-m3', name: 'Sushi Zen',
    address: 'Quận 1, TP. Hồ Chí Minh', imageUrl: assetUrl('Images/Sushi.jpg'),
    description: 'Sushi cá hồi, món Nhật và cơm cuộn cho bữa ăn nhẹ.',
    category: 'FOOD', rating: 4.9, deliveryTime: '35–45 phút', distance: '2.8 km', openingHours: '11:00–22:00',
  },
  {
    id: 'demo-r4', merchantId: 'demo-m4', name: 'Sweetie Coffee',
    address: 'Quận 5, TP. Hồ Chí Minh', imageUrl: assetUrl('Images/latte.jpg'),
    description: 'Cà phê, trà và bánh ngọt làm tại quán.',
    category: 'DRINK', rating: 4.7, deliveryTime: '20–30 phút', distance: '1.8 km', openingHours: '08:00–22:00',
  },
]

export const demoProducts = [
  {
    id: 'demo-p1', name: 'Burger Bò Phô Mai', img: assetUrl('Images/Hambur.jpg'), price: 59000,
    category: 'Burger', restaurantId: 'demo-r1', merchantId: 'demo-m1', restaurant: 'Meowchick Quận 1',
    restaurantName: 'Meowchick Quận 1', description: 'Burger bò nướng, phô mai tan chảy và rau tươi.',
    rating: 4.8, reviews: 125, discount: 10, active: true,
  },
  {
    id: 'demo-p2', name: 'Gà Rán Giòn Cay', img: assetUrl('Images/Garan.jpg'), price: 69000,
    category: 'Gà rán', restaurantId: 'demo-r1', merchantId: 'demo-m1', restaurant: 'Meowchick Quận 1',
    restaurantName: 'Meowchick Quận 1', description: 'Gà rán giòn bên ngoài, mềm mọng bên trong.',
    rating: 4.7, reviews: 98, discount: 0, active: true,
  },
  {
    id: 'demo-p3', name: 'Tacos Thịt Bò Mexico', img: assetUrl('Images/tacos.jpg'), price: 65000,
    category: 'Tacos', restaurantId: 'demo-r2', merchantId: 'demo-m2', restaurant: 'Taco House Bình Thạnh',
    restaurantName: 'Taco House Bình Thạnh', description: 'Tacos bò, rau tươi và sốt Mexico đặc trưng.',
    rating: 4.9, reviews: 143, discount: 5, active: true,
  },
  {
    id: 'demo-p4', name: 'Sushi Cá Hồi', img: assetUrl('Images/Sushi.jpg'), price: 89000,
    category: 'Sushi', restaurantId: 'demo-r3', merchantId: 'demo-m3', restaurant: 'Sushi Zen',
    restaurantName: 'Sushi Zen', description: 'Cá hồi, cơm Nhật và rong biển.',
    rating: 4.9, reviews: 83, discount: 0, active: true,
  },
  {
    id: 'demo-p5', name: 'Latte Đá', img: assetUrl('Images/latte.jpg'), price: 45000,
    category: 'Cà phê', restaurantId: 'demo-r4', merchantId: 'demo-m4', restaurant: 'Sweetie Coffee',
    restaurantName: 'Sweetie Coffee', description: 'Espresso cân bằng cùng sữa tươi.',
    rating: 4.8, reviews: 67, discount: 0, active: true,
  },
]

export const demoCategories = [...new Set(demoProducts.map((product) => product.category))]


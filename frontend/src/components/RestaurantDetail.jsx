import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { fetchRestaurantById, fetchRestaurantMenu } from '../services/restaurants';
import Product from './Product';
import './RestaurantDetail.css'; // We'll create this CSS next

const toAbsoluteUrl = (src) => {
  if (!src) return null;
  if (src.startsWith?.("http")) return src;
  const base = "http://localhost:8080";
  return src.startsWith("/") ? `${base}${src}` : `${base}/${src}`;
};

const RestaurantDetail = ({ onAdd }) => {
  const { id } = useParams();
  const [restaurant, setRestaurant] = useState(null);
  const [menu, setMenu] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        // Lấy restaurant trước để có merchantId
        const resData = await fetchRestaurantById(id);
        setRestaurant(resData);
        
        // Sau đó lấy menu với merchantId từ restaurant
        const menuData = await fetchRestaurantMenu(id);
        setMenu(menuData);
      } catch (error) {
        console.error('Failed to load restaurant detail:', error);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      loadData();
    }
  }, [id]);

  if (loading) return <div className="loading-screen">Đang tải thông tin quán...</div>;
  if (!restaurant) return <div className="error-screen">Không tìm thấy quán!</div>;

  // Group menu by category
  const menuByCategory = menu.reduce((acc, item) => {
    const category = item.category || 'Khác';
    if (!acc[category]) acc[category] = [];
    acc[category].push(item);
    return acc;
  }, {});

  return (
    <div className="restaurant-detail-page">
      {/* Restaurant Header */}
      <div className="restaurant-header">
        <div className="header-container">
          <div className="header-image">
            <img
              src={
                toAbsoluteUrl(restaurant.imageUrl) ||
                toAbsoluteUrl(restaurant.image) ||
                toAbsoluteUrl(restaurant.img) ||
                '/Images/Logo.png'
              }
              alt={restaurant.name}
              onError={(e) => { e.target.src = '/Images/Logo.png'; }}
            />
          </div>
          <div className="header-info">
            <h1>{restaurant.name}</h1>
            <p className="address">{restaurant.address}</p>
            <div className="meta">
              <span className="rating">⭐ {restaurant.rating}</span>
              <span className="dot">•</span>
              <span className="hours">🕒 {restaurant.openingHours}</span>
              <span className="dot">•</span>
              <span className="distance">{restaurant.distance}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Menu Sections */}
      <div className="menu-container">
        <div className="menu-nav">
          <h3>Thực đơn</h3>
          <ul>
            {Object.keys(menuByCategory).map(cat => (
              <li key={cat}><a href={`#cat-${cat}`}>{cat}</a></li>
            ))}
          </ul>
        </div>

        <div className="menu-list">
          {Object.entries(menuByCategory).map(([category, items]) => (
            <div key={category} id={`cat-${category}`} className="menu-category-section">
              <h3 className="category-title">{category}</h3>
              <div className="product-grid">
                {items.map(product => (
                  <Product 
                    key={product.id} 
                    product={{
                      ...product,
                      imageUrl: product.imageUrl || product.image || product.img,
                      img: product.img || product.imageUrl || product.image,
                      restaurant: restaurant?.name || product.restaurantName,
                      restaurantId: restaurant?.merchantId || product.merchantId,
                      restaurantName: restaurant?.name || product.restaurantName
                    }} 
                    onAdd={onAdd}
                  />
                ))}
              </div>
            </div>
          ))}
          {menu.length === 0 && (
            <div className="no-menu">Quán chưa cập nhật thực đơn.</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default RestaurantDetail;

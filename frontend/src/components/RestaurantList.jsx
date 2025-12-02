import React, { useEffect, useState } from 'react';
import { fetchRestaurants } from '../services/restaurants';
import RestaurantCard from './RestaurantCard';
import Banner from './Banner';
import './RestaurantList.css'; // We'll create this CSS next

const RestaurantList = () => {
    const [restaurants, setRestaurants] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('All');

    const bannerImages = ["/Images/1.png", "/Images/Banner2.png", "/Images/Banner3.png"];

    // Categories matching backend Restaurant.category enum
    const categories = [
        { id: 'All', name: 'Tất cả', icon: '🍽️' },
        { id: 'FOOD', name: 'Đồ ăn', icon: '🍔' },
        { id: 'DRINK', name: 'Đồ uống', icon: '🥤' },
    ];

    useEffect(() => {
        const loadRestaurants = async () => {
            try {
                setLoading(true);
                const data = await fetchRestaurants();
                setRestaurants(data);
            } catch (error) {
                console.error('Failed to load restaurants:', error);
            } finally {
                setLoading(false);
            }
        };

        loadRestaurants();
    }, []);

    // Filter logic
    const filteredRestaurants = restaurants.filter(r => {
        const matchSearch = r.name?.toLowerCase().includes(searchTerm.toLowerCase()) ?? false;
        
        // Filter by category
        let matchCategory = true;
        if (selectedCategory && selectedCategory !== 'All') {
            if (selectedCategory === 'FOOD') {
                // "Đồ ăn" = hiển thị FOOD hoặc BOTH (vì BOTH có cả đồ ăn)
                matchCategory = r.category === 'FOOD' || r.category === 'BOTH';
            } else if (selectedCategory === 'DRINK') {
                // "Đồ uống" = hiển thị DRINK hoặc BOTH (vì BOTH có cả đồ uống)
                matchCategory = r.category === 'DRINK' || r.category === 'BOTH';
            } else {
                // Các category khác: match chính xác
                matchCategory = r.category === selectedCategory;
            }
        }
        
        return matchSearch && matchCategory;
    });

    return (
        <div className="restaurant-list-page">
            <div className="banner-wrapper">
                <Banner images={bannerImages} />
            </div>

            <div className="container">
                {/* Search & Filter Section */}
                <div className="filter-section">
                    <div className="search-bar">
                        <input
                            type="text"
                            placeholder="Tìm quán ăn, trà sữa..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    <div className="category-list">
                        {categories.map(cat => (
                            <button
                                key={cat.id}
                                className={`category-item ${selectedCategory === cat.id ? 'active' : ''}`}
                                onClick={() => setSelectedCategory(cat.id)}
                            >
                                <span className="cat-icon">{cat.icon}</span>
                                <span className="cat-name">{cat.name}</span>
                            </button>
                        ))}
                    </div>
                </div>

                {/* Restaurant Grid */}
                <div className="section-title">
                    <h2>Quán ngon quanh đây</h2>
                </div>

                {loading ? (
                    <div className="loading">Đang tải danh sách quán...</div>
                ) : (
                    <div className="restaurant-grid">
                        {filteredRestaurants.length > 0 ? (
                            filteredRestaurants.map(restaurant => (
                                <RestaurantCard key={restaurant.id} restaurant={restaurant} />
                            ))
                        ) : (
                            <div className="no-results">Không tìm thấy quán nào!</div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default RestaurantList;

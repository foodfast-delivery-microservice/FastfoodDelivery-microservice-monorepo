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

    // Mock categories for now (until we have a Category API)
    const categories = [
        { id: 'All', name: 'Tất cả', icon: '🍽️' },
        { id: 'Rice', name: 'Cơm', icon: '🍚' },
        { id: 'Noodle', name: 'Bún/Phở', icon: '🍜' },
        { id: 'FastFood', name: 'Đồ ăn nhanh', icon: '🍔' },
        { id: 'Drink', name: 'Đồ uống', icon: '🥤' },
        { id: 'Snack', name: 'Ăn vặt', icon: '🍟' },
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
        const matchSearch = r.name.toLowerCase().includes(searchTerm.toLowerCase());
        // For now, we don't have category on User entity, so we skip category filter or mock it
        // Let's assume all restaurants are shown for "All", and filter by name only for now
        return matchSearch;
    });

    return (
        <div className="restaurant-list-page">
            <Banner images={bannerImages} />

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

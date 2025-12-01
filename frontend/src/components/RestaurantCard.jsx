import React from 'react';
import { Link } from 'react-router-dom';
import './RestaurantCard.css'; // We'll create this CSS next

const RestaurantCard = ({ restaurant }) => {
    return (
        <Link to={`/restaurant/${restaurant.id}`} className="restaurant-card-link">
            <div className="restaurant-card">
                <div className="restaurant-image-container">
                    <img
                        src={restaurant.image || restaurant.img || '/Images/Logo.png'}
                        alt={restaurant.name}
                        className="restaurant-image"
                        onError={(e) => { e.target.src = '/Images/Logo.png'; }}
                    />
                    {restaurant.discount && (
                        <div className="restaurant-discount-badge">
                            {restaurant.discount}
                        </div>
                    )}
                </div>
                <div className="restaurant-info">
                    <h3 className="restaurant-name">{restaurant.name}</h3>
                    <p className="restaurant-address">{restaurant.address}</p>
                    <div className="restaurant-meta">
                        <span className="restaurant-rating">⭐ {restaurant.rating}</span>
                        <span className="dot">•</span>
                        <span className="restaurant-time">{restaurant.deliveryTime}</span>
                        <span className="dot">•</span>
                        <span className="restaurant-distance">{restaurant.distance}</span>
                    </div>
                </div>
            </div>
        </Link>
    );
};

export default RestaurantCard;

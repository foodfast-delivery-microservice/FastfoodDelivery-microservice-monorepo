// src/components/Product.jsx
import React from "react";
import { Link } from "react-router-dom";
import './ProductCard.css';

function Product({ product, onAdd }) {
    if (!product) return null; // Tránh lỗi render khi dữ liệu chưa sẵn sàng

    const {
        id,
        name = "Sản phẩm chưa đặt tên",
        price = 0,
        imageUrl,
        img,
        image,
        restaurant,
        restaurantName,
    } = product;

    const toAbsoluteUrl = (src) => {
        if (!src) return null;
        if (src.startsWith("http")) return src;
        const base = "http://localhost:8089";
        return src.startsWith("/") ? `${base}${src}` : `${base}/${src}`;
    };

    const displayImage =
        toAbsoluteUrl(imageUrl) ||
        toAbsoluteUrl(image) ||
        toAbsoluteUrl(img) ||
        "/Images/Logo.png";

    // Ưu tiên dùng restaurant -> restaurantName -> fallback
    const displayRestaurant = restaurant || restaurantName || "Không rõ nhà hàng";

    const displayPrice =
        typeof price === "number"
            ? price.toLocaleString("vi-VN")
            : Number(price || 0).toLocaleString("vi-VN");

    return (
        <div className="prd-card">
            <Link to={`/product-detail/${id}`} className="prd-link">
                <img src={displayImage} alt={name} loading="lazy" className="prd-img" />
                <div className="prd-info">
                    <h3 className="prd-name">{name}</h3>
                    <p className="prd-price">{displayPrice} ₫</p>
                    <p className="prd-restaurant">🏠 {displayRestaurant}</p>
                </div>
            </Link>
            <div className="prd-actions">
                <button
                    className="prd-add-btn"
                    onClick={() => {
                        if (onAdd && typeof onAdd === 'function') {
                            onAdd({
                                ...product,
                                restaurantId: product.restaurantId || product.merchantId,
                                // Lưu tên nhà hàng chuẩn vào cart
                                restaurantName: displayRestaurant,
                            });
                        } else {
                            console.warn('onAdd function is not provided');
                        }
                    }}
                >
                    🛒 Thêm vào giỏ
                </button>
            </div>
        </div>
    );
}

export default Product;

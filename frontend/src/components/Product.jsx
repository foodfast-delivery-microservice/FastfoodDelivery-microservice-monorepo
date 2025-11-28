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
        img = "https://via.placeholder.com/150",
        restaurant = "Không rõ nhà hàng",
    } = product;

    const displayPrice =
        typeof price === "number"
            ? price.toLocaleString("vi-VN")
            : Number(price || 0).toLocaleString("vi-VN");

    return (
        <div className="prd-card">
            <Link to={`/product-detail/${id}`} className="prd-link">
                <img src={img} alt={name} loading="lazy" className="prd-img" />
                <div className="prd-info">
                    <h3 className="prd-name">{name}</h3>
                    <p className="prd-price">{displayPrice} ₫</p>
                    <p className="prd-restaurant">🏠 {restaurant}</p>
                </div>
            </Link>
            <div className="prd-actions">
                <button
                    className="prd-add-btn"
                    onClick={() => onAdd(product)}
                >
                    🛒 Thêm vào giỏ
                </button>
            </div>
        </div>
    );
}

export default Product;

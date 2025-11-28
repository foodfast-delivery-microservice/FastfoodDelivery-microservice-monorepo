// src/components/ProductDetail.jsx
import { useParams, Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { fetchProductById, fetchProducts } from "../services/products";
import { fetchRestaurantById } from "../services/restaurants";
import './ProductDetail.css';

function ProductDetail({ onAdd }) {
    const { id } = useParams();
    const [product, setProduct] = useState(null);
    const [restaurant, setRestaurant] = useState(null);
    const [relatedProducts, setRelatedProducts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchProductDetail = async () => {
            setLoading(true);
            try {
                // Fetch product from backend
                const productData = await fetchProductById(id);

                if (productData) {
                    setProduct(productData);

                    // Fetch restaurant details if merchantId exists
                    if (productData.merchantId) {
                        try {
                            const restaurantData = await fetchRestaurantById(productData.merchantId);
                            setRestaurant(restaurantData);
                        } catch (resErr) {
                            console.warn("Could not fetch restaurant details:", resErr);
                            setRestaurant({ name: productData.restaurantName || "Đối tác" });
                        }
                    }
                } else {
                    setProduct(null);
                }
            } catch (err) {
                console.error("Lỗi khi fetch chi tiết sản phẩm:", err);
                setProduct(null);
            } finally {
                setLoading(false);
            }
        };
        fetchProductDetail();
    }, [id]);

    useEffect(() => {
        const fetchRelatedProducts = async () => {
            if (product && product.category) {
                try {
                    // Fetch all products and filter client-side (or implement category filter in API)
                    const allProducts = await fetchProducts();

                    const relatedList = allProducts
                        .filter(p => p.category === product.category && String(p.id) !== String(id))
                        .slice(0, 4);

                    setRelatedProducts(relatedList);
                } catch (err) {
                    console.error("Lỗi khi fetch sản phẩm gợi ý:", err);
                }
            }
        };
        fetchRelatedProducts();
    }, [product, id]);

    if (loading) return <p className="productDetail__loading">⏳ Đang tải sản phẩm...</p>;
    if (!product) return <p className="productDetail__loading">Không tìm thấy sản phẩm.</p>;

    const discountedPrice = product.discount
        ? Math.round(product.price * (1 - product.discount / 100))
        : product.price;

    return (
        <div className="productDetail">
            <div className="productDetail__container">
                <div className="productDetail__image">
                    <img src={product.img} alt={product.name} />

                </div>

                <div className="productDetail__info">
                    <div className="productDetail__heading">
                        <h2 className="productDetail__name">{product.name}</h2>
                        <div className="productDetail__rating">
                            <span className="stars">⭐ {product.rating || 4.5}</span>
                            <span className="reviews">({product.reviews || 100} đánh giá)</span>
                        </div>
                    </div>

                    <div className="productDetail__price">
                        {product.discount > 0 ? (
                            <>
                                <p className="price--discounted">{discountedPrice.toLocaleString()}₫</p>
                                <p className="price--original">{product.price.toLocaleString()}₫</p>
                                <span className="price--badge">-{product.discount}%</span>
                            </>
                        ) : (
                            <p className="price--discounted">{product.price.toLocaleString()}₫</p>
                        )}
                    </div>



                    {restaurant && (
                        <div className="productDetail__restaurantCard">
                            <div>

                                <h4>{restaurant.name}</h4>
                                {restaurant.address && <p className="restaurant-address">{restaurant.address}</p>}
                            </div>

                        </div>
                    )}

                    <p className="productDetail__desc">{product.description}</p>

                    {product.ingredients && product.ingredients.length > 0 && (
                        <div className="productDetail__ingredients">
                            <h4>Nguyên liệu nổi bật</h4>
                            <ul>
                                {product.ingredients.map((item, i) => (
                                    <li key={i}>{item}</li>
                                ))}
                            </ul>
                        </div>
                    )}

                    <div className="productDetail__actions">
                        <button
                            className="productDetail__addBtn"
                            onClick={() =>
                                onAdd({
                                    ...product,
                                    restaurantId: product.merchantId || null,
                                    restaurantName: restaurant?.name || product.restaurantName || "Chưa xác định",
                                })
                            }
                        >
                            🛒 Thêm vào giỏ hàng
                        </button>

                    </div>

                </div>
            </div>

            <div className="relatedProducts">
                <h3>Gợi ý cho bạn</h3>
                <div className="relatedProducts__grid">
                    {relatedProducts.length > 0 ? (
                        relatedProducts.map((item) => (
                            <Link
                                key={item.id}
                                to={`/product-detail/${item.id}`}
                                className="relatedProducts__link"
                            >
                                <div className="relatedProducts__item">
                                    <img src={item.img} alt={item.name} />
                                    <h4>{item.name}</h4>
                                    <p>{item.price.toLocaleString()}₫</p>
                                    <button
                                        className="relatedProducts__addBtn"
                                        onClick={(e) => {
                                            e.preventDefault();
                                            onAdd({
                                                ...item,
                                                restaurantId: item.merchantId || null,
                                                restaurantName: "N/A"
                                            });
                                        }}
                                    >
                                        🛒 Thêm
                                    </button>
                                </div>
                            </Link>
                        ))
                    ) : (
                        <p>Không có sản phẩm tương tự.</p>
                    )}
                </div>
            </div>
        </div>
    );
}

export default ProductDetail;

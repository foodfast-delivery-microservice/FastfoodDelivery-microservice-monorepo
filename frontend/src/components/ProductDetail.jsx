// src/components/ProductDetail.jsx
import { useParams, Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { fetchProductById, fetchProducts } from "../services/products";
import { fetchRestaurantByMerchantId } from "../services/restaurants";
import "./ProductDetail.css";

function ProductDetail({ onAdd }) {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [relatedProducts, setRelatedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [restaurant, setRestaurant] = useState(null);

  // Fetch chi tiết sản phẩm từ BE
  useEffect(() => {
    const fetchProductDetail = async () => {
      setLoading(true);
      setError(null);
      try {
        const productData = await fetchProductById(id);
        setProduct(productData || null);
      } catch (err) {
        console.error("Lỗi khi fetch chi tiết sản phẩm:", err);
        setProduct(null);
        setError("Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.");
      } finally {
        setLoading(false);
      }
    };

    fetchProductDetail();
  }, [id]);

  // Lấy thông tin nhà hàng theo merchantId
  useEffect(() => {
    const loadRestaurant = async () => {
      if (!product?.merchantId) return;

      try {
        const res = await fetchRestaurantByMerchantId(product.merchantId);
        setRestaurant(res || null);
      } catch (err) {
        console.warn("Không thể tải thông tin nhà hàng cho merchant", err);
        setRestaurant(null);
      }
    };

    loadRestaurant();
  }, [product?.merchantId]);

  // Gợi ý sản phẩm cùng category (nếu BE đã có)
  useEffect(() => {
    const fetchRelatedProducts = async () => {
      if (!product?.category) return;

      try {
        const allProducts = await fetchProducts();
        const relatedList = allProducts
          .filter(
            (p) =>
              p.category === product.category && String(p.id) !== String(id)
          )
          .slice(0, 4);

        setRelatedProducts(relatedList);
      } catch (err) {
        console.error("Lỗi khi fetch sản phẩm gợi ý:", err);
      }
    };

    fetchRelatedProducts();
  }, [product, id]);

  if (loading) {
    return (
      <p className="productDetail__loading">⏳ Đang tải thông tin sản phẩm...</p>
    );
  }

  if (!product) {
    return (
      <p className="productDetail__loading">
        {error || "Không tìm thấy sản phẩm."}
      </p>
    );
  }

  // Map dữ liệu theo field BE hiện có
  const {
    name,
    description,
    price,
    stock,
    category,
    active,
    merchantId,
    imageUrl,
  } = product;

  const isInactive = active === false;
  const outOfStock = typeof stock === "number" && stock <= 0;
  const maxQuantity =
    typeof stock === "number" && stock > 0 ? Math.floor(stock) : 99;

  const displayImage = imageUrl || product.img;

  const canAddToCart = !isInactive && !outOfStock && maxQuantity > 0;

  const totalPrice = (price || 0) * (quantity || 1);

  const handleDecrease = () => {
    setQuantity((prev) => Math.max(1, prev - 1));
  };

  const handleIncrease = () => {
    setQuantity((prev) => {
      const next = prev + 1;
      return next > maxQuantity ? maxQuantity : next;
    });
  };

  const handleAddToCart = () => {
    if (!canAddToCart) return;

    const baseProductForCart = {
      ...product,
      img: displayImage || product.img,
      restaurantId: merchantId || product.merchantId || null,
      restaurantName:
        restaurant?.name || product.restaurantName || "Đối tác",
    };

    // Gửi số lượng lựa chọn sang App (App sẽ cộng dồn hoặc set 1 tuỳ logic)
    for (let i = 0; i < quantity; i += 1) {
      onAdd(baseProductForCart);
    }
  };

  return (
    <div className="productDetail">
      <div className="productDetail__container">
        <div className="productDetail__image">
          {displayImage ? (
            <img src={displayImage} alt={name} />
          ) : (
            <div className="productDetail__imageFallback">No Image</div>
          )}
        </div>

        <div className="productDetail__info">
          <div className="productDetail__heading">
            <h2 className="productDetail__name">{name}</h2>

            <div className="productDetail__meta">
              {category && (
                <span className="productDetail__categoryTag">
                  {category === "FOOD"
                    ? "Món ăn"
                    : category === "DRINK"
                    ? "Đồ uống"
                    : category}
                </span>
              )}

              {typeof stock === "number" && (
                <span className="productDetail__stock">
                  {outOfStock
                    ? "Tạm hết hàng"
                    : stock <= 10
                    ? `Sắp hết • Còn ${stock} phần`
                    : `Còn ${stock} phần`}
                </span>
              )}

              {isInactive && (
                <span className="productDetail__badge productDetail__badge--inactive">
                  Tạm ngưng bán
                </span>
              )}
            </div>
          </div>

          <div className="productDetail__price">
            <p className="price--discounted">
              {Number(price || 0).toLocaleString()}₫
            </p>
          </div>

          <p className="productDetail__desc">{description}</p>

          <div className="productDetail__actions">
            <div className="productDetail__qtyRow">
              <div className="productDetail__qtyControl">
                <button
                  type="button"
                  onClick={handleDecrease}
                  disabled={!canAddToCart || quantity <= 1}
                >
                  -
                </button>
                <span>{quantity}</span>
                <button
                  type="button"
                  onClick={handleIncrease}
                  disabled={!canAddToCart || quantity >= maxQuantity}
                >
                  +
                </button>
              </div>

              <div className="productDetail__total">
                <span>Tạm tính</span>
                <strong>{totalPrice.toLocaleString()}₫</strong>
              </div>
            </div>

            <button
              type="button"
              className="productDetail__addBtn"
              onClick={handleAddToCart}
              disabled={!canAddToCart}
            >
              {outOfStock
                ? "Hết hàng"
                : isInactive
                ? "Tạm ngưng bán"
                : "🛒 Thêm vào giỏ hàng"}
            </button>

            {merchantId && (
              <p className="productDetail__merchantHint">
                {restaurant?.name
                  ? `Nhà hàng: ${restaurant.name}`
                  : product.restaurantName
                  ? `Nhà hàng: ${product.restaurantName}`
                  : "Món thuộc đối tác"}
              </p>
            )}
          </div>
        </div>
      </div>

      <div className="relatedProducts">
        <h3>Gợi ý cho bạn</h3>
        <div className="relatedProducts__grid">
          {relatedProducts.length > 0 ? (
            relatedProducts.map((item) => {
              const itemImage = item.imageUrl || item.img;
              return (
                <Link
                  key={item.id}
                  to={`/product-detail/${item.id}`}
                  className="relatedProducts__link"
                >
                  <div className="relatedProducts__item">
                    {itemImage ? (
                      <img src={itemImage} alt={item.name} />
                    ) : (
                      <div className="relatedProducts__imageFallback">
                        No Image
                      </div>
                    )}
                    <h4>{item.name}</h4>
                    <p>{Number(item.price || 0).toLocaleString()}₫</p>
                    <button
                      type="button"
                      className="relatedProducts__addBtn"
                      onClick={(e) => {
                        e.preventDefault();
                        onAdd({
                          ...item,
                          img: itemImage,
                          restaurantId: item.merchantId || null,
                          restaurantName: "Đối tác",
                        });
                      }}
                    >
                      🛒 Thêm
                    </button>
                  </div>
                </Link>
              );
            })
          ) : (
            <p>Không có sản phẩm tương tự.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default ProductDetail;

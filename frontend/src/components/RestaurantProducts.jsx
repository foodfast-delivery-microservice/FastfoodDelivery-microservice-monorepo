import React, { useCallback, useEffect, useMemo, useState } from "react";
import http from "../services/http";
import { useAuth } from "../context/AuthContext";
import "./RestaurantProducts.css";

export default function RestaurantProducts() {
  const { currentUser } = useAuth();
  const role = (currentUser?.role || "").toLowerCase();
  const [products, setProducts] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);

  const [categoryFilter, setCategoryFilter] = useState("all");
  const [editingProduct, setEditingProduct] = useState(null);
  const [showForm, setShowForm] = useState(false);

  // Helper để unwrap ApiResponse
  const unwrapData = (responseData) => {
    // Nếu là ApiResponse wrapper: { status, message, data: T }
    if (responseData?.data !== undefined && responseData?.status !== undefined) {
      return responseData.data
    }
    // Nếu trả về trực tiếp
    return responseData
  }

  // ✅ Lấy sản phẩm
  const fetchProducts = useCallback(async () => {
    try {
      setLoading(true);
      const endpoint = role === "admin" ? "/products" : "/products/merchants/me";
      console.log(`🔄 [RestaurantProducts] Fetching products from: ${endpoint}, role: ${role}`);
      
      const res = await http.get(endpoint);
      console.log("📦 [RestaurantProducts] Raw response:", res.data);

      // Unwrap ApiResponse format
      const unwrapped = unwrapData(res.data);
      console.log("📦 [RestaurantProducts] Unwrapped data:", unwrapped);
      
      // Backend có thể trả về array hoặc Page object
      let products = [];
      if (Array.isArray(unwrapped)) {
        products = unwrapped;
      } else if (unwrapped?.content) {
        products = unwrapped.content;
      } else if (unwrapped) {
        products = [unwrapped]; // Single product
      }
      
      console.log(`✅ [RestaurantProducts] Loaded ${products.length} products`);
      setProducts(products);
    } catch (err) {
      console.error("❌ [RestaurantProducts] Lỗi lấy sản phẩm:", err);
      console.error("❌ [RestaurantProducts] Error details:", {
        message: err.message,
        status: err.response?.status,
        statusText: err.response?.statusText,
        data: err.response?.data
      });
      setProducts([]); // Set empty array on error
    } finally {
      setLoading(false);
    }
  }, [role]);

  // ✅ Lấy danh sách nhà hàng (chỉ admin cần)
  const fetchRestaurants = useCallback(async () => {
    if (role !== "admin") return;
    try {
      const res = await http.get("/restaurants", { params: { size: 100 } });
      const unwrapped = unwrapData(res.data);
      
      // Backend trả về Page object hoặc array
      let restaurants = [];
      if (Array.isArray(unwrapped)) {
        restaurants = unwrapped;
      } else if (unwrapped?.content) {
        restaurants = unwrapped.content;
      }
      
      setRestaurants(restaurants);
    } catch (err) {
      console.error("❌ Lỗi lấy nhà hàng:", err);
      console.error("Response error:", err.response?.data || err.message);
      setRestaurants([]);
    }
  }, [role]);

  useEffect(() => {
    fetchProducts();
    fetchRestaurants();
  }, [fetchProducts, fetchRestaurants]);

  // 🧠 Lấy danh mục duy nhất
  const categories = useMemo(() => {
    const unique = [...new Set(products.map((p) => p.category))];
    return unique.filter(Boolean);
  }, [products]);

  // 🔍 Lọc theo danh mục
  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      return categoryFilter === "all" || p.category === categoryFilter;
    });
  }, [products, categoryFilter]);

  // 🗑️ Xóa sản phẩm
  const handleDelete = async (id) => {
    if (!window.confirm("Bạn có chắc muốn xóa sản phẩm này không?")) return;
    try {
      await http.delete(`/products/${id}`);
      setProducts((prev) => prev.filter((p) => p.id !== id));
      alert("🗑️ Đã xóa sản phẩm!");
    } catch (err) {
      console.error("❌ Lỗi xóa:", err);
      alert("Xóa sản phẩm thất bại");
    }
  };

  // 💾 Thêm / sửa sản phẩm
  const handleSave = async (e) => {
    e.preventDefault();

    const selectedMerchantId =
      role === "admin"
        ? e.target.restaurantId.value
        : currentUser?.id; // Merchant ID is User ID

    const productData = {
      name: e.target.name.value,
      price: Number(e.target.price.value),
      image: e.target.img.value,
      category: e.target.category.value,
      description: e.target.description.value,
      merchantId: selectedMerchantId,
    };

    try {
      if (editingProduct) {
        await http.put(`/products/${editingProduct.id}`, productData);
      } else {
        await http.post("/products", productData);
      }

      setShowForm(false);
      setEditingProduct(null);
      fetchProducts();
      alert("✅ Lưu sản phẩm thành công!");
    } catch (err) {
      console.error("❌ Lỗi lưu:", err);
      alert("Lỗi lưu sản phẩm");
    }
  };

  if (loading) return <p className="rsp-loading">⏳ Đang tải sản phẩm...</p>;

  return (
    <div className="rsp-container">
      <div className="rsp-header">
        <h2>🍽️ Quản lý sản phẩm</h2>
        <button
          className="rsp-btn-add"
          onClick={() => {
            setEditingProduct(null);
            setShowForm(true);
          }}
        >
          ➕ Thêm sản phẩm
        </button>
      </div>

      {/* 🔥 FILTER BAR */}
      <div className="filter-bar">
        <div className="filter-item">
          <label>Danh mục</label>
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            <option value="all">Tất cả</option>
            {categories.map((c, i) => (
              <option key={i} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="table-meta">
        <span>
          Hiển thị: <b>{filteredProducts.length}</b> / {products.length} sản phẩm
        </span>
      </div>

      {filteredProducts.length === 0 ? (
        <p className="rsp-empty">Không có sản phẩm nào phù hợp.</p>
      ) : (
        <table className="rsp-table">
          <thead>
            <tr>
              <th>Hình</th>
              <th>Tên sản phẩm</th>
              <th>Danh mục</th>
              <th>Giá</th>
              {role === "admin" && <th>Nhà hàng</th>}
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.map((p) => (
              <tr key={p.id}>
                <td>
                  <img src={p.image} alt={p.name} className="rsp-img" onError={(e) => e.target.src = 'https://via.placeholder.com/50'} />
                </td>
                <td>{p.name}</td>
                <td>{p.category}</td>
                <td>{p.price?.toLocaleString()}₫</td>
                {role === "admin" && (
                  <td>{p.merchantId || "Không xác định"}</td>
                )}
                <td>
                  <button
                    className="rsp-btn-edit"
                    onClick={() => {
                      setEditingProduct({
                        ...p,
                        img: p.image,
                        restaurantId: p.merchantId
                      });
                      setShowForm(true);
                    }}
                  >
                    ✏️ Sửa
                  </button>
                  <button
                    className="rsp-btn-delete"
                    onClick={() => handleDelete(p.id)}
                  >
                    ❌ Xóa
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {showForm && (
        <div
          className="rsp-modal-overlay"
          onClick={(e) => {
            if (e.target.classList.contains("rsp-modal-overlay")) {
              setShowForm(false);
            }
          }}
        >
          <div className="rsp-modal-content">
            <button
              className="rsp-close"
              onClick={() => setShowForm(false)}
            >
              ✖
            </button>

            <form className="rsp-form" onSubmit={handleSave}>
              <h3>{editingProduct ? "✏️ Sửa sản phẩm" : "➕ Thêm sản phẩm"}</h3>

              <label>Tên sản phẩm</label>
              <input
                name="name"
                placeholder="Tên sản phẩm"
                defaultValue={editingProduct?.name || ""}
                required
              />

              <label>Giá</label>
              <input
                name="price"
                type="number"
                placeholder="Giá"
                defaultValue={editingProduct?.price || ""}
                required
              />

              <label>Link ảnh</label>
              <input
                name="img"
                placeholder="Link ảnh"
                defaultValue={editingProduct?.img || ""}
              />

              <label>Danh mục</label>
              <input
                name="category"
                placeholder="VD: Món chính, Nước uống..."
                defaultValue={editingProduct?.category || ""}
              />

              <label>Mô tả</label>
              <textarea
                name="description"
                placeholder="Mô tả sản phẩm"
                defaultValue={editingProduct?.description || ""}
              />

              {role === "admin" && (
                <>
                  <label>Nhà hàng</label>
                  <select
                    name="restaurantId"
                    defaultValue={editingProduct?.restaurantId || ""}
                  >
                    <option value="">-- Chọn nhà hàng --</option>
                    {restaurants.map((r) => (
                      <option key={r.id} value={r.merchantId}>
                        {r.name}
                      </option>
                    ))}
                  </select>
                </>
              )}

              <div className="rsp-form-actions">
                <button type="submit" className="rsp-btn-save">
                  💾 Lưu
                </button>
                <button
                  type="button"
                  className="rsp-btn-cancel"
                  onClick={() => setShowForm(false)}
                >
                  ❌ Hủy
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}

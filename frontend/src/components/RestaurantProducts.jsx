import React, { useCallback, useEffect, useMemo, useState } from "react";
import http from "../services/http";
import { useAuth } from "../context/AuthContext";
import "./RestaurantProducts.css";

const buildImageUrl = (src) => {
  if (!src) return null;
  if (src.startsWith?.("http")) return src;
  const base = "http://localhost:8080";
  return src.startsWith("/") ? `${base}${src}` : `${base}/${src}`;
};

export default function RestaurantProducts() {
  const { currentUser } = useAuth();
  const role = (currentUser?.role || "").toLowerCase();
  const [products, setProducts] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);

  const [categoryFilter, setCategoryFilter] = useState("all");
  const [editingProduct, setEditingProduct] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);

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

      const config =
        role === "admin"
          ? {}
          : { params: { includeInactive: true } }; // Merchant view: lấy cả active + inactive

      const res = await http.get(endpoint, config);
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
    if (!window.confirm("Bạn có chắc muốn xóa sản phẩm này không?\n\nLưu ý: Sản phẩm sẽ được vô hiệu hóa trước khi xóa.")) return;
    try {
      // First, deactivate the product (set active = false)
      const product = products.find(p => p.id === id);
      if (product && product.active !== false) {
        try {
          await http.put(`/products/${id}`, { active: false });
          console.log("⏳ Đã vô hiệu hóa sản phẩm, đang xóa...");
        } catch (deactivateErr) {
          console.error("Lỗi khi vô hiệu hóa sản phẩm:", deactivateErr);
          // Continue with delete attempt anyway
        }
      }

      // Then delete the product
      await http.delete(`/products/${id}`);
      setProducts((prev) => prev.filter((p) => p.id !== id));
      alert("🗑️ Đã xóa sản phẩm!");
    } catch (err) {
      console.error("❌ Lỗi xóa:", err);
      const errorMessage = err?.response?.data?.message || err?.response?.data?.error || err?.message || "Xóa sản phẩm thất bại";
      
      // Check if error is about product needing to be inactive
      if (errorMessage.includes("inactive") || errorMessage.includes("active")) {
        alert("⚠️ Sản phẩm cần được vô hiệu hóa trước khi xóa. Đang thử lại...");
        // Retry: deactivate then delete
        try {
          await http.put(`/products/${id}`, { active: false });
          await http.delete(`/products/${id}`);
          setProducts((prev) => prev.filter((p) => p.id !== id));
          alert("🗑️ Đã xóa sản phẩm!");
          return;
        } catch (retryErr) {
          console.error("Lỗi khi retry delete:", retryErr);
        }
      }
      
      alert(`❌ ${errorMessage}`);
    }
  };

  const handleToggleActive = async (product) => {
    try {
      await http.put(`/products/${product.id}`, { active: !product.active });
      alert(product.active ? "Đã tạm ẩn sản phẩm" : "Đã mở bán sản phẩm");
      fetchProducts();
    } catch (err) {
      console.error("❌ Lỗi đổi trạng thái:", err);
      alert("Không thể đổi trạng thái sản phẩm");
    }
  };

  // Upload image file
  const handleImageUpload = async (file) => {
    if (!file) return null;
    
    setUploadingImage(true);
    try {
      const formData = new FormData();
      formData.append("file", file);
      
      const response = await http.post("/upload/image", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      
      const imageUrl = response?.data?.data || response?.data;
      if (imageUrl) {
        // Backend returns relative URL like "/api/v1/files/{filename}"
        // Keep it as relative URL for database storage (better for production)
        console.log("📸 Uploaded image URL:", imageUrl);
        alert("✅ Upload ảnh thành công!");
        return imageUrl; // Return relative URL, not full URL
      }
      return null;
    } catch (err) {
      console.error("Lỗi upload ảnh:", err);
      const errorMsg = err?.response?.data?.message || "Upload ảnh thất bại";
      alert(`❌ ${errorMsg}`);
      return null;
    } finally {
      setUploadingImage(false);
    }
  };

  // 💾 Thêm / sửa sản phẩm
  const handleSave = async (e) => {
    e.preventDefault();

    const selectedMerchantId =
      role === "admin"
        ? e.target.restaurantId.value
        : currentUser?.id; // Merchant ID is User ID

    // Validate category
    const category = e.target.category.value;
    if (!category || (category !== "DRINK" && category !== "FOOD")) {
      alert("⚠️ Vui lòng chọn danh mục hợp lệ (Đồ uống hoặc Đồ ăn)!");
      return;
    }

    const activeValue = e.target.active?.value === "true";

    const productData = {
      name: e.target.name.value.trim(),
      price: Number(e.target.price.value),
      stock: Number(e.target.stock?.value || 0),
      imageUrl: (e.target.img.value || "").trim() || "",
      category: category, // DRINK or FOOD
      description: (e.target.description.value || "").trim() || "",
      merchantId: selectedMerchantId,
      active: activeValue,
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
              <th>Tồn kho</th>
              {role === "admin" && <th>Nhà hàng</th>}
              <th>Trạng thái</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.map((p) => (
              <tr key={p.id}>
                <td>
                  {p.imageUrl || p.image ? (
                    <img 
                      src={buildImageUrl(p.imageUrl || p.image)} 
                      alt={p.name} 
                      className="rsp-img" 
                      onError={(e) => { e.target.src = 'https://via.placeholder.com/50'; }} 
                    />
                  ) : (
                    <div className="rsp-img-placeholder" style={{ width: 50, height: 50, backgroundColor: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 4 }}>📷</div>
                  )}
                </td>
                <td>{p.name}</td>
                <td>{p.category === "DRINK" ? "Đồ uống" : p.category === "FOOD" ? "Đồ ăn" : p.category || "—"}</td>
                <td>{p.price?.toLocaleString("vi-VN")}₫</td>
                <td>{p.stock != null ? p.stock.toLocaleString("vi-VN") : "0"}</td>
                {role === "admin" && (
                  <td>{p.merchantId || "Không xác định"}</td>
                )}
                <td>
                  <span className={`rsp-status ${p.active ? "active" : "inactive"}`}>
                    {p.active ? "Đang bán" : "Tạm ẩn"}
                  </span>
                </td>
                <td>
                  <button
                    className="rsp-btn-edit"
                    onClick={() => {
                      setEditingProduct({
                        ...p,
                        img: p.imageUrl || p.image || "",
                        stock: p.stock || 0,
                        restaurantId: p.merchantId,
                        active: p.active ?? true,
                      });
                      setShowForm(true);
                    }}
                  >
                    ✏️ Sửa
                  </button>
                  <button
                    className="rsp-btn-toggle"
                    onClick={() => handleToggleActive(p)}
                  >
                    {p.active ? "Ẩn" : "Mở"}
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
                min="0"
              />

              <label>Số lượng tồn kho</label>
              <input
                name="stock"
                type="number"
                placeholder="Số lượng"
                defaultValue={editingProduct?.stock || 0}
                required
                min="0"
              />

              <label>Trạng thái</label>
              <select
                name="active"
                defaultValue={editingProduct?.active ? "true" : "false"}
              >
                <option value="true">Đang bán</option>
                <option value="false">Tạm ẩn</option>
              </select>

              <label>Hình ảnh</label>
              <input
                type="file"
                accept="image/*"
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    const uploadedUrl = await handleImageUpload(file);
                    if (uploadedUrl) {
                      // Update the form input value by finding the img input and setting its value
                      const imgInput = e.target.form.querySelector('input[name="img"]');
                      if (imgInput) {
                        imgInput.value = uploadedUrl;
                        // Trigger input event to update preview if needed
                        imgInput.dispatchEvent(new Event('input', { bubbles: true }));
                      }
                    }
                  }
                }}
                disabled={uploadingImage}
                style={{ marginBottom: 8 }}
              />
              {uploadingImage && <p style={{ color: "#1890ff", margin: "4px 0" }}>⏳ Đang upload...</p>}
              <input
                name="img"
                id="img-input"
                placeholder="Hoặc nhập URL ảnh"
                defaultValue={editingProduct?.img || ""}
                disabled={uploadingImage}
                onChange={(e) => {
                  // Update preview when URL changes
                  const previewImg = e.target.form.querySelector('img[alt="Preview"]');
                  if (previewImg && e.target.value) {
                    previewImg.src = buildImageUrl(e.target.value);
                  }
                }}
              />
              {editingProduct?.img && (
                <img
                  src={buildImageUrl(editingProduct.img)}
                  alt="Preview"
                  style={{ maxWidth: 200, maxHeight: 200, marginTop: 8, borderRadius: 4 }}
                  onError={(e) => { e.target.style.display = "none"; }}
                />
              )}

              <label>Danh mục</label>
              <select
                name="category"
                defaultValue={editingProduct?.category || ""}
                required
              >
                <option value="">-- Chọn danh mục --</option>
                <option value="DRINK">Đồ uống</option>
                <option value="FOOD">Đồ ăn</option>
              </select>

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

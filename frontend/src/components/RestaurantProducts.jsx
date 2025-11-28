import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  collection,
  getDocs,
  addDoc,
  updateDoc,
  deleteDoc,
  doc,
} from "firebase/firestore";
import { db } from "../firebase";
import { useAuth } from "../context/AuthContext"; // ✅ Dùng AuthContext
import "./RestaurantProducts.css";

export default function RestaurantProducts() {
  const { currentUser } = useAuth(); // ✅ user hiện tại
  const role = (currentUser?.role || "").toLowerCase();
  const [products, setProducts] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);

  const [categoryFilter, setCategoryFilter] = useState("all");
  const [editingProduct, setEditingProduct] = useState(null);
  const [showForm, setShowForm] = useState(false);

  // ✅ Lấy sản phẩm
  const fetchProducts = useCallback(async () => {
    try {
      const snap = await getDocs(collection(db, "products"));
      const data = snap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      // ✅ lọc theo nhà hàng nếu không phải admin
      const filteredData =
        role === "admin"
          ? data
          : data.filter((p) => p.restaurantId === currentUser?.restaurantId);

      setProducts(filteredData);
    } catch (err) {
      console.error("❌ Lỗi lấy sản phẩm:", err);
    } finally {
      setLoading(false);
    }
  }, [currentUser?.restaurantId, role]);

  // ✅ Lấy danh sách nhà hàng (chỉ admin cần)
  const fetchRestaurants = useCallback(async () => {
    if (role !== "admin") return;
    try {
      const snap = await getDocs(collection(db, "restaurants"));
      const data = snap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));
      setRestaurants(data);
    } catch (err) {
      console.error("❌ Lỗi lấy nhà hàng:", err);
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
      await deleteDoc(doc(db, "products", id));
      setProducts((prev) => prev.filter((p) => p.id !== id));
      alert("🗑️ Đã xóa sản phẩm!");
    } catch (err) {
      console.error("❌ Lỗi xóa:", err);
    }
  };

  // 💾 Thêm / sửa sản phẩm
 const handleSave = async (e) => {
  e.preventDefault();

  const selectedRestaurantId =
    role === "admin"
      ? e.target.restaurantId.value
      : currentUser?.restaurantId;

  // ⭐ Lấy tên nhà hàng đúng field Firestore của bạn: name
  let restaurantName = "";
  if (role === "admin") {
    const restaurant = restaurants.find((r) => r.id === selectedRestaurantId);
    restaurantName = restaurant ? restaurant.name : "";
  } else {
    // Nhà hàng đăng nhập → đã có trong user: restaurantName hoặc name?
    restaurantName = currentUser?.restaurantName || currentUser?.name || "";
  }

  const productData = {
    name: e.target.name.value,
    price: Number(e.target.price.value),
    img: e.target.img.value,
    category: e.target.category.value,
    description: e.target.description.value,
    restaurantId: selectedRestaurantId,
    restaurant: restaurantName,   // ⭐⭐ LƯU ĐÚNG FIELD restaurant ⭐⭐
  };

  try {
    if (editingProduct) {
      await updateDoc(doc(db, "products", editingProduct.id), productData);
    } else {
      await addDoc(collection(db, "products"), productData);
    }

    setShowForm(false);
    setEditingProduct(null);
    fetchProducts();
    alert("✅ Lưu sản phẩm thành công!");
  } catch (err) {
    console.error("❌ Lỗi lưu:", err);
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
                  <img src={p.img} alt={p.name} className="rsp-img" />
                </td>
                <td>{p.name}</td>
                <td>{p.category}</td>
                <td>{p.price.toLocaleString()}₫</td>
                {role === "admin" && (
                  <td>{p.restaurantId || "Không xác định"}</td>
                )}
                <td>
                  <button
                    className="rsp-btn-edit"
                    onClick={() => {
                      setEditingProduct(p);
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
                <option key={r.id} value={r.id}>
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

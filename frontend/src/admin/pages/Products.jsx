import { useCallback, useEffect, useState } from "react";
import { Table, Input, Select, Slider, Modal, message, Spin } from "antd";
import http from "../../services/http";
import "./Products.css";

const buildImageUrl = (src) => {
  if (!src) return null;
  if (src.startsWith?.("http")) return src;
  const base = "http://localhost:8080";
  return src.startsWith("/") ? `${base}${src}` : `${base}/${src}`;
};

export default function AdminProducts() {
  const [data, setData] = useState([]);
  const [filteredData, setFilteredData] = useState([]);
  const [restaurantsList, setRestaurantsList] = useState([]);

  const [loading, setLoading] = useState(true);
  const [loadingRestaurants, setLoadingRestaurants] = useState(true);

  const [searchText, setSearchText] = useState("");
  const [category, setCategory] = useState("Tất cả");
  const [restaurantFilter, setRestaurantFilter] = useState("Tất cả");
  const [priceRange, setPriceRange] = useState([0, 1000000]);

  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [form, setForm] = useState({
    id: "",
    name: "",
    category: "",
    restaurantId: "", // This will store merchantId
    price: 0,
    stock: 0,
    img: "",
    description: "",
    active: true,
  });
  const [uploadingImage, setUploadingImage] = useState(false);

  const fetchProducts = useCallback(async () => {
    try {
      const res = await http.get("/products");
      const productsData = res.data?.data || [];
      setData(productsData);
      setFilteredData(productsData);
    } catch (err) {
      console.error("❌ Lỗi khi tải sản phẩm:", err);
      message.error("Không thể tải danh sách sản phẩm!");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchRestaurants = useCallback(async () => {
    try {
      const res = await http.get("/restaurants", { params: { size: 100 } });
      const restaurantData = res.data?.data?.content || [];
      console.log("🔥 Loaded restaurants:", restaurantData);
      setRestaurantsList(restaurantData);
    } catch (err) {
      console.error("❌ Lỗi khi tải nhà hàng:", err);
      message.error("Không thể tải danh sách nhà hàng!");
    } finally {
      setLoadingRestaurants(false);
    }
  }, []);

  // ===== FETCH DATA =====
  useEffect(() => {
    fetchProducts();
    fetchRestaurants();
  }, [fetchProducts, fetchRestaurants]);

  // ===== HELPER =====
  const getRestaurantName = useCallback((merchantId) => {
    const found = restaurantsList.find((r) => r.merchantId === merchantId);
    return found ? found.name : "Không rõ";
  }, [restaurantsList]);

  // ===== FILTER =====
  useEffect(() => {
    let filtered = data.filter((item) => {
      const matchName = item.name?.toLowerCase().includes(searchText.toLowerCase());
      const matchCategory = category === "Tất cả" || item.category === category;
      const matchRestaurant =
        restaurantFilter === "Tất cả" || getRestaurantName(item.merchantId) === restaurantFilter;
      const matchPrice = item.price >= priceRange[0] && item.price <= priceRange[1];
      return matchName && matchCategory && matchRestaurant && matchPrice;
    });
    setFilteredData(filtered);
  }, [searchText, category, restaurantFilter, priceRange, data, restaurantsList, getRestaurantName]);

  // ===== CRUD =====
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
        message.success("✅ Upload ảnh thành công!");
        return imageUrl; // Return relative URL, not full URL
      }
      return null;
    } catch (err) {
      console.error("Lỗi upload ảnh:", err);
      const errorMsg = err?.response?.data?.message || "Upload ảnh thất bại";
      message.error(`❌ ${errorMsg}`);
      return null;
    } finally {
      setUploadingImage(false);
    }
  };

  const handleAdd = async () => {
    console.log("📦 Dữ liệu form gửi:", form);

    // ✅ Kiểm tra dữ liệu đầu vào
    if (!form.name || !form.name.trim()) return message.warning("⚠️ Vui lòng nhập tên sản phẩm!");
    if (!form.restaurantId) return message.warning("⚠️ Vui lòng chọn nhà hàng!");
    if (form.price === "" || isNaN(Number(form.price)))
      return message.warning("⚠️ Vui lòng nhập giá hợp lệ!");
    if (form.stock === "" || isNaN(Number(form.stock)) || Number(form.stock) < 0)
      return message.warning("⚠️ Vui lòng nhập số lượng tồn kho hợp lệ!");

    try {
      const restaurant = restaurantsList.find((r) => r.merchantId === form.restaurantId);
      if (!restaurant) {
        message.error("❌ Nhà hàng không hợp lệ!");
        return;
      }

      // Validate category
      if (!form.category || (form.category !== "DRINK" && form.category !== "FOOD")) {
        return message.warning("⚠️ Vui lòng chọn danh mục hợp lệ (Đồ uống hoặc Đồ ăn)!");
      }

      const productData = {
        name: (form.name || "").trim(),
        category: form.category, // DRINK or FOOD
        merchantId: form.restaurantId, // Using merchantId
        price: Number(form.price),
        stock: Number(form.stock) || 0,
        imageUrl: (form.img || "").trim() || "",
        description: (form.description || "").trim() || "",
        active: form.active,
      };

      console.log("🚀 Gửi lên API:", productData);

      await http.post("/products", productData);
      message.success(`✅ Đã thêm sản phẩm "${form.name}" cho ${restaurant.name}!`);

      // Reset form
      setShowAddModal(false);
      setForm({
        id: "",
        name: "",
        category: "",
        restaurantId: "",
        price: 0,
        stock: 0,
        img: "",
        description: "",
        active: true,
      });

      fetchProducts();
    } catch (err) {
      console.error("🔥 Lỗi khi thêm sản phẩm:", err);
      message.error("❌ Có lỗi xảy ra khi thêm sản phẩm!");
    }
  };


  const handleDelete = async (id) => {
    if (!window.confirm("Bạn có chắc muốn xóa sản phẩm này không?\n\nLưu ý: Sản phẩm sẽ được vô hiệu hóa trước khi xóa.")) return;
    try {
      // First, deactivate the product (set active = false)
      const product = data.find(p => p.id === id);
      if (product && product.active !== false) {
        try {
          await http.put(`/products/${id}`, { active: false });
          message.info("⏳ Đã vô hiệu hóa sản phẩm, đang xóa...");
        } catch (deactivateErr) {
          console.error("Lỗi khi vô hiệu hóa sản phẩm:", deactivateErr);
          // Continue with delete attempt anyway
        }
      }

      // Then delete the product
      const response = await http.delete(`/products/${id}`);
      console.log("✅ Delete response:", response?.data);
      message.success("🗑️ Đã xóa sản phẩm!");
      fetchProducts();
    } catch (err) {
      console.error("🔥 Lỗi xóa sản phẩm:", err);
      const errorMessage = err?.response?.data?.message || err?.response?.data?.error || err?.message || "Xóa sản phẩm thất bại";
      const errorCode = err?.response?.data?.errorCode;
      
      // Check if error is about product needing to be inactive
      if (errorMessage.includes("inactive") || errorMessage.includes("active")) {
        message.warning("⚠️ Sản phẩm cần được vô hiệu hóa trước khi xóa. Đang thử lại...");
        // Retry: deactivate then delete
        try {
          await http.put(`/products/${id}`, { active: false });
          await http.delete(`/products/${id}`);
          message.success("🗑️ Đã xóa sản phẩm!");
          fetchProducts();
          return;
        } catch (retryErr) {
          console.error("Lỗi khi retry delete:", retryErr);
        }
      }
      
      console.error("Error details:", {
        status: err?.response?.status,
        statusText: err?.response?.statusText,
        data: err?.response?.data,
        errorCode
      });
      message.error(`❌ ${errorMessage}${errorCode ? ` (${errorCode})` : ''}`);
    }
  };

  const handleEdit = async () => {
    try {
      const updateData = {
        name: form.name || "",
        price: Number(form.price) || 0,
        stock: Number(form.stock) || 0,
        category: form.category || "",
        imageUrl: form.img || "",
        description: form.description || "",
        active: form.active,
      };
      await http.put(`/products/${form.id}`, updateData);
      message.success("✏️ Đã cập nhật sản phẩm!");
      setShowEditModal(false);
      fetchProducts();
    } catch (err) {
      console.error("🔥 Lỗi cập nhật:", err);
      message.error("❌ Cập nhật thất bại!");
    }
  };

  const handleToggleActive = async (record) => {
    try {
      await http.put(`/products/${record.id}`, { active: !record.active });
      message.success(record.active ? "Đã tạm ẩn sản phẩm" : "Đã mở bán sản phẩm");
      fetchProducts();
    } catch (err) {
      console.error("Lỗi đổi trạng thái:", err);
      message.error("Không thể đổi trạng thái sản phẩm");
    }
  };

  // ===== TABLE COLUMNS =====
  const columns = [
    {
      title: "Hình ảnh",
      dataIndex: "imageUrl",
      render: (imageUrl) => {
        if (!imageUrl) {
          return <div className="product-thumb" style={{ width: 50, height: 50, backgroundColor: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>📷</div>;
        }
        // Convert relative URL to full URL for display
        const fullImageUrl = imageUrl.startsWith("http") 
          ? imageUrl 
          : `http://localhost:8080${imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl}`;
        return <img src={fullImageUrl} alt="product" className="product-thumb" onError={(e) => { e.target.src = 'https://via.placeholder.com/50'; }} />;
      },
    },
    { title: "Tên sản phẩm", dataIndex: "name", sorter: (a, b) => a.name.localeCompare(b.name) },
    { 
      title: "Danh mục", 
      dataIndex: "category",
      render: (cat) => cat === "DRINK" ? "Đồ uống" : cat === "FOOD" ? "Đồ ăn" : cat || "—"
    },
    { title: "Nhà hàng", render: (_, record) => getRestaurantName(record.merchantId) },
    {
      title: "Giá (VND)",
      dataIndex: "price",
      render: (p) => p?.toLocaleString("vi-VN"),
      sorter: (a, b) => a.price - b.price,
    },
    {
      title: "Tồn kho",
      dataIndex: "stock",
      render: (s) => s != null ? s.toLocaleString("vi-VN") : "0",
      sorter: (a, b) => (a.stock || 0) - (b.stock || 0),
    },
    { title: "Mô tả", dataIndex: "description", ellipsis: true },
    {
      title: "Trạng thái",
      dataIndex: "active",
      filters: [
        { text: "Đang bán", value: true },
        { text: "Tạm ẩn", value: false },
      ],
      onFilter: (value, record) => record.active === value,
      render: (value) => (
        <span className={`status-tag ${value ? "active" : "inactive"}`}>
          {value ? "Đang bán" : "Tạm ẩn"}
        </span>
      ),
    },
    {
      title: "Hành động",
      render: (_, record) => (
        <div style={{ display: "flex", gap: 8 }}>
          <button
            className="edit-btn"
            onClick={() => {
              setForm({
                ...record,
                img: record.imageUrl || record.image || "", // Map backend image to form
                restaurantId: record.merchantId || "",
                stock: record.stock || 0,
                active: record.active ?? true,
              });
              setShowEditModal(true);
            }}
          >
            ✏️ Sửa
          </button>
          <button
            className="toggle-btn"
            onClick={() => handleToggleActive(record)}
          >
            {record.active ? "Ẩn" : "Mở bán"}
          </button>
          <button className="delete-btn" onClick={() => handleDelete(record.id)}>
            ❌ Xóa
          </button>
        </div>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: "center", marginTop: 60 }}>
        <Spin size="large" tip="Đang tải dữ liệu..." fullscreen />
      </div>
    );
  }

  return (
    <div className="products-page">
      <h1 className="page-title">🍔 Quản lý sản phẩm (Admin)</h1>

      {/* ===== FILTER ===== */}
      <div className="filter-container">
        <div className="filter-item">
          <label>Tìm kiếm:</label>
          <Input placeholder="Nhập tên sản phẩm..." value={searchText} onChange={(e) => setSearchText(e.target.value)} />
        </div>

        <div className="filter-item">
          <label>Danh mục:</label>
          <Select value={category} onChange={setCategory} style={{ width: "100%" }}>
            {["Tất cả", ...new Set(data.map((item) => item.category || "Khác"))].map((cat, i) => (
              <Select.Option key={i} value={cat}>
                {cat}
              </Select.Option>
            ))}
          </Select>
        </div>

        <div className="filter-item">
          <label>Nhà hàng:</label>
          <Select
            value={restaurantFilter}
            onChange={setRestaurantFilter}
            style={{ width: "100%" }}
            loading={loadingRestaurants}
            placeholder={loadingRestaurants ? "Đang tải..." : "Chọn nhà hàng"}
          >
            {["Tất cả", ...restaurantsList.map((r) => r.name)].map((rest, i) => (
              <Select.Option key={i} value={rest}>
                {rest}
              </Select.Option>
            ))}
          </Select>
        </div>

        <div className="filter-item">
          <label>Khoảng giá:</label>
          <div className="price-range">
            <Slider
              range
              min={0}
              max={1000000}
              step={10000}
              value={priceRange}
              onChange={setPriceRange}
              tooltip={{ formatter: null }}
            />
            <div className="price-values">
              <span>{priceRange[0].toLocaleString()} ₫</span>
              <span>{priceRange[1].toLocaleString()} ₫</span>
            </div>
          </div>
        </div>

      <button
        className="add-btn"
        onClick={() => {
          setForm({
            id: "",
            name: "",
            category: "",
            restaurantId: "",
            price: 0,
            stock: 0,
            img: "",
            description: "",
            active: true,
          });
          setShowAddModal(true);
        }}
      >
          ➕ Thêm sản phẩm
        </button>
      </div>

      {/* ===== TABLE ===== */}
      <Table columns={columns} dataSource={filteredData} rowKey="id" pagination={{ pageSize: 6 }} />

      {/* ===== ADD MODAL ===== */}
      <Modal
        open={showAddModal}
        title="Thêm sản phẩm mới"
        onCancel={() => setShowAddModal(false)}
        onOk={handleAdd}
        okText="Thêm"
        centered
      >
        <label>Tên sản phẩm</label>
        <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />

        <label>Nhà hàng</label>
        <Select
          value={form.restaurantId}
          onChange={(v) => setForm({ ...form, restaurantId: v })}
          style={{ width: "100%" }}
          placeholder="Chọn nhà hàng"
          loading={loadingRestaurants}
          getPopupContainer={(trigger) => trigger.parentNode}
        >
          {restaurantsList.map((r) => (
            <Select.Option key={r.id} value={r.merchantId}>
              {r.name}
            </Select.Option>
          ))}
        </Select>

        <label>Danh mục</label>
        <Select
          value={form.category}
          onChange={(v) => setForm({ ...form, category: v })}
          style={{ width: "100%" }}
          placeholder="Chọn danh mục"
          getPopupContainer={(trigger) => trigger.parentNode}
        >
          <Select.Option value="DRINK">Đồ uống</Select.Option>
          <Select.Option value="FOOD">Đồ ăn</Select.Option>
        </Select>

        <label>Giá</label>
        <Input type="number" value={form.price} onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} />

        <label>Số lượng tồn kho</label>
        <Input type="number" min={0} value={form.stock} onChange={(e) => setForm({ ...form, stock: Number(e.target.value) })} />

        <label>Trạng thái</label>
        <Select
          value={form.active ? "true" : "false"}
          onChange={(v) => setForm({ ...form, active: v === "true" })}
          style={{ width: "100%" }}
          getPopupContainer={(trigger) => trigger.parentNode}
        >
          <Select.Option value="true">Đang bán</Select.Option>
          <Select.Option value="false">Tạm ẩn</Select.Option>
        </Select>

        <label>Hình ảnh</label>
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <Input
            type="file"
            accept="image/*"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (file) {
                const uploadedUrl = await handleImageUpload(file);
                if (uploadedUrl) {
                  setForm({ ...form, img: uploadedUrl });
                }
              }
            }}
            disabled={uploadingImage}
          />
          {uploadingImage && <span style={{ color: "#1890ff" }}>⏳ Đang upload...</span>}
          <Input
            placeholder="Hoặc nhập URL ảnh"
            value={form.img || ""}
            onChange={(e) => setForm({ ...form, img: e.target.value })}
            disabled={uploadingImage}
          />
          {form.img && (
            <img
              src={buildImageUrl(form.img)}
              alt="Preview"
              style={{ maxWidth: 200, maxHeight: 200, marginTop: 8, borderRadius: 4 }}
              onError={(e) => { e.target.style.display = "none"; }}
            />
          )}
        </div>

        <label>Mô tả</label>
        <Input.TextArea
          rows={3}
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
      </Modal>

      {/* ===== EDIT MODAL ===== */}
      <Modal
        open={showEditModal}
        title="Chỉnh sửa sản phẩm"
        onCancel={() => setShowEditModal(false)}
        onOk={handleEdit}
        okText="Cập nhật"
        centered
      >
        <label>Tên sản phẩm</label>
        <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />

        <label>Giá</label>
        <Input type="number" value={form.price} onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} />

        <label>Số lượng tồn kho</label>
        <Input type="number" min={0} value={form.stock} onChange={(e) => setForm({ ...form, stock: Number(e.target.value) })} />

        <label>Trạng thái</label>
        <Select
          value={form.active ? "true" : "false"}
          onChange={(v) => setForm({ ...form, active: v === "true" })}
          style={{ width: "100%" }}
          getPopupContainer={(trigger) => trigger.parentNode}
        >
          <Select.Option value="true">Đang bán</Select.Option>
          <Select.Option value="false">Tạm ẩn</Select.Option>
        </Select>

        <label>Danh mục</label>
        <Select
          value={form.category}
          onChange={(v) => setForm({ ...form, category: v })}
          style={{ width: "100%" }}
          placeholder="Chọn danh mục"
          getPopupContainer={(trigger) => trigger.parentNode}
        >
          <Select.Option value="DRINK">Đồ uống</Select.Option>
          <Select.Option value="FOOD">Đồ ăn</Select.Option>
        </Select>

        <label>Hình ảnh</label>
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <Input
            type="file"
            accept="image/*"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (file) {
                const uploadedUrl = await handleImageUpload(file);
                if (uploadedUrl) {
                  setForm({ ...form, img: uploadedUrl });
                }
              }
            }}
            disabled={uploadingImage}
          />
          {uploadingImage && <span style={{ color: "#1890ff" }}>⏳ Đang upload...</span>}
          <Input
            placeholder="Hoặc nhập URL ảnh"
            value={form.img || ""}
            onChange={(e) => setForm({ ...form, img: e.target.value })}
            disabled={uploadingImage}
          />
          {form.img && (
            <img
              src={buildImageUrl(form.img)}
              alt="Preview"
              style={{ maxWidth: 200, maxHeight: 200, marginTop: 8, borderRadius: 4 }}
              onError={(e) => { e.target.style.display = "none"; }}
            />
          )}
        </div>

        <label>Mô tả</label>
        <Input.TextArea
          rows={3}
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
      </Modal>
    </div>
  );
}

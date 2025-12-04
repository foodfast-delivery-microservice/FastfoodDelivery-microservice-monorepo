import { useEffect, useState } from "react";
import {
  Table,
  Input,
  Select,
  Tag,
  Button,
  Modal,
  Form,
  message,
  Popover,
  Space,
  Popconfirm,
} from "antd";
import http from "../../services/http";
import "./Restaurants.css";

export default function Restaurants() {
  const [restaurants, setRestaurants] = useState([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [approvedFilter, setApprovedFilter] = useState("all");

  const [modalVisible, setModalVisible] = useState(false);
  const [editingRestaurant, setEditingRestaurant] = useState(null);

  const [loadingIds, setLoadingIds] = useState([]);
  const [deletingIds, setDeletingIds] = useState([]);

  // ==========================
  // LOAD RESTAURANTS
  // ==========================
  const loadRestaurants = async () => {
    try {
      // Lấy tất cả restaurants với size lớn
      const res = await http.get("/restaurants", { params: { size: 1000 } });
      const data = res.data?.data?.content || res.data?.data || [];
      setRestaurants(data);
    } catch (err) {
      console.error("Lỗi load restaurants:", err);
      message.error("Không tải được danh sách nhà hàng");
    }
  };

  useEffect(() => {
    loadRestaurants();
  }, []);

  // ==========================
  // FILTER RESTAURANTS
  // ==========================
  const filteredRestaurants = restaurants.filter((r) => {
    const name = (r.name || "").toLowerCase();
    const matchName = name.includes(search.toLowerCase());
    
    const matchActive = 
      statusFilter === "all" || 
      (statusFilter === "active" && r.active !== false) ||
      (statusFilter === "inactive" && r.active === false);
    
    const matchApproved =
      approvedFilter === "all" ||
      (approvedFilter === "approved" && r.approved === true) ||
      (approvedFilter === "pending" && r.approved === false);

    return matchName && matchActive && matchApproved;
  });

  // ==========================
  // UPDATE STATUS
  // ==========================
  const handleChangeActive = async (restaurant, newActive) => {
    const currentActive = restaurant.active !== false;
    
    if (currentActive === newActive) return;

    setLoadingIds((prev) => [...prev, restaurant.id]);

    try {
      // Cập nhật restaurant.active trực tiếp qua endpoint admin
      await http.patch(`/restaurants/${restaurant.id}/status`, { active: newActive });
      message.success(
        newActive
          ? "🟢 Nhà hàng đã được kích hoạt"
          : "🔴 Nhà hàng đã bị khóa"
      );
      loadRestaurants();
    } catch (err) {
      console.error("Lỗi cập nhật trạng thái:", err);
      const errorMessage = err?.response?.data?.message || err?.message || "Cập nhật trạng thái thất bại";
      message.error(`❌ ${errorMessage}`);
    } finally {
      setLoadingIds((prev) => prev.filter((id) => id !== restaurant.id));
    }
  };

  const handleChangeApproved = async (restaurant, newApproved) => {
    const currentApproved = restaurant.approved === true;
    
    if (currentApproved === newApproved) return;

    setLoadingIds((prev) => [...prev, restaurant.id]);

    try {
      if (restaurant.merchantId) {
        const userRes = await http.get(`/users/${restaurant.merchantId}`);
        const user = userRes.data?.data;
        
        if (user) {
          await http.patch(`/users/${user.id}`, { approved: newApproved });
          message.success(
            newApproved
              ? "✅ Nhà hàng đã được duyệt"
              : "⏳ Nhà hàng đã bị hủy duyệt"
          );
          loadRestaurants();
        }
      }
    } catch (err) {
      console.error("Lỗi cập nhật duyệt:", err);
      const errorMessage = err?.response?.data?.message || err?.message || "Cập nhật duyệt thất bại";
      message.error(`❌ ${errorMessage}`);
    } finally {
      setLoadingIds((prev) => prev.filter((id) => id !== restaurant.id));
    }
  };

  // ==========================
  // EDIT RESTAURANT
  // ==========================
  const handleEdit = (restaurant) => {
    setEditingRestaurant(restaurant);
    setModalVisible(true);
  };

  const handleSave = async (values) => {
    try {
      // Cập nhật restaurant thông qua merchant endpoint
      // Admin có thể cần endpoint riêng, tạm thời dùng cách này
      message.info("Chức năng cập nhật đang được phát triển");
      setModalVisible(false);
      setEditingRestaurant(null);
    } catch {
      message.error("Cập nhật thất bại");
    }
  };

  // ==========================
  // VALIDATE BEFORE DELETE
  // ==========================
  const validateBeforeDelete = async (restaurant) => {
    const errors = [];

    // 1. Kiểm tra restaurant có đang inactive không (ràng buộc chính)
    const isRestaurantActive = restaurant.active !== false;
    if (isRestaurantActive) {
      errors.push("⚠️ Nhà hàng đang hoạt động (Active = true). Cần vô hiệu hóa nhà hàng trước khi xóa.");
      return { valid: false, errors, needsDeactivation: true };
    }

    // 2. Kiểm tra merchant ID
    if (!restaurant.merchantId) {
      errors.push("❌ Không tìm thấy merchant ID");
      return { valid: false, errors };
    }

    try {
      // 3. Kiểm tra merchant user có tồn tại không
      const userRes = await http.get(`/users/${restaurant.merchantId}`);
      const user = userRes.data?.data;

      if (!user) {
        errors.push("❌ Không tìm thấy merchant user");
        return { valid: false, errors };
      }

      // 4. Kiểm tra role phải là MERCHANT
      if ((user.role || "").toLowerCase() !== "merchant") {
        errors.push("❌ User không phải là merchant");
        return { valid: false, errors };
      }

      // 5. Kiểm tra merchant user cũng phải inactive
      if (user.active !== false) {
        errors.push("⚠️ Merchant user đang hoạt động (active = true). Cần vô hiệu hóa merchant trước khi xóa.");
        return { valid: false, errors, needsDeactivation: true, user };
      }

      // 6. Validation thành công
      return { valid: true, user, errors: [] };
    } catch (err) {
      console.error("Lỗi validate:", err);
      errors.push(`❌ Lỗi khi kiểm tra: ${err?.message || "Unknown error"}`);
      return { valid: false, errors };
    }
  };

  // ==========================
  // DELETE RESTAURANT
  // ==========================
  const handleDelete = async (restaurant) => {
    setDeletingIds((prev) => [...prev, restaurant.id]);

    try {
      // Bước 1: Validate trạng thái restaurant trước khi xóa
      const validation = await validateBeforeDelete(restaurant);

      if (!validation.valid) {
        // Hiển thị cảnh báo và KHÔNG cho phép xóa
        Modal.warning({
          title: "⚠️ Không thể xóa nhà hàng",
          content: (
            <div>
              <p style={{ marginBottom: 12, fontWeight: "bold", color: "#ff4d4f" }}>
                Nhà hàng chưa đạt điều kiện để xóa:
              </p>
              <ul style={{ marginLeft: 20, marginBottom: 12 }}>
                {validation.errors.map((error, index) => (
                  <li key={index} style={{ marginBottom: 8 }}>
                    {error.replace("❌", "").replace("⚠️", "").trim()}
                  </li>
                ))}
              </ul>
              {validation.needsDeactivation && (
                <div style={{ marginTop: 16, padding: 12, background: "#fff7e6", borderRadius: 4 }}>
                  <p style={{ margin: 0, fontWeight: "bold", color: "#d46b08" }}>
                    💡 Giải pháp:
                  </p>
                  <p style={{ margin: "8px 0 0 0", color: "#d46b08" }}>
                    Vui lòng vô hiệu hóa nhà hàng (chuyển trạng thái Active → Inactive) trước khi xóa.
                  </p>
                </div>
              )}
            </div>
          ),
          okText: "Đã hiểu",
          onOk: () => {
            setDeletingIds((prev) => prev.filter((id) => id !== restaurant.id));
          },
        });
        return; // Dừng lại, không cho xóa
      }

      // Bước 2: Validation thành công (restaurant đã inactive), tiến hành xóa
      const { user } = validation;

      // Xóa merchant user (sẽ tự động xóa restaurant do cascade)
      await http.delete(`/users/${user.id}`);
      message.success("Xóa nhà hàng thành công!");
      loadRestaurants();
    } catch (err) {
      console.error("Lỗi xóa restaurant:", err);
      const errorMessage = err?.response?.data?.message || err?.message || "Xóa nhà hàng thất bại";
      
      // Kiểm tra nếu là MerchantDeletionNotAllowedException
      if (errorMessage.includes("merchant đang hoạt động") || 
          errorMessage.includes("MerchantDeletionNotAllowedException")) {
        message.error("❌ Không thể xóa merchant đang hoạt động. Vui lòng vô hiệu hóa merchant trước khi xóa.");
      } else {
        message.error(`❌ ${errorMessage}`);
      }
    } finally {
      setDeletingIds((prev) => prev.filter((id) => id !== restaurant.id));
    }
  };

  // ==========================
  // TABLE COLUMNS
  // ==========================
  const columns = [
    { title: "ID", dataIndex: "id", key: "id", width: 80 },
    {
      title: "Tên nhà hàng",
      dataIndex: "name",
      key: "name",
      width: 200,
    },
    {
      title: "Merchant ID",
      dataIndex: "merchantId",
      key: "merchantId",
      width: 120,
    },
    {
      title: "Địa chỉ",
      dataIndex: "address",
      key: "address",
      render: (v) => v || "—",
    },
    {
      title: "Thành phố",
      dataIndex: "city",
      key: "city",
      render: (v) => v || "—",
    },
    {
      title: "Danh mục",
      dataIndex: "category",
      key: "category",
      render: (v) => v || "—",
    },
    {
      title: "Đánh giá",
      key: "rating",
      render: (_, r) => (
        <span>
          ⭐ {r.rating ? r.rating.toFixed(1) : "0.0"} ({r.reviewCount || 0})
        </span>
      ),
    },
    {
      title: "Trạng thái",
      key: "active",
      render: (_, restaurant) => {
        const isActive = restaurant.active !== false;
        const loading = loadingIds.includes(restaurant.id);

        const menu = (
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            <div
              onClick={() => handleChangeActive(restaurant, true)}
              style={{
                padding: 6,
                cursor: loading ? "not-allowed" : "pointer",
                borderRadius: 6,
                background: isActive ? "#E8F5E9" : "",
                opacity: loading ? 0.5 : 1,
              }}
            >
              🟢 Active
            </div>

            <div
              onClick={() => handleChangeActive(restaurant, false)}
              style={{
                padding: 6,
                cursor: loading ? "not-allowed" : "pointer",
                borderRadius: 6,
                background: !isActive ? "#FFEBEE" : "",
                opacity: loading ? 0.5 : 1,
              }}
            >
              🔴 Inactive
            </div>
          </div>
        );

        return (
          <Popover content={menu} trigger="click" disabled={loading}>
            <Tag
              color={isActive ? "green" : "red"}
              style={{
                padding: "6px 12px",
                borderRadius: 14,
                cursor: loading ? "not-allowed" : "pointer",
                opacity: loading ? 0.5 : 1,
              }}
            >
              {isActive ? "Active" : "Inactive"} ⌄
            </Tag>
          </Popover>
        );
      },
    },
    {
      title: "Duyệt",
      key: "approved",
      render: (_, restaurant) => {
        const isApproved = restaurant.approved === true;
        const loading = loadingIds.includes(restaurant.id);

        const menu = (
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            <div
              onClick={() => handleChangeApproved(restaurant, true)}
              style={{
                padding: 6,
                cursor: loading ? "not-allowed" : "pointer",
                borderRadius: 6,
                background: isApproved ? "#E8F5E9" : "",
                opacity: loading ? 0.5 : 1,
              }}
            >
              ✅ Approved
            </div>

            <div
              onClick={() => handleChangeApproved(restaurant, false)}
              style={{
                padding: 6,
                cursor: loading ? "not-allowed" : "pointer",
                borderRadius: 6,
                background: !isApproved ? "#FFF3E0" : "",
                opacity: loading ? 0.5 : 1,
              }}
            >
              ⏳ Pending
            </div>
          </div>
        );

        return (
          <Popover content={menu} trigger="click" disabled={loading}>
            <Tag
              color={isApproved ? "green" : "orange"}
              style={{
                padding: "6px 12px",
                borderRadius: 14,
                cursor: loading ? "not-allowed" : "pointer",
                opacity: loading ? 0.5 : 1,
              }}
            >
              {isApproved ? "Approved" : "Pending"} ⌄
            </Tag>
          </Popover>
        );
      },
    },
    {
      title: "Hành động",
      key: "action",
      render: (_, r) => {
        const isDeleting = deletingIds.includes(r.id);
        return (
          <Space>
            <Button onClick={() => handleEdit(r)}>Sửa</Button>
            <Popconfirm
              title="Xóa nhà hàng"
              description="Bạn có chắc chắn muốn xóa nhà hàng này? Merchant user, restaurant và tất cả sản phẩm sẽ bị xóa vĩnh viễn."
              onConfirm={async () => {
                // Validate trước khi xóa
                await handleDelete(r);
              }}
              okText="Xóa"
              cancelText="Hủy"
              okButtonProps={{ danger: true }}
            >
              <Button danger disabled={isDeleting} loading={isDeleting}>
                Xóa
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  // ==========================
  // RETURN JSX
  // ==========================
  return (
    <div className="restaurants-page">
      <h1>Quản lý nhà hàng</h1>

      <div style={{ display: "flex", gap: 12, marginBottom: 16, flexWrap: "wrap" }}>
        <Input.Search
          placeholder="Tìm kiếm theo tên nhà hàng..."
          style={{ width: 300 }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
        />

        <Select
          value={statusFilter}
          onChange={setStatusFilter}
          style={{ width: 150 }}
        >
          <Select.Option value="all">Tất cả trạng thái</Select.Option>
          <Select.Option value="active">Active</Select.Option>
          <Select.Option value="inactive">Inactive</Select.Option>
        </Select>

        <Select
          value={approvedFilter}
          onChange={setApprovedFilter}
          style={{ width: 150 }}
        >
          <Select.Option value="all">Tất cả duyệt</Select.Option>
          <Select.Option value="approved">Đã duyệt</Select.Option>
          <Select.Option value="pending">Chờ duyệt</Select.Option>
        </Select>
      </div>

      <Table
        columns={columns}
        dataSource={filteredRestaurants}
        rowKey="id"
        pagination={{ pageSize: 10 }}
        scroll={{ x: 1200 }}
      />

      {/* MODAL EDIT */}
      <Modal
        title="Chỉnh sửa nhà hàng"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          setEditingRestaurant(null);
        }}
        footer={null}
        width={600}
      >
        {editingRestaurant && (
          <Form layout="vertical" initialValues={editingRestaurant} onFinish={handleSave}>
            <Form.Item label="Tên nhà hàng" name="name">
              <Input />
            </Form.Item>
            <Form.Item label="Địa chỉ" name="address">
              <Input />
            </Form.Item>
            <Form.Item label="Thành phố" name="city">
              <Input />
            </Form.Item>
            <Form.Item label="Danh mục" name="category">
              <Input />
            </Form.Item>
            <Form.Item label="Mô tả" name="description">
              <Input.TextArea rows={4} />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" style={{ width: "100%" }}>
                Lưu
              </Button>
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  );
}


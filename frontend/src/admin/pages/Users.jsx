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

export default function Users() {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState("");

  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);

  const [loadingIds, setLoadingIds] = useState([]);
  const [deletingIds, setDeletingIds] = useState([]);

  // ==========================
  // LOAD USERS
  // ==========================
  const loadUsers = async () => {
    try {
      const res = await http.get("/users");
      const data = res.data?.data || [];
      // Chỉ lấy users có role là "user"
      const userRoleOnly = data.filter((u) => (u.role || "").toLowerCase() === "user");
      setUsers(userRoleOnly);
    } catch (err) {
      console.error("Lỗi load users:", err);
      message.error("Không tải được danh sách users");
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  // ==========================
  // FILTER USERS
  // ==========================
  const filteredUsers = users.filter((u) => {
    const fullname = (u.fullName || `${u.firstname || ""} ${u.lastname || ""}`.trim() || "").toLowerCase();
    const matchName = fullname.includes(search.toLowerCase());
    return matchName;
  });

  // ==========================
  // UPDATE STATUS
  // ==========================
  const handleChangeStatus = async (user, newStatus) => {
    // Map "active"/"banned" to boolean
    const newActive = newStatus === "active";
    const currentActive = user.active !== false; // Default to true if undefined
    
    // Skip if status unchanged
    if (currentActive === newActive) return;

    setLoadingIds((prev) => [...prev, user.id]);

    try {
      // Backend uses 'active' (boolean), not 'status' (string)
      await http.patch(`/users/${user.id}`, { active: newActive });

      message.success(
        newActive
          ? "🟢 Người dùng đã được kích hoạt"
          : "🔴 Người dùng đã bị khóa"
      );

      loadUsers();
    } catch (err) {
      console.error("Lỗi cập nhật trạng thái:", err);
      const errorMessage = err?.response?.data?.message || err?.message || "Cập nhật trạng thái thất bại";
      message.error(`❌ ${errorMessage}`);
    } finally {
      setLoadingIds((prev) => prev.filter((id) => id !== user.id));
    }
  };

  // ==========================
  // EDIT USER
  // ==========================
  const handleEdit = (user) => {
    setEditingUser(user);
    setModalVisible(true);
  };

  const handleSave = async (values) => {
    try {
      await http.patch(`/users/${editingUser.id}`, values);
      message.success("Cập nhật thành công!");
      setModalVisible(false);
      setEditingUser(null);
      loadUsers();
    } catch {
      message.error("Cập nhật thất bại");
    }
  };

  // ==========================
  // DELETE USER
  // ==========================
  const handleDelete = async (user) => {
    setDeletingIds((prev) => [...prev, user.id]);
    try {
      await http.delete(`/users/${user.id}`);
      message.success("Xóa người dùng thành công!");
      loadUsers();
    } catch (err) {
      console.error("Lỗi xóa user:", err);
      const errorMessage = err?.response?.data?.message || err?.message || "Xóa người dùng thất bại";
      message.error(`❌ ${errorMessage}`);
    } finally {
      setDeletingIds((prev) => prev.filter((id) => id !== user.id));
    }
  };

  // ==========================
  // TABLE COLUMNS
  // ==========================
  const columns = [
    { title: "ID", dataIndex: "id", key: "id" },
    {
      title: "Tên đầy đủ",
      key: "fullname",
      render: (_, r) => r.fullName || `${r.firstname || ""} ${r.lastname || ""}`.trim() || "—",
    },


    {
      title: "SĐT",
      dataIndex: "phonenumber",
      key: "phonenumber",
      render: (v) => v || "—",
    },

    {
      title: "Địa chỉ",
      dataIndex: "address",
      key: "address",
      render: (v) => v || "—",
    },

    {
      title: "Role",
      dataIndex: "role",
      key: "role",
      render: (role) => (
        <Tag
          color={(role || "").toLowerCase() === "admin" ? "purple" : "green"}
          style={{ padding: "5px 10px", borderRadius: 14 }}
        >
          {role}
        </Tag>
      ),
    },


    {
      title: "Trạng thái",
      key: "status",
      render: (_, user) => {
        // Backend uses 'active' (boolean), default to true if undefined
        const isActive = user.active !== false;
        const loading = loadingIds.includes(user.id);

        const menu = (
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            <div
              onClick={() => handleChangeStatus(user, "active")}
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
              onClick={() => handleChangeStatus(user, "banned")}
              style={{
                padding: 6,
                cursor: loading ? "not-allowed" : "pointer",
                borderRadius: 6,
                background: !isActive ? "#FFEBEE" : "",
                opacity: loading ? 0.5 : 1,
              }}
            >
              🔴 Banned
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
              {isActive ? "Active" : "Banned"} ⌄
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
              title="Xóa người dùng"
              description="Bạn có chắc chắn muốn xóa người dùng này?"
              onConfirm={() => handleDelete(r)}
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
    <div className="users-page">
      <h1> Quản lý người dùng</h1>

      <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
        <Input.Search
          placeholder="Tìm kiếm theo tên..."
          style={{ width: 300 }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
        />
      </div>

      <Table
        columns={columns}
        dataSource={filteredUsers}
        rowKey="id"
        pagination={{ pageSize: 6 }}
      />

      {/* MODAL EDIT */}
      <Modal
        title="Chỉnh sửa người dùng"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          setEditingUser(null);
        }}
        footer={null}
      >
        {editingUser && (
          <Form layout="vertical" initialValues={editingUser} onFinish={handleSave}>
            <Form.Item label="Tên" name="firstname">
              <Input />
            </Form.Item>
            <Form.Item label="Họ" name="lastname">
              <Input />
            </Form.Item>
            <Form.Item label="SĐT" name="phonenumber">
              <Input />
            </Form.Item>
            <Form.Item label="Địa chỉ" name="address">
              <Input />
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

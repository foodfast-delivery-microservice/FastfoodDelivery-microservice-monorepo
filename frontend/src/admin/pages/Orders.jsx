// src/admin/pages/OrdersList.jsx
import { useEffect, useState, useMemo } from "react";
import { Input, Table, Tag, Select } from "antd";
import { useNavigate } from "react-router-dom";
import http from "../../services/http";
import "./Orders.css";

export default function OrdersList() {
  const [orders, setOrders] = useState([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [timeFilter, setTimeFilter] = useState("all");
  const [restaurantFilter, setRestaurantFilter] = useState("all");

  const navigate = useNavigate();

  // 🔥 Fetch Orders
  useEffect(() => {
    async function fetchOrders() {
      try {
        const res = await http.get("/orders", { params: { size: 100 } });
        const data = res.data?.data?.content || [];

        // Map backend data to frontend structure if needed
        const mappedData = data.map(item => ({
          ...item,
          // Backend returns LocalDateTime array [yyyy, MM, dd, HH, mm, ss]
          createdAt: Array.isArray(item.createdAt)
            ? new Date(item.createdAt[0], item.createdAt[1] - 1, item.createdAt[2], item.createdAt[3], item.createdAt[4], item.createdAt[5])
            : new Date(item.createdAt),
          total: item.grandTotal, // Map grandTotal to total
          restaurantName: "Loading...", // We might need to fetch restaurant name or it might be in response? 
          // OrderListResponse doesn't have restaurantName, only merchantId.
          // We might need to fetch restaurants to map names or just show ID.
          // For now let's try to fetch restaurants separately or just show ID.
        }));

        setOrders(mappedData);
        console.log("✅ API loaded orders:", mappedData);
      } catch (error) {
        console.error("❌ Lỗi tải đơn hàng:", error);
      }
    }
    fetchOrders();
  }, []);

  // Fetch restaurants to map names
  const [restaurants, setRestaurants] = useState([]);
  useEffect(() => {
    async function fetchRestaurants() {
      try {
        const res = await http.get("/restaurants", { params: { size: 100 } });
        setRestaurants(res.data?.data?.content || []);
      } catch (e) {
        console.error("Failed to fetch restaurants", e);
      }
    }
    fetchRestaurants();
  }, []);

  const getRestaurantName = (merchantId) => {
    const r = restaurants.find(res => res.merchantId === merchantId);
    return r ? r.name : `Merchant #${merchantId}`;
  }


  // 🧠 Filter + sort Orders
  const filteredOrders = useMemo(() => {
    const now = new Date();
    return orders
      .map(o => ({
        ...o,
        restaurantName: getRestaurantName(o.merchantId)
      }))
      .filter((o) => {
        const matchSearch =
          o.receiverName?.toLowerCase().includes(search.toLowerCase()) ||
          o.orderCode?.toLowerCase().includes(search.toLowerCase()) ||
          o.id.toString().includes(search);

        const matchStatus =
          statusFilter === "all" ||
          (o.status &&
            o.status.toLowerCase().includes(statusFilter.toLowerCase()));

        const matchRestaurant =
          restaurantFilter === "all" ||
          o.restaurantName === restaurantFilter;

        // Filter by time
        let matchTime = true;
        if (timeFilter !== "all" && o.createdAt) {
          const diffHours = (now - o.createdAt) / (1000 * 60 * 60);
          if (timeFilter === "24h" && diffHours > 24) matchTime = false;
          if (timeFilter === "3d" && diffHours > 72) matchTime = false;
          if (timeFilter === "7d" && diffHours > 168) matchTime = false;
        }

        return matchSearch && matchStatus && matchRestaurant && matchTime;
      })
      .sort(
        (a, b) =>
          (b.createdAt?.getTime() || 0) - (a.createdAt?.getTime() || 0)
      );
  }, [orders, search, statusFilter, timeFilter, restaurantFilter, restaurants]);

  // ===== Table Columns =====
  const columns = [
    {
      title: "Mã ĐH",
      dataIndex: "orderCode", // Use orderCode from backend
      key: "orderCode",
      render: (text, record) => (
        <span
          style={{ cursor: "pointer" }}
          onClick={() => navigate(`/admin/orders/${record.id}`)}
        >
          {text || record.id}
        </span>
      ),
    },
    {
      title: "Khách hàng",
      dataIndex: "receiverName", // Use receiverName
      key: "receiverName",
      render: (text, record) => (
        <span
          style={{ cursor: "pointer" }}
          onClick={() => navigate(`/admin/orders/${record.id}`)}
        >
          {text}
        </span>
      ),
    },
    {
      title: "SĐT",
      dataIndex: "receiverPhone", // Use receiverPhone
      key: "receiverPhone",
      render: (text, record) => (
        <span
          style={{ cursor: "pointer" }}
          onClick={() => navigate(`/admin/orders/${record.id}`)}
        >
          {text}
        </span>
      ),
    },
    {
      title: "Nhà hàng",
      dataIndex: "restaurantName",
      key: "restaurantName",
      render: (_, record) => (
        <span
          style={{ cursor: "pointer" }}
          onClick={() => navigate(`/admin/orders/${record.id}`)}
        >
          {record.restaurantName || "—"}
        </span>
      ),
    },
    {
      title: "Ngày đặt",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (val, record) => (
        <span
          style={{ cursor: "pointer" }}
          onClick={() => navigate(`/admin/orders/${record.id}`)}
        >
          {val ? val.toLocaleString("vi-VN") : "—"}
        </span>
      ),
    },
    {
      title: "Thành tiền",
      dataIndex: "total",
      key: "total",
      render: (val, record) => (
        <span
          style={{ cursor: "pointer" }}
          onClick={() => navigate(`/admin/orders/${record.id}`)}
        >
          {`${Number(val || 0).toLocaleString("vi-VN")}₫`}
        </span>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status = "", record) => {
        const s = status.toLowerCase();
        let color = "blue";
        if (s.includes("delivered") || s.includes("confirmed")) color = "green";
        else if (s.includes("processing") || s.includes("pending"))
          color = "orange";
        else if (s.includes("delivering")) color = "geekblue";
        else if (s.includes("cancelled")) color = "red";

        return (
          <Tag
            color={color}
            style={{ cursor: "pointer" }}
            onClick={() => navigate(`/admin/orders/${record.id}`)}
          >
            {status}
          </Tag>
        );
      },
    },
  ];

  return (
    <div className="orders-page">
      <div className="orders-header">
        <h1>📦 Quản lý đơn hàng (Admin)</h1>
      </div>

      {/* ===== Bộ lọc ===== */}
      <div className="filter-container">
        <div className="filter-item">
          <label>Tìm kiếm:</label>
          <Input
            placeholder="Nhập tên hoặc mã đơn hàng..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            allowClear
          />
        </div>

        <div className="filter-item">
          <label>Trạng thái:</label>
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { label: "Tất cả", value: "all" },
              { label: "Chờ xác nhận", value: "pending" },
              { label: "Đang xử lý", value: "processing" },
              { label: "Đang giao", value: "delivering" },
              { label: "Đã giao", value: "delivered" },
              { label: "Đã hủy", value: "cancelled" },
            ]}
          />
        </div>

        <div className="filter-item">
          <label>Thời gian:</label>
          <Select
            value={timeFilter}
            onChange={setTimeFilter}
            options={[
              { label: "Tất cả", value: "all" },
              { label: "24 giờ", value: "24h" },
              { label: "3 ngày", value: "3d" },
              { label: "7 ngày", value: "7d" },
            ]}
          />
        </div>

        <div className="filter-item">
          <label>Nhà hàng:</label>
          <Select
            value={restaurantFilter}
            onChange={setRestaurantFilter}
            options={[
              { label: "Tất cả", value: "all" },
              ...Array.from(
                new Set(
                  orders.map((o) => getRestaurantName(o.merchantId))
                )
              )
                .filter(Boolean)
                .map((r) => ({ label: r, value: r })),
            ]}
            showSearch
            optionFilterProp="label"
          />
        </div>
      </div>

      {/* ===== Bảng đơn hàng ===== */}
      <Table
        columns={columns}
        dataSource={filteredOrders}
        rowKey="id"
        pagination={{ pageSize: 8 }}
        className="orders-table"
      />
    </div>
  );
}

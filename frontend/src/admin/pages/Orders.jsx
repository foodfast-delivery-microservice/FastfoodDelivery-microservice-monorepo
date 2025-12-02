// src/admin/pages/Orders.jsx
import { useEffect, useState, useMemo, useCallback } from "react";
import { Input, Table, Tag, Select, message, Spin } from "antd";
import { useNavigate } from "react-router-dom";
import http from "../../services/http";
import "./Orders.css";

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState({
    orders: null,
    restaurants: null,
  });

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [timeFilter, setTimeFilter] = useState("all");
  const [restaurantFilter, setRestaurantFilter] = useState("all");

  const navigate = useNavigate();

  const logApi = (label, payload) => {
    if (process.env.NODE_ENV === "production") return;
    // eslint-disable-next-line no-console
    console.log(`[Orders] ${label}`, payload);
  };

  const serializeError = (err) => {
    if (!err) return null;
    return {
      message: err?.message || "Lỗi không xác định",
      status: err?.response?.status,
      details: err?.response?.data || err?.stack,
    };
  };

  const parseDate = (createdAt) => {
    if (!createdAt) return null;
    
    // Handle array format [yyyy, MM, dd, HH, mm, ss]
    if (Array.isArray(createdAt)) {
      return new Date(
        createdAt[0],
        createdAt[1] - 1,
        createdAt[2],
        createdAt[3] || 0,
        createdAt[4] || 0,
        createdAt[5] || 0
      );
    }
    
    // Handle object format (Java LocalDateTime)
    if (
      typeof createdAt === "object" &&
      createdAt !== null &&
      (createdAt.year || createdAt.monthValue)
    ) {
      const { year, monthValue, month, dayOfMonth, day, hour, minute, second } = createdAt;
      return new Date(
        year || new Date().getFullYear(),
        (monthValue || month || 1) - 1,
        dayOfMonth || day || 1,
        hour || 0,
        minute || 0,
        second || 0
      );
    }
    
    // Handle number (timestamp)
    if (typeof createdAt === "number") {
      return new Date(createdAt);
    }
    
    // Handle string
    const date = new Date(createdAt);
    return Number.isFinite(date.getTime()) ? date : null;
  };

  // 🔥 Fetch Orders
  const fetchOrders = useCallback(async () => {
    try {
      logApi("GET /orders params", { size: 100, page: 0 });
      const res = await http.get("/orders", { params: { size: 100, page: 0 } });
      logApi("GET /orders response", res?.data);

      // Try multiple response structures
      const data =
        res?.data?.data?.content ||
        res?.data?.content ||
        res?.data?.data ||
        res?.data ||
        [];

      if (!Array.isArray(data)) {
        throw new Error(`Response không phải mảng: ${typeof data}`);
      }

      logApi("Parsed orders array length", data.length);

      // Map backend data to frontend structure
      const mappedData = data.map((item) => {
        const parsedDate = parseDate(item.createdAt);
        return {
          ...item,
          createdAt: parsedDate,
          total: item.grandTotal || item.total || 0,
          restaurantName: "Loading...", // Will be updated after restaurants load
        };
      });

      setOrders(mappedData);
      logApi("✅ Mapped orders", mappedData);
    } catch (error) {
      const errPayload = serializeError(error);
      setErrors((prev) => ({ ...prev, orders: errPayload }));
      logApi("❌ Error fetching orders", errPayload);
      message.error("Không thể tải danh sách đơn hàng");
    }
  }, []);

  // Fetch restaurants to map names
  const fetchRestaurants = useCallback(async () => {
    try {
      logApi("GET /restaurants params", { size: 100, page: 0 });
      const res = await http.get("/restaurants", {
        params: { size: 100, page: 0 },
      });
      logApi("GET /restaurants response", res?.data);

      const data =
        res?.data?.data?.content ||
        res?.data?.content ||
        res?.data?.data ||
        res?.data ||
        [];

      if (!Array.isArray(data)) {
        throw new Error(`Response không phải mảng: ${typeof data}`);
      }

      setRestaurants(data);
      logApi("✅ Loaded restaurants", data.length);
    } catch (error) {
      const errPayload = serializeError(error);
      setErrors((prev) => ({ ...prev, restaurants: errPayload }));
      logApi("❌ Error fetching restaurants", errPayload);
      message.error("Không thể tải danh sách nhà hàng");
    }
  }, []);

  // Fetch all data
  useEffect(() => {
    const loadAll = async () => {
      setLoading(true);
      setErrors({ orders: null, restaurants: null });
      
      await Promise.allSettled([fetchOrders(), fetchRestaurants()]);
      
      setLoading(false);
    };
    
    loadAll();
  }, [fetchOrders, fetchRestaurants]);

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

  const errorEntries = useMemo(
    () => Object.entries(errors).filter(([, value]) => !!value),
    [errors]
  );

  const handleRetry = useCallback(() => {
    setLoading(true);
    setErrors({ orders: null, restaurants: null });
    Promise.allSettled([fetchOrders(), fetchRestaurants()]).then(() => {
      setLoading(false);
    });
  }, [fetchOrders, fetchRestaurants]);

  if (loading) {
    return (
      <div className="orders-page">
        <div style={{ textAlign: "center", padding: "60px 20px" }}>
          <Spin size="large" tip="Đang tải dữ liệu..." />
        </div>
      </div>
    );
  }

  return (
    <div className="orders-page">
      <div className="orders-header">
        <h1>📦 Quản lý đơn hàng (Admin)</h1>
      </div>

      {errorEntries.length > 0 && (
        <div className="orders-error-banner">
          <div>
            <p><strong>Không thể tải đầy đủ dữ liệu:</strong></p>
            <ul>
              {errorEntries.map(([key, value]) => (
                <li key={key}>
                  <strong>{key}:</strong> {value?.message || "Lỗi không xác định"}
                  {value?.status && ` (HTTP ${value.status})`}
                </li>
              ))}
            </ul>
          </div>
          <button type="button" onClick={handleRetry} className="retry-btn">
            🔄 Thử lại
          </button>
        </div>
      )}

      {/* ===== Bộ lọc ===== */}
      <div className="filter-container">
        <div className="filter-item">
          <span className="filter-label">Tìm kiếm:</span>
          <Input
            placeholder="Nhập tên hoặc mã đơn hàng..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            allowClear
            aria-label="Tìm kiếm đơn hàng"
          />
        </div>

        <div className="filter-item">
          <span className="filter-label">Trạng thái:</span>
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
          <span className="filter-label">Thời gian:</span>
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
          <span className="filter-label">Nhà hàng:</span>
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

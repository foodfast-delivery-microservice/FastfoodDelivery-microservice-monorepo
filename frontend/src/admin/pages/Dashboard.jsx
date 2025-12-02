import React, { useEffect, useState, useCallback, useMemo } from "react";
import "./Dashboard.css";
import http from "../../services/http";
import { message } from "antd";
import { getSystemKPIs } from "../../services/statisticsApi";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Legend,
} from "recharts";

export default function RestaurantDashboard() {

  const [orders, setOrders] = useState([]);
  const [restaurants, setRestaurants] = useState([]);

  const [loading, setLoading] = useState(true);
  const [kpis, setKpis] = useState(null);
  const [errors, setErrors] = useState({
    restaurants: null,
    orders: null,
    kpis: null,
  });

  // === FILTER STATE (new) ===
  const [restaurantFilter, setRestaurantFilter] = useState("all");
  const [timeFilter, setTimeFilter] = useState("all");

  // === CHART STATE ===
  const [chartData, setChartData] = useState([]);     // revenue
  const [orderChart, setOrderChart] = useState([]);    // order count

  // === DASHBOARD COUNTER ===
  const [stats, setStats] = useState({
    totalOrders: 0,
    delivered: 0,
    delivering: 0,
    processing: 0,
    totalRevenue: 0,
  });

  // ================================
  // 🔥 FETCH DATA
  // ================================
  const fetchAll = useCallback(async () => {
    const serializeError = (err) => {
      if (!err) return null;
      return {
        message: err?.message,
        status: err?.response?.status,
        details: err?.response?.data || err?.stack,
      };
    };

    const logApi = (label, payload) => {
      if (process.env.NODE_ENV === "production") return;
      // eslint-disable-next-line no-console
      console.log(`[Dashboard] ${label}`, payload);
    };

    logApi("Trigger fetchAll", { restaurantFilter, timeFilter });

    try {
      setLoading(true);
      setErrors({ restaurants: null, orders: null, kpis: null });

      const fetchRestaurants = async () => {
        const params = { size: 100, page: 0 };
        logApi("GET /restaurants params", params);
        const response = await http.get("/restaurants", { params });
        logApi("GET /restaurants response", response?.data);
        const list =
          response?.data?.data?.content ||
          response?.data?.content ||
          response?.data?.data ||
          [];
        if (!Array.isArray(list)) {
          throw new Error("Danh sách nhà hàng trả về không hợp lệ");
        }
        return list;
      };

      const fetchOrders = async () => {
        const params = { size: 1000, page: 0 };
        logApi("GET /orders params", params);
        const response = await http.get("/orders", { params });
        logApi("GET /orders response", response?.data);
        const list =
          response?.data?.data?.content ||
          response?.data?.content ||
          response?.data?.data ||
          [];
        if (!Array.isArray(list)) {
          throw new Error("Danh sách đơn hàng trả về không hợp lệ");
        }
        return list;
      };

      const fetchKpis = async () => {
        logApi("GET /admin/dashboard/kpis", {});
        const response = await getSystemKPIs();
        logApi("GET /admin/dashboard/kpis response", response);
        return response;
      };

      const results = await Promise.allSettled([
        fetchRestaurants(),
        fetchOrders(),
        fetchKpis(),
      ]);

      const [restaurantsResult, ordersResult, kpisResult] = results;

      if (restaurantsResult.status === "fulfilled") {
        setRestaurants(restaurantsResult.value);
      } else {
        const errPayload = serializeError(restaurantsResult.reason);
        setErrors((prev) => ({ ...prev, restaurants: errPayload }));
        message.error("Không thể tải danh sách nhà hàng");
      }

      let filteredOrders = [];
      if (ordersResult.status === "fulfilled") {
        const rawOrders = ordersResult.value;

        filteredOrders = rawOrders;
        if (restaurantFilter !== "all") {
          filteredOrders = filteredOrders.filter(
            (o) => String(o.merchantId) === String(restaurantFilter)
          );
        }

        setOrders(filteredOrders);
      } else {
        const errPayload = serializeError(ordersResult.reason);
        setErrors((prev) => ({ ...prev, orders: errPayload }));
        message.error("Không thể tải danh sách đơn hàng");
      }

      if (kpisResult.status === "fulfilled") {
        setKpis(kpisResult.value || null);
      } else {
        const errPayload = serializeError(kpisResult.reason);
        setErrors((prev) => ({ ...prev, kpis: errPayload }));
        setKpis(null);
        message.warning("Không thể tải KPI hệ thống, sử dụng dữ liệu tạm thời");
      }

      // === STATS ===
      const delivered = filteredOrders.filter((o) =>
        (o.status || "")
          .toLowerCase()
          .includes("delivered") || (o.status || "").toLowerCase().includes("đã giao")
      );
      const delivering = filteredOrders.filter((o) =>
        (o.status || "").toLowerCase().includes("delivering") ||
        (o.status || "").toLowerCase().includes("đang giao")
      );
      const processing = filteredOrders.filter((o) =>
        (o.status || "").toLowerCase().includes("processing") ||
        (o.status || "").toLowerCase().includes("pending") ||
        (o.status || "").toLowerCase().includes("confirmed")
      );

      const totalRevenue = delivered.reduce(
        (sum, o) => sum + Number(o.grandTotal || 0),
        0
      );

      setStats({
        totalOrders: filteredOrders.length,
        delivered: delivered.length,
        delivering: delivering.length,
        processing: processing.length,
        totalRevenue,
      });
    } catch (err) {
      console.error("Lỗi:", err);
      message.error("Không thể tải dữ liệu Dashboard");
    } finally {
      setLoading(false);
    }
  }, [restaurantFilter]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);


  // ================================
  // 🔥 CHART PROCESSING
  // ================================
  useEffect(() => {
    if (orders.length === 0) {
      setChartData([]);
      setOrderChart([]);
      return;
    }

    const daily = {};
    const now = Date.now();

    const toMillis = (createdAt) => {
      if (!createdAt) return 0;
      // Handle array format [yyyy, MM, dd, HH, mm, ss] or string
      if (Array.isArray(createdAt)) {
        const date = new Date(createdAt[0], createdAt[1] - 1, createdAt[2], createdAt[3], createdAt[4], createdAt[5]);
        return date.getTime();
      }
      if (
        typeof createdAt === "object" &&
        createdAt !== null &&
        (createdAt.year || createdAt.monthValue)
      ) {
        const {
          year,
          monthValue,
          month,
          dayOfMonth,
          day,
          hour,
          minute,
          second,
        } = createdAt;
        const jsDate = new Date(
          year || createdAt.year || new Date().getFullYear(),
          (monthValue || month || 1) - 1,
          dayOfMonth || day || 1,
          hour || 0,
          minute || 0,
          second || 0
        );
        return jsDate.getTime();
      }
      if (typeof createdAt === "number") {
        return createdAt;
      }
      const ms = new Date(createdAt).getTime();
      return Number.isFinite(ms) ? ms : 0;
    };

    orders.forEach((o) => {
      const ms = toMillis(o.createdAt);
      if (!ms) return;

      // Time filter
      if (timeFilter === "24h" && ms < now - 24 * 3600 * 1000) return;
      if (timeFilter === "3d" && ms < now - 3 * 24 * 3600 * 1000) return;
      if (timeFilter === "7d" && ms < now - 7 * 24 * 3600 * 1000) return;
      if (timeFilter === "30d" && ms < now - 30 * 24 * 3600 * 1000) return;

      const date = new Date(ms).toLocaleDateString("vi-VN");

      if (!daily[date]) {
        daily[date] = {
          date,
          timestamp: ms,
          revenue: 0,
          count: 0,
        };
      }

      // Revenue only for delivered orders
      const status = (o.status || "").toLowerCase();
      if (status.includes("delivered") || status.includes("đã giao")) {
        daily[date].revenue += Number(o.grandTotal || 0);
      }

      daily[date].count += 1;
    });

    const sorted = Object.values(daily).sort(
      (a, b) => a.timestamp - b.timestamp
    );

    setChartData(sorted);
    setOrderChart(sorted);
  }, [orders, timeFilter, restaurantFilter]);

  const displayStats = useMemo(() => {
    const shouldUseKpis = restaurantFilter === "all" && kpis;
    const statusCount = (statusKey) => {
      if (!shouldUseKpis || !kpis?.todayOrdersByStatus) return undefined;
      const entry = Object.entries(kpis.todayOrdersByStatus || {}).find(
        ([key]) => (key || "").toLowerCase() === statusKey.toLowerCase()
      );
      return entry ? Number(entry[1]) : undefined;
    };

    return {
      totalOrders: shouldUseKpis && kpis?.todayTotalOrders != null ? Number(kpis.todayTotalOrders) : stats.totalOrders,
      delivered: statusCount("delivered") ?? stats.delivered,
      delivering: statusCount("delivering") ?? stats.delivering,
      processing: shouldUseKpis && kpis?.pendingOrdersCount != null ? Number(kpis.pendingOrdersCount) : stats.processing,
      totalRevenue: stats.totalRevenue,
    };
  }, [kpis, stats, restaurantFilter]);

  const errorEntries = useMemo(
    () => Object.entries(errors).filter(([, value]) => !!value),
    [errors]
  );

  if (loading) return <p>⏳ Đang tải dữ liệu...</p>;

  // ================================
  // 🔥 UI
  // ================================
  return (
    <div className="restaurant-dashboard">
      <h2>Dashboard Nhà hàng</h2>

      {errorEntries.length > 0 && (
        <div className="dashboard-error-banner">
          <div>
            <p>Không thể tải đầy đủ dữ liệu:</p>
            <ul>
              {errorEntries.map(([key, value]) => (
                <li key={key}>
                  <strong>{key}:</strong> {value?.message || "Lỗi không xác định"}
                  {value?.status && ` (HTTP ${value.status})`}
                </li>
              ))}
            </ul>
          </div>
          <button type="button" onClick={fetchAll}>
            Thử lại
          </button>
        </div>
      )}

      {/* SUMMARY CARDS */}
      <div className="cards">
        <div className="card purple">
          <h2>{displayStats.totalOrders}</h2>
          <p>Tổng đơn hàng</p>
        </div>

        <div className="card orange">
          <h2>{displayStats.processing}</h2>
          <p>Đang xử lý</p>
        </div>

        <div className="card green">
          <h2>{displayStats.delivering}</h2>
          <p>Đang giao</p>
        </div>

        <div className="card blue">
          <h2>{displayStats.totalRevenue.toLocaleString()}₫</h2>
          <p>Tổng doanh thu</p>
        </div>
      </div>

      {/* FILTER BAR */}
      <div className="filter-bar">

        <div className="filter-item">
          <label>Nhà hàng</label>
          <select
            value={restaurantFilter}
            onChange={(e) => setRestaurantFilter(e.target.value)}
          >
            <option value="all">Tất cả</option>
            {restaurants.map((r) => (
              <option key={r.id} value={r.merchantId}>
                {r.name}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-item">
          <label>Thời gian</label>
          <select
            value={timeFilter}
            onChange={(e) => setTimeFilter(e.target.value)}
          >
            <option value="all">Tất cả</option>
            <option value="24h">24 giờ qua</option>
            <option value="3d">3 ngày qua</option>
            <option value="7d">7 ngày qua</option>
            <option value="30d">30 ngày qua</option>
          </select>
        </div>

        <button
          className="btn reset"
          onClick={() => {
            setRestaurantFilter("all");
            setTimeFilter("all");
          }}
        >
          Xóa lọc
        </button>
      </div>

      {/* =======================
          CHART REVENUE
      ========================= */}
      <div className="chart-container">
        <h3> Doanh thu theo ngày</h3>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip formatter={(v) => `${v.toLocaleString()}₫`} />
            <Legend />
            <Line
              type="monotone"
              dataKey="revenue"
              stroke="#4f46e5"
              strokeWidth={3}
              name="Doanh thu"
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* =======================
          CHART ORDER COUNT
      ========================= */}
      <div className="chart-container">
        <h3> Số đơn theo ngày</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={orderChart}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar
              dataKey="count"
              fill="#10b981"
              barSize={40}
              name="Số đơn"
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

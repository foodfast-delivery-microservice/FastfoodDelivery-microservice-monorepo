import React, { useEffect, useState, useCallback, useMemo } from "react";
import "./Dashboard.css";
import http from "../../services/http";
import { message } from "antd";
import { getSystemKPIs, getRevenueByRestaurant } from "../../services/statisticsApi";

import {
  Tooltip,
  ResponsiveContainer,
  Legend,
  PieChart,
  Pie,
  Cell,
} from "recharts";

export default function RestaurantDashboard() {

  const [orders, setOrders] = useState([]);
  const [restaurants, setRestaurants] = useState([]);

  const [loading, setLoading] = useState(true);
  const [kpis, setKpis] = useState(null);
  const [revenueByRestaurantData, setRevenueByRestaurantData] = useState([]);
  const [errors, setErrors] = useState({
    restaurants: null,
    orders: null,
    kpis: null,
    revenueByRestaurant: null,
  });

  // === FILTER STATE (new) ===
  const [restaurantFilter, setRestaurantFilter] = useState("all");
  const [timeFilter, setTimeFilter] = useState("all");

  // === CHART STATE ===
  // Removed orderChart - only using PieChart now

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
      setErrors({ restaurants: null, orders: null, kpis: null, revenueByRestaurant: null });

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

      const fetchRevenueByRestaurant = async () => {
        // Tính toán date range từ timeFilter
        let fromDate = null;
        let toDate = null;
        
        // Chỉ set date range nếu timeFilter không phải "all"
        if (timeFilter !== "all") {
          const now = new Date();
          
          if (timeFilter === "24h") {
            fromDate = new Date(now.getTime() - 24 * 3600 * 1000);
            toDate = now;
          } else if (timeFilter === "3d") {
            fromDate = new Date(now.getTime() - 3 * 24 * 3600 * 1000);
            toDate = now;
          } else if (timeFilter === "7d") {
            fromDate = new Date(now.getTime() - 7 * 24 * 3600 * 1000);
            toDate = now;
          } else if (timeFilter === "30d") {
            fromDate = new Date(now.getTime() - 30 * 24 * 3600 * 1000);
            toDate = now;
          }
        }
        
        const params = {};
        if (fromDate) {
          // Format theo ISO 8601 với timezone
          params.fromDate = fromDate.toISOString();
        }
        if (toDate) {
          params.toDate = toDate.toISOString();
        }
        
        logApi("GET /admin/dashboard/revenue-by-restaurant params", params);
        const response = await getRevenueByRestaurant(params);
        logApi("GET /admin/dashboard/revenue-by-restaurant response", response);
        return response;
      };

      const results = await Promise.allSettled([
        fetchRestaurants(),
        fetchOrders(),
        fetchKpis(),
        fetchRevenueByRestaurant(),
      ]);

      const [restaurantsResult, ordersResult, kpisResult, revenueResult] = results;

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

      if (revenueResult.status === "fulfilled") {
        const revenueData = revenueResult.value;
        logApi("Revenue data received", revenueData);
        
        // Handle different response structures
        let restaurants = [];
        if (revenueData) {
          if (Array.isArray(revenueData)) {
            restaurants = revenueData;
          } else if (revenueData.restaurants && Array.isArray(revenueData.restaurants)) {
            restaurants = revenueData.restaurants;
          } else if (revenueData.data && revenueData.data.restaurants) {
            restaurants = revenueData.data.restaurants;
          }
        }
        
        logApi("Parsed restaurants data", restaurants);
        setRevenueByRestaurantData(restaurants);
      } else {
        const errPayload = serializeError(revenueResult.reason);
        setErrors((prev) => ({ ...prev, revenueByRestaurant: errPayload }));
        setRevenueByRestaurantData([]);
        message.warning("Không thể tải doanh thu theo nhà hàng");
        logApi("Revenue API error", errPayload);
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
  }, [restaurantFilter, timeFilter]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);


  // ================================
  // 🔥 CHART PROCESSING
  // ================================
  // Removed orderChart processing - only using PieChart for revenue from API

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

  // Tính toán doanh thu theo nhà hàng cho biểu đồ tròn từ API
  const revenueByRestaurant = useMemo(() => {
    console.log("[PieChart] revenueByRestaurantData:", revenueByRestaurantData);
    console.log("[PieChart] restaurants:", restaurants);
    
    if (revenueByRestaurantData.length === 0) {
      console.log("[PieChart] No revenue data available");
      return [];
    }
    
    if (restaurants.length === 0) {
      console.log("[PieChart] No restaurants data available");
      return [];
    }

    // Tạo map merchantId -> tên nhà hàng
    const restaurantMap = new Map();
    restaurants.forEach((r) => {
      if (r.merchantId) {
        restaurantMap.set(String(r.merchantId), r.name || `Nhà hàng ${r.merchantId}`);
      }
    });

    // Map dữ liệu từ API với tên nhà hàng
    const result = revenueByRestaurantData
      .map((item) => {
        const merchantId = item.merchantId || item.merchant_id;
        const revenue = item.revenue || item.value || 0;
        const revenueValue = typeof revenue === 'string' ? parseFloat(revenue) : Number(revenue);
        
        return {
          name: restaurantMap.get(String(merchantId)) || `Nhà hàng ${merchantId}`,
          value: revenueValue,
          merchantId: merchantId,
        };
      })
      .filter((item) => item.value > 0 && item.merchantId) // Chỉ lấy nhà hàng có doanh thu > 0 và có merchantId
      .sort((a, b) => b.value - a.value)
      .slice(0, 10); // Chỉ lấy top 10 nhà hàng

    console.log("[PieChart] Final mapped data:", result);
    return result;
  }, [revenueByRestaurantData, restaurants]);

  // Màu sắc cho biểu đồ tròn
  const COLORS = [
    "#0088FE",
    "#00C49F",
    "#FFBB28",
    "#FF8042",
    "#8884D8",
    "#82CA9D",
    "#FFC658",
    "#FF7C7C",
    "#8DD1E1",
    "#D084D0",
  ];

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
          CHART REVENUE BY RESTAURANT (PIE CHART)
      ========================= */}
      <div className="chart-container">
        <h3>📊 Doanh thu theo nhà hàng</h3>
        {revenueByRestaurant.length > 0 ? (
          <ResponsiveContainer width="100%" height={400}>
            <PieChart>
              <Pie
                data={revenueByRestaurant}
                cx="50%"
                cy="50%"
                labelLine={true}
                label={({ name, percent }) => {
                  // Chỉ hiển thị label nếu phần trăm > 5% để tránh quá nhiều text
                  if (percent > 0.05) {
                    return `${name}: ${(percent * 100).toFixed(1)}%`;
                  }
                  return "";
                }}
                outerRadius={120}
                fill="#8884d8"
                dataKey="value"
              >
                {revenueByRestaurant.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={COLORS[index % COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip
                formatter={(value) => `${Number(value).toLocaleString()}₫`}
              />
              <Legend
                formatter={(value, entry) => {
                  const data = revenueByRestaurant.find(
                    (item) => item.name === value
                  );
                  return data
                    ? `${value}: ${data.value.toLocaleString()}₫`
                    : value;
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        ) : (
          <div style={{ textAlign: "center", padding: "40px", color: "#999" }}>
            {revenueByRestaurantData.length === 0 
              ? "⏳ Đang tải dữ liệu doanh thu từ API..." 
              : "📊 Chưa có dữ liệu doanh thu để hiển thị"}
            <p style={{ fontSize: "12px", marginTop: "10px" }}>
              Dữ liệu: {revenueByRestaurantData.length} nhà hàng
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

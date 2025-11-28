import React, { useEffect, useState, useCallback } from "react";
import "./Dashboard.css";
import { collection, getDocs } from "firebase/firestore";
import { db } from "../../firebase";
import { message } from "antd";

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
    try {
      setLoading(true);

      // Fetch restaurants
      const restaurantsSnap = await getDocs(collection(db, "restaurants"));
      const restaurantList = restaurantsSnap.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      }));
      setRestaurants(restaurantList);

      // Fetch orders
      const ordersSnap = await getDocs(collection(db, "orders"));
      const oData = ordersSnap.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      }));

      let filteredOrders = oData;

      // If filter by restaurant
      if (restaurantFilter !== "all") {
        filteredOrders = filteredOrders.filter(
          (o) => String(o.restaurantId) === String(restaurantFilter)
        );
      }

      setOrders(filteredOrders);

      // === STATS ===
      const delivered = filteredOrders.filter((o) =>
        (o.status || "").toLowerCase().includes("đã giao")
      );
      const delivering = filteredOrders.filter((o) =>
        (o.status || "").toLowerCase().includes("đang giao")
      );
      const processing = filteredOrders.filter((o) =>
        (o.status || "").toLowerCase().includes("xử lý")
      );

      const totalRevenue = delivered.reduce(
        (sum, o) => sum + Number(o.total || o.totalPrice || 0),
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
      if (createdAt.seconds) return createdAt.seconds * 1000;
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
      if ((o.status || "").toLowerCase().includes("đã giao")) {
        daily[date].revenue += Number(o.total || o.totalPrice || 0);
      }

      daily[date].count += 1;
    });

    const sorted = Object.values(daily).sort(
      (a, b) => a.timestamp - b.timestamp
    );

    setChartData(sorted);
    setOrderChart(sorted);
  }, [orders, timeFilter, restaurantFilter]);


  if (loading) return <p>⏳ Đang tải dữ liệu...</p>;

  // ================================
  // 🔥 UI
  // ================================
  return (
    <div className="restaurant-dashboard">
      <h2>Dashboard Nhà hàng</h2>

      {/* SUMMARY CARDS */}
      <div className="cards">
        <div className="card purple">
          <h2>{stats.totalOrders}</h2>
          <p>Tổng đơn hàng</p>
        </div>

        <div className="card orange">
          <h2>{stats.processing}</h2>
          <p>Đang xử lý</p>
        </div>

        <div className="card green">
          <h2>{stats.delivering}</h2>
          <p>Đang giao</p>
        </div>

        <div className="card blue">
          <h2>{stats.totalRevenue.toLocaleString()}₫</h2>
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
              <option key={r.id} value={r.id}>
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

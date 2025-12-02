import React, { useEffect, useMemo, useState, useCallback } from "react";
import "./RestaurantDashboard.css";
import http from "../services/http";
import { useAuth } from "../context/AuthContext";
import { message } from "antd";
import { fetchRestaurantByMerchantId } from "../services/restaurants";
import { getDrones, assignDroneToOrder } from "../services/droneApi";
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
import { useNavigate } from "react-router-dom";

const toNumber = (value) => {
  if (value === null || value === undefined) return null;
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : null;
  }
  const parsed = parseFloat(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const geocodeAddress = async (address) => {
  if (!address) return null;
  try {
    const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(
      address
    )}`;
    const response = await fetch(url, {
      headers: {
        "Accept-Language": "vi",
        "User-Agent": "FastFoodDeliveryDashboard/1.0",
      },
    });
    if (!response.ok) {
      console.warn("Geocoding request failed", response.status);
      return null;
    }
    const payload = await response.json();
    if (!Array.isArray(payload) || payload.length === 0) {
      return null;
    }
    const lat = toNumber(payload[0]?.lat);
    const lng = toNumber(payload[0]?.lon);
    if (lat === null || lng === null) return null;
    return { lat, lng };
  } catch (err) {
    console.error("Geocoding error:", err);
    return null;
  }
};

export default function RestaurantDashboard() {
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const role = (currentUser?.role || "").toLowerCase();

  const [orders, setOrders] = useState([]);
  const [drones, setDrones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDrone, setSelectedDrone] = useState({});
  const [assigningDrone, setAssigningDrone] = useState({});

  const [statusFilter, setStatusFilter] = useState("all");
  const [droneFilter, setDroneFilter] = useState("all");
  const [timeFilter, setTimeFilter] = useState("all");

  const [stats, setStats] = useState({
    totalOrders: 0,
    totalRevenue: 0,
    delivering: 0,
    processing: 0,
    delivered: 0,
  });

  const [chartData, setChartData] = useState([]);

  // Trạng thái đang chọn / đang cập nhật cho từng đơn
  const [statusDraft, setStatusDraft] = useState({});
  const [updatingStatus, setUpdatingStatus] = useState({});

  const resolveRestaurantCoordinates = useCallback(async (orderDetail) => {
    console.log("🏪 [resolveRestaurantCoordinates] orderDetail:", orderDetail);
    
    // BƯỚC 1: Thử lấy từ restaurant object trong order detail (nếu có)
    const restaurant = orderDetail?.restaurant || {};
    let lat = toNumber(restaurant.lat) ?? toNumber(restaurant.latitude);
    let lng = toNumber(restaurant.lng) ?? toNumber(restaurant.longitude);
    
    if (lat !== null && lng !== null) {
      console.log("✅ [resolveRestaurantCoordinates] Found coordinates from restaurant object in order detail");
      return { lat, lng };
    }
    
    // BƯỚC 2: Lấy merchantId từ order và fetch restaurant info từ API
    const merchantId = orderDetail?.merchantId;
    if (merchantId) {
      console.log("🏪 [resolveRestaurantCoordinates] Fetching restaurant info for merchantId:", merchantId);
      try {
        const restaurantInfo = await fetchRestaurantByMerchantId(merchantId);
        console.log("🏪 [resolveRestaurantCoordinates] Restaurant info from API:", restaurantInfo);
        
        if (restaurantInfo) {
          // Thử lấy tọa độ từ restaurant info
          lat = toNumber(restaurantInfo.lat) ?? toNumber(restaurantInfo.latitude);
          lng = toNumber(restaurantInfo.lng) ?? toNumber(restaurantInfo.longitude);
          
          if (lat !== null && lng !== null) {
            console.log("✅ [resolveRestaurantCoordinates] Found coordinates from restaurant API:", { lat, lng });
            return { lat, lng };
          }
          
          // Nếu không có tọa độ, thử geocode từ address
          const restaurantAddress = restaurantInfo.address || "";
          if (restaurantAddress) {
            const cleanedAddress = restaurantAddress
              .replace(/,\s*Not Specified/gi, '')
              .replace(/,\s*,/g, ',')
              .replace(/,\s*$/, '')
              .trim();
            
            console.log("🏪 [resolveRestaurantCoordinates] Attempting geocode for restaurant address:", cleanedAddress);
            const coords = await geocodeAddress(cleanedAddress);
            if (coords) {
              console.log("✅ [resolveRestaurantCoordinates] Geocoded successfully from restaurant address:", coords);
              return coords;
            } else {
              console.warn("⚠️ [resolveRestaurantCoordinates] Geocoding failed for restaurant address:", cleanedAddress);
            }
          }
        }
      } catch (err) {
        console.error("❌ [resolveRestaurantCoordinates] Error fetching restaurant info:", err);
      }
    }
    
    // BƯỚC 3: Fallback - thử lấy từ restaurantAddress trong order detail (nếu có)
    const restaurantAddress = orderDetail?.restaurantAddress || orderDetail?.restaurant?.address || "";
    console.log("🏪 [resolveRestaurantCoordinates] Fallback - Restaurant address from order:", restaurantAddress);
    
    if (restaurantAddress) {
      const cleanedAddress = restaurantAddress
        .replace(/,\s*Not Specified/gi, '')
        .replace(/,\s*,/g, ',')
        .replace(/,\s*$/, '')
        .trim();
      
      if (cleanedAddress) {
        console.log("🏪 [resolveRestaurantCoordinates] Attempting geocode for fallback address:", cleanedAddress);
        const coords = await geocodeAddress(cleanedAddress);
        if (coords) {
          console.log("✅ [resolveRestaurantCoordinates] Geocoded successfully from fallback address:", coords);
          return coords;
        } else {
          console.warn("⚠️ [resolveRestaurantCoordinates] Geocoding failed for fallback address:", cleanedAddress);
        }
      }
    }
    
    console.error("❌ [resolveRestaurantCoordinates] Could not resolve restaurant coordinates");
    return null;
  }, []);

  const resolveDeliveryCoordinates = useCallback(async (orderDetail, fallbackAddress) => {
    console.log("📍 [resolveDeliveryCoordinates] orderDetail:", orderDetail);
    console.log("📍 [resolveDeliveryCoordinates] fallbackAddress:", fallbackAddress);
    
    const delivery = orderDetail?.deliveryAddress || {};
    console.log("📍 [resolveDeliveryCoordinates] delivery object:", delivery);
    
    let lat =
      toNumber(delivery.lat) ??
      toNumber(delivery.latitude) ??
      toNumber(delivery.deliveryLatitude);
    let lng =
      toNumber(delivery.lng) ??
      toNumber(delivery.longitude) ??
      toNumber(delivery.lon) ??
      toNumber(delivery.deliveryLongitude);

    console.log("📍 [resolveDeliveryCoordinates] Parsed lat/lng:", lat, lng);

    if (lat !== null && lng !== null) {
      console.log("✅ [resolveDeliveryCoordinates] Found coordinates from deliveryAddress");
      return { lat, lng };
    }

    // Try geocoding from fullAddress - clean up address first
    let addressToGeocode = delivery.fullAddress || fallbackAddress || orderDetail?.fullAddress;
    
    // Clean up address: remove "Not Specified" and normalize
    if (addressToGeocode) {
      addressToGeocode = addressToGeocode
        .replace(/,\s*Not Specified/gi, '') // Remove "Not Specified"
        .replace(/,\s*,/g, ',') // Remove double commas
        .replace(/,\s*$/, '') // Remove trailing comma
        .trim();
    }
    
    // Fallback: try addressLine1 if fullAddress is not good
    if (!addressToGeocode || addressToGeocode.includes('Not Specified')) {
      addressToGeocode = delivery.addressLine1 || orderDetail?.note;
    }
    
    console.log("📍 [resolveDeliveryCoordinates] Attempting geocode for:", addressToGeocode);
    
    if (addressToGeocode) {
      const coords = await geocodeAddress(addressToGeocode);
      if (coords) {
        console.log("✅ [resolveDeliveryCoordinates] Geocoded successfully:", coords);
        return coords;
      } else {
        console.warn("⚠️ [resolveDeliveryCoordinates] Geocoding failed for:", addressToGeocode);
      }
    }

    console.error("❌ [resolveDeliveryCoordinates] Could not resolve coordinates");
    return null;
  }, []);

  const fetchAll = useCallback(async () => {
    try {
      setLoading(true);

      const dronesPromise = getDrones().catch((err) => {
        console.error("Lỗi tải danh sách drone:", err);
        message.warning("Không thể tải danh sách drone");
        return [];
      });

      let ordersRes;

      // Fetch Orders
      if (role === "merchant" || role === "restaurant") {
        ordersRes = await http.get("/orders/merchants/me", { params: { size: 1000 } });
      } else {
        ordersRes = await http.get("/orders", { params: { size: 1000 } });
      }

      // Backend trả PageResponse trực tiếp, không bọc trong ApiResponse
      const oData = ordersRes.data?.content || [];
      setOrders(oData);

      const droneList = await dronesPromise;
      const normalizedDrones = (droneList || []).map((d) => ({
        ...d,
        name: d.serialNumber || d.model || `Drone #${d.id}`,
        battery: d.batteryLevel ?? 0,
      }));
      setDrones(normalizedDrones);

      const delivered = oData.filter((o) =>
        (o.status || "").toLowerCase().includes("delivered") || (o.status || "").toLowerCase().includes("đã giao")
      );
      const delivering = oData.filter((o) =>
        (o.status || "").toLowerCase().includes("delivering") || (o.status || "").toLowerCase().includes("đang giao")
      );
      const processing = oData.filter((o) =>
        (o.status || "").toLowerCase().includes("processing") || (o.status || "").toLowerCase().includes("pending") || (o.status || "").toLowerCase().includes("confirmed")
      );

      const totalRevenue = delivered.reduce(
        (sum, o) => sum + Number(o.grandTotal || 0),
        0
      );

      setStats({
        totalOrders: oData.length,
        totalRevenue,
        delivering: delivering.length,
        processing: processing.length,
        delivered: delivered.length,
      });

      const dailyStats = {};
      delivered.forEach((o) => {
        let dateObj;
        if (Array.isArray(o.createdAt)) {
          dateObj = new Date(o.createdAt[0], o.createdAt[1] - 1, o.createdAt[2], o.createdAt[3], o.createdAt[4], o.createdAt[5]);
        } else if (o.createdAt) {
          dateObj = new Date(o.createdAt);
        } else {
          dateObj = new Date();
        }

        const dateKey = dateObj.toLocaleDateString("vi-VN");
        const timestamp = dateObj.getTime();

        if (!dailyStats[dateKey]) {
          dailyStats[dateKey] = {
            date: dateKey,
            timestamp,
            revenue: 0,
            count: 0,
          };
        }
        dailyStats[dateKey].revenue += Number(o.grandTotal || 0);
        dailyStats[dateKey].count += 1;
      });

      setChartData(
        Object.values(dailyStats).sort((a, b) => a.timestamp - b.timestamp)
      );
    } catch (err) {
      console.error("Lỗi tải dữ liệu:", err);
      message.error("Không thể tải dữ liệu Dashboard");
    } finally {
      setLoading(false);
    }
  }, [currentUser, role]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  const handleAssignDrone = useCallback(
    async (order) => {
      console.log("🚁 [Drone] Bắt đầu gán drone cho order:", order?.id);
      const orderId = order?.id;
      if (!orderId) {
        console.warn("🚁 [Drone] Không có orderId");
        return;
      }
      
      const chosenDroneId = selectedDrone[orderId];
      console.log("🚁 [Drone] Drone đã chọn:", chosenDroneId);
      
      if (!chosenDroneId) {
        message.warning("Vui lòng chọn drone trước khi gán");
        return;
      }

      const drone = drones.find((d) => String(d.id) === String(chosenDroneId));
      if (!drone) {
        console.error("🚁 [Drone] Không tìm thấy drone với ID:", chosenDroneId);
        message.error("Không tìm thấy drone đã chọn");
        return;
      }

      console.log("🚁 [Drone] Drone được chọn:", drone);

      setAssigningDrone((prev) => ({ ...prev, [orderId]: true }));

      try {
        console.log("🚁 [Drone] Đang lấy chi tiết đơn hàng...");
        const detailRes = await http.get(`/orders/merchants/me/${orderId}`);
        const detail = detailRes.data?.data || detailRes.data;
        if (!detail) {
          throw new Error("Không thể tải chi tiết đơn hàng");
        }

        console.log("🚁 [Drone] Chi tiết đơn:", detail);
        console.log("🚁 [Drone] Order detail structure:", JSON.stringify(detail, null, 2));

        // BƯỚC 1: Lấy tọa độ nhà hàng (pickup location) - QUAN TRỌNG!
        console.log("🏪 [Drone] Đang resolve tọa độ nhà hàng (pickup location)...");
        const pickupCoords = await resolveRestaurantCoordinates(detail);
        console.log("🏪 [Drone] Restaurant (pickup) coordinates:", pickupCoords);

        if (!pickupCoords) {
          const useDefault = window.confirm(
            "Không thể xác định tọa độ nhà hàng từ địa chỉ.\n" +
            "Bạn có muốn sử dụng tọa độ mặc định (trung tâm TP.HCM) để tiếp tục?"
          );
          if (!useDefault) {
            message.error("Đã hủy gán drone. Vui lòng cập nhật địa chỉ nhà hàng có tọa độ GPS.");
            return;
          }
          message.warning("Đang sử dụng tọa độ mặc định cho nhà hàng.");
        }

        // BƯỚC 2: Lấy tọa độ giao hàng (delivery location)
        console.log("📍 [Drone] Đang resolve tọa độ giao hàng...");
        const deliveryCoords = await resolveDeliveryCoordinates(
          detail,
          order?.fullAddress || detail?.fullAddress
        );

        console.log("📍 [Drone] Delivery coordinates:", deliveryCoords);

        // Sử dụng tọa độ mặc định nếu không tìm được
        const defaultCoords = { lat: 10.776389, lng: 106.700806 }; // Trung tâm TP.HCM
        const finalPickupCoords = pickupCoords || defaultCoords;
        const finalDeliveryCoords = deliveryCoords || defaultCoords;

        if (!pickupCoords || !deliveryCoords) {
          const missing = [];
          if (!pickupCoords) missing.push("nhà hàng");
          if (!deliveryCoords) missing.push("giao hàng");
          message.warning(`Đang sử dụng tọa độ mặc định cho: ${missing.join(", ")}`);
        }

        // Tạo payload với tọa độ nhà hàng làm pickup location
        const payload = {
          orderId,
          pickupLatitude: finalPickupCoords.lat,
          pickupLongitude: finalPickupCoords.lng,
          deliveryLatitude: finalDeliveryCoords.lat,
          deliveryLongitude: finalDeliveryCoords.lng,
        };

        console.log("🚁 [Drone] Gọi API assignDroneToOrder với payload:", payload);
        console.log("🏪 [Drone] Pickup (nhà hàng):", finalPickupCoords);
        console.log("📍 [Drone] Delivery (giao hàng):", finalDeliveryCoords);
        
        const result = await assignDroneToOrder(payload);
        
        console.log("🚁 [Drone] Kết quả từ API:", result);

        message.success(
          `✅ Đã gán drone ${drone.serialNumber || drone.name || drone.id} cho đơn #${orderId}\n` +
          `🚁 Drone sẽ đi: Base → Nhà hàng → Giao hàng → Base`
        );
        setSelectedDrone((prev) => ({ ...prev, [orderId]: "" }));
        
        console.log("🚁 [Drone] Đang refresh dữ liệu...");
          await fetchAll();
          console.log("🚁 [Drone] Hoàn tất!");
        } catch (err) {
          console.error("❌ [Drone] Lỗi gán drone:", err);
          console.error("❌ [Drone] Error details:", {
            response: err?.response?.data,
            status: err?.response?.status,
            stack: err?.stack
          });
        
          let errorMessage =
            err?.response?.data?.message ||
            err?.response?.data?.error ||
            err?.message ||
            "Không thể gán drone. Vui lòng kiểm tra console để biết chi tiết.";

          if (err?.response?.status === 403) {
            errorMessage = "Bạn không có quyền thao tác trên đơn hàng này.";
          }
        
          message.error(`❌ ${errorMessage}`);
        } finally {
        setAssigningDrone((prev) => ({ ...prev, [orderId]: false }));
        }
      },
      [drones, selectedDrone, fetchAll, resolveRestaurantCoordinates, resolveDeliveryCoordinates]
    );

  const refreshData = async () => await fetchAll();

  const findDroneById = (id) => drones.find((d) => String(d.id) === String(id));

  const toMillis = (createdAt) => {
    if (!createdAt) return null;
    if (Array.isArray(createdAt)) {
      return new Date(createdAt[0], createdAt[1] - 1, createdAt[2], createdAt[3], createdAt[4], createdAt[5]).getTime();
    }
    const t = new Date(createdAt).getTime();
    return Number.isFinite(t) ? t : null;
  };

  const filteredOrders = useMemo(() => {
    const now = Date.now();

    const inTimeRange = (o) => {
      if (timeFilter === "all") return true;
      const ms = toMillis(o.createdAt);
      if (!ms) return false;
      if (timeFilter === "24h") return ms >= now - 24 * 60 * 60 * 1000;
      if (timeFilter === "3d") return ms >= now - 3 * 24 * 60 * 60 * 1000;
      if (timeFilter === "7d") return ms >= now - 7 * 24 * 60 * 60 * 1000;
      return true;
    };

    const normalizeStatus = (s = "") => s.toLowerCase();
    const matchStatus = (o) => {
      if (statusFilter === "all") return true;
      const s = normalizeStatus(o.status || "");
      if (statusFilter === "processing")
        return s.includes("xử lý") || s.includes("processing") || s === "confirmed" || s === "pending";
      if (statusFilter === "delivering")
        return s.includes("đang giao") || s.includes("delivering");
      if (statusFilter === "delivered")
        return s.includes("đã giao") || s.includes("delivered");
      if (statusFilter === "other") {
        const isProc = s.includes("xử lý") || s.includes("processing") || s === "confirmed" || s === "pending";
        const isDeliv = s.includes("đang giao") || s.includes("delivering");
        const isDone = s.includes("đã giao") || s.includes("delivered");
        return !isProc && !isDeliv && !isDone;
      }
      return true;
    };

    const matchDrone = (o) => {
      if (droneFilter === "all") return true;
      if (!o.droneId) return false;
      return String(o.droneId) === String(droneFilter);
    };

    return orders
      .filter(inTimeRange)
      .filter(matchStatus)
      .filter(matchDrone)
      .sort((a, b) => (toMillis(b.createdAt) ?? 0) - (toMillis(a.createdAt) ?? 0));
  }, [orders, statusFilter, droneFilter, timeFilter]);

  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 8;
  useEffect(() => setCurrentPage(1), [statusFilter, droneFilter, timeFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / pageSize));
  const paginatedOrders = filteredOrders.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  const formatStatusBadge = (status) => {
    if (!status) return <span className="badge other">—</span>;
    const s = status.toLowerCase();
    if (s.includes("giao") || s.includes("delivering")) {
      if (s === "đang giao" || s.includes("delivering"))
        return <span className="badge delivering">Đang giao</span>;
      if (s === "đã giao" || s.includes("delivered"))
        return <span className="badge done">Đã giao</span>;
    }
    if (s === "confirmed" || s.includes("xử lý") || s.includes("processing") || s === "pending")
      return <span className="badge pending">Đang xử lý</span>;
    return <span className="badge other">{status}</span>;
  };

  // ====== MERCHANT: Cập nhật trạng thái đơn ======
  const handleUpdateStatus = async (orderId, newStatus) => {
    try {
      setUpdatingStatus((prev) => ({ ...prev, [orderId]: true }));
      await http.put(`/orders/merchants/me/${orderId}/status`, {
        status: newStatus,
      });
      message.success("Cập nhật trạng thái đơn hàng thành công");
      await refreshData();
    } catch (err) {
      console.error("Lỗi cập nhật trạng thái đơn:", err);
      message.error("Không thể cập nhật trạng thái đơn hàng");
    } finally {
      setUpdatingStatus((prev) => ({ ...prev, [orderId]: false }));
    }
  };

  if (loading) return <p>⏳ Đang tải dữ liệu...</p>;

  return (
    <div className="restaurant-dashboard">

      <h2>Dashboard Nhà hàng</h2>

      {/* === CARDS === */}
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

      {/* === CHARTS === */}
      <div className="charts">
        <div className="chart-container">
          <h3>💰 Doanh thu theo ngày</h3>
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

        <div className="chart-container">
          <h3>📦 Số đơn hàng theo ngày</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="count" fill="#10b981" barSize={40} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* === FILTER BAR === */}
      <div className="filter-bar">
        <div className="filter-item">
          <label>Trạng thái</label>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="all">Tất cả</option>
            <option value="processing">Đang xử lý</option>
            <option value="delivering">Đang giao</option>
            <option value="delivered">Đã giao</option>
            <option value="other">Chờ xác nhận</option>
          </select>
        </div>

        <div className="filter-item">
          <label>Drone</label>
          <select value={droneFilter} onChange={(e) => setDroneFilter(e.target.value)}>
            <option value="all">Tất cả</option>
            {drones.map((d) => (
              <option key={d.id} value={d.id}>{d.name} ({d.battery}%)</option>
            ))}
          </select>
        </div>

        <div className="filter-item">
          <label>Thời gian</label>
          <select value={timeFilter} onChange={(e) => setTimeFilter(e.target.value)}>
            <option value="all">Tất cả</option>
            <option value="24h">24 giờ qua</option>
            <option value="3d">3 ngày qua</option>
            <option value="7d">7 ngày qua</option>
          </select>
        </div>

        <button
          className="btn reset"
          onClick={() => {
            setStatusFilter("all");
            setDroneFilter("all");
            setTimeFilter("all");
          }}
        >
          Xóa lọc
        </button>
      </div>

      {/* === TABLE META === */}
      <div className="table-meta">
        <span>
          Hiển thị: <b>{paginatedOrders.length}</b> / {filteredOrders.length} đơn
        </span>
      </div>

      {/* === TABLE === */}
      <table className="orders-table">
        <thead>
          <tr>
            <th>Mã</th>
            <th>Khách</th>
            <th>Địa chỉ</th>
            <th>Sản phẩm</th>
            <th>Thời gian</th>
            <th>Trạng thái</th>
            <th>Drone</th>
            <th>Hành động</th>
          </tr>
        </thead>

        <tbody>
          {paginatedOrders.map((order) => {
            const oStatus = order.status || "";
            const assignedDrone = order.droneId
              ? findDroneById(order.droneId)
              : null;
            const normalizedStatus = (oStatus || "").toLowerCase();
            const isDelivering =
              normalizedStatus.includes("đang giao") ||
              normalizedStatus.includes("delivering");
            const isDelivered =
              normalizedStatus.includes("đã giao") ||
              normalizedStatus.includes("delivered");
            const assignmentValue = selectedDrone[order.id] || "";

            const createdAtMs = toMillis(order.createdAt);
            const createdAtTxt = createdAtMs
              ? new Date(createdAtMs).toLocaleString()
              : "—";

            return (
              <tr key={order.id}>

                {/* MÃ ĐƠN */}
                <td
                  className="order-link"
                  onClick={() => navigate(`/restaurantadmin/order/${order.id}`)}
                >
                  #{order.id}
                </td>

                {/* KHÁCH */}
                <td>
                  <div className="cust-name">{order.receiverName}</div>
                  <div className="small">{order.receiverPhone}</div>
                </td>

                {/* ĐỊA CHỈ */}
                <td>{order.fullAddress}</td>

                {/* ⭐⭐⭐ SẢN PHẨM + GIÁ ⭐⭐⭐ */}
                <td>
                  <ul className="product-list">
                    <li>
                      <div>
                        <strong>{order.itemCount} sản phẩm</strong>
                      </div>
                      <span className="prod-price">
                        {Number(order.grandTotal).toLocaleString()}₫
                      </span>
                    </li>
                  </ul>
                </td>

                {/* THỜI GIAN */}
                <td>{createdAtTxt}</td>

                {/* STATUS BADGE */}
                <td>{formatStatusBadge(oStatus)}</td>

                {/* DRONE */}
                <td>
                  {isDelivered || isDelivering ? (
                    assignedDrone ? (
                      <div className="drone-status">
                        <strong>{assignedDrone.name}</strong>
                        <p>Pin: {assignedDrone.battery ?? "—"}%</p>
                      </div>
                    ) : (
                      <span>—</span>
                    )
                  ) : (
                    <div className="drone-assign">
                      <select
                        value={assignmentValue}
                        onChange={(e) => {
                          console.log("🚁 [Drone] Dropdown changed:", e.target.value, "for order:", order.id);
                          setSelectedDrone((prev) => ({
                            ...prev,
                            [order.id]: e.target.value,
                          }));
                        }}
                        disabled={!drones.length || assigningDrone[order.id]}
                      >
                        <option value="">{drones.length === 0 ? "Không có drone khả dụng" : "Chọn drone khả dụng"}</option>
                        {drones.map((d) => (
                          <option key={d.id} value={d.id}>
                            {d.name} ({d.battery ?? 0}%) - {d.state || "UNKNOWN"}
                          </option>
                        ))}
                      </select>
                      <button
                        className="btn primary"
                        disabled={
                          !assignmentValue || assigningDrone[order.id] || !drones.length
                        }
                        onClick={() => {
                          console.log("🚁 [Drone] Button clicked for order:", order.id);
                          console.log("🚁 [Drone] assignmentValue:", assignmentValue);
                          console.log("🚁 [Drone] assigningDrone:", assigningDrone[order.id]);
                          handleAssignDrone(order);
                        }}
                        title={
                          !drones.length
                            ? "Không có drone khả dụng"
                            : !assignmentValue
                            ? "Vui lòng chọn drone"
                            : assigningDrone[order.id]
                            ? "Đang xử lý..."
                            : "Gán drone cho đơn hàng này"
                        }
                      >
                        {assigningDrone[order.id] ? "Đang gán..." : "Gán drone"}
                      </button>
                    </div>
                  )}
                </td>

                {/* ACTION BUTTON */}
                <td>
                  {/* Chỉ cho phép merchant thao tác qua endpoint riêng */}
                  {role === "merchant" ? (
                    <div className="status-actions">
                      <select
                        value={statusDraft[order.id] || order.status || "PENDING"}
                        onChange={(e) =>
                          setStatusDraft((prev) => ({
                            ...prev,
                            [order.id]: e.target.value,
                          }))
                        }
                      >
                        <option value="PENDING">Chờ xác nhận</option>
                        <option value="CONFIRMED">Đã xác nhận</option>
                        <option value="PAID">Đã thanh toán</option>
                        <option value="SHIPPED">Đang giao</option>
                        <option value="DELIVERED">Đã giao</option>
                        <option value="CANCELLED">Hủy đơn</option>
                      </select>
                      <button
                        className="btn primary"
                        disabled={updatingStatus[order.id]}
                        onClick={() =>
                          handleUpdateStatus(
                            order.id,
                            statusDraft[order.id] || order.status || "PENDING"
                          )
                        }
                      >
                        {updatingStatus[order.id] ? "Đang lưu..." : "Cập nhật"}
                      </button>
                      <button
                        className="btn secondary"
                        type="button"
                        onClick={() => navigate(`/restaurantadmin/order/${order.id}`)}
                      >
                        Xem tracking
                      </button>
                    </div>
                  ) : (
                    <span>—</span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      {/* PAGINATION */}
      {totalPages > 1 && (
        <div className="orders-pagination">
          <button
            className="orders-page-btn"
            onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
            disabled={currentPage === 1}
          >
            ← Prev
          </button>

          {[...Array(totalPages)].map((_, i) => (
            <button
              key={i}
              className={`orders-page-btn ${currentPage === i + 1 ? "active" : ""
                }`}
              onClick={() => setCurrentPage(i + 1)}
            >
              {i + 1}
            </button>
          ))}

          <button
            className="orders-page-btn"
            onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
            disabled={currentPage === totalPages}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}

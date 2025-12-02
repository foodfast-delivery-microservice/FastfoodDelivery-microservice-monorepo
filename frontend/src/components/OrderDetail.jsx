import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getOrderById } from "../services/orders";
import { fetchProductById } from "../services/products";
import { getMissionByOrderId, getTrackingByOrderId } from "../services/droneApi";
import { MapContainer, TileLayer, Marker, Popup, Polyline } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./OrderDetail.css";

const formatMoney = (value, currency = "VND") => {
  const amount = Number(value || 0);
  if (Number.isNaN(amount)) return "0";

  if (currency === "VND") {
    return `${amount.toLocaleString("vi-VN")}₫`;
  }

  return amount.toLocaleString("vi-VN", {
    style: "currency",
    currency,
    minimumFractionDigits: 0,
  });
};

// Leaflet marker icons
const droneIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const pickupIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const deliveryIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const baseIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-grey.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const formatCoordinate = (value) => {
  if (value === null || value === undefined) return "—";
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed.toFixed(5) : "—";
};

export default function OrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [itemsWithImage, setItemsWithImage] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mission, setMission] = useState(null);
  const [tracking, setTracking] = useState(null);
  const [trackingLoading, setTrackingLoading] = useState(false);
  const [trackingError, setTrackingError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  useEffect(() => {
    const loadOrder = async () => {
      try {
        // Fetch order details from backend
        const orderData = await getOrderById(id);

        if (!orderData) {
          alert("Không tìm thấy đơn hàng!");
          navigate("/order-history");
          return;
        }

        // Check status for redirection
        const status = (orderData.status || "").toUpperCase();
        if (status === "DELIVERING" || status === "CONFIRMED") {
          navigate(`/waiting/${id}`);
          return;
        }

        const rawDelivery = orderData.deliveryAddress || orderData.shippingAddress || {};
        const deliveryParts = [
          rawDelivery.addressLine1,
          rawDelivery.ward,
          rawDelivery.district,
          rawDelivery.city,
        ].filter(Boolean);

        const formattedOrder = {
          ...orderData,
          date: orderData.createdAt ? new Date(orderData.createdAt) : null,
          code: orderData.orderCode || `ORD-${orderData.id}`,
          currency: orderData.currency || "VND",
          note: orderData.note || orderData.customerNote || "",
          // Ensure items array exists
          items: orderData.items || orderData.orderItems || [],
          subtotal:
            orderData.subtotal ?? orderData.totalBeforeDiscount ?? orderData.totalAmount ?? 0,
          shippingFee: orderData.shippingFee ?? orderData.deliveryFee ?? 0,
          discount: orderData.discount ?? orderData.totalDiscount ?? 0,
          grandTotal:
            orderData.grandTotal ?? orderData.totalAmount ?? orderData.total ?? 0,
          deliveryInfo: {
            receiverName:
              rawDelivery.receiverName ||
              orderData.customer?.name ||
              "Khách hàng",
            receiverPhone:
              rawDelivery.receiverPhone ||
              orderData.customer?.phone ||
              rawDelivery.phoneNumber ||
              "",
            addressLine1:
              rawDelivery.addressLine1 ||
              rawDelivery.address ||
              rawDelivery.addressLine2 ||
              "",
            fullAddress:
              deliveryParts.join(", ") ||
              rawDelivery.address ||
              rawDelivery.addressLine1 ||
              "N/A",
          },
        };

        setOrder(formattedOrder);

        // ⭐ Fetch ảnh từng món from Product Service
        const items = await Promise.all(
          formattedOrder.items.map(async (item) => {
            try {
              // item.productId is likely what we have from backend
              const productId = item.productId || item.id;
              const product = await fetchProductById(productId);

              return {
                ...item,
                // Use fetched product image, or fallback
                image: product?.img || product?.image || "/Images/Logo.png",
                name: product?.name || item.productName || item.name
              };
            } catch (err) {
              console.warn(`Failed to fetch product info for ${item.productId}`, err);
              return {
                ...item,
                image: "/Images/Logo.png"
              };
            }
          })
        );

        setItemsWithImage(items);
      } catch (err) {
        console.error("🔥 Lỗi load order:", err);
        // Handle 404 specifically if needed
        if (err?.response?.status === 404) {
          alert("Không tìm thấy đơn hàng!");
          navigate("/order-history");
        }
      } finally {
        setLoading(false);
      }
    };

    loadOrder();
  }, [id, navigate]);

  const fetchMission = useCallback(async () => {
    if (!id) return;
    try {
      const data = await getMissionByOrderId(id);
      setMission(data);
    } catch (err) {
      setMission(null);
    }
  }, [id]);

  const fetchTracking = useCallback(async () => {
    if (!id) return;
    try {
      setTrackingLoading(true);
      const data = await getTrackingByOrderId(id);
      setTracking(data);
      setTrackingError(null);
    } catch (err) {
      if (err?.response?.status === 404) {
        setTracking(null);
        setTrackingError("Chưa có nhiệm vụ drone nào cho đơn này.");
      } else {
        console.error("Lỗi tracking drone:", err);
        setTrackingError("Không thể tải thông tin tracking drone.");
      }
    } finally {
      setTrackingLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchMission();
    fetchTracking();
  }, [fetchMission, fetchTracking]);

  useEffect(() => {
    if (!autoRefresh) return;
    const timer = setInterval(() => {
      fetchTracking();
    }, 10000);
    return () => clearInterval(timer);
  }, [autoRefresh, fetchTracking]);

  // Calculate map center and bounds from mission/tracking data
  const mapData = useMemo(() => {
    if (!mission && !tracking) return null;

    const points = [];
    let center = null;

    // Drone current position (from tracking)
    if (tracking?.currentLatitude != null && tracking?.currentLongitude != null) {
      points.push([tracking.currentLatitude, tracking.currentLongitude]);
      center = [tracking.currentLatitude, tracking.currentLongitude];
    }

    // Pickup location (from mission)
    if (mission?.pickupLatitude != null && mission?.pickupLongitude != null) {
      points.push([mission.pickupLatitude, mission.pickupLongitude]);
      if (!center) center = [mission.pickupLatitude, mission.pickupLongitude];
    }

    // Delivery location (from mission)
    if (mission?.deliveryLatitude != null && mission?.deliveryLongitude != null) {
      points.push([mission.deliveryLatitude, mission.deliveryLongitude]);
    }

    // Base location (from tracking or mission - if available)
    if (tracking?.baseLatitude != null && tracking?.baseLongitude != null) {
      points.push([tracking.baseLatitude, tracking.baseLongitude]);
    }

    // Default center (Ho Chi Minh City)
    if (!center) center = [10.776389, 106.700806];

    return { center, points };
  }, [mission, tracking]);

  if (loading) return <p>⏳ Đang tải...</p>;
  if (!order) return <p>Không tìm thấy đơn hàng.</p>;

  return (
    <div className="order-detail-page">
      <div className="order-detail-card">

        <h2 className="order-detail-title">Chi tiết đơn hàng</h2>

        {/* ================== INFO ================== */}
        <div className="order-info enhanced">
          <div className="order-info-left">
            <p>
              <strong>Mã đơn:</strong> {order.code}
              <span className="order-id-hint"> (#{order.id})</span>
            </p>
            <p>
              <strong>Ngày đặt:</strong>{" "}
              {order.date
                ? order.date.toLocaleString("vi-VN", {
                    hour: "2-digit",
                    minute: "2-digit",
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                  })
                : "N/A"}
            </p>

            <p>
              <strong>Trạng thái:</strong>
              <span
                className={`status-tag ${order.status
                  .replace(/\s+/g, "-")
                  .toLowerCase()}`}
              >
                {order.status}
              </span>
            </p>

            <p>
              <strong>Nhà hàng:</strong> {order.restaurantName || "Đối tác"}
            </p>

            <p>
              <strong>Người nhận:</strong>{" "}
              {order.deliveryInfo?.receiverName || "Khách hàng"}
            </p>
            <p>
              <strong>Số điện thoại:</strong>{" "}
              {order.deliveryInfo?.receiverPhone || "N/A"}
            </p>
            <p>
              <strong>Giao đến:</strong>{" "}
              {order.deliveryInfo?.fullAddress ||
                order.deliveryInfo?.addressLine1 ||
                "N/A"}
            </p>

            {order.note && (
              <p>
                <strong>Ghi chú:</strong> {order.note}
              </p>
            )}
          </div>
        </div>

        {/* ================== ITEMS ================== */}
        <h3 className="section-title">Sản phẩm đã mua</h3>

        <ul className="order-items-list highlight">
          {itemsWithImage.map((item, idx) => (
            <li
              className="order-item highlight-item"
              key={idx}
              onClick={() => navigate(`/product-detail/${item.productId || item.id}`)}
              style={{ cursor: "pointer" }}
            >
              <img
                src={item.image}
                alt={item.name}
                className="order-item-image"
              />

              <div className="item-left">
                <span className="item-qty">{item.quantity}x</span>
                <span className="item-name">{item.name}</span>
              </div>

              <span className="item-price">
                {formatMoney(item.unitPrice * item.quantity, order.currency)}
              </span>
            </li>
          ))}
        </ul>


        {/* ================== TOTAL ================== */}
        <div className="order-payment-breakdown">
          <div className="order-payment-row">
            <span>Tạm tính</span>
            <span>{formatMoney(order.subtotal, order.currency)}</span>
          </div>
          {!!order.discount && (
            <div className="order-payment-row">
              <span>Giảm giá</span>
              <span>-{formatMoney(order.discount, order.currency)}</span>
            </div>
          )}
          <div className="order-payment-row">
            <span>Phí giao hàng</span>
            <span>{formatMoney(order.shippingFee, order.currency)}</span>
          </div>
          <div className="order-payment-total">
            <span>Tổng cộng</span>
            <strong>
              {formatMoney(order.grandTotal, order.currency)}
            </strong>
          </div>
        </div>

        {/* ================== DRONE TRACKING ================== */}
        {(mission || tracking) && (
          <div className="order-drone-tracking">
            <h3 className="section-title">🚁 Theo dõi Drone</h3>
            
            {trackingLoading ? (
              <p>Đang tải dữ liệu tracking...</p>
            ) : tracking ? (
              <div className="tracking-info-box">
                <ul className="tracking-info-list">
                  <li><b>Drone:</b> {tracking.droneSerialNumber || tracking.droneId || "—"}</li>
                  <li><b>Pin:</b> {tracking.batteryLevel ?? "—"}%</li>
                  <li><b>Trạng thái:</b> {tracking.status || "—"}</li>
                  <li>
                    <b>Vị trí:</b> Lat {formatCoordinate(tracking.currentLatitude)} / Lon{" "}
                    {formatCoordinate(tracking.currentLongitude)}
                  </li>
                  <li>
                    <b>ETA:</b>{" "}
                    {tracking.estimatedArrivalMinutes != null
                      ? `${tracking.estimatedArrivalMinutes} phút`
                      : "Đang tính toán"}
                  </li>
                </ul>
              </div>
            ) : (
              <p className="tracking-no-data">{trackingError || "Chưa có dữ liệu tracking."}</p>
            )}

            {/* Map Tracking */}
            {mapData && (
              <div className="tracking-map-container">
                <h4>🗺️ Bản đồ theo dõi</h4>
                <MapContainer
                  center={mapData.center}
                  zoom={13}
                  style={{ height: "400px", width: "100%", borderRadius: "8px", marginTop: "10px" }}
                  scrollWheelZoom={true}
                >
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />
                  
                  {/* Drone current position */}
                  {tracking?.currentLatitude != null && tracking?.currentLongitude != null && (
                    <Marker
                      position={[tracking.currentLatitude, tracking.currentLongitude]}
                      icon={droneIcon}
                    >
                      <Popup>
                        <strong>🚁 Drone hiện tại</strong>
                        <br />
                        {tracking.droneSerialNumber || tracking.droneId}
                        <br />
                        Pin: {tracking.batteryLevel ?? "—"}%
                        <br />
                        Trạng thái: {tracking.status || "—"}
                      </Popup>
                    </Marker>
                  )}

                  {/* Pickup location */}
                  {mission?.pickupLatitude != null && mission?.pickupLongitude != null && (
                    <Marker
                      position={[mission.pickupLatitude, mission.pickupLongitude]}
                      icon={pickupIcon}
                    >
                      <Popup>
                        <strong>📍 Điểm lấy hàng</strong>
                      </Popup>
                    </Marker>
                  )}

                  {/* Delivery location */}
                  {mission?.deliveryLatitude != null && mission?.deliveryLongitude != null && (
                    <Marker
                      position={[mission.deliveryLatitude, mission.deliveryLongitude]}
                      icon={deliveryIcon}
                    >
                      <Popup>
                        <strong>🏠 Điểm giao hàng</strong>
                        <br />
                        {order.deliveryInfo?.fullAddress || "Địa chỉ giao hàng"}
                      </Popup>
                    </Marker>
                  )}

                  {/* Base location */}
                  {tracking?.baseLatitude != null && tracking?.baseLongitude != null && (
                    <Marker
                      position={[tracking.baseLatitude, tracking.baseLongitude]}
                      icon={baseIcon}
                    >
                      <Popup>
                        <strong>🏠 Base của drone</strong>
                      </Popup>
                    </Marker>
                  )}

                  {/* Route lines - Vẽ đường đi đầy đủ */}
                  {mission && (
                    <>
                      {/* Luôn hiển thị đường đi đầy đủ: Base → Pickup → Delivery → Base */}
                      {tracking?.baseLatitude != null && 
                       mission.pickupLatitude != null && 
                       mission.deliveryLatitude != null && (
                        <>
                          {/* Đường Base → Pickup (xanh dương, nét đứt) */}
                          <Polyline
                            positions={[
                              [tracking.baseLatitude, tracking.baseLongitude],
                              [mission.pickupLatitude, mission.pickupLongitude]
                            ]}
                            color="blue"
                            dashArray="10, 5"
                            weight={2}
                            opacity={0.5}
                          />
                          
                          {/* Đường Pickup → Delivery (xanh lá, nét đứt) */}
                          <Polyline
                            positions={[
                              [mission.pickupLatitude, mission.pickupLongitude],
                              [mission.deliveryLatitude, mission.deliveryLongitude]
                            ]}
                            color="green"
                            dashArray="10, 5"
                            weight={2}
                            opacity={0.5}
                          />
                          
                          {/* Đường Delivery → Base (xám, nét đứt) */}
                          <Polyline
                            positions={[
                              [mission.deliveryLatitude, mission.deliveryLongitude],
                              [tracking.baseLatitude, tracking.baseLongitude]
                            ]}
                            color="grey"
                            dashArray="10, 5"
                            weight={2}
                            opacity={0.5}
                          />
                        </>
                      )}

                      {/* Đường đi thực tế: Current Position → Next Destination (đường đậm, real-time) */}
                      {tracking?.currentLatitude != null && tracking?.currentLongitude != null && (
                        <>
                          {/* ASSIGNED: Đang đi từ Base đến Pickup (đỏ) */}
                          {tracking.status === "ASSIGNED" && mission.pickupLatitude != null && (
                            <Polyline
                              positions={[
                                [tracking.currentLatitude, tracking.currentLongitude],
                                [mission.pickupLatitude, mission.pickupLongitude]
                              ]}
                              color="red"
                              weight={5}
                              opacity={0.9}
                            />
                          )}
                          
                          {/* IN_PROGRESS: Đang đi từ Pickup đến Delivery (cam) */}
                          {tracking.status === "IN_PROGRESS" && mission.deliveryLatitude != null && (
                            <Polyline
                              positions={[
                                [tracking.currentLatitude, tracking.currentLongitude],
                                [mission.deliveryLatitude, mission.deliveryLongitude]
                              ]}
                              color="orange"
                              weight={5}
                              opacity={0.9}
                            />
                          )}
                          
                          {/* RETURNING: Đang quay về Base (tím) */}
                          {tracking.status === "RETURNING" && tracking.baseLatitude != null && (
                            <Polyline
                              positions={[
                                [tracking.currentLatitude, tracking.currentLongitude],
                                [tracking.baseLatitude, tracking.baseLongitude]
                              ]}
                              color="purple"
                              weight={5}
                              opacity={0.9}
                            />
                          )}
                        </>
                      )}
                    </>
                  )}
                </MapContainer>
              </div>
            )}

            <div className="tracking-actions">
              <button
                className="btn-refresh"
                onClick={fetchTracking}
                disabled={trackingLoading}
              >
                {trackingLoading ? "Đang cập nhật..." : "🔄 Làm mới"}
              </button>
              <label className="tracking-auto">
                <input
                  type="checkbox"
                  checked={autoRefresh}
                  onChange={(e) => setAutoRefresh(e.target.checked)}
                />
                Tự động cập nhật mỗi 10s
              </label>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}

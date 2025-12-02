import React, { useEffect, useState, useCallback, useRef, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getOrderById } from "../services/orders";
import { createPayment, getPaymentByOrderId } from "../services/payments";
import { getMissionByOrderId, getTrackingByOrderId } from "../services/droneApi";
import { MapContainer, TileLayer, Marker, Popup, Polyline } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./WaitingForConfirmation.css";

const ORDER_PAYMENT_META_PREFIX = "orderPaymentMeta_";
const POLLING_INTERVAL_MS = 5000;

const STATUS_MESSAGES = {
  PENDING: "Đơn hàng đã được tạo và đang chờ nhà hàng xác nhận.",
  CONFIRMED: "Nhà hàng đã xác nhận. Nếu bạn chọn thanh toán QR, vui lòng hoàn tất thanh toán.",
  PAID: "Đơn hàng đã thanh toán và sẽ được chuẩn bị giao.",
  SHIPPED: "Đơn hàng đang được giao đến bạn.",
  DELIVERED: "Đơn hàng đã được giao thành công.",
  CANCELLED: "Đơn hàng đã bị hủy. Vui lòng không thanh toán.",
  REFUNDED: "Đơn hàng đã được hoàn tiền."
};

const parseAmount = (value) => {
  if (value === null || value === undefined) return 0;
  const num = typeof value === "number" ? value : parseFloat(value);
  return Number.isNaN(num) ? 0 : num;
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

const buildFullAddress = (deliveryAddress) => {
  if (!deliveryAddress) return "";
  if (deliveryAddress.fullAddress) return deliveryAddress.fullAddress;
  return [
    deliveryAddress.addressLine1,
    deliveryAddress.ward,
    deliveryAddress.district,
    deliveryAddress.city
  ]
    .filter(Boolean)
    .join(", ")
    .replace(/^,\s*|,\s*$/g, "");
};

const mapOrderResponse = (data) => {
  if (!data) return null;
  return {
    id: data.id,
    orderCode: data.orderCode,
    status: data.status,
    merchantId: data.merchantId,
    userId: data.userId,
    currency: data.currency || "VND",
    grandTotal: parseAmount(data.grandTotal),
    subtotal: parseAmount(data.subtotal),
    discount: parseAmount(data.discount),
    shippingFee: parseAmount(data.shippingFee),
    note: data.note,
    createdAt: data.createdAt,
    receiverName: data.deliveryAddress?.receiverName,
    receiverPhone: data.deliveryAddress?.receiverPhone,
    fullAddress: buildFullAddress(data.deliveryAddress),
    restaurantName: data.restaurantName || "Nhà hàng",
    items:
      data.orderItems?.map((item) => ({
        id: item.id,
        productId: item.productId,
        name: item.productName,
        price: parseAmount(item.unitPrice),
        quantity: item.quantity,
        lineTotal: parseAmount(item.lineTotal)
      })) || []
  };
};

const readPaymentMeta = (orderId) => {
  if (!orderId) return null;
  try {
    const raw = localStorage.getItem(`${ORDER_PAYMENT_META_PREFIX}${orderId}`);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    console.warn("Không thể đọc thông tin phương thức thanh toán từ localStorage:", error);
    return null;
  }
};

export default function WaitingForConfirmation() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [paymentMeta, setPaymentMeta] = useState(() => readPaymentMeta(orderId));
  const [paymentStatus, setPaymentStatus] = useState(null);
  const [isInitiatingPayment, setIsInitiatingPayment] = useState(false);
  const [pollingError, setPollingError] = useState(null);
  const [mission, setMission] = useState(null);
  const [tracking, setTracking] = useState(null);
  const [trackingLoading, setTrackingLoading] = useState(false);
  const [trackingError, setTrackingError] = useState(null);
  const hasLoadedOnceRef = useRef(false);

  const fetchPaymentStatus = useCallback(async () => {
    try {
      const payment = await getPaymentByOrderId(orderId);
      setPaymentStatus(payment?.status || null);
    } catch (error) {
      setPaymentStatus(null);
    }
  }, [orderId]);

  const fetchLatestOrder = useCallback(
    async ({ silent = false, ignoreError = false } = {}) => {
      if (!orderId) {
        if (!silent) {
          alert("Không có ID đơn hàng");
          navigate("/");
        }
        return null;
      }

      if (!silent) {
        setLoading(true);
      }

      try {
        const data = await getOrderById(orderId);
        if (!data) {
          throw new Error("Không tìm thấy thông tin đơn hàng");
        }

        const mappedOrder = mapOrderResponse(data);
        setOrder(mappedOrder);
        setPollingError(null);
        hasLoadedOnceRef.current = true;

        const needsPaymentStatus =
          paymentMeta?.method === "qr" ||
          ["PAID", "REFUNDED", "SHIPPED", "DELIVERED"].includes(mappedOrder.status);
        if (needsPaymentStatus) {
          await fetchPaymentStatus();
        }

        if (mappedOrder.status === "PAID") {
          try {
            localStorage.removeItem(`${ORDER_PAYMENT_META_PREFIX}${orderId}`);
            setPaymentMeta(null);
          } catch (storageError) {
            console.warn("Không thể xóa thông tin phương thức thanh toán:", storageError);
          }
        }

        return mappedOrder;
      } catch (error) {
        setPollingError(error);
        if (!ignoreError) {
          const message = error?.response?.data?.message || error?.message || "Không thể tải đơn hàng";
          alert(message);
          if (!hasLoadedOnceRef.current) {
            navigate("/");
          }
        }
        throw error;
      } finally {
        if (!silent) {
          setLoading(false);
        }
      }
    },
    [orderId, navigate, paymentMeta?.method, fetchPaymentStatus]
  );

  useEffect(() => {
    setPaymentMeta(readPaymentMeta(orderId));
  }, [orderId]);

  useEffect(() => {
    let intervalId;
    fetchLatestOrder().catch(() => {});

    intervalId = setInterval(() => {
      fetchLatestOrder({ silent: true, ignoreError: true }).catch(() => {});
    }, POLLING_INTERVAL_MS);

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [fetchLatestOrder]);

  // Fetch mission and tracking data for drone delivery
  const fetchMission = useCallback(async () => {
    if (!orderId) return;
    try {
      const data = await getMissionByOrderId(orderId);
      setMission(data);
    } catch (err) {
      setMission(null);
    }
  }, [orderId]);

  const fetchTracking = useCallback(async () => {
    if (!orderId) return;
    try {
      setTrackingLoading(true);
      const data = await getTrackingByOrderId(orderId);
      setTracking(data);
      setTrackingError(null);
    } catch (err) {
      if (err?.response?.status === 404) {
        setTracking(null);
        setTrackingError(null); // Don't show error if no mission yet
      } else {
        console.error("Lỗi tracking drone:", err);
        setTrackingError("Không thể tải thông tin tracking drone.");
      }
    } finally {
      setTrackingLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    // Only fetch if order status is DELIVERING or PROCESSING (case-insensitive)
    if (order) {
      const normalizedStatus = (order.status || "").toUpperCase();
      if (normalizedStatus === "DELIVERING" || normalizedStatus === "PROCESSING") {
        fetchMission();
        fetchTracking();
        
        // Auto-refresh tracking every 10 seconds
        const timer = setInterval(() => {
          fetchTracking();
        }, 10000);
        
        return () => clearInterval(timer);
      }
    }
  }, [order, fetchMission, fetchTracking]);

  // Calculate map center and bounds from mission/tracking data
  // Map should show if we have mission (even without tracking yet) or tracking
  const mapData = useMemo(() => {
    // If we have mission, show map even without tracking (drone might not have started yet)
    if (!mission && !tracking) return null;

    const points = [];
    let center = null;

    // Drone current position (from tracking)
    if (tracking?.currentLatitude != null && tracking?.currentLongitude != null) {
      points.push([tracking.currentLatitude, tracking.currentLongitude]);
      center = [tracking.currentLatitude, tracking.currentLongitude];
    }

    // Pickup location (from mission) - prioritize this if we have mission
    if (mission?.pickupLatitude != null && mission?.pickupLongitude != null) {
      points.push([mission.pickupLatitude, mission.pickupLongitude]);
      if (!center) center = [mission.pickupLatitude, mission.pickupLongitude];
    }

    // Delivery location (from mission)
    if (mission?.deliveryLatitude != null && mission?.deliveryLongitude != null) {
      points.push([mission.deliveryLatitude, mission.deliveryLongitude]);
      // Use delivery as center if we don't have drone position yet
      if (!center) center = [mission.deliveryLatitude, mission.deliveryLongitude];
    }

    // Base location (from tracking or mission - if available)
    if (tracking?.baseLatitude != null && tracking?.baseLongitude != null) {
      points.push([tracking.baseLatitude, tracking.baseLongitude]);
    }

    // Default center (Ho Chi Minh City) - only if we have no other points
    if (!center) center = [10.776389, 106.700806];

    return { center, points };
  }, [mission, tracking]);

  // Normalize order status to uppercase for comparison (must be before early returns)
  const normalizedStatus = order ? (order.status || "").toUpperCase() : "";
  const isDeliveringOrProcessing = normalizedStatus === "DELIVERING" || normalizedStatus === "PROCESSING";

  // Debug: Log status and map data (must be before early returns - Rules of Hooks)
  useEffect(() => {
    if (order) {
      console.log("🔍 [WaitingForConfirmation] Order status:", order.status, "→ Normalized:", normalizedStatus);
      console.log("🔍 [WaitingForConfirmation] isDeliveringOrProcessing:", isDeliveringOrProcessing);
      console.log("🔍 [WaitingForConfirmation] Mission:", mission ? "✅" : "❌");
      console.log("🔍 [WaitingForConfirmation] Tracking:", tracking ? "✅" : "❌");
      console.log("🔍 [WaitingForConfirmation] MapData:", mapData ? "✅" : "❌");
    }
  }, [order, normalizedStatus, isDeliveringOrProcessing, mission, tracking, mapData]);

  // Handler functions (can be before early returns)
  const handleInitiatePayment = async () => {
    if (!order) return;
    setIsInitiatingPayment(true);
    try {
      const payload = {
        orderId: Number(order.id),
        grandTotal: Number(order.grandTotal),
        currency: order.currency || "VND"
      };

      if (order.merchantId) {
        payload.merchantId = Number(order.merchantId);
      }

      await createPayment(payload);
      alert("Yêu cầu thanh toán đã được gửi. Hệ thống sẽ cập nhật trạng thái ngay khi có phản hồi.");
      await fetchLatestOrder({ silent: true, ignoreError: true });
    } catch (error) {
      console.error("❌ Thanh toán thất bại:", error);
      const message =
        error?.response?.data?.message ||
        error?.message ||
        "Thanh toán thất bại, vui lòng thử lại!";
      alert(message);
    } finally {
      setIsInitiatingPayment(false);
    }
  };

  const handleReceived = async () => {
    alert("Tính năng xác nhận đã nhận hàng đang được cập nhật.");
  };

  if (loading && !hasLoadedOnceRef.current) {
    return <p>⏳ Đang tải đơn hàng...</p>;
  }

  if (!order) {
    return <p>❌ Không tìm thấy đơn hàng.</p>;
  }

  const statusMessage = order ? (STATUS_MESSAGES[order.status] || "Đơn hàng đang được xử lý.") : "";
  const statusClass = order ? `wfc-status-badge status-${(order.status || "").toLowerCase()}` : "";
  const paymentAmount =
    Number(paymentMeta?.grandTotal) || (order ? Number(order.grandTotal) : 0) || 0;

  const renderPaymentSection = () => {
    if (!paymentMeta || paymentMeta.method !== "qr") {
      return (
        <div className="wfc-payment-note">
          <strong>Phương thức thanh toán:</strong> Thanh toán khi nhận hàng (COD)
        </div>
      );
    }

    if (order.status === "PENDING") {
      return (
        <div className="wfc-payment-panel info">
          <h4>Thanh toán QR</h4>
          <p>Đơn hàng đang chờ nhà hàng xác nhận. QR sẽ xuất hiện ngay sau khi xác nhận.</p>
        </div>
      );
    }

    if (order.status === "CONFIRMED") {
      return (
        <div className="wfc-payment-panel">
          <h4>Thanh toán bằng quét mã QR</h4>
          <img
            src={`https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=ORDER-${order.id}`}
            alt="QR Code"
            className="wfc-qr-image"
          />
          <p>
            Số tiền cần thanh toán: <strong>{paymentAmount.toLocaleString("vi-VN")}₫</strong>
          </p>
          <button
            className="wfc-btn-pay"
            onClick={handleInitiatePayment}
            disabled={isInitiatingPayment}
          >
            {isInitiatingPayment ? "Đang gửi yêu cầu..." : "Tôi đã thanh toán"}
          </button>
          <small>Sau khi quét mã và bấm xác nhận, hệ thống sẽ gửi yêu cầu thanh toán đến cổng.</small>
        </div>
      );
    }

    if (["PAID", "SHIPPED", "DELIVERED", "REFUNDED"].includes(order.status)) {
      return (
        <div className="wfc-payment-panel success">
          <h4>Trạng thái thanh toán</h4>
          <p>
            Cổng thanh toán:{" "}
            <strong>{paymentStatus || "Đang cập nhật..."}</strong>
          </p>
          <p>Chúng tôi sẽ tiếp tục cập nhật tiến trình giao hàng bên dưới.</p>
        </div>
      );
    }

    if (order.status === "CANCELLED") {
      return (
        <div className="wfc-payment-panel warning">
          <h4>Đơn hàng đã bị hủy</h4>
          <p>Vui lòng không tiếp tục thanh toán cho đơn hàng này.</p>
        </div>
      );
    }

    return null;
  };

  return (
    <div className="wfc-page">
      <h2>📦 Theo dõi đơn hàng #{order.orderCode || order.id}</h2>

      <div className="wfc-container" style={{ display: "block", maxWidth: "800px", margin: "0 auto" }}>
        <div className="wfc-info-panel" style={{ width: "100%" }}>
          <div className="wfc-info-content">
            <h3>Chi tiết đơn hàng</h3>

            {pollingError && (
              <div className="wfc-alert">
                Không thể cập nhật trạng thái mới nhất. Hệ thống sẽ thử lại tự động.
              </div>
            )}

            <div className="wfc-status-section">
              <span className={statusClass}>{order.status}</span>
              <p className="wfc-status-message">{statusMessage}</p>
              <small>Cập nhật tự động mỗi {POLLING_INTERVAL_MS / 1000} giây.</small>
            </div>

            {renderPaymentSection()}

            <div className="wfc-box">
              <h4 className="wfc-box-title">Khách hàng</h4>
              <div className="wfc-detail-row">
                <span>Tên:</span>
                <span>{order.receiverName}</span>
              </div>
              <div className="wfc-detail-row">
                <span>Số điện thoại:</span>
                <span className="wfc-text-wrap">{order.receiverPhone}</span>
              </div>
              <div className="wfc-detail-row">
                <span>Địa chỉ:</span>
                <span className="wfc-text-wrap">{order.fullAddress}</span>
              </div>
            </div>

            <div className="wfc-item-list">
              <strong>Món ăn:</strong>
              {order.items && order.items.length > 0 ? (
                <ul>
                  {order.items.map((item) => (
                    <li key={item.id || item.productId} className="wfc-item-row">
                      <span>
                        {item.quantity} x {item.name}
                      </span>
                      <span className="wfc-item-price">
                        {(item.price * item.quantity).toLocaleString("vi-VN")}₫
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p>
                  <i>Chi tiết sản phẩm không có sẵn.</i>
                </p>
              )}
            </div>

            <p className="wfc-section-title">
              Tổng tiền: <strong>{order.grandTotal?.toLocaleString("vi-VN")}₫</strong>
            </p>

            {/* Drone Tracking Map */}
            {isDeliveringOrProcessing ? (
              mapData ? (
                <div className="wfc-tracking-box" style={{ marginTop: "20px" }}>
                  <h4 style={{ marginBottom: "10px" }}>🗺️ Bản đồ theo dõi drone</h4>
                  {trackingLoading && <p>⏳ Đang tải thông tin tracking...</p>}
                  {trackingError && <p className="wfc-alert">{trackingError}</p>}
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
                          <strong>🏠 Base</strong>
                        </Popup>
                      </Marker>
                    )}

                    {/* Route lines - Vẽ lộ trình đường đi */}
                    {mission && (
                      <>
                        {/* Lộ trình đầy đủ: Base → Pickup → Delivery → Base (nét đứt, màu nhạt) */}
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

                        {/* Đường đi thực tế của drone (nét đậm, real-time) */}
                        {tracking?.currentLatitude != null && 
                         tracking?.currentLongitude != null && (
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

                            {/* Nếu không có status cụ thể, vẽ đường từ vị trí hiện tại đến điểm gần nhất */}
                            {!["ASSIGNED", "IN_PROGRESS", "RETURNING"].includes(tracking.status) && (
                              <>
                                {/* Đường từ Base đến vị trí hiện tại (nếu có base) */}
                                {tracking?.baseLatitude != null && (
                                  <Polyline
                                    positions={[
                                      [tracking.baseLatitude, tracking.baseLongitude],
                                      [tracking.currentLatitude, tracking.currentLongitude]
                                    ]}
                                    color="red"
                                    weight={3}
                                    opacity={0.7}
                                  />
                                )}
                              </>
                            )}
                          </>
                        )}
                      </>
                    )}
                  </MapContainer>
                  {tracking && (
                    <div style={{ marginTop: "10px", fontSize: "14px", color: "#666" }}>
                      <p>🚁 Drone: {tracking.droneSerialNumber || tracking.droneId || "—"}</p>
                      <p>🔋 Pin: {tracking.batteryLevel ?? "—"}%</p>
                      <p>⏱️ ETA: {tracking.estimatedArrivalMinutes ?? "—"} phút</p>
                    </div>
                  )}
                </div>
              ) : (
                <div className="wfc-tracking-box" style={{ marginTop: "20px", padding: "15px", textAlign: "center" }}>
                  <p>📍 Chưa có drone được gán cho đơn hàng này.</p>
                  {order.status === "PROCESSING" && (
                    <p style={{ fontSize: "12px", color: "#666", marginTop: "5px" }}>
                      Nhà hàng đang chuẩn bị món. Drone sẽ được gán sau khi món sẵn sàng.
                    </p>
                  )}
                </div>
              )
            ) : null}

            {(order.status === "delivering" || order.status === "Đang giao") && (
              <button className="wfc-btn-received" onClick={handleReceived} style={{ marginTop: "20px" }}>
                ✅ Đã nhận hàng
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
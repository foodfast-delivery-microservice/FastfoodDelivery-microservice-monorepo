import React, { useEffect, useState, useCallback, useMemo } from "react";
import "./RestaurantOrderDetail.css";
import http from "../services/http";
import { useParams, useNavigate } from "react-router-dom";
import { getMissionByOrderId, getTrackingByOrderId } from "../services/droneApi";
import { MapContainer, TileLayer, Marker, Popup, Polyline } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

const toDate = (input) => {
    if (!input) return null;
    if (Array.isArray(input)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = input;
        return new Date(year, month - 1, day, hour, minute, second);
    }
    const parsed = new Date(input);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const formatDateTime = (input) => {
    const date = toDate(input);
    return date ? date.toLocaleString("vi-VN") : "—";
};

const formatCoordinate = (value) => {
    if (value === null || value === undefined) return "—";
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed.toFixed(5) : "—";
};

const formatDistance = (value) => {
    if (value === null || value === undefined) return "—";
    const parsed = Number(value);
    return Number.isFinite(parsed) ? `${parsed.toFixed(2)} km` : "—";
};

const formatDuration = (value) => {
    if (value === null || value === undefined) return "—";
    const parsed = Number(value);
    return Number.isFinite(parsed) ? `${parsed} phút` : "—";
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

export default function RestaurantOrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [order, setOrder] = useState(null);
    const [mission, setMission] = useState(null);
    const [tracking, setTracking] = useState(null);
    const [trackingLoading, setTrackingLoading] = useState(false);
    const [trackingError, setTrackingError] = useState(null);
    const [autoRefresh, setAutoRefresh] = useState(true);

    const fetchOrder = useCallback(async () => {
        try {
            const res = await http.get(`/orders/${id}`);
            const data = res.data?.data;

            if (!data) {
                alert("Đơn hàng không tồn tại");
                navigate("/restaurant/orders");
                return;
            }

            setOrder({
                ...data,
                total: data.grandTotal,
                // Map items if available, or handle empty
                items: data.items || []
            });
        } catch (err) {
            console.error("Lỗi load order:", err);
        }
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
        fetchOrder();
    }, [fetchOrder]);

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

    if (!order) return <p>⏳ Đang tải chi tiết đơn...</p>;

    return (
        <div className="order-detail-container">
            <button className="back-btn" onClick={() => navigate(-1)}>
                ⬅ Quay lại
            </button>

            <h2>📦 Chi tiết đơn hàng #{order.orderCode || order.id}</h2>

            <div className="info-box">
                <h3> Khách hàng</h3>
                <p><b>Tên:</b> {order.receiverName}</p>
                <p><b>SĐT:</b> {order.receiverPhone}</p>
                <p><b>Địa chỉ:</b> {order.fullAddress}</p>
            </div>

            <div className="info-box">
                <h3> Sản phẩm</h3>
                {order.items && order.items.length > 0 ? (
                    <ul className="order-items-list">
                        {order.items.map((i, index) => (
                            <li key={index} className="order-item">
                                <span className="item-name">{i.name}</span>
                                <span className="item-qty">× {i.quantity}</span>
                                <span className="item-price">{i.price?.toLocaleString()}₫</span>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p><i>Chi tiết sản phẩm không có sẵn.</i></p>
                )}
            </div>

            <div className="info-box">
                <h3> Thanh toán</h3>
                <p><b>Tổng tiền:</b> {(order.total || 0).toLocaleString()}₫</p>
                <p><b>Trạng thái:</b> {order.status}</p>
            </div>

            <div className="info-box">
                <h3>🚀 Nhiệm vụ & Theo dõi Drone</h3>

                {mission ? (
                    <div className="mission-grid">
                        <div>
                            <span className="label">Mission ID</span>
                            <strong>#{mission.id}</strong>
                        </div>
                        <div>
                            <span className="label">Drone</span>
                            <strong>{mission.droneSerialNumber || mission.droneId || "—"}</strong>
                        </div>
                        <div>
                            <span className="label">Trạng thái</span>
                            <span className={`badge ${String(mission.status || "").toLowerCase()}`}>
                                {mission.status || "—"}
                            </span>
                        </div>
                        <div>
                            <span className="label">Khoảng cách</span>
                            <span>{formatDistance(mission.distanceKm)}</span>
                        </div>
                        <div>
                            <span className="label">Dự kiến</span>
                            <span>{formatDuration(mission.estimatedDurationMinutes)}</span>
                        </div>
                        <div>
                            <span className="label">Bắt đầu</span>
                            <span>{formatDateTime(mission.startedAt)}</span>
                        </div>
                        <div>
                            <span className="label">Hoàn thành</span>
                            <span>{formatDateTime(mission.completedAt)}</span>
                        </div>
                    </div>
                ) : (
                    <p>Chưa có nhiệm vụ drone nào được khởi tạo cho đơn hàng này.</p>
                )}

                <div className="tracking-section">
                    <h4>📡 Tracking thời gian thực</h4>
                    {trackingLoading ? (
                        <p>Đang tải dữ liệu tracking...</p>
                    ) : tracking ? (
                        <ul className="tracking-info">
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
                    ) : (
                        <p>{trackingError || "Chưa có dữ liệu tracking."}</p>
                    )}

                    {/* Map Tracking */}
                    {mapData && (
                        <div className="tracking-map-container">
                            <h5>🗺️ Bản đồ theo dõi</h5>
                            <MapContainer
                                center={mapData.center}
                                zoom={13}
                                style={{ height: "400px", width: "100%", borderRadius: "8px" }}
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

                                {/* Base location (if available) */}
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

                                {/* Route line: Base -> Pickup -> Delivery -> Base */}
                                {mission && tracking && (
                                    <>
                                        {/* Base to Pickup (if drone hasn't reached pickup yet) */}
                                        {tracking.status === "ASSIGNED" && mission.pickupLatitude != null && tracking.baseLatitude != null && (
                                            <Polyline
                                                positions={[
                                                    [tracking.baseLatitude, tracking.baseLongitude],
                                                    [mission.pickupLatitude, mission.pickupLongitude]
                                                ]}
                                                color="blue"
                                                dashArray="5, 5"
                                            />
                                        )}
                                        
                                        {/* Pickup to Delivery (if drone is delivering) */}
                                        {tracking.status === "IN_PROGRESS" && mission.pickupLatitude != null && mission.deliveryLatitude != null && (
                                            <Polyline
                                                positions={[
                                                    [mission.pickupLatitude, mission.pickupLongitude],
                                                    [mission.deliveryLatitude, mission.deliveryLongitude]
                                                ]}
                                                color="green"
                                                dashArray="5, 5"
                                            />
                                        )}

                                        {/* Current position to target */}
                                        {tracking.currentLatitude != null && (
                                            <>
                                                {tracking.status === "ASSIGNED" && mission.pickupLatitude != null && (
                                                    <Polyline
                                                        positions={[
                                                            [tracking.currentLatitude, tracking.currentLongitude],
                                                            [mission.pickupLatitude, mission.pickupLongitude]
                                                        ]}
                                                        color="red"
                                                        weight={3}
                                                    />
                                                )}
                                                {tracking.status === "IN_PROGRESS" && mission.deliveryLatitude != null && (
                                                    <Polyline
                                                        positions={[
                                                            [tracking.currentLatitude, tracking.currentLongitude],
                                                            [mission.deliveryLatitude, mission.deliveryLongitude]
                                                        ]}
                                                        color="orange"
                                                        weight={3}
                                                    />
                                                )}
                                                {tracking.status === "COMPLETED" && tracking.baseLatitude != null && (
                                                    <Polyline
                                                        positions={[
                                                            [tracking.currentLatitude, tracking.currentLongitude],
                                                            [tracking.baseLatitude, tracking.baseLongitude]
                                                        ]}
                                                        color="grey"
                                                        dashArray="5, 5"
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
                            className="btn primary"
                            onClick={fetchTracking}
                            disabled={trackingLoading}
                        >
                            {trackingLoading ? "Đang cập nhật..." : "Làm mới"}
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
                    <p className="tracking-note">
                        Việc gán drone hiện được thực hiện trong bảng đơn hàng của Dashboard nhà hàng.
                    </p>
                </div>
            </div>
        </div>
    );
}

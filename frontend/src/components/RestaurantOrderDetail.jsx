import React, { useEffect, useState, useCallback } from "react";
import "./RestaurantOrderDetail.css";
import http from "../services/http";
import { useParams, useNavigate } from "react-router-dom";
import OrderTrackingMap from "./OrderTrackingMap";

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

export default function RestaurantOrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [order, setOrder] = useState(null);
    const [mission, setMission] = useState(null);
    const [missionReady, setMissionReady] = useState(false);
    const [tracking, setTracking] = useState(null);
    const [trackingReady, setTrackingReady] = useState(false);
    const [trackingError, setTrackingError] = useState(null);

    const handleMissionUpdate = useCallback((data) => {
        setMissionReady(true);
        setMission(data);
    }, []);

    const handleTrackingUpdate = useCallback((data) => {
        setTrackingReady(true);
        setTracking(data);
        setTrackingError(null);
    }, []);

    const handleTrackingError = useCallback((message) => {
        setTrackingReady(true);
        setTrackingError(message || null);
    }, []);

    const fetchOrder = useCallback(async () => {
        try {
            const res = await http.get(`/orders/merchants/me/${id}`);
            const data = res.data?.data || res.data;

            if (!data) {
                alert("Đơn hàng không tồn tại");
                navigate("/restaurant/orders");
                return;
            }
            const rawDelivery = data.deliveryAddress || {};
            const deliveryParts = [
                rawDelivery.addressLine1,
                rawDelivery.ward,
                rawDelivery.district,
                rawDelivery.city,
            ].filter(Boolean);

            const mappedItems = (data.orderItems || []).map((it) => ({
                id: it.id,
                productId: it.productId,
                name: it.productName,
                quantity: it.quantity,
                price: Number(it.unitPrice ?? 0),
                lineTotal: Number(it.lineTotal ?? 0),
            }));

            setOrder({
                ...data,
                receiverName:
                    rawDelivery.receiverName ||
                    data.customerName ||
                    data.userName ||
                    "Khách hàng",
                receiverPhone:
                    rawDelivery.receiverPhone ||
                    rawDelivery.phoneNumber ||
                    data.customerPhone ||
                    "",
                fullAddress:
                    deliveryParts.join(", ") ||
                    rawDelivery.address ||
                    rawDelivery.addressLine1 ||
                    data.fullAddress ||
                    "N/A",
                total: Number(data.grandTotal ?? 0),
                items: mappedItems,
            });
        } catch (err) {
            console.error("Lỗi load order:", err);
            if (err?.response?.status === 403) {
                alert("Bạn không có quyền xem đơn hàng này.");
                navigate("/restaurantadmin");
            } else if (err?.response?.status === 404) {
                alert("Đơn hàng không tồn tại.");
                navigate("/restaurantadmin");
            } else {
                alert("Không thể tải đơn hàng. Vui lòng thử lại sau.");
            }
        }
    }, [id, navigate]);

    useEffect(() => {
        fetchOrder();
    }, [fetchOrder]);

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
                    <p>
                        {missionReady
                            ? "Chưa có nhiệm vụ drone nào được khởi tạo cho đơn hàng này."
                            : "Đang tải thông tin nhiệm vụ drone..."}
                    </p>
                )}

                <div className="tracking-section">
                    <h4>📡 Tracking thời gian thực</h4>
                    {!trackingReady ? (
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

                    <OrderTrackingMap
                        orderId={id}
                        deliveryAddress={order.fullAddress}
                        showHeader={false}
                        onMissionChange={handleMissionUpdate}
                        onTrackingChange={handleTrackingUpdate}
                        onTrackingError={handleTrackingError}
                    />
                </div>
            </div>
        </div>
    );
}

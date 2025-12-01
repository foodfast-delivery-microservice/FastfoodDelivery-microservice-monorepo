import React, { useEffect, useState, useCallback } from "react";
import "./RestaurantOrderDetail.css";
import http from "../services/http";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function RestaurantOrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { currentUser } = useAuth();

    const [order, setOrder] = useState(null);
    const [drones, setDrones] = useState([]);
    const [selectedDrone, setSelectedDrone] = useState("");

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

    // Drone functionality is disabled for now
    /*
    const fetchDrones = useCallback(async () => {
        try {
            if (!currentUser.restaurantId) return;

            const q = query(
                collection(db, "drones"),
                where("restaurantId", "==", currentUser.restaurantId)
            );
            const snap = await getDocs(q);

            setDrones(
                snap.docs.map((d) => ({ id: d.id, ...d.data() }))
            );
        } catch (err) {
            console.error("Lỗi load drone:", err);
        }
    }, [currentUser]);
    */

    useEffect(() => {
        fetchOrder();
        // fetchDrones();
    }, [fetchOrder]);

    const assignDrone = async () => {
        alert("Tính năng gán Drone đang được phát triển trên hệ thống mới.");
        /*
        if (!selectedDrone) {
            alert("Vui lòng chọn drone");
            return;
        }

        try {
            const droneDoc = doc(db, "drones", selectedDrone);
            const orderDoc = doc(db, "orders", order.id);

            await updateDoc(droneDoc, {
                status: "Đang giao",
                currentOrderId: order.id,
            });

            await updateDoc(orderDoc, {
                droneId: selectedDrone,
                status: "Đang giao",
            });

            alert("🚁 Đã gán drone giao đơn!");
            fetchOrder();
        } catch (err) {
            console.error("Lỗi gán drone:", err);
            alert("Không thể gán drone");
        }
        */
    };

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

            {/* ==== GÁN DRONE ==== */}
            <div className="info-box">
                <h3> Giao bằng drone</h3>

                {order.status === "delivered" || order.status === "Đã giao" ? (
                    <p>Đơn đã giao xong.</p>
                ) : (
                    <>
                        <p><i>Tính năng đang bảo trì</i></p>
                        <select
                            value={selectedDrone}
                            onChange={(e) => setSelectedDrone(e.target.value)}
                            disabled
                        >
                            <option value="">-- Chọn drone --</option>
                            {drones.map((d) => (
                                <option key={d.id} value={d.id}>
                                    {d.name} ({d.battery}%)
                                </option>
                            ))}
                        </select>

                        <button className="assign-btn" onClick={assignDrone} disabled>
                            Gán drone đi giao
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}

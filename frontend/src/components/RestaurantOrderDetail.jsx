import React, { useEffect, useState, useCallback } from "react";
import "./RestaurantOrderDetail.css";
import {
    doc,
    getDoc,
    getDocs,
    updateDoc,
    collection,
    query,
    where,
} from "firebase/firestore";
import { db } from "../firebase";
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
            const snap = await getDoc(doc(db, "orders", id));
            if (!snap.exists()) {
                alert("Đơn hàng không tồn tại");
                navigate("/restaurant/orders");
                return;
            }
            setOrder({ id, ...snap.data() });
        } catch (err) {
            console.error("Lỗi load order:", err);
        }
    }, [id, navigate]);

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

    useEffect(() => {
        fetchOrder();
        fetchDrones();
    }, [fetchOrder, fetchDrones]);

    const assignDrone = async () => {
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
    };

    if (!order) return <p>⏳ Đang tải chi tiết đơn...</p>;

    return (
        <div className="order-detail-container">
            <button className="back-btn" onClick={() => navigate(-1)}>
                ⬅ Quay lại
            </button>

            <h2>📦 Chi tiết đơn hàng #{order.id}</h2>

            <div className="info-box">
                <h3> Khách hàng</h3>
                <p><b>Tên:</b> {order.customer?.name}</p>
                <p><b>SĐT:</b> {order.customer?.phone}</p>
                <p><b>Địa chỉ:</b> {order.customer?.address}</p>
            </div>

            <div className="info-box">
                <h3> Sản phẩm</h3>
                <ul className="order-items-list">
                    {order.items?.map((i) => (
                        <li key={i.id} className="order-item">
                            <span className="item-name">{i.name}</span>
                            <span className="item-qty">× {i.quantity}</span>
                            <span className="item-price">{i.price?.toLocaleString()}₫</span>

                        </li>
                    ))}
                </ul>

            </div>

            <div className="info-box">
                <h3> Thanh toán</h3>
                <p><b>Tổng tiền:</b> {(order.total || 0).toLocaleString()}₫</p>
                <p><b>Trạng thái:</b> {order.status}</p>
            </div>

            {/* ==== GÁN DRONE ==== */}
            <div className="info-box">
                <h3> Giao bằng drone</h3>

                {order.status === "Đã giao" ? (
                    <p>Đơn đã giao xong.</p>
                ) : (
                    <>
                        <select
                            value={selectedDrone}
                            onChange={(e) => setSelectedDrone(e.target.value)}
                        >
                            <option value="">-- Chọn drone --</option>
                            {drones.map((d) => (
                                <option key={d.id} value={d.id}>
                                    {d.name} ({d.battery}%)
                                </option>
                            ))}
                        </select>

                        <button className="assign-btn" onClick={assignDrone}>
                            Gán drone đi giao
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}

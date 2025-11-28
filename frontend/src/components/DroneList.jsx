import React, { useCallback, useEffect, useState } from "react";
import { collection, getDocs } from "firebase/firestore";
import { db } from "../firebase";
import { useAuth } from "../context/AuthContext"; // ✅ thêm dòng này
import "./DroneList.css";

export default function DroneList() {
  const { currentUser } = useAuth(); // ✅ lấy user đang đăng nhập
  const role = (currentUser?.role || "").toLowerCase();
  const [drones, setDrones] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchAll = useCallback(async () => {
    try {
      const [dronesSnap, ordersSnap] = await Promise.all([
        getDocs(collection(db, "drones")),
        getDocs(collection(db, "orders")),
      ]);

      const dronesData = dronesSnap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      const ordersData = ordersSnap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      // ✅ Nếu là admin → xem tất cả
      // ✅ Nếu là restaurant → chỉ thấy drone thuộc nhà hàng mình
      const filteredDrones =
        role === "admin"
          ? dronesData
          : dronesData.filter(
              (d) => d.restaurantId === currentUser?.restaurantId
            );

      setDrones(filteredDrones);
      setOrders(ordersData);
    } catch (err) {
      console.error("❌ Lỗi lấy dữ liệu Firestore:", err);
    } finally {
      setLoading(false);
    }
  }, [currentUser?.restaurantId, role]);

  useEffect(() => {
    fetchAll();
    const interval = setInterval(fetchAll, 5000);
    return () => clearInterval(interval);
  }, [fetchAll]);

  const renderStatus = (d) => {
    if (d.currentOrderId)
      return <span className="drone-status busy">🔵 Đang giao</span>;
    if (d.status === "Rảnh")
      return <span className="drone-status idle">🟢 Rảnh</span>;
    if (d.status === "Bảo trì")
      return <span className="drone-status maintenance">🔴 Bảo trì</span>;
    return <span className="drone-status">{d.status || "Không rõ"}</span>;
  };

  const findOrder = (orderId) => orders.find((o) => o.id === orderId);

  if (loading) return <p className="drone-loading">⏳ Đang tải danh sách drone...</p>;

  return (
    <div className="drone-container">
      <h2 className="drone-title">
        🚁 Danh sách Drone{" "}
        {role === "admin"
          ? "(Tất cả)"
          : ``}
      </h2>

      {drones.length === 0 ? (
        <p className="drone-empty">Không có drone nào thuộc nhà hàng của bạn.</p>
      ) : (
        <table className="drone-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên</th>
              <th>Trạng thái</th>
              <th>Pin</th>
              <th>Đang giao đơn</th>
              <th>Đích đến</th>
            </tr>
          </thead>
          <tbody>
            {drones.map((d) => {
              const order = d.currentOrderId ? findOrder(d.currentOrderId) : null;
              return (
                <tr key={d.id}>
                  <td>#{d.id}</td>
                  <td>{d.name || "Không tên"}</td>
                  <td>{renderStatus(d)}</td>
                  <td>{d.battery ?? "?"}%</td>
                  <td>
                    {order ? (
                      <div>
                        <strong>#{order.id}</strong> —{" "}
                        {order.customer?.name || "Khách không rõ"}
                      </div>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td>{d.destination || "—"}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}

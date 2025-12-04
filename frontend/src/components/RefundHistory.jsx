import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listMyOrders } from "../services/orders";
import "./RefundHistory.css";

const formatMoney = (value = 0) => {
  const amount = Number(value || 0);
  if (Number.isNaN(amount)) return "0₫";
  return `${amount.toLocaleString("vi-VN")}₫`;
};

export default function RefundHistory() {
  const [refundOrders, setRefundOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchRefunds = async () => {
      try {
        const orders = await listMyOrders();
        const refunds = orders.filter(
          (order) => (order.status || "").toUpperCase() === "REFUNDED"
        );
        setRefundOrders(refunds);
      } catch (err) {
        console.error("Không thể tải danh sách hoàn tiền:", err);
        setError("Không thể tải danh sách hoàn tiền. Vui lòng thử lại sau.");
      } finally {
        setLoading(false);
      }
    };

    fetchRefunds();
  }, []);

  if (loading) {
    return <p className="refund-history-loading">Đang tải lịch sử hoàn tiền...</p>;
  }

  if (error) {
    return <p className="refund-history-error">{error}</p>;
  }

  return (
    <div className="refund-history-page">
      <h2>Lịch sử hoàn tiền</h2>
      {refundOrders.length === 0 ? (
        <p className="refund-history-empty">
          Bạn chưa có yêu cầu hoàn tiền nào được xử lý.
        </p>
      ) : (
        <ul className="refund-history-list">
          {refundOrders.map((order) => {
            const lastUpdated =
              order.updatedAt || order.refundedAt || order.createdAt;
            const formattedUpdate = lastUpdated
              ? new Date(lastUpdated).toLocaleString("vi-VN")
              : "N/A";
            return (
              <li key={order.id} className="refund-card">
                <div className="refund-card-header">
                  <div>
                    <h3>Đơn #{String(order.orderCode || order.id)}</h3>
                    <span>Cập nhật: {formattedUpdate}</span>
                  </div>
                  <span className="refund-pill">REFUNDED</span>
                </div>

                <div className="refund-card-body">
                  <p>
                    Số tiền hoàn:
                    <strong>
                      {" "}
                      {formatMoney(order.grandTotal || order.total)}
                    </strong>
                  </p>
                  {order.receiverName && (
                    <p>
                      Người nhận: <strong>{order.receiverName}</strong>
                    </p>
                  )}
                </div>

                <div className="refund-card-actions">
                  <button
                    className="refund-detail-btn"
                    onClick={() => navigate(`/order/${order.id}`)}
                  >
                    Xem chi tiết đơn
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}


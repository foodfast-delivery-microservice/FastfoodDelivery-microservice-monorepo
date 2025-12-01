// src/components/OrderHistory.jsx
import React, { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { listMyOrders } from "../services/orders";
import "./OrderHistory.css";

function OrderHistory() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchOrders = async () => {
      const userId = currentUser?.id ?? currentUser?.uid;
      if (!userId) {
        console.log("⛔ Không có currentUser hoặc chưa đăng nhập");
        setLoading(false);
        return;
      }

      try {
        // Fetch orders from backend API
        const userOrders = await listMyOrders();

        // Map API response to component state structure
        // Backend OrderListResponse có: id, orderCode, status, grandTotal, itemCount, createdAt, receiverName, receiverPhone, fullAddress
        // KHÔNG có items list (chỉ có itemCount)
        console.log('📦 [OrderHistory] Raw orders from backend:', userOrders);
        
        const formattedOrders = userOrders.map((order) => {
          console.log('📦 [OrderHistory] Processing order:', order);
          return {
          id: String(order.id), // Ensure ID is string for routing
            orderCode: order.orderCode,
            status: order.status || "PENDING",
            // Map grandTotal to total/totalAmount for compatibility
            total: order.grandTotal ? parseFloat(order.grandTotal) : null,
            totalAmount: order.grandTotal ? parseFloat(order.grandTotal) : null,
            grandTotal: order.grandTotal ? parseFloat(order.grandTotal) : null,
            // Backend OrderListResponse giờ đã có orderItems
            items: order.orderItems || order.items || [],
            itemCount: order.itemCount || 0,
            // Delivery info
            receiverName: order.receiverName,
            receiverPhone: order.receiverPhone,
            fullAddress: order.fullAddress,
            // Date
            createdAt: order.createdAt,
          date: order.createdAt ? new Date(order.createdAt) : null,
            // Other fields
            currency: order.currency,
            subtotal: order.subtotal ? parseFloat(order.subtotal) : null,
            discount: order.discount ? parseFloat(order.discount) : null,
            shippingFee: order.shippingFee ? parseFloat(order.shippingFee) : null,
          };
        });
        
        console.log('📦 [OrderHistory] Formatted orders:', formattedOrders);

        // Ưu tiên hiển thị trạng thái
        // Backend status might be uppercase (PENDING, DELIVERING, DELIVERED)
        // Map to Vietnamese display if needed, or handle in UI
        const sortedOrders = formattedOrders.sort((a, b) => {
          // Simple date sort desc
          return new Date(b.createdAt) - new Date(a.createdAt);
        });

        setOrders(sortedOrders);
      } catch (err) {
        console.error("🔥 Lỗi lấy lịch sử đơn hàng:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, [currentUser]);

  if (loading) return <p className="loading-message">⏳ Đang tải lịch sử đơn hàng...</p>;

  return (
    <div className="order-history-page">
      <h2>Lịch sử đơn hàng</h2>

      {orders.length === 0 ? (
        <p className="no-orders-message">Bạn chưa có đơn hàng nào.</p>
      ) : (
        <ul className="orders-list">
          {orders.map((order) => (
            <li
              key={order.id}
              className="order-card"
              onClick={() => {
                // Adjust status check for backend values (usually uppercase EN)
                const s = (order.status || "").toUpperCase();
                if (s === "DELIVERING" || s === "PENDING" || s === "CONFIRMED") {
                  // Map backend status to frontend route logic if needed
                  // For now assuming /waiting is for active orders
                  // You might need to adjust this logic based on exact backend status enums
                  if (s === "DELIVERING" || s === "CONFIRMED") {
                    navigate(`/waiting/${order.id}`);
                  } else {
                    navigate(`/order/${order.id}`);
                  }
                } else {
                  navigate(`/order/${order.id}`);
                }
              }}
              style={{ cursor: "pointer" }}
            >
              <div className="order-header">
                <h3>Đơn hàng #{String(order.id).substring(0, 8)}...</h3>
                <span>
                  {order.date ? order.date.toLocaleDateString("vi-VN") : "N/A"}
                </span>
              </div>

              <div className="order-body">
                {order.items && order.items.length > 0 ? (
                <ul className="order-items-list">
                    {order.items.map((item, index) => (
                    <li
                      key={`${order.id}-${index}`}
                      className="order-item clickable-item"
                      onClick={(e) => {
                        e.stopPropagation();       // tránh trigger click vào order-card
                        navigate(`/product-detail/${item.productId || item.id}`);
                      }}
                    >
                      <span>{item.quantity}x {item.productName || item.name}</span>
                        <span>{(item.unitPrice || item.price) * item.quantity ? 
                          ((item.unitPrice || item.price) * item.quantity).toLocaleString() + "₫" : 
                          (item.lineTotal ? parseFloat(item.lineTotal).toLocaleString() + "₫" : "N/A")}</span>
                    </li>
                  ))}
                </ul>
                ) : (
                  <p style={{ padding: "10px", color: "#666", fontStyle: "italic" }}>
                    {order.itemCount ? `${order.itemCount} sản phẩm` : "Không có thông tin sản phẩm"}
                  </p>
                )}
              </div>

              <div className="order-footer">
                <div className="order-total">
                  <strong>
                    Tổng tiền:{" "}
                    {order?.grandTotal || order?.totalAmount || order?.total
                      ? (order.grandTotal || order.totalAmount || order.total).toLocaleString("vi-VN") + "₫"
                      : "Đang cập nhật"}
                  </strong>
                </div>

                <div className="order-status">
                  Trạng thái:
                  <span
                    className={`status-tag ${order.status
                      ?.replace(/\s+/g, "-")
                      .toLowerCase()}`}
                  >
                    {order.status}
                  </span>
                </div>

                {/* Nút theo dõi đơn */}
                {/* Adjust conditions for backend status */}
                {["CONFIRMED", "DELIVERING", "PENDING"].includes((order.status || "").toUpperCase()) && (
                  <button
                    className="track-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/waiting/${order.id}`);
                    }}
                  >
                    Theo dõi đơn
                  </button>
                )}

                {/* Nút xem chi tiết */}
                {!["CONFIRMED", "DELIVERING", "PENDING"].includes((order.status || "").toUpperCase()) && (
                  <button
                    className="detail-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/order/${order.id}`);
                    }}
                  >
                    Xem chi tiết
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default OrderHistory;

import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getOrderById } from "../services/orders";
import { fetchProductById } from "../services/products";
import "./OrderDetail.css";

export default function OrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [itemsWithImage, setItemsWithImage] = useState([]);
  const [loading, setLoading] = useState(true);

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

        const formattedOrder = {
          ...orderData,
          date: orderData.createdAt ? new Date(orderData.createdAt) : null,
          // Ensure items array exists
          items: orderData.items || orderData.orderItems || []
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

  if (loading) return <p>⏳ Đang tải...</p>;
  if (!order) return <p>Không tìm thấy đơn hàng.</p>;

  return (
    <div className="order-detail-page">
      <div className="order-detail-card">

        <h2 className="order-detail-title">Chi tiết đơn hàng</h2>

        {/* ================== INFO ================== */}
        <div className="order-info enhanced">
          <div className="order-info-left">
            <p><strong>Mã đơn:</strong> #{order.id}</p>
            <p><strong>Ngày đặt:</strong> {order.date?.toLocaleString("vi-VN")}</p>

            <p>
              <strong>Trạng thái:</strong>
              <span className={`status-tag ${order.status.replace(/\s+/g, "-").toLowerCase()}`}>
                {order.status}
              </span>
            </p>

            <p><strong>Nhà hàng:</strong> {order.restaurantName || "Đối tác"}</p>

            <p><strong>Giao đến:</strong> {order.deliveryAddress?.address || order.customer?.address || "N/A"}</p>
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
                {(item.unitPrice * item.quantity).toLocaleString()}₫
              </span>
            </li>
          ))}
        </ul>


        {/* ================== TOTAL ================== */}
        <div className="order-total-section">
          Tổng tiền: <strong>{(order.totalAmount || order.total)?.toLocaleString()}₫</strong>
        </div>

      </div>
    </div>
  );
}

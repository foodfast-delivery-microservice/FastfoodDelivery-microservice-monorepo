import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getOrderById } from "../services/orders";
import { fetchProductById } from "../services/products";
import OrderTrackingMap from "./OrderTrackingMap";
import "./OrderDetail.css";

const buildImageUrl = (src) => {
  if (!src) return null;
  if (src.startsWith?.("http")) return src;
  const base = "http://localhost:8089";
  return src.startsWith("/") ? `${base}${src}` : `${base}/${src}`;
};

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
                image: buildImageUrl(
                  product?.imageUrl ||
                  product?.img ||
                  product?.image
                ) || "/Images/Logo.png",
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
        <div className="order-drone-tracking">
          <OrderTrackingMap
            orderId={id}
            deliveryAddress={order.deliveryInfo?.fullAddress}
            showHeader
          />
        </div>

      </div>
    </div>
  );
}

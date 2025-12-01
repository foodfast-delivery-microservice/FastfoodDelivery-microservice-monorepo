import React, { useEffect, useState, useCallback, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getOrderById } from "../services/orders";
import { createPayment, getPaymentByOrderId } from "../services/payments";
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

  const statusMessage = STATUS_MESSAGES[order.status] || "Đơn hàng đang được xử lý.";
  const statusClass = `wfc-status-badge status-${(order.status || "").toLowerCase()}`;
  const paymentAmount =
    Number(paymentMeta?.grandTotal) || Number(order.grandTotal) || 0;

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

            <div
              className="wfc-tracking-box"
              style={{ marginTop: "20px", padding: "15px", textAlign: "center" }}
            >
              <p>📍 Tính năng theo dõi trực tiếp trên bản đồ đang được bảo trì.</p>
            </div>

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
import React from "react";

const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return "N/A";
  return date.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const normalizeStatus = (status) => (status || "").toUpperCase();

export default function RefundInfo({
  order,
  summary,
  recentRefund,
  loading,
  error,
  formatMoney,
}) {
  const orderStatus = normalizeStatus(order?.status);
  const paymentStatus = normalizeStatus(summary?.paymentStatus);
  const recentStatus = normalizeStatus(recentRefund?.status);
  const isRefunded =
    orderStatus === "REFUNDED" ||
    paymentStatus === "REFUNDED" ||
    recentStatus === "REFUNDED";

  const effectiveAmount =
    recentRefund?.refundAmount ?? summary?.amount ?? order?.grandTotal ?? 0;
  const currency = summary?.currency || order?.currency || "VND";
  const updatedAt = recentRefund?.refundedAt || summary?.updatedAt;

  return (
    <div className="refund-info-card">
      <div className="refund-info-header">
        <h4>Trạng thái hoàn tiền</h4>
        <span
          className={`refund-status-pill ${
            isRefunded ? "pill-success" : "pill-idle"
          }`}
        >
          {isRefunded ? "ĐÃ HOÀN TIỀN" : "CHƯA HOÀN"}
        </span>
      </div>

      {loading && <p className="refund-info-note">Đang tải thông tin thanh toán...</p>}
      {error && <p className="refund-info-error">{error}</p>}

      {!loading && !error && (
        <>
          {!summary && (
            <p className="refund-info-note">
              Đơn hàng chưa có bản ghi thanh toán. Vui lòng kiểm tra lại sau khi
              thanh toán thành công.
            </p>
          )}

          {summary && (
            <ul className="refund-info-list">
              <li>
                <span>Trạng thái đơn hàng</span>
                <strong>{order?.status || "N/A"}</strong>
              </li>
              <li>
                <span>Trạng thái thanh toán</span>
                <strong>{summary?.paymentStatus || "N/A"}</strong>
              </li>
              <li>
                <span>
                  {isRefunded ? "Số tiền đã hoàn" : "Số tiền đã thanh toán"}
                </span>
                <strong>{formatMoney(effectiveAmount, currency)}</strong>
              </li>
              {summary?.transactionNo && (
                <li>
                  <span>Mã giao dịch</span>
                  <strong>{summary.transactionNo}</strong>
                </li>
              )}
              <li>
                <span>Cập nhật gần nhất</span>
                <strong>{formatDateTime(updatedAt)}</strong>
              </li>
            </ul>
          )}

          {recentRefund?.reason && (
            <div className="refund-info-reason">
              <span>Lý do yêu cầu gần nhất:</span>
              <p>{recentRefund.reason}</p>
            </div>
          )}

          {summary?.failReason && (
            <div className="refund-info-warning">
              <strong>Payment warning:</strong> {summary.failReason}
            </div>
          )}
        </>
      )}
    </div>
  );
}


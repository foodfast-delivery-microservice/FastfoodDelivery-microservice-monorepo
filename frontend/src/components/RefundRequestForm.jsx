import React, { useState } from "react";

export default function RefundRequestForm({
  amount,
  currency = "VND",
  loading,
  error,
  onSubmit,
  onCancel,
  formatMoney,
}) {
  const [reason, setReason] = useState("");
  const [agree, setAgree] = useState(false);
  const [localError, setLocalError] = useState(null);

  const displayAmount = formatMoney ? formatMoney(amount, currency) : amount;

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!reason.trim()) {
      setLocalError("Vui lòng mô tả lý do hoàn tiền.");
      return;
    }
    if (!agree) {
      setLocalError("Bạn cần xác nhận chính sách hoàn tiền.");
      return;
    }

    setLocalError(null);
    onSubmit?.({
      amount,
      reason: reason.trim(),
    });
  };

  return (
    <form className="refund-form" onSubmit={handleSubmit}>
      <div className="refund-form-row">
        <label>Số tiền hoàn</label>
        <input type="text" value={displayAmount} readOnly />
      </div>

      <div className="refund-form-row">
        <label htmlFor="refund-reason">Lý do hoàn tiền *</label>
        <textarea
          id="refund-reason"
          rows={4}
          placeholder="Món ăn bị nguội, giao sai món..."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </div>

      <label className="refund-checkbox">
        <input
          type="checkbox"
          checked={agree}
          onChange={(e) => setAgree(e.target.checked)}
          disabled={loading}
        />
        <span>Tôi xác nhận thông tin trên là chính xác.</span>
      </label>

      {(localError || error) && (
        <p className="refund-form-error">{localError || error}</p>
      )}

      <div className="refund-form-actions">
        <button
          type="button"
          className="refund-btn secondary"
          onClick={onCancel}
          disabled={loading}
        >
          Hủy
        </button>
        <button type="submit" className="refund-btn primary" disabled={loading}>
          {loading ? "Đang gửi..." : "Gửi yêu cầu hoàn tiền"}
        </button>
      </div>
    </form>
  );
}


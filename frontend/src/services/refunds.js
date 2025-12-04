import http from './http'

const unwrapData = (responseData) => {
  if (responseData?.data !== undefined && responseData?.status !== undefined) {
    return responseData.data
  }
  return responseData
}

/**
 * Gửi yêu cầu hoàn tiền cho một đơn hàng cụ thể.
 * Backend sẽ tự động lấy grandTotal nếu không truyền refundAmount,
 * nhưng FE vẫn gửi xuống để tránh sai lệch giá trị.
 */
export const requestRefund = async (orderId, payload = {}) => {
  const { data } = await http.post(`/orders/${orderId}/refund`, payload)
  return unwrapData(data)
}

/**
 * Lấy thông tin thanh toán/order phục vụ hiển thị trạng thái refund hiện tại.
 * Hiện tại backend expose qua Payment Service: GET /payments/order/{orderId}.
 */
export const getRefundSummary = async (orderId) => {
  if (!orderId) return null
  try {
    const { data } = await http.get(`/payments/order/${orderId}`)
    const payment = unwrapData(data)
    if (!payment) return null

    return {
      orderId: payment.orderId,
      paymentId: payment.id,
      amount: typeof payment.amount === 'number' ? payment.amount : Number(payment.amount) || 0,
      currency: payment.currency || 'VND',
      paymentStatus: payment.status || 'UNKNOWN',
      transactionNo: payment.transactionNo,
      failReason: payment.failReason,
      updatedAt: payment.timestamp || payment.updatedAt,
    }
  } catch (error) {
    // Payment record might not exist yet (order chưa thanh toán)
    if (error?.response?.status === 404) {
      return null
    }
    throw error
  }
}


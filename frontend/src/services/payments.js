import http from './http'

const unwrapData = (responseData) => {
  if (responseData?.data !== undefined && responseData?.status !== undefined) {
    return responseData.data
  }
  return responseData
}

export const createPayment = async (payload) => {
  const { data } = await http.post('/payments', payload)
  return unwrapData(data)
}

export const getPaymentByOrderId = async (orderId) => {
  const { data } = await http.get(`/payments/order/${orderId}`)
  return unwrapData(data)
}



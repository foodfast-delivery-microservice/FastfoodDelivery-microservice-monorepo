/**
 * API Service Configuration
 * Các service đang chạy ở các port riêng biệt trong môi trường development
 */

// Sử dụng Gateway cho tất cả requests (Gateway đã config CORS)
// Gateway sẽ route requests đến các microservices tương ứng
const GATEWAY_URL = 'http://localhost:8080/api/v1'

const API_SERVICES = {
    USER: GATEWAY_URL,
    PRODUCT: GATEWAY_URL,
    ORDER: GATEWAY_URL,
    PAYMENT: GATEWAY_URL,
    DRONE: GATEWAY_URL,
    GATEWAY: GATEWAY_URL,
}

// Export để sử dụng trong các file khác
export default API_SERVICES

// Utility function để get service URL
export const getServiceUrl = (serviceName) => {
    return API_SERVICES[serviceName] || API_SERVICES.GATEWAY
}

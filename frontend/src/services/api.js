import axios from 'axios';

const API_URL = 'http://localhost:8080/api/v1'; // Gateway URL

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add a request interceptor to include the token
api.interceptors.request.use(
    (config) => {
        const user = JSON.parse(localStorage.getItem('user'));
        if (user && user.token) {
            config.headers.Authorization = `Bearer ${user.token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

export const merchantService = {
    getStatistics: (fromDate, toDate) =>
        api.get('/payments/merchants/me/statistics', { params: { fromDate, toDate } }),

    getOrders: (params) =>
        api.get('/orders/merchants/me', { params }),

    getProducts: (params) =>
        api.get('/products/merchants/me', { params: { ...params, includeInactive: true } }),

    createProduct: (productData) =>
        api.post('/products', productData),

    updateProduct: (productId, productData) =>
        api.put(`/products/${productId}`, productData),

    deleteProduct: (productId) =>
        api.delete(`/products/${productId}`),

    updateOrderStatus: (orderId, status) =>
        api.put(`/orders/merchants/me/${orderId}/status`, { status }),

    getOrderDetail: (orderId) =>
        api.get(`/orders/merchants/me/${orderId}`),
};

export const adminService = {
    getUsers: () => api.get('/users'),
    getAllOrders: (params) => api.get('/orders', { params }),
    getMerchantStats: (merchantId, fromDate, toDate) =>
        api.get(`/payments/merchants/${merchantId}/statistics`, { params: { fromDate, toDate } }),
    getMerchantProducts: (merchantId, params) =>
        api.get(`/products/merchants/me`, { params: { ...params, merchantId, includeInactive: true } }),
    updateUserStatus: (userId, active) =>
        api.patch(`/users/${userId}`, { active }),
    getUserStatistics: (userId) => api.get(`/orders/users/${userId}/statistics`),
    getOrderDetails: (orderId) => api.get(`/orders/${orderId}`),

    // Admin Analytics
    getSystemKPIs: (date) =>
        api.get('/admin/dashboard/kpis', { params: { date } }),
};

export const authService = {
    login: (credentials) => api.post('/auth/login', credentials),
};

export default api;

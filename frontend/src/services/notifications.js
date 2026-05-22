import { gatewayHttp as http } from './http';

const unwrapData = (responseData) => {
  if (responseData?.data !== undefined && responseData?.status !== undefined) {
    return responseData.data;
  }
  return responseData;
};

/**
 * Fetch paginated in-app notifications
 * @param {number} page - 0-indexed page number
 * @param {number} size - Page size
 */
export const getInAppNotifications = async (page = 0, size = 20) => {
  const { data } = await http.get(`/notifications/in-app?page=${page}&size=${size}`);
  return unwrapData(data);
};

/**
 * Fetch unread notification count
 */
export const getUnreadCount = async () => {
  const { data } = await http.get('/notifications/in-app/unread-count');
  return unwrapData(data);
};

/**
 * Mark a single in-app notification as read
 * @param {number} id - Notification ID
 */
export const markAsRead = async (id) => {
  await http.put(`/notifications/in-app/${id}/read`);
};

/**
 * Mark all in-app notifications as read
 */
export const markAllAsRead = async () => {
  await http.put('/notifications/in-app/read-all');
};

/**
 * Fetch email notification history (Admin only)
 */
export const getEmailHistory = async (params = {}) => {
  const { data } = await http.get('/notifications/email/history', { params });
  return unwrapData(data);
};

/**
 * Resend a failed email notification (Admin only)
 * @param {number} id - Email notification ID
 */
export const resendFailedEmail = async (id) => {
  const { data } = await http.post(`/notifications/email/${id}/resend`);
  return unwrapData(data);
};

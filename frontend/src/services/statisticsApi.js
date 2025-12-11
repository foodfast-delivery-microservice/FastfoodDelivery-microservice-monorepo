import http from "./http";

const unwrap = (responseData) => {
  if (!responseData) return responseData;

  if (
    typeof responseData === "object" &&
    Object.prototype.hasOwnProperty.call(responseData, "data") &&
    Object.prototype.hasOwnProperty.call(responseData, "status")
  ) {
    return responseData.data;
  }

  return responseData;
};

const log = (label, payload) => {
  if (process.env.NODE_ENV !== "production") {
    // eslint-disable-next-line no-console
    console.log(`[statisticsApi] ${label}`, payload);
  }
};

export const getSystemKPIs = async (params = {}) => {
  log("GET /admin/dashboard/kpis params", params);
  const response = await http.get("/admin/dashboard/kpis", { params });
  const data = unwrap(response.data);
  log("GET /admin/dashboard/kpis response", data);
  return data;
};

export const getUserStatistics = async (userId) => {
  if (!userId) throw new Error("userId is required for getUserStatistics");
  log("GET /orders/users/:id/statistics userId", userId);
  const response = await http.get(`/orders/users/${userId}/statistics`);
  const data = unwrap(response.data);
  log("GET /orders/users/:id/statistics response", data);
  return data;
};

export const getMerchantStatistics = async (merchantId, params = {}) => {
  if (!merchantId) throw new Error("merchantId is required for getMerchantStatistics");
  log("GET /payments/merchants/:id/statistics params", { merchantId, params });
  const response = await http.get(`/payments/merchants/${merchantId}/statistics`, {
    params,
  });
  const data = unwrap(response.data);
  log("GET /payments/merchants/:id/statistics response", data);
  return data;
};

export const getRevenueByRestaurant = async (params = {}) => {
  log("GET /admin/dashboard/revenue-by-restaurant params", params);
  const response = await http.get("/admin/dashboard/revenue-by-restaurant", { params });
  const data = unwrap(response.data);
  log("GET /admin/dashboard/revenue-by-restaurant response", data);
  return data;
};



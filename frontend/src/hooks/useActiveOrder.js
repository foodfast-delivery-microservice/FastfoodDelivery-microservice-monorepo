import { useEffect, useState } from "react";
import http from "../services/http";
import { useAuth } from "../context/AuthContext";

export default function useActiveOrder() {
  const { currentUser } = useAuth();
  const [activeOrder, setActiveOrder] = useState(null);

  useEffect(() => {
    const userId = currentUser?.id;
    if (!userId) return;

    const fetchActiveOrder = async () => {
      try {
        // Fetch orders with status pending/processing/delivering
        // We might need a specific endpoint or filter.
        // For now, let's try to fetch recent orders and check status.
        // Or if backend supports status filter array.
        // Let's assume we fetch latest orders and check client side for now.
        const res = await http.get("/orders", { params: { userId, size: 5, sort: "createdAt,desc" } });
        const orders = res.data?.data?.content || [];

        const active = orders.find(o =>
          ["pending", "processing", "delivering", "chờ xác nhận", "đang giao", "đang xử lý"].includes(o.status?.toLowerCase())
        );

        if (active) {
          setActiveOrder(active);
        } else {
          setActiveOrder(null);
        }
      } catch (err) {
        console.error("Failed to fetch active order", err);
      }
    };

    fetchActiveOrder();

    // Poll every 10 seconds
    const interval = setInterval(fetchActiveOrder, 10000);
    return () => clearInterval(interval);
  }, [currentUser]);

  return activeOrder;
}

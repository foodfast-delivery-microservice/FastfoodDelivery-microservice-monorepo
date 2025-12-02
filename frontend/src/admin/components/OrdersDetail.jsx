import { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, Table, Tag, Button, message, Spin } from "antd";
import http from "../../services/http";
import "./OrdersDetail.css";

export default function OrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [order, setOrder] = useState(null);
    const [restaurantName, setRestaurantName] = useState("Loading...");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const logApi = (label, payload) => {
        if (process.env.NODE_ENV === "production") return;
        // eslint-disable-next-line no-console
        console.log(`[OrderDetail] ${label}`, payload);
    };

    const parseDate = (createdAt) => {
        if (!createdAt) return null;
        
        // Handle array format [yyyy, MM, dd, HH, mm, ss]
        if (Array.isArray(createdAt)) {
            return new Date(
                createdAt[0],
                createdAt[1] - 1,
                createdAt[2],
                createdAt[3] || 0,
                createdAt[4] || 0,
                createdAt[5] || 0
            );
        }
        
        // Handle object format (Java LocalDateTime)
        if (
            typeof createdAt === "object" &&
            createdAt !== null &&
            (createdAt.year || createdAt.monthValue)
        ) {
            const { year, monthValue, month, dayOfMonth, day, hour, minute, second } = createdAt;
            return new Date(
                year || new Date().getFullYear(),
                (monthValue || month || 1) - 1,
                dayOfMonth || day || 1,
                hour || 0,
                minute || 0,
                second || 0
            );
        }
        
        // Handle number (timestamp)
        if (typeof createdAt === "number") {
            return new Date(createdAt);
        }
        
        // Handle string
        const date = new Date(createdAt);
        return Number.isFinite(date.getTime()) ? date : null;
    };

    const fetchRestaurantName = useCallback(async (merchantId) => {
        try {
            logApi("GET /restaurants for merchantId", merchantId);
            const res = await http.get("/restaurants", { params: { size: 100, page: 0 } });
            logApi("GET /restaurants response", res?.data);

            const restaurants =
                res?.data?.data?.content ||
                res?.data?.content ||
                res?.data?.data ||
                res?.data ||
                [];

            const found = restaurants.find((r) => r.merchantId === merchantId);
            if (found) {
                setRestaurantName(found.name);
                logApi("Found restaurant", found.name);
            } else {
                setRestaurantName(`Merchant #${merchantId}`);
                logApi("Restaurant not found, using merchantId");
            }
        } catch (e) {
            logApi("Error fetching restaurant", e);
            setRestaurantName(`Merchant #${merchantId}`);
        }
    }, []);

    useEffect(() => {
        async function fetchOrder() {
            setLoading(true);
            setError(null);
            
            try {
                logApi("GET /orders/:id params", { id });
                const res = await http.get(`/orders/${id}`);
                logApi("GET /orders/:id response", res?.data);

                // Try multiple response structures
                const data =
                    res?.data?.data ||
                    res?.data ||
                    null;

                if (!data) {
                    throw new Error("Không tìm thấy dữ liệu đơn hàng");
                }

                logApi("Parsed order data", data);

                // Map backend structure to frontend
                // Backend returns: deliveryAddress, orderItems
                const mappedOrder = {
                    ...data,
                    id: data.id,
                    orderCode: data.orderCode,
                    status: data.status,
                    grandTotal: data.grandTotal,
                    createdAt: parseDate(data.createdAt),
                    // Map deliveryAddress fields
                    receiverName: data.deliveryAddress?.receiverName || data.receiverName,
                    receiverPhone: data.deliveryAddress?.receiverPhone || data.receiverPhone,
                    fullAddress: data.deliveryAddress?.fullAddress || data.fullAddress,
                    // Map orderItems to items for table
                    items: (data.orderItems || data.items || []).map((item) => ({
                        ...item,
                        name: item.productName || item.name,
                        price: item.unitPrice || item.price,
                        quantity: item.quantity,
                    })),
                    // Keep original structure for reference
                    deliveryAddress: data.deliveryAddress,
                    orderItems: data.orderItems,
                };

                setOrder(mappedOrder);
                logApi("Mapped order", mappedOrder);

                // Fetch restaurant name if we have merchantId
                if (data.merchantId) {
                    await fetchRestaurantName(data.merchantId);
                } else {
                    setRestaurantName("—");
                }
            } catch (e) {
                const errorMessage = e?.response?.data?.message || e?.message || "Không thể tải chi tiết đơn hàng";
                setError(errorMessage);
                logApi("Error fetching order", {
                    message: errorMessage,
                    status: e?.response?.status,
                    data: e?.response?.data,
                });
                message.error(errorMessage);
            } finally {
                setLoading(false);
            }
        }
        fetchOrder();
    }, [id, fetchRestaurantName]);

    if (loading) {
        return (
            <div className="order-detail-page">
                <div style={{ textAlign: "center", padding: "60px 20px" }}>
                    <Spin size="large" tip="Đang tải chi tiết đơn hàng..." />
                </div>
            </div>
        );
    }

    if (error || !order) {
        return (
            <div className="order-detail-page">
                <Button onClick={() => navigate(-1)} className="back-btn">
                    ← Quay lại
                </Button>
                <Card className="order-card">
                    <div style={{ textAlign: "center", padding: "40px 20px" }}>
                        <p style={{ color: "#ef4444", fontSize: "16px", marginBottom: "16px" }}>
                            {error || "Không tìm thấy đơn hàng"}
                        </p>
                        <Button type="primary" onClick={() => navigate("/admin/orders")}>
                            Quay lại danh sách
                        </Button>
                    </div>
                </Card>
            </div>
        );
    }

    const columns = [
        { title: "Tên sản phẩm", dataIndex: "name", key: "name" },
        { title: "Số lượng", dataIndex: "quantity", key: "quantity" },
        {
            title: "Đơn giá",
            dataIndex: "price",
            render: (v) => `${Number(v).toLocaleString()}₫`,
        },
        {
            title: "Thành tiền",
            render: (_, r) => `${(r.price * r.quantity).toLocaleString()}₫`,
        },
    ];

    const statusColor = (s) => {
        const ss = (s || "").toLowerCase();
        if (ss.includes("delivered") || ss.includes("confirmed")) return "green";
        if (ss.includes("delivering")) return "blue";
        if (ss.includes("processing") || ss.includes("pending")) return "orange";
        return "volcano";
    };

    return (
        <div className="order-detail-page">
            <Button onClick={() => navigate(-1)} className="back-btn">
                ← Quay lại
            </Button>

            <Card title={`Chi tiết đơn hàng `} className="order-card">
                <h3>Mã đơn hàng: #{order.orderCode || order.id}</h3>
                <h3>Khách hàng</h3>
                <p><b>Tên:</b> {order.receiverName}</p>
                <p><b>SĐT:</b> {order.receiverPhone}</p>
                <p><b>Địa chỉ:</b> {order.fullAddress}</p>

                <h3>Nhà hàng giao:</h3>
                <p>{restaurantName}</p>

                {order.droneId && (
                    <>
                        <h3>Drone giao hàng:</h3>
                        <p>ID Drone: {order.droneId}</p>
                    </>
                )}

                <h3>Ngày đặt:</h3>
                <p>{order.createdAt?.toLocaleString("vi-VN")}</p>

                <h3>Trạng thái:</h3>
                <Tag color={statusColor(order.status)}>{order.status}</Tag>

                {/* Backend might not return items in detail yet, handle empty */}
                {order.items && order.items.length > 0 ? (
                    <Table
                        columns={columns}
                        dataSource={order.items}
                        pagination={false}
                        style={{ marginTop: 20 }}
                    />
                ) : (
                    <p><i>Chi tiết sản phẩm không có sẵn (Backend limitation)</i></p>
                )}

                <div className="total-section">
                    Tổng cộng: <b>{(order.grandTotal || order.total || 0).toLocaleString("vi-VN")}₫</b>
                </div>
            </Card>
        </div>
    );
}

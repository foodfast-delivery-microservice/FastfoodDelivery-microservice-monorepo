import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, Table, Tag, Button } from "antd";
import http from "../../services/http";
import "./OrdersDetail.css";

export default function OrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [order, setOrder] = useState(null);
    const [restaurantName, setRestaurantName] = useState("Loading...");

    useEffect(() => {
        async function fetchOrder() {
            try {
                const res = await http.get(`/orders/${id}`);
                const data = res.data?.data;

                if (data) {
                    setOrder({
                        ...data,
                        createdAt: Array.isArray(data.createdAt)
                            ? new Date(data.createdAt[0], data.createdAt[1] - 1, data.createdAt[2], data.createdAt[3], data.createdAt[4], data.createdAt[5])
                            : new Date(data.createdAt),
                        total: data.grandTotal,
                        items: data.items || [] // Assuming backend returns items in detail view, if not we might need to fetch them or they are missing
                    });

                    // Fetch restaurant name if we have merchantId
                    if (data.merchantId) {
                        fetchRestaurantName(data.merchantId);
                    }
                }
            } catch (e) {
                console.error("Lỗi tải chi tiết:", e);
            }
        }
        fetchOrder();
    }, [id]);

    const fetchRestaurantName = async (merchantId) => {
        try {
            // We can't fetch single restaurant by merchantId easily unless we list all or have specific endpoint.
            // But we can try to fetch all restaurants and find it, or if we have /restaurants/{id} but that takes ID not merchantId.
            // Let's assume we can fetch by ID if we had restaurantId, but we have merchantId.
            // Actually RestaurantResponse has id and merchantId.
            // Let's try to list restaurants with a filter if possible, or just fetch all (caching would be good but for now simple fetch)
            const res = await http.get("/restaurants", { params: { size: 100 } });
            const restaurants = res.data?.data?.content || [];
            const found = restaurants.find(r => r.merchantId === merchantId);
            if (found) setRestaurantName(found.name);
            else setRestaurantName(`Merchant #${merchantId}`);
        } catch (e) {
            console.error("Failed to fetch restaurant name", e);
        }
    }

    if (!order) return <div className="loading">Đang tải chi tiết...</div>;

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
                    Tổng cộng: <b>{order.total?.toLocaleString()}₫</b>
                </div>
            </Card>
        </div>
    );
}

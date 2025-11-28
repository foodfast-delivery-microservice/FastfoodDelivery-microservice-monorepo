import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, Table, Tag, Button } from "antd";
import { doc, getDoc } from "firebase/firestore";
import { db } from "../../firebase";
import "./OrdersDetail.css";

export default function OrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [order, setOrder] = useState(null);

    useEffect(() => {
        async function fetchOrder() {
            try {
                const ref = doc(db, "orders", id);
                const snap = await getDoc(ref);

                if (snap.exists()) {
                    const data = snap.data();
                    setOrder({
                        id,
                        ...data,
                        createdAt: data.createdAt?.seconds
                            ? new Date(data.createdAt.seconds * 1000)
                            : null,
                    });
                }
            } catch (e) {
                console.error("Lỗi tải chi tiết:", e);
            }
        }
        fetchOrder();
    }, [id]);

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
        const ss = s.toLowerCase();
        if (ss.includes("đã giao")) return "green";
        if (ss.includes("đang giao")) return "blue";
        if (ss.includes("đang xử lý")) return "orange";
        return "volcano";
    };

    return (
        <div className="order-detail-page">
            <Button onClick={() => navigate(-1)} className="back-btn">
                ← Quay lại
            </Button>

            <Card title={`Chi tiết đơn hàng `} className="order-card">
                <h3>Mã đơn hàng: #{order.id}</h3>
                <h3>Khách hàng</h3>
                <p><b>Tên:</b> {order.customer?.name}</p>
                <p><b>SĐT:</b> {order.customer?.phone}</p>
                <p><b>Địa chỉ:</b> {order.customer?.address}</p>

                <h3>Nhà hàng giao:</h3>
                <p>{order.restaurantName}</p>

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

                <Table
                    columns={columns}
                    dataSource={order.items || []}
                    pagination={false}
                    style={{ marginTop: 20 }}
                />

                <div className="total-section">
                    Tổng cộng: <b>{order.total?.toLocaleString()}₫</b>
                </div>
            </Card>
        </div>
    );
}

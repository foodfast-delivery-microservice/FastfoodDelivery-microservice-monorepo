import { useEffect, useState } from "react";
import {
    BarChart, Bar, LineChart, Line,
    XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

export default function DashboardCharts() {
    const [orders, setOrders] = useState([]);

    useEffect(() => {
        fetch("http://localhost:5002/orders")
            .then(res => res.json())
            .then(data => setOrders(data))
            .catch(err => console.error("Lỗi tải dữ liệu:", err));
    }, []);

    // Gom dữ liệu theo ngày
    const revenueByDate = orders.reduce((acc, order) => {
        const date = order.date;
        const total = order.total;
        acc[date] = (acc[date] || 0) + total;
        return acc;
    }, {});

    const ordersByDate = orders.reduce((acc, order) => {
        const date = order.date;
        acc[date] = (acc[date] || 0) + 1;
        return acc;
    }, {});

    // Chuyển sang mảng cho biểu đồ
    const revenueData = Object.keys(revenueByDate).map(date => ({
        date,
        revenue: revenueByDate[date]
    }));

    const orderCountData = Object.keys(ordersByDate).map(date => ({
        date,
        orders: ordersByDate[date]
    }));

    return (
        <div style={{
            display: "flex",
            gap: "20px",
            marginBottom: "30px",
            flexWrap: "wrap"
        }}>
            {/* Biểu đồ doanh thu */}
            <div style={{
                flex: "1",
                background: "#fff",
                borderRadius: "12px",
                padding: "20px",
                boxShadow: "0 2px 10px rgba(0,0,0,0.1)"
            }}>
                <h3 style={{ marginBottom: 20, textAlign: "center" }}>📈 Doanh thu theo ngày</h3>
                <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={revenueData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="revenue" fill="#82ca9d" name="Doanh thu (VNĐ)" />
                    </BarChart>
                </ResponsiveContainer>
            </div>

            {/* Biểu đồ đơn hàng */}
            <div style={{
                flex: "1",
                background: "#fff",
                borderRadius: "12px",
                padding: "20px",
                boxShadow: "0 2px 10px rgba(0,0,0,0.1)"
            }}>
                <h3 style={{ marginBottom: 20, textAlign: "center" }}>📊 Số đơn hàng theo ngày</h3>
                <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={orderCountData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Line type="monotone" dataKey="orders" stroke="#8884d8" name="Số đơn hàng" />
                    </LineChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}

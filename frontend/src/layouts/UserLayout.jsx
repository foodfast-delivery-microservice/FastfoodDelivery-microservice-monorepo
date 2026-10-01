import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import Footer from "../components/Footer";

export default function UserLayout({ cartCount }) {
    return (
        <div className="user-layout">
            <Header cartCount={cartCount} /> {/* ✅ Chỉ cần cartCount */}
            {import.meta.env.VITE_DEMO_MODE === "true" && (
                <div role="status" style={{ background: "#fff4d6", color: "#694b00", padding: "9px 16px", textAlign: "center", fontSize: 13 }}>
                    Bản demo giao diện · Dữ liệu mẫu · API, đăng nhập và đặt hàng chưa được bật.
                </div>
            )}
            <main>
                <Outlet />
            </main>
            <Footer />
        </div>
    );
}

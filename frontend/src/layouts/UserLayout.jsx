import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import Footer from "../components/Footer";

export default function UserLayout({ cartCount }) {
    return (
        <div className="user-layout">
            <Header cartCount={cartCount} /> {/* ✅ Chỉ cần cartCount */}
            <main>
                <Outlet />
            </main>
            <Footer />
        </div>
    );
}
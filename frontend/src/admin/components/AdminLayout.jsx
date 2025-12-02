import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";

export default function AdminLayout() {
    return (
        <div style={{ display: "flex", minHeight: "100vh" }}>
            <Sidebar />
            <main style={{ flex: 1, backgroundColor: "#f5f6fa", padding: "20px" }}>
                <Outlet />
            </main>
        </div>
    );
}

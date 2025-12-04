import { Outlet, Link, useLocation, Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./AdminLayout.css";

export default function AdminLayout() {
  const location = useLocation();
  const { currentUser, logout } = useAuth();

  // 🔒 Chặn nếu không phải admin
  const role = (currentUser?.role || "").toLowerCase();
  if (!currentUser || role !== "admin") {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <h2>MeowChick Pro</h2>

        <nav className="menu-links">

          <Link to="/admin/dashboards" className={location.pathname.includes("/admin/dashboards") ? "active" : ""}>
            Dashboard
          </Link>
          <Link to="/admin/orders" className={location.pathname.includes("/admin/orders") ? "active" : ""}>
            Orders
          </Link>
          <Link to="/admin/products" className={location.pathname.includes("/admin/products") ? "active" : ""}>
            Products
          </Link>
          <Link to="/admin/users" className={location.pathname.includes("/admin/users") ? "active" : ""}>
            Users
          </Link>
          <Link to="/admin/restaurants" className={location.pathname.includes("/admin/restaurants") ? "active" : ""}>
            Restaurants
          </Link>
          <Link
            to="/admin/drones"
            className={location.pathname.includes("/admin/drones") ? "active" : ""}
          >
            Drones
          </Link>
          <Link
            to="/admin/create-restaurant"
            className={location.pathname.includes("/admin/create-restaurant") ? "active" : ""}
          >
            Tạo Nhà hàng
          </Link>

          {/* 🔥 Nút Logout ở cuối sidebar */}
          <button className="logout-btn" onClick={logout}>
            Đăng xuất
          </button>
        </nav>


      </aside>

      <div className="admin-content">
        <main>
          <Outlet />
        </main>
      </div>
    </div>
  );
}

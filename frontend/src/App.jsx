import React, { useEffect, useState } from "react";
import { Routes, Route, BrowserRouter, Navigate } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import "leaflet/dist/leaflet.css";
import "antd/dist/reset.css";
import "./App.css";
import { Modal, message } from "antd";

/* USER PAGES */
import Profile from "./components/Profile";
import OrderDetail from "./components/OrderDetail";
import ProductList from "./components/ProductList";
import ProductDetail from "./components/ProductDetail";
import Cart from "./components/Cart";
import Checkout from "./components/Checkout";
import OrderHistory from "./components/OrderHistory";
import Login from "./components/Login";
import Register from "./components/Register";
import RestaurantList from "./components/RestaurantList";
import RestaurantDetail from "./components/RestaurantDetail";
import WaitingForConfirmation from "./components/WaitingForConfirmation";

/* LAYOUTS */
import UserLayout from "./layouts/UserLayout";
import AdminLayout from "./layouts/AdminLayout";
import RestaurantLayout from "./layouts/RestaurantLayout";

/* ADMIN PAGES */
import Dashboard from "./admin/pages/Dashboard";
import Orders from "./admin/pages/Orders";
import Users from "./admin/pages/Users";
import AdminOrderDetail from "./admin/components/OrdersDetail";
import Products from "./admin/pages/Products";
import AdminCreateRestaurant from "./admin/pages/AdminCreateRestaurant";
import AdminDroneManager from "./admin/pages/AdminDroneManager";

/* RESTAURANT ADMIN */
import RestaurantDashboard from "./components/RestaurantDashboard";
import RestaurantOrderDetail from "./components/RestaurantOrderDetail";
import RestaurantProducts from "./components/RestaurantProducts";
import DroneList from "./components/DroneList";

/* ✅ Protected Routes */
const SESSION_KEY = "app_session";

function AdminRoute({ children }) {
  const { currentUser, loading } = useAuth();

  console.log("🧩 [AdminRoute] loading:", loading, "user:", currentUser?.role);

  if (loading) {
    // ⏳ Chờ cho AuthContext xong rồi mới render
    return <p>⏳ Đang xác thực Admin...</p>;
  }

  if (!currentUser) {
    console.log("❌ [AdminRoute] Không có user → login");
    return <Navigate to="/login" replace />;
  }

  const role = (currentUser.role || "").toLowerCase();
  if (role !== "admin") {
    console.log("🚫 [AdminRoute] Không phải admin, role:", currentUser.role);
    return <Navigate to="/login" replace />;
  }

  console.log("✅ [AdminRoute] Cho phép truy cập admin");
  return children;
}

function RestaurantRoute({ children }) {
  const { currentUser, loading } = useAuth();

  if (loading) return <p>⏳ Đang xác thực tài khoản...</p>;
  const role = (currentUser?.role || "").toLowerCase();
  if (!currentUser || (role !== "restaurant" && role !== "merchant")) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function App() {
  const { currentUser, loading } = useAuth();

  // ✅ Giữ giỏ hàng theo user
  const userKey = (user) => user?.id || user?.uid || user?.phonenumber || user?.username

  const [cart, setCart] = useState(() => {
    try {
      const storedUser = JSON.parse(localStorage.getItem(SESSION_KEY));
      const identifier = userKey(storedUser);
      const key = identifier ? `cart_${encodeURIComponent(identifier)}` : "cart_guest";
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });

  const [modalVisible, setModalVisible] = useState(false);
  const [pendingProduct, setPendingProduct] = useState(null);

  // ✅ Lưu giỏ hàng
  useEffect(() => {
    if (loading) return;
    const identifier = userKey(currentUser);
    const key = identifier ? `cart_${encodeURIComponent(identifier)}` : "cart_guest";
    localStorage.setItem(key, JSON.stringify(cart));
  }, [cart, currentUser, loading]);

  // ✅ Reset khi logout
  useEffect(() => {
    if (!loading && !currentUser) {
      setCart([]);
      localStorage.removeItem("cart_guest");
    }
  }, [currentUser, loading]);

  const handleAdd = (product) => {
    setCart((prev) => {
      if (prev.length === 0)
        return [{ ...product, quantity: 1, restaurantId: product.restaurantId }];

      const currentRes = prev[0].restaurantId;
      if (product.restaurantId !== currentRes) {
        setPendingProduct(product);
        setModalVisible(true);
        return prev;
      }

      const existing = prev.find((p) => p.id === product.id);
      if (existing)
        return prev.map((p) =>
          p.id === product.id ? { ...p, quantity: p.quantity + 1 } : p
        );

      return [...prev, { ...product, quantity: 1 }];
    });
  };

  const confirmResetCart = () => {
    if (!pendingProduct) return;
    const newCart = [
      {
        ...pendingProduct,
        quantity: 1,
        restaurantId: pendingProduct.restaurantId,
      },
    ];
    setCart(newCart);
    const identifier = userKey(currentUser);
    const key = identifier ? `cart_${encodeURIComponent(identifier)}` : "cart_guest";
    localStorage.setItem(key, JSON.stringify(newCart));
    message.success(`Đã bắt đầu giỏ hàng mới từ ${pendingProduct.restaurantName}`);
    setModalVisible(false);
    setPendingProduct(null);
  };

  if (loading) return <p>⏳ Đang tải...</p>;

  return (
    <>
      <BrowserRouter>
        <Routes>
          {/* USER */}
          <Route
            path="/"
            element={
              <UserLayout cartCount={cart.reduce((s, i) => s + i.quantity, 0)} />
            }
          >
            <Route path="/menu/:categoryKey" element={<ProductList />} />
            <Route path="profile" element={<Profile />} />
            <Route index element={<RestaurantList />} />
            <Route path="login" element={<Login />} />
            <Route path="register" element={<Register />} />
            <Route path="product-detail/:id" element={<ProductDetail onAdd={handleAdd} />} />
            <Route path="cart" element={<Cart cart={cart} setCart={setCart} />} />
            <Route path="checkout" element={<Checkout cart={cart} setCart={setCart} />} />
            <Route path="order-history" element={<OrderHistory />} />
            <Route path="order/:id" element={<OrderDetail />} />
            <Route path="waiting/:orderId" element={<WaitingForConfirmation />} />
            <Route path="restaurant/:id" element={<RestaurantDetail onAdd={handleAdd} />} />
          </Route>

          {/* ADMIN */}
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminLayout />
              </AdminRoute>
            }
          >
            <Route index element={<Navigate to="/admin/dashboards" />} />
            <Route path="dashboards" element={<Dashboard />} />
            <Route path="orders" element={<Orders />} />
            <Route path="orders/:id" element={<AdminOrderDetail />} />
            <Route path="products" element={<Products />} />
            <Route path="users" element={<Users />} />
            <Route path="create-restaurant" element={<AdminCreateRestaurant />} />
            <Route path="drones" element={<AdminDroneManager />} />
          </Route>

          {/* RESTAURANT ADMIN */}
          <Route
            path="/restaurantadmin"
            element={
              <RestaurantRoute>
                <RestaurantLayout />
              </RestaurantRoute>
            }
          >
            <Route index element={<RestaurantDashboard />} />
            <Route path="products" element={<RestaurantProducts />} />
            <Route path="order/:id" element={<RestaurantOrderDetail />} />
            <Route path="drones" element={<DroneList />} />
          </Route>
        </Routes>
      </BrowserRouter>

      <Modal
        title="Giỏ hàng chứa món từ nhà hàng khác"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          setPendingProduct(null);
        }}
        onOk={confirmResetCart}
        okText="Đồng ý"
        cancelText="Hủy"
      >
        <p>
          Một đơn hàng chỉ có thể đặt từ 1 nhà hàng. Bạn có muốn xóa giỏ cũ và
          bắt đầu giỏ mới từ{" "}
          <strong>{pendingProduct?.restaurantName || "nhà hàng này"}</strong>?
        </p>
      </Modal>
    </>
  );
}

export default App;

// src/components/Header.jsx
import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FaUserCircle } from "react-icons/fa";
import { useAuth } from "../context/AuthContext";
import NotificationBell from "./NotificationBell";
import http from "../services/http";
import "./Header.css";

function Header({ cartCount }) {
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState("");
  const [categories, setCategories] = useState([]);
  const { currentUser, logout } = useAuth();

  // 🧷 Load categories từ API
  useEffect(() => {
    const loadCategories = async () => {
      try {
        // Fetch products to get categories
        // Ideally backend should have /products/categories endpoint
        const res = await http.get("/products");
        const list = res.data?.data || [];

        const all = [...new Set(list.map((p) => p.category?.trim()))]
          .filter((c) => c && c !== "");

        setCategories(all);
      } catch (err) {
        console.error("🔥 Lỗi load category:", err);
      }
    };

    loadCategories();
  }, []);

  if (currentUser === undefined) return null;

  const handleLogout = async () => {
    if (logout) {
      await logout();
      navigate("/login");
    }
  };

  // 🔎 SEARCH FUNCTION
  const handleSearch = (e) => {
    e.preventDefault();
    if (searchValue.trim() !== "") {
      const searchQuery = encodeURIComponent(searchValue.trim());
      navigate(`/menu/All?search=${searchQuery}`);
      setSearchValue("");
      window.scrollTo(0, 0);
    }
  };

  return (
    <header className="header">
      {/* LEFT - LOGO */}
      <div className="header-left">
        <Link to="/">
          <img src="/Images/Logo.png" alt="MEOWCHICK Logo" />
        </Link>
      </div>

      {/* CENTER - SEARCH BAR */}
      <div className="header-center">
        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="text"
            placeholder="Tìm món ăn..."
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
          />
          <button type="submit" aria-label="Tìm kiếm">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              viewBox="0 0 48 48"
              fill="none"
            >
              <path
                d="m29.175 31.99 2.828-2.827 12.019 12.019-2.828 2.827z"
                fill="#2b2623"
                opacity="0.6"
              />
              <circle cx="20" cy="20" r="16" fill="#2b2623" opacity="0.4" />
              <circle cx="20" cy="20" r="13" fill="#2b2623" opacity="0.7" />
              <path
                d="M26.9 14.2c-1.7-2-4.2-3.2-6.9-3.2s-5.2 1.2-6.9 3.2c-.4.4-.3 1.1.1 1.4.4.4 1.1.3 1.4-.1C16 13.9 17.9 13 20 13s4 .9 5.4 2.5c.2.2.5.4.8.4.2 0 .5-.1.6-.2.4-.4.4-1.1.1-1.5z"
                fill="#2b2623"
                opacity="0.9"
              />
            </svg>
          </button>
        </form>
      </div>

      {/* RIGHT - NAVIGATION / ACCOUNT */}
      <div className="header-right">
        <button onClick={() => navigate("/")}>Trang chủ</button>

        {/* 🔥 MENU DROPDOWN — CATEGORIES FROM API */}
        <div className="menu-dropdown">
          <button onClick={() => navigate("/menu/All")}>Thực đơn</button>

          <div className="dropdown-content">

            {/* Luôn có "Tất cả" */}
            <Link to="/menu/All">

              <span>Tất cả</span>
            </Link>

            {/* Render category động */}
            {categories.map((c) => (
              <Link key={c} to={`/menu/${c}`}>

                <span>{c}</span>
              </Link>
            ))}
          </div>
        </div>

        {/* CART */}
        <Link to="/Cart" className="cart-button">
          Giỏ hàng ({cartCount > 0 ? cartCount : 0})
        </Link>

        {/* NOTIFICATION BELL */}
        {currentUser && <NotificationBell />}

        {/* USER MENU */}
        <div className="user-actions">
          {currentUser ? (
            <div className="user-menu">
              <div className="user-menu-trigger">
                <FaUserCircle size={22} />
                <span>
                  {currentUser.firstname} {currentUser.lastname}
                </span>
              </div>

              {/* ACCOUNT DROPDOWN */}
              <div className="dropdown-menu">
                <button
                  className="dropdown-item"
                  onClick={() => navigate("/profile")}
                >
                  Tài khoản của tôi
                </button>

                <button
                  className="dropdown-item"
                  onClick={() => navigate("/order-history")}
                >
                  Lịch sử đơn hàng
                </button>

                <button className="dropdown-item" onClick={handleLogout}>
                  Đăng xuất
                </button>
              </div>
            </div>
          ) : (
            <Link to="/login" className="login-button">
              Đăng nhập
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;

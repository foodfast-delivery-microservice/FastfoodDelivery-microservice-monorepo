import React, { useEffect, useState } from "react";
import { useParams, useLocation } from "react-router-dom";
import { fetchProducts } from "../services/products";

import Product from "./Product";
import Banner from "./Banner";
import "./ProductList.css";

function ProductList({ onAdd, defaultCategory = "All" }) {
    const { categoryKey } = useParams();
    const location = useLocation();
    const initialSearch = new URLSearchParams(location.search).get("search") || "";

    const [products, setProducts] = useState([]);
    const [searchTerm, setSearchTerm] = useState(initialSearch);
    const [selectedCategory, setSelectedCategory] = useState(categoryKey || defaultCategory);
    const [currentPage, setCurrentPage] = useState(1);
    const [sortOption, setSortOption] = useState("default");

    const [minPrice, setMinPrice] = useState(0);
    const [maxPrice, setMaxPrice] = useState(200000);
    const [priceRange, setPriceRange] = useState({ min: 0, max: 200000 });

    const productsPerPage = 6;
    const bannerImages = ["/Images/1.png", "/Images/Banner2.png", "/Images/Banner3.png"];

    // Load products
    useEffect(() => {
        const loadProducts = async () => {
            try {
                // Fetch products from backend API
                const fetched = await fetchProducts();

                // Filter active products
                const safeProducts = fetched.filter(p => p.active);

                setProducts(safeProducts);

                // Set price range
                if (safeProducts.length > 0) {
                    const prices = safeProducts.map((p) => Number(p.price ?? 0));
                    const min = Math.min(...prices, 0);
                    const max = Math.max(...prices, 200000);
                    setPriceRange({ min, max });
                    setMinPrice(min);
                    setMaxPrice(max);
                }
            } catch (err) {
                console.error("🔥 Lỗi load product:", err);
                setProducts([]);
            }
        };

        loadProducts();
    }, []);

    // Update searchTerm when URL changes
    useEffect(() => {
        const q = new URLSearchParams(location.search).get("search") || "";
        setSearchTerm(q);
        setCurrentPage(1);
    }, [location.search]);

    // Update category
    useEffect(() => {
        if (categoryKey) {
            setSelectedCategory(categoryKey);
            const q = new URLSearchParams(location.search).get("search");
            if (!q) setSearchTerm("");
            setCurrentPage(1);
        } else {
            setSelectedCategory(defaultCategory);
        }
    }, [categoryKey, location.search, defaultCategory]);

    // Reset filter khi quay về trang chủ
    useEffect(() => {
        const search = new URLSearchParams(location.search).get("search") || "";
        const isHome = location.pathname === "/" && search === "" && !categoryKey;

        if (isHome) {
            setSelectedCategory("All");
            setSortOption("default");
            setCurrentPage(1);

            // reset search
            setSearchTerm("");

            // reset giá
            setMinPrice(priceRange.min);
            setMaxPrice(priceRange.max);
        }
    }, [location.pathname, location.search, categoryKey, priceRange]);

    // Categories
    const categories = ["All", ...new Set(products.map((p) => p.category))];

    // Filter logic
    let filteredProducts = products.filter((p) => {
        const matchSearch = p.name.toLowerCase().includes(searchTerm.toLowerCase());
        const matchCategory =
            selectedCategory.toLowerCase() === "all" ||
            p.category?.toLowerCase() === selectedCategory.toLowerCase();

        const matchPrice = Number(p.price) >= minPrice && Number(p.price) <= maxPrice;

        if (searchTerm.trim() !== "") {
            return matchSearch && matchPrice;
        }
        return matchCategory && matchPrice;
    });

    // Sorting
    filteredProducts.sort((a, b) => {
        switch (sortOption) {
            case "price-asc":
                return a.price - b.price;
            case "price-desc":
                return b.price - a.price;
            case "name-asc":
                return a.name.localeCompare(b.name);
            case "name-desc":
                return b.name.localeCompare(a.name);
            default:
                return 0;
        }
    });

    // Pagination
    const indexOfLastProduct = currentPage * productsPerPage;
    const indexOfFirstProduct = indexOfLastProduct - productsPerPage;
    const currentProducts = filteredProducts.slice(indexOfFirstProduct, indexOfLastProduct);
    const totalPages = Math.ceil(filteredProducts.length / productsPerPage);

    return (
        <div className="main-home">
            {searchTerm.trim() === "" && <Banner images={bannerImages} />}

            {/* ẨN TITLE NẾU ĐANG TÌM KIẾM */}
            {searchTerm.trim() === "" && (
                <div className="main-title">
                    <h1>Hôm nay ăn gì?</h1>
                </div>
            )}

            <div className="content-wrapper">

                {/* ẨN SIDEBAR KHI KHÔNG CÓ SẢN PHẨM */}
                {!(searchTerm.trim() !== "" &&
                    products.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase())).length === 0
                ) && (
                        <aside className="sidebar">

                            {/* DANH MỤC — ẨN KHI ĐANG TÌM KIẾM */}
                            {searchTerm.trim() === "" && (
                                <>
                                    <h3>Danh mục</h3>
                                    <div className="menu">
                                        {categories.map((c) => (
                                            <div key={c}>
                                                <button
                                                    className={selectedCategory === c ? "active" : ""}
                                                    onClick={() => {
                                                        setSelectedCategory(c);
                                                        const q = new URLSearchParams(location.search).get("search");
                                                        if (!q) setSearchTerm("");
                                                        setCurrentPage(1);
                                                    }}
                                                >
                                                    <span>{c === "All" ? "Tất cả" : c}</span>
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )}

                            <h3>Lọc theo giá</h3>
                            <div className="price-filter">
                                <label>Từ:</label>
                                <input
                                    type="number"
                                    value={minPrice}
                                    onChange={(e) => setMinPrice(Number(e.target.value))}
                                />

                                <label>Đến:</label>
                                <input
                                    type="number"
                                    value={maxPrice}
                                    onChange={(e) => setMaxPrice(Number(e.target.value))}
                                />

                                <div className="range-slider">
                                    <input
                                        type="range"
                                        min={priceRange.min}
                                        max={priceRange.max}
                                        value={minPrice}
                                        onChange={(e) => setMinPrice(Number(e.target.value))}
                                    />
                                    <input
                                        type="range"
                                        min={priceRange.min}
                                        max={priceRange.max}
                                        value={maxPrice}
                                        onChange={(e) => setMaxPrice(Number(e.target.value))}
                                    />
                                </div>

                                <p>
                                    Khoảng giá:{" "}
                                    <strong>
                                        {minPrice.toLocaleString()}₫ - {maxPrice.toLocaleString()}₫
                                    </strong>
                                </p>
                            </div>

                            <h3>Sắp xếp</h3>
                            <div className="sort-filter">
                                <select value={sortOption} onChange={(e) => setSortOption(e.target.value)}>
                                    <option value="default">Mặc định</option>
                                    <option value="price-asc">Giá tăng dần</option>
                                    <option value="price-desc">Giá giảm dần</option>
                                    <option value="name-asc">Tên A → Z</option>
                                    <option value="name-desc">Tên Z → A</option>
                                </select>
                            </div>

                        </aside>
                    )}

                {/* PRODUCTS */}
                <div className="product-show">

                    {currentProducts.length > 0 ? (
                        <>
                            <div className="product-grid">
                                {currentProducts.map((p) => (
                                    <Product key={p.id} product={p} onAdd={onAdd} />
                                ))}
                            </div>

                            {totalPages > 1 && (
                                <div className="pagination">
                                    <button
                                        onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                                        disabled={currentPage === 1}
                                    >
                                        Prev
                                    </button>

                                    {[...Array(totalPages)].map((_, i) => (
                                        <button
                                            key={i}
                                            className={currentPage === i + 1 ? "active" : ""}
                                            onClick={() => setCurrentPage(i + 1)}
                                        >
                                            {i + 1}
                                        </button>
                                    ))}

                                    <button
                                        onClick={() =>
                                            setCurrentPage((prev) => Math.min(prev + 1, totalPages))
                                        }
                                        disabled={currentPage === totalPages}
                                    >
                                        Next
                                    </button>
                                </div>
                            )}
                        </>
                    ) : (
                        <p className="no-results">
                            ❌ Không tìm thấy sản phẩm nào phù hợp!
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}

export default ProductList;

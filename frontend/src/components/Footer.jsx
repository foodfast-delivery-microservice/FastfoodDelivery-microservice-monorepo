import React from "react";
import { Link } from "react-router-dom"; // Thêm import Link
import './Footer.css';

// Xóa các props không cần thiết
function Footer() {
    return (
        <footer className="footer">
            <div className="footer-container">

                {/* Dùng chung className="footer-column" */}
                <div className="footer-column">
                    <h3 className="footer-title">MEOWCHICK VIETNAM</h3>
                    <p>📍 273 An Dương Vương , Quận 5, TP. Hồ Chí Minh</p>
                    <p>📞 (028) 393 11 039</p>
                    <p>🌐 www.MEOWCHICK.com</p>
                    {/* Dùng thẻ <a> cho link ngoài, hoặc <Link> cho link trong app */}
                    <a
                        href="https://maps.google.com/maps?q=$"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="map-button"
                    >
                        Xem bản đồ
                    </a>
                </div>



                {/* Dùng chung className="footer-column" */}
                <div className="footer-column">
                    <h3 className="footer-title">Nhận thông báo từ chúng tôi</h3>
                    <div className="subscribe">
                        <input type="email" placeholder="Email của bạn" />
                        <button>Gửi</button>
                    </div>
                </div>
            </div>

            <div className="footer-bottom">
                <p>Chính sách quy định | Chính sách bảo mật | Copyright © 2025 Fast Food Vietnam</p>
            </div>
        </footer>
    );
}

export default Footer;
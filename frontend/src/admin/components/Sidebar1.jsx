import { Link } from "react-router-dom";

import { FaHome, FaBox, FaUsers, FaChartBar, FaClipboardList } from "react-icons/fa";
import "./Sidebar1.css";
export default function Sidebar() {
    return (
        <div className="sidebar1">
            <h2 className="logo">MeowChick Admin</h2>
            <ul>
                <li><Link to="/"><FaHome /> Bảng điều khiển</Link></li>
                <li><Link to="/orders"><FaClipboardList /> Quản lý đơn hàng</Link></li>
                <li><Link to="/products"><FaBox /> Quản lý sản phẩm</Link></li>
                <li><Link to="/customers"><FaUsers /> Quản lý khách hàng</Link></li>
                <li><Link to="/stats"><FaChartBar /> Thống kê</Link></li>
            </ul>
        </div>
    );
}

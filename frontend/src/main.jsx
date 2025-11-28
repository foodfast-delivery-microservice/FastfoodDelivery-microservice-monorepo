import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { AuthProvider } from "./context/AuthContext";
import { App as AntdApp } from "antd"; // ✅ thêm App wrapper của Ant Design
import "antd/dist/reset.css"; // ✅ bắt buộc để reset style của antd
import "./index.css"; // nếu bạn có file CSS global

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AntdApp>
      <AuthProvider>
        <App />
      </AuthProvider>
    </AntdApp>
  </React.StrictMode>
);

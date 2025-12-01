import React, { useState } from "react";
import "./DroneList.css";

export default function DroneList() {
  const [drones] = useState([]);

  return (
    <div className="drone-container">
      <h2 className="drone-title">
        🚁 Danh sách Drone
      </h2>

      <p className="drone-empty">
        Tính năng Drone đang được bảo trì và chuyển đổi sang hệ thống mới.
        <br />
        Vui lòng quay lại sau.
      </p>
    </div>
  );
}

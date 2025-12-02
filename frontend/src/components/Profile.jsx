import React, { useEffect, useState } from "react";
import "./Profile.css";
import { message } from "antd";
import { getProfile, updateProfile } from "../services/auth";
import AddressManager from "./AddressManager";

export default function Profile() {
  const [userData, setUserData] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const profile = await getProfile();
        setUserData(profile);
      } catch (err) {
        console.error("Load profile error:", err);
        message.error("Không thể tải thông tin người dùng.");
      }
    };

    fetchUser();
  }, []);

  const handleChange = (field, value) => {
    setUserData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSave = async () => {
    if (!userData) return;
    setSaving(true);
    try {
      const updated = await updateProfile({ email: userData.email });
      setUserData((prev) => ({ ...prev, ...updated }));
      message.success("Đã cập nhật thông tin.");
    } catch (err) {
      console.error("Update profile error:", err);
      message.error("Cập nhật thất bại.");
    } finally {
      setSaving(false);
    }
  };

  if (!userData) return <p style={{ textAlign: "center" }}>⏳ Đang tải thông tin...</p>;

  return (
    <div className="profile-page">
      <div className="profile-card">
        <h2>Thông tin cá nhân</h2>

        <label>Tên đăng nhập</label>
        <input type="text" value={userData.username} disabled />

        <label>Email</label>
        <input type="email" value={userData.email || ""} onChange={(e) => handleChange("email", e.target.value)} />

        <label>Vai trò</label>
        <input type="text" value={userData.role} disabled />

        <button className="save-btn" onClick={handleSave} disabled={saving}>
          {saving ? "Đang lưu..." : "Lưu thay đổi"}
        </button>
      </div>

      <AddressManager />
    </div>
  );
}

import React, { useState } from "react";
import { collection, doc, setDoc, getDocs } from "../../shims/firestore";
const db = null; // Mock db for shim
import "./AdminCreateRestaurant.css";

export default function AdminCreateRestaurant() {
  const [form, setForm] = useState({
    name: "",
    address: "",

    description: "",
    image: "",
    phone: "",
    password: "",
  });

  const [loading, setLoading] = useState(false);

  // ⚙️ Cập nhật input form
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  // 🟩 Tự tạo ID dạng r1, r2, r3... và không trùng
  const generateRestaurantId = async () => {
    const snapshot = await getDocs(collection(db, "restaurants"));
    let maxNumber = 0;

    snapshot.forEach((doc) => {
      const id = doc.id;
      if (id.startsWith("r")) {
        const num = parseInt(id.substring(1));
        if (!isNaN(num) && num > maxNumber) maxNumber = num;
      }
    });

    return `r${maxNumber + 1}`;
  };

  // 🟦 Geocoding địa chỉ → lat/lng
  const geocodeAddress = async (address) => {
    try {
      const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(
        address + ", Vietnam"
      )}&format=json&limit=1&countrycodes=vn`;

      const response = await fetch(url);
      const data = await response.json();

      if (data.length === 0) return null;

      return {
        lat: parseFloat(data[0].lat),
        lng: parseFloat(data[0].lon),
      };
    } catch {
      return null;
    }
  };

  // 🟥 Submit tạo nhà hàng
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!form.name || !form.phone || !form.password) {
      alert("⚠️ Vui lòng nhập đầy đủ Tên nhà hàng, Số điện thoại và Mật khẩu!");
      return;
    }

    try {
      setLoading(true);

      // 1️⃣ KIỂM TRA SỐ ĐIỆN THOẠI CÓ BỊ TRÙNG KHÔNG
      const usersRef = collection(db, "users");
      const userSnap = await getDocs(usersRef);

      let phoneExists = false;
      userSnap.forEach((d) => {
        if (d.data().phonenumber === form.phone) {
          phoneExists = true;
        }
      });

      if (phoneExists) {
        alert("❌ Số điện thoại này đã tồn tại! Vui lòng dùng số khác.");
        setLoading(false);
        return;
      }

      // 2️⃣ Tạo ID mới r1, r2, r3...
      const newId = await generateRestaurantId();

      // 3️⃣ Geocode địa chỉ
      const coords = await geocodeAddress(form.address);
      if (!coords) {
        alert("❌ Không tìm thấy tọa độ của địa chỉ! Vui lòng nhập đúng và cụ thể hơn.");
        setLoading(false);
        return;
      }

      // 4️⃣ Lưu nhà hàng vào Firestore
      await setDoc(doc(db, "restaurants", newId), {
        id: newId,
        name: form.name,
        address: form.address,

        description: form.description || "",
        image: form.image || "",
        latitude: coords.lat,
        longitude: coords.lng,
        status: "active",
      });

      // 5️⃣ Lưu user đăng nhập (role: restaurant)
      await setDoc(doc(db, "users", newId), {
        uid: newId,
        phonenumber: form.phone,
        password: form.password,
        role: "restaurant",
        restaurantId: newId,
        restaurantName: form.name,
        status: "active",
      });

      alert(`🎉 Tạo nhà hàng thành công! Mã nhà hàng: ${newId}`);

      // Reset form
      setForm({
        name: "",
        address: "",

        description: "",
        image: "",
        phone: "",
        password: "",
      });

    } catch (err) {
      console.error("🔥 Lỗi khi tạo:", err);
      alert("❌ Có lỗi xảy ra, vui lòng thử lại!");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="acr-container">
      <h2 className="acr-title">🏪 Tạo Nhà Hàng Mới</h2>

      <form className="acr-form" onSubmit={handleSubmit}>
        <div className="acr-grid">
          <label>
            Tên Nhà hàng
            <input name="name" value={form.name} onChange={handleChange} required />
          </label>

          <label>
            Địa chỉ
            <input name="address" value={form.address} onChange={handleChange} required />
          </label>



          <label>
            Mô tả
            <input name="description" value={form.description} onChange={handleChange} />
          </label>

          <label>
            Ảnh/logo
            <input name="image" value={form.image} onChange={handleChange} />
          </label>
        </div>

        <hr className="acr-divider" />

        <h3 className="acr-subtitle">🔑 Tài khoản đăng nhập Nhà hàng</h3>

        <div className="acr-grid">
          <label>
            Số điện thoại
            <input name="phone" value={form.phone} onChange={handleChange} required />
          </label>

          <label>
            Mật khẩu
            <input name="password" type="password" value={form.password} onChange={handleChange} required />
          </label>
        </div>

        <button className="acr-btn" type="submit" disabled={loading}>
          {loading ? "Đang tạo..." : "Tạo Nhà hàng"}
        </button>
      </form>
    </div>
  );
}

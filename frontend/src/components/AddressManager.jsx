import React, { useEffect, useMemo, useState } from "react";
import { MapContainer, TileLayer, Marker } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./AddressManager.css";
import { message } from "antd";
import {
  createUserAddress,
  getCommunes,
  getProvinces,
  getUserAddresses,
  updateAddressLocation,
} from "../services/addresses";

const defaultPosition = { lat: 10.776389, lng: 106.700806 };

const markerIcon = new L.Icon({
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [0, -41],
});

export default function AddressManager() {
  const [provinces, setProvinces] = useState([]);
  const [communes, setCommunes] = useState([]);
  const [form, setForm] = useState({
    street: "",
    provinceCode: "",
    communeCode: "",
    note: "",
  });
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [loadingList, setLoadingList] = useState(false);

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    try {
      setLoadingList(true);
      const [provinceList, addressList] = await Promise.all([
        getProvinces(),
        getUserAddresses(),
      ]);
      setProvinces(provinceList);
      if (provinceList.length > 0) {
        const defaultProvince = provinceList[0].code;
        setForm((prev) => ({ ...prev, provinceCode: defaultProvince }));
        await loadCommunes(defaultProvince);
      }
      setAddresses(addressList);
      if (addressList.length > 0) {
        setSelectedAddressId(addressList[0].id);
      }
    } catch (err) {
      console.error("Load address metadata error:", err);
      message.error("Không thể tải dữ liệu địa chỉ.");
    } finally {
      setLoadingList(false);
    }
  };

  const loadCommunes = async (provinceCode) => {
    try {
      const data = await getCommunes(provinceCode);
      setCommunes(data);
      if (data.length > 0) {
        setForm((prev) => ({ ...prev, communeCode: data[0].code }));
      } else {
        setForm((prev) => ({ ...prev, communeCode: "" }));
      }
    } catch (err) {
      console.error("Load communes error:", err);
      message.error("Không thể tải danh sách phường/xã.");
    }
  };

  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (field === "provinceCode") {
      loadCommunes(value);
    }
  };

  const selectedAddress = useMemo(
    () => addresses.find((addr) => addr.id === selectedAddressId),
    [addresses, selectedAddressId]
  );

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!form.street || !form.provinceCode || !form.communeCode) {
      message.warning("Vui lòng nhập đầy đủ thông tin.");
      return;
    }

    try {
      setSaving(true);
      const payload = {
        street: form.street.trim(),
        provinceCode: form.provinceCode,
        communeCode: form.communeCode,
        note: form.note?.trim() || "",
      };

      const created = await createUserAddress(payload);
      setAddresses((prev) => [created, ...prev]);
      setSelectedAddressId(created.id);
      setForm((prev) => ({ ...prev, street: "", note: "" }));
      message.success("Đã lưu địa chỉ. Vui lòng điều chỉnh marker nếu cần.");
    } catch (err) {
      console.error("Create address error:", err);
      message.error(err.response?.data?.message || "Không thể lưu địa chỉ.");
    } finally {
      setSaving(false);
    }
  };

  const handleMarkerDrag = async (position) => {
    if (!selectedAddress) return;
    try {
      await updateAddressLocation(selectedAddress.id, {
        lat: position.lat,
        lng: position.lng,
        source: "GEOCODE_USER_ADJUST",
      });
      setAddresses((prev) =>
        prev.map((addr) =>
          addr.id === selectedAddress.id
            ? { ...addr, lat: position.lat, lng: position.lng, source: "GEOCODE_USER_ADJUST" }
            : addr
        )
      );
      message.success("Đã cập nhật vị trí trên bản đồ.");
    } catch (err) {
      console.error("Update marker error:", err);
      message.error("Không thể cập nhật vị trí.");
    }
  };

  return (
    <div className="address-manager">
      <div className="address-form-card">
        <h3>🗂️ Lưu địa chỉ giao hàng</h3>
        <form onSubmit={handleCreate}>
          <label>Địa chỉ chi tiết</label>
          <input
            type="text"
            value={form.street}
            onChange={(e) => handleChange("street", e.target.value)}
            placeholder="Ví dụ: Số 4 Đường 30"
          />

          <label>Tỉnh/Thành</label>
          <select
            value={form.provinceCode}
            onChange={(e) => handleChange("provinceCode", e.target.value)}
          >
            {provinces.map((province) => (
              <option key={province.code} value={province.code}>
                {province.name}
              </option>
            ))}
          </select>

          <label>Phường/Xã</label>
          <select
            value={form.communeCode}
            onChange={(e) => handleChange("communeCode", e.target.value)}
          >
            {communes.map((commune) => (
              <option key={commune.code} value={commune.code}>
                {commune.name} {commune.districtName ? `- ${commune.districtName}` : ""}
              </option>
            ))}
          </select>

          <label>Ghi chú (chung cư, block...)</label>
          <input
            type="text"
            value={form.note}
            onChange={(e) => handleChange("note", e.target.value)}
            placeholder="Chung cư A, block B, lầu 10..."
          />

          <button type="submit" className="address-save-btn" disabled={saving}>
            {saving ? "Đang lưu..." : "Lưu địa chỉ"}
          </button>
        </form>
      </div>

      <div className="address-map-card">
        <div className="address-list-header">
          <h3>📍 Địa chỉ đã lưu</h3>
          {loadingList && <span>Đang tải...</span>}
        </div>
        <div className="address-list">
          {addresses.length === 0 ? (
            <p>Chưa có địa chỉ nào. Hãy thêm mới.</p>
          ) : (
            addresses.map((addr) => (
              <button
                key={addr.id}
                className={`address-item ${selectedAddressId === addr.id ? "active" : ""}`}
                onClick={() => setSelectedAddressId(addr.id)}
              >
                <span>{addr.fullAddress || addr.street}</span>
                <small>
                  Nguồn: {addr.source === "GEOCODE_ONLY" ? "Geocode" :
                    addr.source === "GEOCODE_USER_ADJUST" ? "Người dùng chỉnh" : "Shipper chỉnh"}
                </small>
              </button>
            ))
          )}
        </div>

        <div className="address-map-wrapper">
          {selectedAddress ? (
            <MapWithDraggableMarker address={selectedAddress} onDragEnd={handleMarkerDrag} />
          ) : (
            <p>Hãy chọn địa chỉ để xem trên bản đồ.</p>
          )}
        </div>
      </div>
    </div>
  );
}

function MapWithDraggableMarker({ address, onDragEnd }) {
  const [position, setPosition] = useState(() => ({
    lat: address.lat ?? defaultPosition.lat,
    lng: address.lng ?? defaultPosition.lng,
  }));

  useEffect(() => {
    setPosition({
      lat: address.lat ?? defaultPosition.lat,
      lng: address.lng ?? defaultPosition.lng,
    });
  }, [address.lat, address.lng]);

  return (
    <MapContainer center={[position.lat, position.lng]} zoom={16} scrollWheelZoom style={{ height: "300px", width: "100%" }}>
      <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      <Marker
        draggable
        icon={markerIcon}
        position={[position.lat, position.lng]}
        eventHandlers={{
          dragend: (event) => {
            const latLng = event.target.getLatLng();
            setPosition({ lat: latLng.lat, lng: latLng.lng });
            onDragEnd({ lat: latLng.lat, lng: latLng.lng });
          },
        }}
      />
    </MapContainer>
  );
}




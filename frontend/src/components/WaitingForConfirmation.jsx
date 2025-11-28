import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { MapContainer, TileLayer, Marker, Popup, Polyline } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./WaitingForConfirmation.css";

// Firebase import
// Firebase import (shimmed)
const db = null; // Mock db for shim
import { doc, getDoc, updateDoc } from "../shims/firestore";

// -------------------- HÀM HELPER --------------------
function formatTime(totalSeconds) {
  if (!totalSeconds || totalSeconds <= 0) return "Đã đến nơi";
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = Math.floor(totalSeconds % 60);
  if (minutes > 0) return `${minutes} phút ${seconds} giây`;
  return `${seconds} giây`;
}

function formatDistance(totalMeters) {
  if (!totalMeters || totalMeters <= 0) return "0 km";
  return `${(totalMeters / 1000).toFixed(1)} km`;
}

// -------------------- COMPONENT CHÍNH --------------------
export default function WaitingForConfirmation() {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [drone, setDrone] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const [restaurantPos, setRestaurantPos] = useState(null);
  const [restaurantInfo, setRestaurantInfo] = useState(null);
  const [customerPos, setCustomerPos] = useState(null);
  const [dronePos, setDronePos] = useState(null);

  const [pathLine, setPathLine] = useState([]); // đường bay
  const [remainingTime, setRemainingTime] = useState(null);
  const [remainingDistance, setRemainingDistance] = useState(null);

  // -------------------- FETCH FIRESTORE --------------------
  useEffect(() => {
    const fetchAllData = async () => {
      try {
        setLoading(true);

        const orderRef = doc(db, "orders", orderId);
        const orderSnap = await getDoc(orderRef);
        if (!orderSnap.exists()) throw new Error("Không tìm thấy đơn hàng");

        const dataOrder = orderSnap.data();
        setOrder({ id: orderSnap.id, ...dataOrder });

        // Drone
        if (dataOrder.droneId) {
          const droneRef = doc(db, "drones", dataOrder.droneId);
          const droneSnap = await getDoc(droneRef);
          if (droneSnap.exists()) setDrone(droneSnap.data());
        }

        // Restaurant
        if (dataOrder.restaurantId) {
          const restRef = doc(db, "restaurants", dataOrder.restaurantId);
          const restSnap = await getDoc(restRef);
          if (restSnap.exists()) {
            const d = restSnap.data();
            setRestaurantPos({ lat: d.latitude, lng: d.longitude });
          }
        }
        if (dataOrder.restaurantId) {
          const restRef = doc(db, "restaurants", dataOrder.restaurantId);
          const restSnap = await getDoc(restRef);
          if (restSnap.exists()) {
            const d = restSnap.data();

            setRestaurantPos({
              lat: d.latitude,
              lng: d.longitude,
            });

            setRestaurantInfo({
              name: d.name,
              address: d.address,
            });
          }
        }

        // Customer
        if (dataOrder.customer?.latitude && dataOrder.customer?.longitude) {
          setCustomerPos({
            lat: dataOrder.customer.latitude,
            lng: dataOrder.customer.longitude,
          });
        }

      } catch (err) {
        console.error("❌ Lỗi khi fetch:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchAllData();
  }, [orderId]);


  // -------------------- DRONE BAY THẲNG + VẼ ĐƯỜNG BAY --------------------
  useEffect(() => {
    if (!order || !restaurantPos || !customerPos) return;

    // VẼ ĐƯỜNG THẲNG GIỮA A → B
    setPathLine([
      [restaurantPos.lat, restaurantPos.lng],
      [customerPos.lat, customerPos.lng],
    ]);

    // Nếu đã giao → đứng tại nhà khách
    if (order.status === "Đã giao") {
      setDronePos(customerPos);
      setRemainingDistance(0);
      setRemainingTime(0);
      return;
    }

    if (order.status === "Đang giao" || order.status === "Đang giao bằng drone") {
      setDronePos(restaurantPos);

      const totalSteps = 250;
      const intervalMs = 200;
      let step = 0;

      const start = restaurantPos;
      const end = customerPos;

      const interval = setInterval(() => {
        if (step >= totalSteps) {
          clearInterval(interval);
          setDronePos(end);
          setRemainingDistance(0);
          setRemainingTime(0);
          return;
        }

        const t = step / totalSteps;

        // Linear LERP
        const lat = start.lat + (end.lat - start.lat) * t;
        const lng = start.lng + (end.lng - start.lng) * t;

        setDronePos({ lat, lng });

        // Khoảng cách còn lại (m)
        const dx = (end.lat - lat) * 111320;
        const dy = (end.lng - lng) * 111320 * Math.cos(lat * Math.PI / 180);
        const distanceLeft = Math.sqrt(dx * dx + dy * dy);

        setRemainingDistance(distanceLeft);
        setRemainingTime(distanceLeft / 12); // tốc độ 12 m/s

        step++;
      }, intervalMs);

      return () => clearInterval(interval);
    }

  }, [order, restaurantPos, customerPos]);


  // -------------------- CẬP NHẬT "ĐÃ NHẬN HÀNG" --------------------
  const handleReceived = async () => {
    try {
      const newOrder = { ...order, status: "Đã giao" };
      await updateDoc(doc(db, "orders", orderId), newOrder);

      if (order.droneId) {
        await updateDoc(doc(db, "drones", order.droneId), {
          status: "Rảnh",
          currentOrderId: null,
          destination: "",
          restaurantId: order.restaurantId,
        });
      }

      setOrder(newOrder);
      alert("Đã xác nhận giao hàng!");
      navigate("/order-history");
    } catch (err) {
      console.error("Không thể cập nhật đơn hàng:", err);
      alert("Không thể cập nhật!");
    }
  };


  // -------------------- RENDER --------------------
  if (loading) return <p>⏳ Đang tải đơn hàng...</p>;
  if (!order) return <p>❌ Không tìm thấy đơn hàng.</p>;
  if (!restaurantPos || !customerPos) return <p>❌ Thiếu tọa độ.</p>;

  const droneIcon = L.icon({
    iconUrl: "https://cdn-icons-png.flaticon.com/512/10419/10419013.png",
    iconSize: [40, 40],
  });

  const markerIcon = L.icon({
    iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    iconSize: [35, 35],
  });

  return (
    <div className="wfc-page">
      <h2>📦 Theo dõi đơn hàng #{order.id}</h2>

      <div className="wfc-container">

        {/* ==== PANEL INFO ==== */}
        <div className="wfc-info-panel">
          <div className="wfc-info-content">

            <h3>Chi tiết đơn hàng</h3>

            {/* —— KHUNG NHÀ HÀNG —— */}
            <div className="wfc-box">
              <h4 className="wfc-box-title"> Nhà hàng</h4>

              <div className="wfc-detail-row">
                <span>Tên:</span>
                <span>{restaurantInfo?.name || order.restaurantName}</span>
              </div>

              <div className="wfc-detail-row">
                <span>Địa chỉ:</span>
                <span className="wfc-text-wrap">{restaurantInfo?.address}</span>
              </div>
            </div>

            {/* —— KHUNG KHÁCH HÀNG —— */}
            <div className="wfc-box">
              <h4 className="wfc-box-title"> Khách hàng</h4>

              <div className="wfc-detail-row">
                <span>Tên:</span>
                <span>{order.customer?.name}</span>
              </div>
              <div className="wfc-detail-row">
                <span>Số điện thoại:</span>
                <span className="wfc-text-wrap">{order.customer?.phone}</span>
              </div>
              <div className="wfc-detail-row">
                <span>Địa chỉ:</span>
                <span className="wfc-text-wrap">{order.customer?.address}</span>
              </div>
            </div>

            {/* —— DANH SÁCH MÓN —— */}
            <div className="wfc-item-list">
              <strong>Món ăn:</strong>
              <ul>
                {order.items?.map(i => (
                  <li key={i.id} className="wfc-item-row">
                    <span>{i.quantity} x {i.name}</span>
                    <span className="wfc-item-price">
                      {(i.price * i.quantity).toLocaleString()}₫
                    </span>
                  </li>
                ))}
              </ul>
            </div>


            <p className="wfc-section-title">Tổng tiền: <strong>{order.total?.toLocaleString()}₫</strong></p>


            {(order.status === "Đang giao" || order.status === "Đang giao bằng drone") && (
              <>
                <h3 className="wfc-tracking-details">Theo dõi trực tiếp</h3>
                {drone && <p><strong>Drone:</strong> {drone?.name}</p>}
                <p><strong>Khoảng cách còn lại:</strong> {formatDistance(remainingDistance)}</p>
                <p><strong>Thời gian còn lại:</strong> {formatTime(remainingTime)}</p>
              </>
            )}


            {(order.status.includes("Đang giao") &&
              remainingDistance !== null &&
              remainingDistance < 80) && (
                <button className="wfc-btn-received" onClick={handleReceived}>
                  ✅ Đã nhận hàng
                </button>
              )}
          </div>
        </div>


        {/* ==== MAP ==== */}
        <div className="wfc-map-panel">
          <MapContainer
            center={dronePos || restaurantPos}
            zoom={15}
            style={{ height: "700px", width: "100%" }}
          >

            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* Vẽ đường thẳng */}
            {pathLine.length > 0 && (
              <Polyline positions={pathLine} color="blue" weight={4} opacity={0.7} />
            )}

            {/* Drone */}
            {dronePos && (
              <Marker position={dronePos} icon={droneIcon}>
                <Popup>🚁 Drone đang bay</Popup>
              </Marker>
            )}

            {/* Nhà hàng */}
            <Marker position={restaurantPos} icon={markerIcon}>
              <Popup>🍽 Nhà hàng</Popup>
            </Marker>

            {/* Khách */}
            <Marker position={customerPos} icon={markerIcon}>
              <Popup>🏠 Khách hàng</Popup>
            </Marker>

          </MapContainer>
        </div>

      </div>
    </div>
  );
}



/* import React, { useEffect, useState, useRef, useCallback } from "react";
import { useParams } from "react-router-dom";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "leaflet-routing-machine";
import "leaflet-routing-machine/dist/leaflet-routing-machine.css";
import "./WaitingForConfirmation.css";

// Firebase import
import { db } from "../firebase";
import { doc, getDoc, updateDoc } from "firebase/firestore";

// -------------------- HÀM HELPER --------------------
function formatTime(totalSeconds) {
  if (totalSeconds === null || totalSeconds === undefined) return "";
  if (totalSeconds < 1) return "Đã đến nơi";
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = Math.floor(totalSeconds % 60);
  if (minutes > 0) return `${minutes} phút ${seconds} giây`;
  return `${seconds} giây`;
}

function formatDistance(totalMeters) {
  if (totalMeters === null || totalMeters === undefined) return "";
  if (totalMeters < 1) return "0 km";
  const kilometers = totalMeters / 1000;
  return `${kilometers.toFixed(1)} km`;
}

// -------------------- COMPONENT: ROUTING MACHINE --------------------
function RoutingMachine({ from, to, onRouteFound }) {
  const map = useMap();
  const routingControlRef = useRef(null);
  useEffect(() => {
    if (!map || !from || !to) return;

    if (routingControlRef.current) {
      try {
        routingControlRef.current.getPlan()?.setWaypoints([]);
        map.removeControl(routingControlRef.current);
      } catch (e) {
        console.warn("⚠️ Không thể xóa routing control cũ:", e);
      }
      routingControlRef.current = null;
    }

    const control = L.Routing.control({
      router: L.Routing.osrmv1({
        serviceUrl: "https://routing.openstreetmap.de/routed-car/route/v1",
      }),
      waypoints: [L.latLng(from.lat, from.lng), L.latLng(to.lat, to.lng)],
      lineOptions: { styles: [{ color: "#007bff", weight: 5, opacity: 0.8 }] },
      addWaypoints: false,
      draggableWaypoints: false,
      fitSelectedRoutes: true,
      showAlternatives: false,
      show: false,          // ⛔ Ẩn bảng hướng dẫn
      createMarker: () => null,
    });


    control.on("routesfound", (e) => {
      if (e.routes && e.routes[0]) {
        const route = e.routes[0];
        onRouteFound({
          coordinates: route.coordinates,
          distance: route.summary.totalDistance,
          time: route.summary.totalTime,
        });
      }
    });

    // 🛡️ Thêm try-catch khi addTo map để ngăn lỗi removeLayer khi map null
    try {
      control.addTo(map);
      routingControlRef.current = control;
    } catch (e) {
      console.warn("⚠️ Không thể add routing control:", e);
    }

    return () => {
      if (routingControlRef.current) {
        try {
          routingControlRef.current.getPlan()?.setWaypoints([]);
          map.removeControl(routingControlRef.current);
        } catch (e) {
          console.warn("⚠️ Bỏ qua lỗi removeControl khi cleanup:", e);
        }
        routingControlRef.current = null;
      }
    };
  }, [map, from, to, onRouteFound]);


}

// -------------------- COMPONENT CHÍNH --------------------
export default function WaitingForConfirmation() {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [drone, setDrone] = useState(null);
  const [loading, setLoading] = useState(true);
  const [restaurantPos, setRestaurantPos] = useState(null);
  const [customerPos, setCustomerPos] = useState(null);
  const [dronePos, setDronePos] = useState(null);
  const [routePoints, setRoutePoints] = useState(null);
  const [totalDistance, setTotalDistance] = useState(null);
  const [totalTime, setTotalTime] = useState(null);
  const [remainingTime, setRemainingTime] = useState(null);
  const [remainingDistance, setRemainingDistance] = useState(null);

  // -------------------- FETCH DỮ LIỆU FIRESTORE --------------------
  useEffect(() => {
    const fetchAllData = async () => {
      try {
        setLoading(true);

        const orderRef = doc(db, "orders", orderId);
        const orderSnap = await getDoc(orderRef);
        if (!orderSnap.exists()) throw new Error("Không tìm thấy đơn hàng");

        const dataOrder = orderSnap.data();
        setOrder({ id: orderSnap.id, ...dataOrder });

        if (dataOrder.droneId) {
          const droneRef = doc(db, "drones", dataOrder.droneId);
          const droneSnap = await getDoc(droneRef);
          if (droneSnap.exists()) setDrone(droneSnap.data());
        }

        if (dataOrder.restaurantId) {
          const restRef = doc(db, "restaurants", dataOrder.restaurantId);
          const restSnap = await getDoc(restRef);
          if (restSnap.exists()) {
            const dataRest = restSnap.data();
            setRestaurantPos({ lat: dataRest.latitude, lng: dataRest.longitude });
          }
        }

        if (dataOrder.customer?.latitude && dataOrder.customer?.longitude) {
          setCustomerPos({
            lat: dataOrder.customer.latitude,
            lng: dataOrder.customer.longitude,
          });
        } else {
          console.error("❌ Lỗi: thiếu tọa độ khách hàng!");
        }
      } catch (err) {
        console.error("❌ Lỗi khi tải dữ liệu:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchAllData();
  }, [orderId]);

  // -------------------- MÔ PHỎNG DRONE BAY --------------------
  useEffect(() => {
    if (!order || !routePoints || !customerPos || !restaurantPos || totalTime === null || totalDistance === null)
      return;

    if (order.status === "Đang giao" || order.status === "Đang giao bằng drone") {
      setDronePos(restaurantPos);
      let currentStep = 0;
      const totalSteps = routePoints.length;
      const intervalTime = 200;

      const move = setInterval(() => {
        if (currentStep >= totalSteps) {
          clearInterval(move);
          setDronePos(customerPos);
          setRemainingTime(0);
          setRemainingDistance(0);
        } else {
          const currentPoint = routePoints[currentStep];
          setDronePos({ lat: currentPoint.lat, lng: currentPoint.lng });
          const progress = currentStep / totalSteps;
          setRemainingTime(totalTime * (1 - progress));
          setRemainingDistance(totalDistance * (1 - progress));
          currentStep++;
        }
      }, intervalTime);

      return () => clearInterval(move);
    }

    if (order.status === "Đã giao") {
      setDronePos(customerPos);
      setRemainingTime(0);
    }
  }, [order, routePoints, customerPos, restaurantPos, totalDistance, totalTime]);

  // -------------------- CẬP NHẬT TRẠNG THÁI ĐƠN --------------------
  const handleReceived = async () => {
    try {
      const updatedOrder = { ...order, status: "Đã giao" };
      await updateDoc(doc(db, "orders", orderId), updatedOrder);

      if (order.droneId) {
        await updateDoc(doc(db, "drones", order.droneId), {
          status: "Rảnh",

          currentOrderId: null,
          destination: "",

          restaurantId: order.restaurantId,
        });
      }

      setOrder(updatedOrder);
      alert("✅ Đơn hàng đã được giao thành công!");
    } catch (err) {
      console.error("❌ Lỗi khi cập nhật trạng thái:", err);
      alert("❌ Không thể cập nhật trạng thái đơn hàng!");
    }
  };

  const handleRouteFound = useCallback(({ coordinates, distance, time }) => {
    setRoutePoints(coordinates);
    setTotalDistance(distance);
    setTotalTime(time);
    setRemainingDistance(distance);
    setRemainingTime(time);
  }, []);

  // -------------------- RENDER --------------------
  if (loading) return <p>⏳ Đang tải dữ liệu đơn hàng và bản đồ...</p>;
  if (!order) return <p>❌ Không tìm thấy đơn hàng #{orderId}</p>;
  if (!restaurantPos || !customerPos)
    return <p>❌ Không thể tải tọa độ nhà hàng hoặc khách hàng.</p>;

  // Icon — thay icon bị 404 bằng icon mặc định
  const droneIcon = L.icon({
    iconUrl: "https://cdn-icons-png.flaticon.com/512/10419/10419013.png",
    iconSize: [40, 40],
  });
  const restaurantIcon = L.icon({
    iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    iconSize: [35, 35],
  });
  const customerIcon = L.icon({
    iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    iconSize: [35, 35],
  });

  return (
    <div className="wfc-page">
      <h2>📦 Theo dõi đơn hàng #{order.id}</h2>

      <div className="wfc-container">
        <div className="wfc-info-panel">
          <div className="wfc-info-content">
            <h3>Chi tiết đơn hàng</h3>
            <p><strong>Nhà hàng:</strong> {order.restaurantName}</p>
            <p><strong>Khách hàng:</strong> {order.customer?.name}</p>
            <p><strong>Địa chỉ:</strong> {order.customer?.address}</p>

            <div className="wfc-item-list">
              <strong>Món ăn đã đặt:</strong>
              <ul>
                {order.items?.map((item) => (
                  <li key={item.id}>
                    {item.quantity} x {item.name}
                  </li>
                ))}
              </ul>
            </div>

            <p><strong>Tổng tiền:</strong> {order.total?.toLocaleString()}₫</p>
            {drone && <p><strong>Drone:</strong> {drone.name}</p>}
            <p><strong>Trạng thái:</strong> {order.status}</p>

            <h3 className="wfc-tracking-details">Theo dõi trực tiếp</h3>
            <p><strong>Khoảng cách còn lại:</strong> {formatDistance(remainingDistance)}</p>
            <p><strong>Thời gian còn lại:</strong> {formatTime(remainingTime)}</p>
          </div>

          {(order.status === "Đang giao" || order.status === "Đang giao bằng drone") &&
            remainingDistance !== null &&
            remainingDistance < 80 && ( // < 80m mới hiện
              <button className="wfc-btn-received" onClick={handleReceived}>
                ✅ Đã nhận hàng
              </button>
            )}
        </div>

        <div className="wfc-map-panel">
          <MapContainer
            center={dronePos || restaurantPos}
            zoom={15}
            style={{ height: "700px", width: "100%" }}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; OpenStreetMap contributors'
            />
            {dronePos && <Marker position={dronePos} icon={droneIcon}><Popup>🚁 Drone đang giao hàng</Popup></Marker>}
            <Marker position={restaurantPos} icon={restaurantIcon}><Popup>🍽️ Nhà hàng</Popup></Marker>
            <Marker position={customerPos} icon={customerIcon}><Popup>🏠 Khách hàng</Popup></Marker>

            <RoutingMachine from={restaurantPos} to={customerPos} onRouteFound={handleRouteFound} />
          </MapContainer>
        </div>
      </div>
    </div>
  );
}
*/
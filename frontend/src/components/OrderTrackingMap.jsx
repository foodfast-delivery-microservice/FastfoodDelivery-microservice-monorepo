import React, { useCallback, useEffect, useMemo, useState } from "react";
import { MapContainer, Marker, Popup, Polyline, TileLayer } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { getMissionByOrderId, getTrackingByOrderId } from "../services/droneApi";
import "./OrderTrackingMap.css";

const createIcon = (url) =>
  new L.Icon({
    iconUrl: url,
    shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
  });

const droneIcon = createIcon(
  "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png"
);
const pickupIcon = createIcon(
  "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png"
);
const deliveryIcon = createIcon(
  "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png"
);
const baseIcon = createIcon(
  "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-grey.png"
);

const formatCoordinate = (value) => {
  if (value === null || value === undefined) return "—";
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed.toFixed(5) : "—";
};

export default function OrderTrackingMap({
  orderId,
  deliveryAddress,
  showHeader = true,
  showControls = true,
  autoRefreshInterval = 10000,
  onMissionChange,
  onTrackingChange,
  onTrackingError,
}) {
  const [mission, setMission] = useState(null);
  const [tracking, setTracking] = useState(null);
  const [trackingLoading, setTrackingLoading] = useState(false);
  const [trackingError, setTrackingError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const fetchMission = useCallback(async () => {
    if (!orderId) return;
    try {
      const data = await getMissionByOrderId(orderId);
      setMission(data);
      onMissionChange?.(data || null);
    } catch (err) {
      setMission(null);
      onMissionChange?.(null);
    }
  }, [orderId]);

  const fetchTracking = useCallback(async () => {
    if (!orderId) return;
    try {
      setTrackingLoading(true);
      const data = await getTrackingByOrderId(orderId);
      setTracking(data);
      setTrackingError(null);
      onTrackingChange?.(data || null);
      onTrackingError?.(null);
    } catch (err) {
      let errorMsg;
      if (err?.response?.status === 404) {
        setTracking(null);
        errorMsg = "Chưa có nhiệm vụ drone nào cho đơn này.";
        setTrackingError(errorMsg);
      } else if (err?.response?.status === 403) {
        setTracking(null);
        errorMsg = "Bạn không có quyền xem tracking cho đơn này.";
        setTrackingError(errorMsg);
      } else {
        console.error("Lỗi tracking drone:", err);
        errorMsg = "Không thể tải thông tin tracking drone.";
        setTrackingError(errorMsg);
      }
      onTrackingChange?.(null);
      onTrackingError?.(errorMsg);
    } finally {
      setTrackingLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    fetchMission();
    fetchTracking();
  }, [fetchMission, fetchTracking]);

  useEffect(() => {
    if (!autoRefresh || !orderId) return;
    const timer = setInterval(() => {
      fetchTracking();
    }, autoRefreshInterval);
    return () => clearInterval(timer);
  }, [autoRefresh, autoRefreshInterval, fetchTracking, orderId]);

  const mapData = useMemo(() => {
    if (!mission && !tracking) return null;

    const points = [];
    let center = null;

    if (tracking?.currentLatitude != null && tracking?.currentLongitude != null) {
      points.push([tracking.currentLatitude, tracking.currentLongitude]);
      center = [tracking.currentLatitude, tracking.currentLongitude];
    }

    if (mission?.pickupLatitude != null && mission?.pickupLongitude != null) {
      points.push([mission.pickupLatitude, mission.pickupLongitude]);
      if (!center) center = [mission.pickupLatitude, mission.pickupLongitude];
    }

    if (mission?.deliveryLatitude != null && mission?.deliveryLongitude != null) {
      points.push([mission.deliveryLatitude, mission.deliveryLongitude]);
    }

    if (tracking?.baseLatitude != null && tracking?.baseLongitude != null) {
      points.push([tracking.baseLatitude, tracking.baseLongitude]);
    }

    if (!center) center = [10.776389, 106.700806];
    return { center, points };
  }, [mission, tracking]);

  if (!orderId) {
    return <p className="tracking-no-data">Không có mã đơn để hiển thị tracking.</p>;
  }

  return (
    <div className="tracking-widget">
      {showHeader && <h3 className="section-title">🚁 Theo dõi Drone</h3>}

      {trackingLoading ? (
        <p className="tracking-no-data">Đang tải dữ liệu tracking...</p>
      ) : tracking ? (
        <div className="tracking-info-box">
          <ul className="tracking-info-list">
            <li>
              <b>Drone:</b> {tracking.droneSerialNumber || tracking.droneId || "—"}
            </li>
            <li>
              <b>Pin:</b> {tracking.batteryLevel ?? "—"}%
            </li>
            <li>
              <b>Trạng thái:</b> {tracking.status || "—"}
            </li>
            <li>
              <b>Vị trí:</b> Lat {formatCoordinate(tracking.currentLatitude)} / Lon{" "}
              {formatCoordinate(tracking.currentLongitude)}
            </li>
            <li>
              <b>ETA:</b>{" "}
              {tracking.estimatedArrivalMinutes != null
                ? `${tracking.estimatedArrivalMinutes} phút`
                : "Đang tính toán"}
            </li>
          </ul>
        </div>
      ) : (
        <p className="tracking-no-data">{trackingError || "Chưa có dữ liệu tracking."}</p>
      )}

      {mapData && (
        <div className="tracking-map-container">
          <h4>🗺️ Bản đồ theo dõi</h4>
          <MapContainer
            center={mapData.center}
            zoom={13}
            style={{ height: "360px", width: "100%", borderRadius: "8px", marginTop: "10px" }}
            scrollWheelZoom
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {tracking?.currentLatitude != null && tracking?.currentLongitude != null && (
              <Marker
                position={[tracking.currentLatitude, tracking.currentLongitude]}
                icon={droneIcon}
              >
                <Popup>
                  <strong>🚁 Drone hiện tại</strong>
                  <br />
                  {tracking.droneSerialNumber || tracking.droneId || "—"}
                  <br />
                  Pin: {tracking.batteryLevel ?? "—"}%
                  <br />
                  Trạng thái: {tracking.status || "—"}
                </Popup>
              </Marker>
            )}

            {mission?.pickupLatitude != null && mission?.pickupLongitude != null && (
              <Marker position={[mission.pickupLatitude, mission.pickupLongitude]} icon={pickupIcon}>
                <Popup>
                  <strong>📍 Điểm lấy hàng</strong>
                </Popup>
              </Marker>
            )}

            {mission?.deliveryLatitude != null && mission?.deliveryLongitude != null && (
              <Marker
                position={[mission.deliveryLatitude, mission.deliveryLongitude]}
                icon={deliveryIcon}
              >
                <Popup>
                  <strong>🏠 Điểm giao hàng</strong>
                  <br />
                  {deliveryAddress || "Địa chỉ giao hàng"}
                </Popup>
              </Marker>
            )}

            {tracking?.baseLatitude != null && tracking?.baseLongitude != null && (
              <Marker position={[tracking.baseLatitude, tracking.baseLongitude]} icon={baseIcon}>
                <Popup>
                  <strong>🏠 Base của drone</strong>
                </Popup>
              </Marker>
            )}

            {mission && tracking?.baseLatitude != null && mission.pickupLatitude != null && (
              <>
                <Polyline
                  positions={[
                    [tracking.baseLatitude, tracking.baseLongitude],
                    [mission.pickupLatitude, mission.pickupLongitude],
                  ]}
                  color="blue"
                  dashArray="10, 5"
                  weight={2}
                  opacity={0.5}
                />
                {mission.deliveryLatitude != null && (
                  <>
                    <Polyline
                      positions={[
                        [mission.pickupLatitude, mission.pickupLongitude],
                        [mission.deliveryLatitude, mission.deliveryLongitude],
                      ]}
                      color="green"
                      dashArray="10, 5"
                      weight={2}
                      opacity={0.5}
                    />
                    <Polyline
                      positions={[
                        [mission.deliveryLatitude, mission.deliveryLongitude],
                        [tracking.baseLatitude, tracking.baseLongitude],
                      ]}
                      color="grey"
                      dashArray="10, 5"
                      weight={2}
                      opacity={0.5}
                    />
                  </>
                )}
              </>
            )}

            {tracking?.currentLatitude != null && tracking?.currentLongitude != null && (
              <>
                {tracking.status === "ASSIGNED" && mission?.pickupLatitude != null && (
                  <Polyline
                    positions={[
                      [tracking.currentLatitude, tracking.currentLongitude],
                      [mission.pickupLatitude, mission.pickupLongitude],
                    ]}
                    color="red"
                    weight={5}
                    opacity={0.9}
                  />
                )}

                {tracking.status === "IN_PROGRESS" && mission?.deliveryLatitude != null && (
                  <Polyline
                    positions={[
                      [tracking.currentLatitude, tracking.currentLongitude],
                      [mission.deliveryLatitude, mission.deliveryLongitude],
                    ]}
                    color="orange"
                    weight={5}
                    opacity={0.9}
                  />
                )}

                {tracking.status === "RETURNING" && tracking?.baseLatitude != null && (
                  <Polyline
                    positions={[
                      [tracking.currentLatitude, tracking.currentLongitude],
                      [tracking.baseLatitude, tracking.baseLongitude],
                    ]}
                    color="purple"
                    weight={5}
                    opacity={0.9}
                  />
                )}
              </>
            )}
          </MapContainer>
        </div>
      )}

      {showControls && (
        <div className="tracking-actions">
          <button className="btn-refresh" onClick={fetchTracking} disabled={trackingLoading}>
            {trackingLoading ? "Đang cập nhật..." : "🔄 Làm mới"}
          </button>
          <label className="tracking-auto">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            Tự động cập nhật mỗi {autoRefreshInterval / 1000}s
          </label>
        </div>
      )}
    </div>
  );
}



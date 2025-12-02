import React, { useEffect, useState, useCallback, useMemo } from "react";
import "./AdminDroneManager.css";
import { message } from "antd";
import {
  getDrones,
  updateDroneBattery,
  updateDroneState,
  getMissionsByDroneId,
  getTrackingByMissionId,
} from "../../services/droneApi";
import { MapContainer, TileLayer, Marker, Popup, Polyline } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

const DRONE_STATES = ["IDLE", "DELIVERING", "RETURNING", "CHARGING", "MAINTENANCE"];

const formatCoordinate = (value) => {
  if (value === null || value === undefined) return "—";
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed.toFixed(4) : "—";
};

// Leaflet marker icons
const droneIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const pickupIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const deliveryIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const baseIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-grey.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

export default function AdminDroneManager() {
  const [drones, setDrones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [batteryDraft, setBatteryDraft] = useState({});
  const [stateDraft, setStateDraft] = useState({});
  const [rowSaving, setRowSaving] = useState({});
  const [selectedDrone, setSelectedDrone] = useState(null);
  const [missions, setMissions] = useState([]);
  const [trackingData, setTrackingData] = useState({});
  const [trackingLoading, setTrackingLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState("ALL"); // ALL, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED

  const loadDrones = useCallback(async () => {
    try {
      setLoading(true);
      const list = await getDrones();
      setDrones(list);
      setBatteryDraft({});
      setStateDraft({});
    } catch (err) {
      console.error("Lỗi tải danh sách drone:", err);
      message.error("Không thể tải danh sách drone");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDrones();
  }, [loadDrones]);

  // Auto-refresh tracking data when modal is open (only for active missions)
  useEffect(() => {
    if (!selectedDrone) return;

    const refreshTracking = async () => {
      try {
        const missionsList = await getMissionsByDroneId(selectedDrone.id);
        setMissions(missionsList);

        // Only refresh tracking for active missions (not COMPLETED or CANCELLED)
        const activeMissions = missionsList.filter(
          (m) => m.status === "ASSIGNED" || m.status === "IN_PROGRESS"
        );

        const trackingPromises = activeMissions.map(async (mission) => {
          try {
            const tracking = await getTrackingByMissionId(mission.id);
            return { missionId: mission.id, tracking };
          } catch (err) {
            return { missionId: mission.id, tracking: null };
          }
        });

        const trackingResults = await Promise.all(trackingPromises);
        const trackingMap = {};
        trackingResults.forEach(({ missionId, tracking }) => {
          if (tracking) trackingMap[missionId] = tracking;
        });
        setTrackingData(trackingMap);
      } catch (err) {
        console.error("Error refreshing tracking:", err);
      }
    };

    refreshTracking();
    const interval = setInterval(refreshTracking, 10000); // Refresh every 10 seconds

    return () => clearInterval(interval);
  }, [selectedDrone]);

  const handleBatterySave = async (droneId) => {
    const nextLevel = batteryDraft[droneId];
    if (nextLevel === undefined || nextLevel === null || nextLevel === "") {
      message.warning("Nhập phần trăm pin trước khi cập nhật");
      return;
    }
    if (nextLevel < 0 || nextLevel > 100) {
      message.warning("Mức pin phải nằm trong khoảng 0 - 100%");
      return;
    }
    setRowSaving((prev) => ({ ...prev, [droneId]: true }));
    try {
      await updateDroneBattery(droneId, nextLevel);
      message.success("Đã cập nhật pin drone");
      await loadDrones();
    } catch (err) {
      console.error("Lỗi cập nhật pin:", err);
      const errorMsg =
        err?.response?.data?.message || err.message || "Không thể cập nhật pin drone";
      message.error(errorMsg);
    } finally {
      setRowSaving((prev) => ({ ...prev, [droneId]: false }));
    }
  };

  const handleStateSave = async (droneId) => {
    const nextState = stateDraft[droneId];
    if (!nextState) {
      message.warning("Chọn trạng thái trước khi cập nhật");
      return;
    }
    setRowSaving((prev) => ({ ...prev, [droneId]: true }));
    try {
      await updateDroneState(droneId, nextState);
      message.success("Đã cập nhật trạng thái drone");
      await loadDrones();
    } catch (err) {
      console.error("Lỗi cập nhật trạng thái:", err);
      const errorMsg =
        err?.response?.data?.message || err.message || "Không thể cập nhật trạng thái drone";
      message.error(errorMsg);
    } finally {
      setRowSaving((prev) => ({ ...prev, [droneId]: false }));
    }
  };

  const handleViewTracking = async (drone) => {
    setSelectedDrone(drone);
    setStatusFilter("ALL");
    setTrackingLoading(true);
    try {
      // Load ALL missions for this drone (including COMPLETED and CANCELLED)
      const missionsList = await getMissionsByDroneId(drone.id);
      setMissions(missionsList);

      // Load tracking for active missions (ASSIGNED, IN_PROGRESS, RETURNING)
      // Also try to load for COMPLETED missions to show final route
      const missionsToTrack = missionsList.filter(
        (m) => m.status === "ASSIGNED" || m.status === "IN_PROGRESS" || m.status === "COMPLETED"
      );
      
      const trackingPromises = missionsToTrack.map(async (mission) => {
        try {
          const tracking = await getTrackingByMissionId(mission.id);
          return { missionId: mission.id, tracking };
        } catch (err) {
          // For completed missions, tracking might not be available, that's OK
          console.log(`Tracking not available for mission ${mission.id} (status: ${mission.status})`);
          return { missionId: mission.id, tracking: null };
        }
      });

      const trackingResults = await Promise.all(trackingPromises);
      const trackingMap = {};
      trackingResults.forEach(({ missionId, tracking }) => {
        if (tracking) trackingMap[missionId] = tracking;
      });
      setTrackingData(trackingMap);
    } catch (err) {
      console.error("Lỗi tải missions:", err);
      message.error("Không thể tải thông tin missions");
    } finally {
      setTrackingLoading(false);
    }
  };

  const handleCloseTracking = () => {
    setSelectedDrone(null);
    setMissions([]);
    setTrackingData({});
  };

  // Filter missions by status
  const filteredMissions = useMemo(() => {
    if (statusFilter === "ALL") return missions;
    return missions.filter((m) => m.status === statusFilter);
  }, [missions, statusFilter]);

  // Calculate map center and bounds for selected drone
  // Show active mission first, otherwise show most recent mission
  const mapData = useMemo(() => {
    if (!selectedDrone || filteredMissions.length === 0) return null;

    // Find active mission first
    let selectedMission = filteredMissions.find(
      (m) => m.status === "ASSIGNED" || m.status === "IN_PROGRESS"
    );

    // If no active mission, use the first filtered mission (or most recent)
    if (!selectedMission) {
      selectedMission = filteredMissions[0];
    }

    if (!selectedMission) {
      // If no mission at all, center on drone's current position or base
      const lat = selectedDrone.currentLatitude || selectedDrone.baseLatitude;
      const lng = selectedDrone.currentLongitude || selectedDrone.baseLongitude;
      if (lat && lng) {
        return { center: [lat, lng], zoom: 13 };
      }
      return null;
    }

    const tracking = trackingData[selectedMission.id];
    const points = [];

    // Base
    if (tracking?.baseLatitude && tracking?.baseLongitude) {
      points.push([tracking.baseLatitude, tracking.baseLongitude]);
    } else if (selectedDrone.baseLatitude && selectedDrone.baseLongitude) {
      points.push([selectedDrone.baseLatitude, selectedDrone.baseLongitude]);
    }

    // Pickup
    if (selectedMission.pickupLatitude && selectedMission.pickupLongitude) {
      points.push([selectedMission.pickupLatitude, selectedMission.pickupLongitude]);
    }

    // Delivery
    if (selectedMission.deliveryLatitude && selectedMission.deliveryLongitude) {
      points.push([selectedMission.deliveryLatitude, selectedMission.deliveryLongitude]);
    }

    // Current position (only for active missions)
    if (selectedMission.status === "ASSIGNED" || selectedMission.status === "IN_PROGRESS") {
      if (tracking?.currentLatitude && tracking?.currentLongitude) {
        points.push([tracking.currentLatitude, tracking.currentLongitude]);
      } else if (selectedDrone.currentLatitude && selectedDrone.currentLongitude) {
        points.push([selectedDrone.currentLatitude, selectedDrone.currentLongitude]);
      }
    }

    if (points.length === 0) return null;

    // Calculate center
    const avgLat = points.reduce((sum, [lat]) => sum + lat, 0) / points.length;
    const avgLng = points.reduce((sum, [, lng]) => sum + lng, 0) / points.length;

    return {
      center: [avgLat, avgLng],
      zoom: 13,
      points,
      mission: selectedMission,
      tracking,
    };
  }, [selectedDrone, filteredMissions, trackingData]);

  return (
    <div className="admin-drones-page">
      <div className="drone-header">
        <div>
          <h1 className="page-title">🚁 Quản lý Drone</h1>
          <p className="page-subtitle">
            Theo dõi tình trạng drone, pin, tọa độ và điều chỉnh trạng thái vận hành.
          </p>
        </div>
        <button className="refresh-btn" onClick={loadDrones} disabled={loading}>
          {loading ? "Đang tải..." : "↻ Làm mới"}
        </button>
      </div>

      {drones.length === 0 && !loading ? (
        <div className="empty-state">
          <p>Chưa có drone nào được đăng ký.</p>
          <p>Hãy dùng REST API để tạo drone, sau đó bấm làm mới để cập nhật danh sách.</p>
        </div>
      ) : (
        <div className="drone-table-wrapper">
          <table className="drone-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Serial</th>
                <th>Model</th>
                <th>Pin (%)</th>
                <th>Trạng thái</th>
                <th>Vị trí hiện tại</th>
                <th>Base</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: 40 }}>
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : (
                drones.map((drone) => (
                  <tr key={drone.id}>
                    <td>#{drone.id}</td>
                    <td>{drone.serialNumber || "—"}</td>
                    <td>{drone.model || "—"}</td>
                    <td>
                      <div className="inline-control">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          placeholder="0-100"
                          value={
                            batteryDraft[drone.id] ??
                            (drone.batteryLevel ?? "")
                          }
                          onChange={(e) =>
                            setBatteryDraft((prev) => ({
                              ...prev,
                              [drone.id]: e.target.value === "" ? "" : Number(e.target.value),
                            }))
                          }
                        />
                        <button
                          onClick={() => handleBatterySave(drone.id)}
                          disabled={rowSaving[drone.id]}
                        >
                          {rowSaving[drone.id] ? "Đang lưu..." : "Lưu"}
                        </button>
                      </div>
                    </td>
                    <td>
                      <div className="inline-control">
                        <select
                          value={stateDraft[drone.id] ?? drone.state ?? ""}
                          onChange={(e) =>
                            setStateDraft((prev) => ({
                              ...prev,
                              [drone.id]: e.target.value,
                            }))
                          }
                        >
                          <option value="">— Chọn trạng thái —</option>
                          {DRONE_STATES.map((state) => (
                            <option key={state} value={state}>
                              {state}
                            </option>
                          ))}
                        </select>
                        <button
                          onClick={() => handleStateSave(drone.id)}
                          disabled={rowSaving[drone.id]}
                        >
                          {rowSaving[drone.id] ? "Đang lưu..." : "Cập nhật"}
                        </button>
                      </div>
                    </td>
                    <td>
                      <div>
                        Lat: {formatCoordinate(drone.currentLatitude)}
                        <br />
                        Lon: {formatCoordinate(drone.currentLongitude)}
                      </div>
                    </td>
                    <td>
                      <div>
                        Lat: {formatCoordinate(drone.baseLatitude)}
                        <br />
                        Lon: {formatCoordinate(drone.baseLongitude)}
                      </div>
                    </td>
                    <td>
                      <button
                        className="tracking-btn"
                        onClick={() => handleViewTracking(drone)}
                        disabled={rowSaving[drone.id]}
                      >
                        🗺️ Xem tracking
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Tracking Modal */}
      {selectedDrone && (
        <div className="tracking-modal-overlay" onClick={handleCloseTracking}>
          <div className="tracking-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="tracking-modal-header">
              <h2>
                🚁 Tracking Drone: {selectedDrone.serialNumber || `#${selectedDrone.id}`}
              </h2>
              <button className="close-btn" onClick={handleCloseTracking}>
                ✕
              </button>
            </div>

            <div className="tracking-modal-body">
              {trackingLoading ? (
                <p>⏳ Đang tải thông tin tracking...</p>
              ) : missions.length === 0 ? (
                <p>📭 Drone này chưa có missions nào.</p>
              ) : (
                <>
                  <div className="missions-list">
                    <div className="missions-header">
                      <h3>📋 Missions ({missions.length} tổng cộng)</h3>
                      <div className="status-filter">
                        <label>Lọc theo trạng thái:</label>
                        <select
                          value={statusFilter}
                          onChange={(e) => setStatusFilter(e.target.value)}
                          className="status-filter-select"
                        >
                          <option value="ALL">Tất cả ({missions.length})</option>
                          <option value="ASSIGNED">
                            ASSIGNED ({missions.filter((m) => m.status === "ASSIGNED").length})
                          </option>
                          <option value="IN_PROGRESS">
                            IN_PROGRESS ({missions.filter((m) => m.status === "IN_PROGRESS").length})
                          </option>
                          <option value="COMPLETED">
                            COMPLETED ({missions.filter((m) => m.status === "COMPLETED").length})
                          </option>
                          <option value="CANCELLED">
                            CANCELLED ({missions.filter((m) => m.status === "CANCELLED").length})
                          </option>
                        </select>
                      </div>
                    </div>
                    <div className="missions-grid">
                      {filteredMissions.length === 0 ? (
                        <p className="no-missions">Không có missions nào với trạng thái đã chọn.</p>
                      ) : (
                        filteredMissions.map((mission) => {
                          const tracking = trackingData[mission.id];
                          const isActive =
                            mission.status === "ASSIGNED" ||
                            mission.status === "IN_PROGRESS";
                          return (
                            <div
                              key={mission.id}
                              className={`mission-card ${isActive ? "active" : ""}`}
                            >
                              <div className="mission-header">
                                <strong>Mission #{mission.id}</strong>
                                <span className={`status-badge ${mission.status.toLowerCase()}`}>
                                  {mission.status}
                                </span>
                              </div>
                              <div className="mission-details">
                                <p>📦 Order: #{mission.orderId}</p>
                                {tracking && (
                                  <>
                                    <p>🔋 Pin: {tracking.batteryLevel ?? "—"}%</p>
                                    {tracking.estimatedArrivalMinutes != null && (
                                      <p>⏱️ ETA: {tracking.estimatedArrivalMinutes} phút</p>
                                    )}
                                  </>
                                )}
                                {mission.distanceKm != null && (
                                  <p>📏 Khoảng cách: {mission.distanceKm.toFixed(2)} km</p>
                                )}
                                {mission.estimatedDurationMinutes != null && (
                                  <p>⏱️ Thời gian ước tính: {mission.estimatedDurationMinutes} phút</p>
                                )}
                                {mission.startedAt && (
                                  <p>🕐 Bắt đầu: {new Date(mission.startedAt).toLocaleString("vi-VN")}</p>
                                )}
                                {mission.completedAt && (
                                  <p>✅ Hoàn thành: {new Date(mission.completedAt).toLocaleString("vi-VN")}</p>
                                )}
                              </div>
                            </div>
                          );
                        })
                      )}
                    </div>
                  </div>

                  {mapData && (
                    <div className="tracking-map-section">
                      <h3>🗺️ Bản đồ tracking</h3>
                      <MapContainer
                        center={mapData.center}
                        zoom={mapData.zoom}
                        style={{ height: "500px", width: "100%", borderRadius: "8px" }}
                        scrollWheelZoom={true}
                      >
                        <TileLayer
                          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />

                        {/* Drone current position */}
                        {mapData.tracking?.currentLatitude != null &&
                          mapData.tracking?.currentLongitude != null && (
                            <Marker
                              position={[
                                mapData.tracking.currentLatitude,
                                mapData.tracking.currentLongitude,
                              ]}
                              icon={droneIcon}
                            >
                              <Popup>
                                <strong>🚁 Drone hiện tại</strong>
                                <br />
                                Pin: {mapData.tracking.batteryLevel ?? "—"}%
                                <br />
                                Trạng thái: {mapData.tracking.status || "—"}
                              </Popup>
                            </Marker>
                          )}

                        {/* Pickup location */}
                        {mapData.mission?.pickupLatitude != null &&
                          mapData.mission?.pickupLongitude != null && (
                            <Marker
                              position={[
                                mapData.mission.pickupLatitude,
                                mapData.mission.pickupLongitude,
                              ]}
                              icon={pickupIcon}
                            >
                              <Popup>
                                <strong>📍 Điểm lấy hàng</strong>
                              </Popup>
                            </Marker>
                          )}

                        {/* Delivery location */}
                        {mapData.mission?.deliveryLatitude != null &&
                          mapData.mission?.deliveryLongitude != null && (
                            <Marker
                              position={[
                                mapData.mission.deliveryLatitude,
                                mapData.mission.deliveryLongitude,
                              ]}
                              icon={deliveryIcon}
                            >
                              <Popup>
                                <strong>🏠 Điểm giao hàng</strong>
                              </Popup>
                            </Marker>
                          )}

                        {/* Base location */}
                        {mapData.tracking?.baseLatitude != null &&
                          mapData.tracking?.baseLongitude != null && (
                            <Marker
                              position={[mapData.tracking.baseLatitude, mapData.tracking.baseLongitude]}
                              icon={baseIcon}
                            >
                              <Popup>
                                <strong>🏠 Base</strong>
                              </Popup>
                            </Marker>
                          )}

                        {/* Route lines */}
                        {mapData.mission && (
                          <>
                            {/* Full route (dashed) */}
                            {mapData.tracking?.baseLatitude != null &&
                              mapData.mission.pickupLatitude != null &&
                              mapData.mission.deliveryLatitude != null && (
                                <>
                                  {/* Base → Pickup */}
                                  <Polyline
                                    positions={[
                                      [mapData.tracking.baseLatitude, mapData.tracking.baseLongitude],
                                      [mapData.mission.pickupLatitude, mapData.mission.pickupLongitude],
                                    ]}
                                    color="blue"
                                    dashArray="10, 5"
                                    weight={2}
                                    opacity={0.5}
                                  />
                                  {/* Pickup → Delivery */}
                                  <Polyline
                                    positions={[
                                      [mapData.mission.pickupLatitude, mapData.mission.pickupLongitude],
                                      [
                                        mapData.mission.deliveryLatitude,
                                        mapData.mission.deliveryLongitude,
                                      ],
                                    ]}
                                    color="green"
                                    dashArray="10, 5"
                                    weight={2}
                                    opacity={0.5}
                                  />
                                  {/* Delivery → Base */}
                                  <Polyline
                                    positions={[
                                      [
                                        mapData.mission.deliveryLatitude,
                                        mapData.mission.deliveryLongitude,
                                      ],
                                      [mapData.tracking.baseLatitude, mapData.tracking.baseLongitude],
                                    ]}
                                    color="grey"
                                    dashArray="10, 5"
                                    weight={2}
                                    opacity={0.5}
                                  />
                                </>
                              )}

                        {/* Real-time route (solid) - only for active missions */}
                        {mapData.mission.status !== "COMPLETED" &&
                          mapData.mission.status !== "CANCELLED" &&
                          mapData.tracking?.currentLatitude != null &&
                          mapData.tracking?.currentLongitude != null && (
                            <>
                              {/* ASSIGNED: Base → Pickup */}
                              {mapData.tracking.status === "ASSIGNED" &&
                                mapData.mission.pickupLatitude != null && (
                                  <Polyline
                                    positions={[
                                      [
                                        mapData.tracking.currentLatitude,
                                        mapData.tracking.currentLongitude,
                                      ],
                                      [mapData.mission.pickupLatitude, mapData.mission.pickupLongitude],
                                    ]}
                                    color="red"
                                    weight={5}
                                    opacity={0.9}
                                  />
                                )}
                              {/* IN_PROGRESS: Pickup → Delivery or Delivery → Base */}
                              {mapData.tracking.status === "IN_PROGRESS" && (
                                <>
                                  {mapData.mission.deliveryLatitude != null && (
                                    <Polyline
                                      positions={[
                                        [
                                          mapData.tracking.currentLatitude,
                                          mapData.tracking.currentLongitude,
                                        ],
                                        [
                                          mapData.mission.deliveryLatitude,
                                          mapData.mission.deliveryLongitude,
                                        ],
                                      ]}
                                      color="orange"
                                      weight={5}
                                      opacity={0.9}
                                    />
                                  )}
                                  {/* If returning to base */}
                                  {mapData.tracking.baseLatitude != null && (
                                    <Polyline
                                      positions={[
                                        [
                                          mapData.tracking.currentLatitude,
                                          mapData.tracking.currentLongitude,
                                        ],
                                        [mapData.tracking.baseLatitude, mapData.tracking.baseLongitude],
                                      ]}
                                      color="purple"
                                      weight={5}
                                      opacity={0.9}
                                    />
                                  )}
                                </>
                              )}
                            </>
                          )}
                        
                        {/* For completed missions, show full route as solid line */}
                        {mapData.mission.status === "COMPLETED" && (
                          <>
                            {mapData.tracking?.baseLatitude != null &&
                              mapData.mission.pickupLatitude != null && (
                                <Polyline
                                  positions={[
                                    [mapData.tracking.baseLatitude, mapData.tracking.baseLongitude],
                                    [mapData.mission.pickupLatitude, mapData.mission.pickupLongitude],
                                  ]}
                                  color="blue"
                                  weight={3}
                                  opacity={0.8}
                                />
                              )}
                            {mapData.mission.pickupLatitude != null &&
                              mapData.mission.deliveryLatitude != null && (
                                <Polyline
                                  positions={[
                                    [mapData.mission.pickupLatitude, mapData.mission.pickupLongitude],
                                    [mapData.mission.deliveryLatitude, mapData.mission.deliveryLongitude],
                                  ]}
                                  color="green"
                                  weight={3}
                                  opacity={0.8}
                                />
                              )}
                            {mapData.mission.deliveryLatitude != null &&
                              mapData.tracking?.baseLatitude != null && (
                                <Polyline
                                  positions={[
                                    [mapData.mission.deliveryLatitude, mapData.mission.deliveryLongitude],
                                    [mapData.tracking.baseLatitude, mapData.tracking.baseLongitude],
                                  ]}
                                  color="purple"
                                  weight={3}
                                  opacity={0.8}
                                />
                              )}
                          </>
                        )}
                          </>
                        )}
                      </MapContainer>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

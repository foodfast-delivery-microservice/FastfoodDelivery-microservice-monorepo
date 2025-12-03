package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.AssignDroneRequest;
import com.example.droneservice.application.dto.MissionResponse;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.model.Status;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.infrastructure.config.RabbitMQConfig;
import com.example.droneservice.infrastructure.event.DroneAssignedEvent;
import com.example.droneservice.infrastructure.service.OrderServiceAdapter;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case to assign a drone to a delivery order.
 * This creates a mission and updates the drone state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignDroneToOrderUseCase {

    private final DroneRepository droneRepository;
    private final DroneMissionRepository missionRepository;
    private final GetAvailableDroneUseCase getAvailableDroneUseCase;
    private final RabbitTemplate rabbitTemplate;
    private final OrderServiceAdapter orderServiceAdapter;

    private static final double AVERAGE_DRONE_SPEED_KMH = 40.0; // 40 km/h

    @Transactional
    public MissionResponse execute(AssignDroneRequest request) {
        log.info("🚁 Assigning drone to order: {}", request.getOrderId());

        // Validate order status must be PROCESSING
        try {
            var orderDetail = orderServiceAdapter.getOrderDetail(request.getOrderId());
            String orderStatus = orderDetail.getStatus();
            
            log.info("📋 Order {} current status: {}", request.getOrderId(), orderStatus);
            
            if (!"PROCESSING".equalsIgnoreCase(orderStatus)) {
                log.error("❌ Cannot assign drone to order {}: Order status is {} but must be PROCESSING", 
                        request.getOrderId(), orderStatus);
                throw new IllegalStateException(
                        String.format("Không thể gán drone cho đơn hàng %d. Đơn hàng phải ở trạng thái PROCESSING (đang xử lý) nhưng hiện tại là %s. " +
                                "Vui lòng đợi đơn hàng được thanh toán và chuyển sang trạng thái PROCESSING.",
                                request.getOrderId(), orderStatus));
            }
            
            log.info("✅ Order status validation passed: Order {} is in PROCESSING status", request.getOrderId());
        } catch (RuntimeException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            log.warn("⚠️ Could not validate order status (Order Service may be unavailable): {}", e.getMessage());
            // In case Order Service is unavailable, we still allow assignment but log a warning
            // This is a trade-off between strict validation and system resilience
        }

        // Find available drone (đã validate pin đủ cho toàn bộ lộ trình)
        Drone drone = null;
        
        // Nếu có droneId được chỉ định, thử dùng drone đó trước
        // QUAN TRỌNG: Nếu user chỉ định drone cụ thể, phải validate và throw error nếu không đủ điều kiện
        // KHÔNG tự động chọn drone khác
        if (request.getDroneId() != null) {
            log.info("🎯 User specified drone ID: {}", request.getDroneId());
            Optional<Drone> specifiedDrone = droneRepository.findById(request.getDroneId());
            
            if (!specifiedDrone.isPresent()) {
                throw new IllegalStateException(
                        String.format("Drone với ID %d không tồn tại. Vui lòng chọn drone khác.", request.getDroneId()));
            }
            
            Drone candidate = specifiedDrone.get();
            
            // Kiểm tra drone có ở state IDLE hoặc RETURNING không
            if (candidate.getState() != State.IDLE && candidate.getState() != State.RETURNING) {
                throw new IllegalStateException(
                        String.format("Drone %s (ID: %d) không thể được gán vì đang ở trạng thái %s. " +
                                "Chỉ có drone ở trạng thái IDLE hoặc RETURNING mới có thể được gán.",
                                candidate.getSerialNumber(), candidate.getId(), candidate.getState()));
            }
            
            // Tính toán và validate battery dựa trên state
            double requiredBattery;
            double minimumBatteryNeeded;
            double totalDistance;
            
            if (candidate.getState() == State.IDLE) {
                // IDLE: Base → Pickup → Delivery → Base
                totalDistance = calculateTotalDistance(
                        candidate.getBaseLatitude(), candidate.getBaseLongitude(),
                        request.getPickupLatitude(), request.getPickupLongitude(),
                        request.getDeliveryLatitude(), request.getDeliveryLongitude());
                requiredBattery = totalDistance * 2.0; // 2% per km
            } else {
                // RETURNING: Current → Pickup (hoặc Current → Base → Pickup) → Delivery → Base
                // QUAN TRỌNG: Cả 2 route đều phải tính đủ pin để về base cuối cùng
                // Tính route tối ưu
                double currentToPickup = HaversineDistanceCalculator.calculate(
                        candidate.getCurrentLatitude(), candidate.getCurrentLongitude(),
                        request.getPickupLatitude(), request.getPickupLongitude());
                double currentToBase = HaversineDistanceCalculator.calculate(
                        candidate.getCurrentLatitude(), candidate.getCurrentLongitude(),
                        candidate.getBaseLatitude(), candidate.getBaseLongitude());
                double baseToPickup = HaversineDistanceCalculator.calculate(
                        candidate.getBaseLatitude(), candidate.getBaseLongitude(),
                        request.getPickupLatitude(), request.getPickupLongitude());
                double pickupToDelivery = HaversineDistanceCalculator.calculate(
                        request.getPickupLatitude(), request.getPickupLongitude(),
                        request.getDeliveryLatitude(), request.getDeliveryLongitude());
                // QUAN TRỌNG: Phải tính đủ pin để về base sau khi giao hàng
                double deliveryToBase = HaversineDistanceCalculator.calculate(
                        request.getDeliveryLatitude(), request.getDeliveryLongitude(),
                        candidate.getBaseLatitude(), candidate.getBaseLongitude());
                
                // So sánh 2 route (cả 2 đều về base):
                // Route 1: Current → Pickup → Delivery → Base
                // Route 2: Current → Base → Pickup → Delivery → Base
                double route1Distance = currentToPickup + pickupToDelivery + deliveryToBase;
                double route2Distance = currentToBase + baseToPickup + pickupToDelivery + deliveryToBase;
                
                totalDistance = Math.min(route1Distance, route2Distance);
                requiredBattery = totalDistance * 2.0; // 2% per km
            }
            
            minimumBatteryNeeded = requiredBattery + 10; // +10% reserve
            
            // QUAN TRỌNG: Nếu user chỉ định drone cụ thể nhưng không đủ pin, throw error
            if (candidate.getBatteryLevel() < minimumBatteryNeeded) {
                log.error("❌ Specified drone {} has insufficient battery: {}% (Required: {:.1f}%, Distance: {:.2f}km)",
                        candidate.getSerialNumber(), candidate.getBatteryLevel(), minimumBatteryNeeded, totalDistance);
                throw new IllegalStateException(
                        String.format("Drone %s (ID: %d) không đủ pin để hoàn thành đơn hàng này. " +
                                "Pin hiện tại: %d%%. Pin cần thiết: %.1f%% (Quãng đường: %.2f km). " +
                                "Vui lòng chọn drone khác có đủ pin.",
                                candidate.getSerialNumber(), candidate.getId(), 
                                candidate.getBatteryLevel(), minimumBatteryNeeded, totalDistance));
            }
            
            // Drone được chỉ định hợp lệ
            drone = candidate;
            log.info("✅ Using specified drone {} (State: {}, Battery: {}%, Required: {:.1f}%, Distance: {:.2f}km)",
                    candidate.getSerialNumber(), candidate.getState(), 
                    candidate.getBatteryLevel(), minimumBatteryNeeded, totalDistance);
        }
        
        // Nếu không có drone được chỉ định, tự động chọn
        if (drone == null) {
            log.info("🔍 Auto-selecting available drone...");
            drone = getAvailableDroneUseCase.execute(
                            request.getPickupLatitude(),
                            request.getPickupLongitude(),
                            request.getDeliveryLatitude(),
                            request.getDeliveryLongitude())
                    .orElseThrow(() -> new IllegalStateException(
                            "Không có drone nào khả dụng cho đơn hàng này. " +
                            "Tất cả drone đều không đủ pin hoặc không ở trạng thái IDLE/RETURNING."));
        }

        // Double-check: Validate battery one more time before assignment
        // Tính toán dựa trên state của drone
        double totalDistance;
        if (drone.getState() == State.IDLE) {
            // IDLE: Base → Pickup → Delivery → Base
            totalDistance = calculateTotalDistance(
                    drone.getBaseLatitude(), drone.getBaseLongitude(),
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    request.getDeliveryLatitude(), request.getDeliveryLongitude());
        } else if (drone.getState() == State.RETURNING) {
            // RETURNING: Current → Pickup (hoặc Current → Base → Pickup) → Delivery → Base
            // QUAN TRỌNG: Cả 2 route đều phải tính đủ pin để về base cuối cùng
            // Chọn route tối ưu
            double currentToPickup = HaversineDistanceCalculator.calculate(
                    drone.getCurrentLatitude(), drone.getCurrentLongitude(),
                    request.getPickupLatitude(), request.getPickupLongitude());
            double currentToBase = HaversineDistanceCalculator.calculate(
                    drone.getCurrentLatitude(), drone.getCurrentLongitude(),
                    drone.getBaseLatitude(), drone.getBaseLongitude());
            double baseToPickup = HaversineDistanceCalculator.calculate(
                    drone.getBaseLatitude(), drone.getBaseLongitude(),
                    request.getPickupLatitude(), request.getPickupLongitude());
            double pickupToDelivery = HaversineDistanceCalculator.calculate(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    request.getDeliveryLatitude(), request.getDeliveryLongitude());
            // QUAN TRỌNG: Phải tính đủ pin để về base sau khi giao hàng
            double deliveryToBase = HaversineDistanceCalculator.calculate(
                    request.getDeliveryLatitude(), request.getDeliveryLongitude(),
                    drone.getBaseLatitude(), drone.getBaseLongitude());
            
            // So sánh 2 route (cả 2 đều về base):
            // Route 1: Current → Pickup → Delivery → Base
            // Route 2: Current → Base → Pickup → Delivery → Base
            double route1Distance = currentToPickup + pickupToDelivery + deliveryToBase;
            double route2Distance = currentToBase + baseToPickup + pickupToDelivery + deliveryToBase;
            
            totalDistance = Math.min(route1Distance, route2Distance);
            log.info("🔄 RETURNING drone route calculation - Route 1: {:.2f}km, Route 2: {:.2f}km, Selected: {:.2f}km (all routes return to base)",
                    route1Distance, route2Distance, totalDistance);
        } else {
            throw new IllegalStateException(
                    String.format("Cannot assign mission to drone %s in state %s. Only IDLE or RETURNING drones can be assigned.",
                            drone.getSerialNumber(), drone.getState()));
        }
        
        // Validate battery: Pin cần = Tổng quãng đường × 2% mỗi km + 10% dự phòng
        double requiredBattery = totalDistance * 2.0; // 2% per km
        double minimumBatteryNeeded = requiredBattery + 10; // +10% reserve
        
        if (drone.getBatteryLevel() < minimumBatteryNeeded) {
            log.error("❌ Battery validation failed! Drone {} has {}% but needs {:.1f}%",
                    drone.getSerialNumber(), drone.getBatteryLevel(), minimumBatteryNeeded);
            throw new IllegalStateException(
                    String.format("Drone %s has insufficient battery: %d%% (Required: %.1f%%)",
                            drone.getSerialNumber(), drone.getBatteryLevel(), minimumBatteryNeeded));
        }
        
        log.info("✅ Battery validation passed: Drone {} (State: {}) has {}% (Required: {:.1f}%)",
                drone.getSerialNumber(), drone.getState(), drone.getBatteryLevel(), minimumBatteryNeeded);

        // Calculate mission details (sử dụng totalDistance đã tính ở trên)

        int estimatedDuration = (int) Math.ceil((totalDistance / AVERAGE_DRONE_SPEED_KMH) * 60); // Convert to minutes

        // Create mission
        DroneMission mission = new DroneMission();
        mission.setDrone(drone);
        mission.setOrderId(request.getOrderId());
        mission.setPickupLatitude(request.getPickupLatitude());
        mission.setPickupLongitude(request.getPickupLongitude());
        mission.setDeliveryLatitude(request.getDeliveryLatitude());
        mission.setDeliveryLongitude(request.getDeliveryLongitude());
        mission.setStatus(Status.ASSIGNED);
        mission.setDistanceKm(totalDistance);
        mission.setEstimatedDurationMinutes(estimatedDuration);
        mission.setStartedAt(LocalDateTime.now());

        DroneMission savedMission = missionRepository.save(mission);

        // Update drone state to DELIVERING
        // Nếu drone đang RETURNING, chuyển hướng đến pickup thay vì về base
        if (drone.getState() == State.RETURNING) {
            log.info("🔄 Drone {} is RETURNING, redirecting to new pickup location instead of base",
                    drone.getSerialNumber());
        }
        drone.setState(State.DELIVERING);
        droneRepository.save(drone);

        log.info("✅ Drone {} assigned to order {}. Mission ID: {}, Distance: {:.2f}km, ETA: {} minutes",
                drone.getSerialNumber(), request.getOrderId(), savedMission.getId(),
                totalDistance, estimatedDuration);

        // Publish DRONE_ASSIGNED event to update order status to DELIVERING
        publishDroneAssignedEvent(savedMission, drone, estimatedDuration);

        return mapToResponse(savedMission, drone);
    }

    /**
     * Calculate total distance for IDLE drone:
     * Base → Pickup → Delivery → Base
     * 
     * QUAN TRỌNG: Luôn phải tính đủ quãng đường để về base sau khi giao hàng
     */
    private double calculateTotalDistance(Double baseLat, Double baseLon,
                                          Double pickupLat, Double pickupLon,
                                          Double deliveryLat, Double deliveryLon) {

        double baseToPickup = HaversineDistanceCalculator.calculate(baseLat, baseLon, pickupLat, pickupLon);
        double pickupToDelivery = HaversineDistanceCalculator.calculate(pickupLat, pickupLon, deliveryLat, deliveryLon);
        // QUAN TRỌNG: Phải tính đủ quãng đường để về base sau khi giao hàng
        double deliveryToBase = HaversineDistanceCalculator.calculate(deliveryLat, deliveryLon, baseLat, baseLon);

        return baseToPickup + pickupToDelivery + deliveryToBase;
    }

    /**
     * Publish DRONE_ASSIGNED event to notify Order Service
     * Order Service will update order status to DELIVERING
     */
    private void publishDroneAssignedEvent(DroneMission mission, Drone drone, Integer estimatedDurationMinutes) {
        DroneAssignedEvent event = DroneAssignedEvent.builder()
                .orderId(mission.getOrderId())
                .droneId(drone.getId())
                .droneSerialNumber(drone.getSerialNumber())
                .missionId(mission.getId())
                .estimatedArrival(LocalDateTime.now().plusMinutes(estimatedDurationMinutes))
                .estimatedDurationMinutes(estimatedDurationMinutes)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DRONE_EXCHANGE,
                RabbitMQConfig.DRONE_ASSIGNED_ROUTING_KEY,
                event);

        log.info("📡 Published DRONE_ASSIGNED event for order {} - Order status sẽ được đổi thành 'DELIVERING'",
                mission.getOrderId());
    }

    private MissionResponse mapToResponse(DroneMission mission, Drone drone) {
        return MissionResponse.builder()
                .id(mission.getId())
                .droneId(drone.getId())
                .droneSerialNumber(drone.getSerialNumber())
                .orderId(mission.getOrderId())
                .pickupLatitude(mission.getPickupLatitude())
                .pickupLongitude(mission.getPickupLongitude())
                .deliveryLatitude(mission.getDeliveryLatitude())
                .deliveryLongitude(mission.getDeliveryLongitude())
                .status(mission.getStatus())
                .distanceKm(mission.getDistanceKm())
                .estimatedDurationMinutes(mission.getEstimatedDurationMinutes())
                .startedAt(mission.getStartedAt())
                .completedAt(mission.getCompletedAt())
                .build();
    }
}

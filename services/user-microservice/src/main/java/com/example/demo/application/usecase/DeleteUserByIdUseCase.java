package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidId;
import com.example.demo.domain.exception.MerchantDeletionNotAllowedException;
import com.example.demo.domain.exception.MerchantHasActiveOrdersException;
import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.client.OrderServiceClient;
import com.example.demo.infrastructure.client.dto.MerchantOrderCheckResponse;
import com.example.demo.infrastructure.messaging.EventPublisher;
import com.example.demo.infrastructure.messaging.event.MerchantDeletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
public class DeleteUserByIdUseCase {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final EventPublisher eventPublisher;
    private final OrderServiceClient orderServiceClient;

    public DeleteUserByIdUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository,
            EventPublisher eventPublisher, OrderServiceClient orderServiceClient) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.eventPublisher = eventPublisher;
        this.orderServiceClient = orderServiceClient;
    }

    @Transactional
    public String execute(Long id) {
        log.info("Attempting to delete user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new InvalidId(id));

        // Xóa restaurant và publish event nếu user là merchant
        if (user.getRole() == User.UserRole.MERCHANT) {
            log.info("Deleting merchant user with id: {}, processing restaurant deletion", id);

            // 1. Kiểm tra restaurant và validate trạng thái active
            Optional<Restaurant> restaurant = restaurantRepository.findByMerchantId(id);

            // Ràng buộc 1: Restaurant phải inactive (blocked) trước khi xóa merchant
            if (restaurant.isPresent() && restaurant.get().getActive()) {
                log.warn("Cannot delete merchant with active restaurant. Merchant id: {}, Restaurant id: {}",
                        id, restaurant.get().getId());
                throw new MerchantDeletionNotAllowedException(
                        "Không thể xóa merchant đang hoạt động. Vui lòng block merchant trước khi xóa.");
            }

            // Ràng buộc 2: Tất cả orders phải DELIVERED hoặc CANCELLED
            log.info("Checking orders for merchant id: {}", id);
            try {
                MerchantOrderCheckResponse orderCheck = orderServiceClient.checkMerchantCanBeDeleted(id);
                log.info("Order check result for merchant {}: canDelete={}, activeOrders={}",
                        id, orderCheck.isCanDelete(), orderCheck.getActiveOrderCount());

                if (!orderCheck.isCanDelete()) {
                    String statusList = String.join(", ", orderCheck.getActiveStatuses());
                    log.warn("Cannot delete merchant {} with active orders: {}", id, statusList);
                    throw new MerchantHasActiveOrdersException(
                            orderCheck.getActiveOrderCount(), statusList);
                }
            } catch (MerchantHasActiveOrdersException e) {
                throw e; // Re-throw business exception
            } catch (Exception e) {
                log.error("Failed to check orders for merchant {}", id, e);
                throw new RuntimeException(
                        "Không thể kiểm tra đơn hàng của merchant. Vui lòng thử lại sau.", e);
            }

            // 2. Xóa restaurant (để tránh FK constraint)
            if (restaurant.isPresent()) {
                log.info("Found restaurant for merchant id: {}, deleting restaurant id: {}", id,
                        restaurant.get().getId());
                restaurantRepository.delete(restaurant.get());
                log.info("Successfully deleted restaurant for merchant id: {}", id);
            } else {
                log.warn("No restaurant found for merchant id: {}", id);
            }

            // 2. Publish event để các service khác xóa dữ liệu liên quan (products, orders,
            // payments)
            MerchantDeletedEvent deletedEvent = MerchantDeletedEvent.builder()
                    .merchantId(id)
                    .occurredAt(Instant.now())
                    .reason("Merchant user deleted by admin")
                    .build();
            eventPublisher.publishMerchantDeleted(deletedEvent);
            log.info("Published MerchantDeletedEvent for merchant id: {}", id);
        }

        // 3. Xóa user cuối cùng
        userRepository.delete(user);
        log.info("Successfully deleted user with id: {}", id);

        return "User deleted successfully with id: " + id;
    }
}

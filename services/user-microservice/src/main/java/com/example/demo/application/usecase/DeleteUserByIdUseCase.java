package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidId;
import com.example.demo.domain.exception.MerchantDeletionNotAllowedException;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.messaging.EventPublisher;
import com.example.demo.infrastructure.messaging.event.MerchantDeletedEvent;

import java.time.Instant;

public class DeleteUserByIdUseCase {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final EventPublisher eventPublisher;

    public DeleteUserByIdUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.eventPublisher = eventPublisher;
    }

    public String execute(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new InvalidId(id));

        // Ràng buộc: Merchant phải inactive (blocked) trước khi xóa
        if (user.getRole() == User.UserRole.MERCHANT && user.isActive()) {
            throw new MerchantDeletionNotAllowedException(
                    "Không thể xóa merchant đang hoạt động. Vui lòng block merchant trước khi xóa.");
        }

        // Xóa restaurant nếu user là merchant
        if (user.getRole() == User.UserRole.MERCHANT) {
            // Publish event để Product Service xóa products trước khi xóa restaurant
            MerchantDeletedEvent deletedEvent = MerchantDeletedEvent.builder()
                    .merchantId(id)
                    .occurredAt(Instant.now())
                    .reason("Merchant user deleted by admin")
                    .build();
            eventPublisher.publishMerchantDeleted(deletedEvent);

            restaurantRepository.findByMerchantId(id)
                    .ifPresent(restaurantRepository::delete);
        }

        userRepository.delete(user);
        return "User deleted successfully with id: " + id;
    }
}

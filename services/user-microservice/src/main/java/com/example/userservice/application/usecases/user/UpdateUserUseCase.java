package com.example.userservice.application.usecases.user;

import com.example.userservice.domain.exception.*;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.entities.OutboxEvent;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.domain.valueobjects.EventStatus;
import com.example.userservice.infrastructure.messaging.event.MerchantActivatedEvent;
import com.example.userservice.infrastructure.messaging.event.MerchantDeactivatedEvent;
import com.example.userservice.application.DTOs.event.UserUpdatedEventDTO;
import com.example.userservice.application.DTOs.user.UserContext;
import com.example.userservice.application.DTOs.user.UserPatchDTO;
import com.example.userservice.application.service.EventPayloadSerializer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserUseCase {
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventPayloadSerializer eventPayloadSerializer;
    private final ValidateUserAccessUseCase validateUserAccessUseCase;

    @Transactional
    // user tự thay đổi thông tin của mình
    public User updateUser(Long id, UserPatchDTO userPatchDTO, UserContext userContext) {
        // Validate: User can only update their own account (unless ADMIN)
        validateUserAccessUseCase.execute(id, userContext);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new InvalidId(id));

        // 1. CHECK USERNAME
        // only update when field was sent (not null)
        if (userPatchDTO.getUsername() != null) {
            // Chỉ check trùng nếu username MỚI khác username CŨ
            if (!userPatchDTO.getUsername().equals(existingUser.getUsername())) {
                // nếu tên mới khác tên cũ thì kiểm tra tên mới có trùng với ai trong database
                // không
                if (userRepository.existsByUsername(userPatchDTO.getUsername())) {
                    throw new UsernameAlreadyExistException(userPatchDTO.getUsername());
                }
                // nếu không trùng set giá trị mới
                existingUser.setUsername(userPatchDTO.getUsername());
            }

        }

        // 2. CHECK EMAIL
        // only update when field was sent (not null)
        if (userPatchDTO.getEmail() != null) {
            if (!userPatchDTO.getEmail().equals(existingUser.getEmail())) {
                if (userRepository.existsByEmail(userPatchDTO.getEmail())) {
                    throw new EmailAlreadyExistException(userPatchDTO.getEmail());
                }
                existingUser.setEmail(userPatchDTO.getEmail());
            }
        }
        if (userPatchDTO.getApproved() != null) {
            existingUser.setApproved(userPatchDTO.getApproved());
        }

        // Map Profile Fields
        if (userPatchDTO.getFullName() != null)
            existingUser.setFullName(userPatchDTO.getFullName());
        if (userPatchDTO.getPhone() != null)
            existingUser.setPhone(userPatchDTO.getPhone());
        if (userPatchDTO.getAddress() != null)
            existingUser.setAddress(userPatchDTO.getAddress());
        if (userPatchDTO.getAvatar() != null)
            existingUser.setAvatar(userPatchDTO.getAvatar());

        // Map Merchant Fields
        if (userPatchDTO.getRestaurantName() != null)
            existingUser.setRestaurantName(userPatchDTO.getRestaurantName());
        if (userPatchDTO.getRestaurantAddress() != null)
            existingUser.setRestaurantAddress(userPatchDTO.getRestaurantAddress());
        if (userPatchDTO.getRestaurantImage() != null)
            existingUser.setRestaurantImage(userPatchDTO.getRestaurantImage());
        if (userPatchDTO.getOpeningHours() != null)
            existingUser.setOpeningHours(userPatchDTO.getOpeningHours());

        boolean merchantDeactivated = false;
        boolean merchantActivated = false;
        if (userPatchDTO.getActive() != null) {
            if (!userContext.isAdmin()) {
                throw new AdminAccessDeniedException();
            }
            boolean requestedActive = userPatchDTO.getActive();
            boolean currentlyActive = existingUser.isActive();
            if (!requestedActive && currentlyActive && existingUser.getRole() == User.UserRole.MERCHANT) {
                merchantDeactivated = true;
            } else if (requestedActive && !currentlyActive && existingUser.getRole() == User.UserRole.MERCHANT) {
                merchantActivated = true;
            }
            existingUser.setActive(requestedActive);
        }

        User updatedUser = userRepository.save(existingUser);

        // -- CREATE OUTBOX EVENTS FOR RELIABLE EVENT PUBLISHING --
        // Always create UserUpdated event
        createUserUpdatedOutboxEvent(updatedUser);

        if (merchantDeactivated) {
            createMerchantDeactivatedOutboxEvent(updatedUser);
        }

        if (merchantActivated) {
            createMerchantActivatedOutboxEvent(updatedUser, userContext);
        }

        return updatedUser;
    }

    /**
     * Create OutboxEvent for UserUpdated event
     */
    private void createUserUpdatedOutboxEvent(User user) {
        UserUpdatedEventDTO eventDTO = UserUpdatedEventDTO.builder()
                .userId(user.getId())
                .newUsername(user.getUsername())
                .newEmail(user.getEmail())
                .build();

        String payloadJson = eventPayloadSerializer.serialize(eventDTO);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("User")
                    .aggregateId(user.getId().toString())
                    .type("UserUpdated")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

        outboxEventRepository.save(event);
        log.debug("Created UserUpdated outbox event for userId: {}", user.getId());
    }

    /**
     * Create OutboxEvent for MerchantDeactivated event
     */
    private void createMerchantDeactivatedOutboxEvent(User user) {
        MerchantDeactivatedEvent eventDTO = MerchantDeactivatedEvent.builder()
                .merchantId(user.getId())
                .occurredAt(java.time.Instant.now())
                .reason("Merchant deactivated via admin request")
                .build();

        String payloadJson = eventPayloadSerializer.serialize(eventDTO);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("User")
                    .aggregateId(user.getId().toString())
                    .type("MerchantDeactivated")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

        outboxEventRepository.save(event);
        log.debug("Created MerchantDeactivated outbox event for merchantId: {}", user.getId());
    }

    /**
     * Create OutboxEvent for MerchantActivated event
     */
    private void createMerchantActivatedOutboxEvent(User user, UserContext userContext) {
        MerchantActivatedEvent eventDTO = MerchantActivatedEvent.builder()
                .merchantId(user.getId())
                .occurredAt(java.time.Instant.now())
                .reason("Merchant reactivated via admin request")
                .triggeredBy(userContext != null ? userContext.username() : "system")
                .build();

        String payloadJson = eventPayloadSerializer.serialize(eventDTO);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("User")
                    .aggregateId(user.getId().toString())
                    .type("MerchantActivated")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

        outboxEventRepository.save(event);
        log.debug("Created MerchantActivated outbox event for merchantId: {}", user.getId());
    }

    // thay đổi role của user
    // only admin

    public User updateRoleUser(User currentUser, Long id, String newRole) {
        // check if caller is admin
        if (!currentUser.getRole().equals(User.UserRole.ADMIN)) {
            throw new AdminAccessDeniedException();
        }
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new InvalidId(id));
        Set<String> allowedRole = Set.of("ADMIN", "USER", "MERCHANT");
        String roleUpper = newRole.toUpperCase();
        if (!allowedRole.contains(roleUpper)) {
            throw new InvalidRoleException(roleUpper);
        }
        targetUser.setRole(User.UserRole.valueOf(roleUpper));
        return userRepository.save(targetUser);
    }

}

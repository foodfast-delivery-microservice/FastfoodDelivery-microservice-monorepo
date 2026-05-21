package com.example.notificationservice.infrastructure.websocket;

import com.example.notificationservice.application.dto.InAppNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to push real-time STOMP notifications to connected clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Pushes an in-app notification DTO directly to the specified user.
     * Maps to the STOMP destination: /user/{userId}/queue/notifications
     *
     * @param userId the numeric ID of the user to receive the notification
     * @param dto    the notification content DTO
     */
    public void pushToUser(Long userId, InAppNotificationDto dto) {
        if (userId == null) {
            log.warn("Cannot push WebSocket notification: userId is null");
            return;
        }
        if (dto == null) {
            log.warn("Cannot push WebSocket notification: dto is null");
            return;
        }

        String destination = "/queue/notifications";
        String user = String.valueOf(userId);

        log.info("Pushing real-time STOMP notification to user {} at destination /user/{}/queue/notifications", user, user);
        
        try {
            messagingTemplate.convertAndSendToUser(user, destination, dto);
            log.debug("Successfully pushed STOMP message to user {}", user);
        } catch (Exception e) {
            log.error("Failed to push STOMP message to user {}", user, e);
        }
    }
}

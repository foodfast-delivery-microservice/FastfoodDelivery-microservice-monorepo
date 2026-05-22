package com.example.notificationservice.interfaces.rest;

import com.example.notificationservice.application.dto.ApiResponse;
import com.example.notificationservice.application.dto.EmailNotificationDto;
import com.example.notificationservice.application.dto.InAppNotificationDto;
import com.example.notificationservice.application.dto.ResendResultDto;
import com.example.notificationservice.application.usecase.GetInAppNotificationsUseCase;
import com.example.notificationservice.application.usecase.GetNotificationHistoryUseCase;
import com.example.notificationservice.application.usecase.MarkNotificationReadUseCase;
import com.example.notificationservice.application.usecase.ResendFailedEmailUseCase;
import com.example.notificationservice.domain.entities.EmailNotification;
import com.example.notificationservice.domain.entities.InAppNotification;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.domain.valueobjects.NotificationType;
import com.example.notificationservice.infrastructure.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final GetNotificationHistoryUseCase getNotificationHistoryUseCase;
    private final ResendFailedEmailUseCase resendFailedEmailUseCase;
    private final GetInAppNotificationsUseCase getInAppNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final EmailNotificationRepository emailNotificationRepository;
    private final JwtTokenService jwtTokenService;

    /**
     * GET /api/v1/notifications/email/history
     * Lịch sử email (phân trang + filter) - ADMIN ONLY
     */
    @GetMapping("/email/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EmailNotificationDto>>> getEmailHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String recipientEmail,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        log.info("Fetching email notification history with status={}, type={}, recipient={}", status, type, recipientEmail);
        
        EmailStatus emailStatus = (status != null && !status.isBlank()) ? EmailStatus.valueOf(status.toUpperCase()) : null;
        NotificationType notifType = (type != null && !type.isBlank()) ? NotificationType.fromString(type) : null;

        Page<EmailNotification> history = getNotificationHistoryUseCase.execute(
                emailStatus, notifType, recipientEmail, fromDate, toDate, pageable
        );

        Page<EmailNotificationDto> historyDto = history.map(this::toEmailNotificationDto);
        return ResponseEntity.ok(ApiResponse.success(historyDto, "Email history retrieved successfully"));
    }

    /**
     * GET /api/v1/notifications/email/{id}
     * Chi tiết 1 email - ADMIN ONLY
     */
    @GetMapping("/email/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmailNotificationDto>> getEmailDetails(@PathVariable Long id) {
        log.info("Fetching email notification details for ID: {}", id);
        EmailNotification email = emailNotificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email notification not found with ID: " + id));

        return ResponseEntity.ok(ApiResponse.success(toEmailNotificationDto(email), "Email details retrieved successfully"));
    }

    /**
     * POST /api/v1/notifications/email/{id}/resend
     * Gửi lại email FAILED - ADMIN ONLY
     */
    @PostMapping("/email/{id}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResendResultDto>> resendFailedEmail(@PathVariable Long id) {
        log.info("Request to resend failed email ID: {}", id);
        try {
            EmailNotification newAttempt = resendFailedEmailUseCase.execute(id);
            ResendResultDto result = ResendResultDto.builder()
                    .success(true)
                    .message("Email resent successfully")
                    .newAttempt(toEmailNotificationDto(newAttempt))
                    .build();
            return ResponseEntity.ok(ApiResponse.success(result, "Resend attempt triggered successfully"));
        } catch (IllegalStateException e) {
            log.warn("Resend conflict/failed for ID: {}. Reason: {}", id, e.getMessage());
            ResendResultDto result = ResendResultDto.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            // Map state exceptions to 409 Conflict as requested by the plan
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.success(result, e.getMessage()));
        }
    }

    /**
     * GET /api/v1/notifications/in-app
     * In-app notifications của user - AUTHENTICATED
     */
    @GetMapping("/in-app")
    public ResponseEntity<ApiResponse<Page<InAppNotificationDto>>> getInAppNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        Long userId = jwtTokenService.extractUserId(jwt);
        log.info("Fetching in-app notifications for userId={}", userId);

        Page<InAppNotification> notifications = getInAppNotificationsUseCase.execute(userId, pageable);
        Page<InAppNotificationDto> notificationsDto = notifications.map(this::toInAppNotificationDto);

        return ResponseEntity.ok(ApiResponse.success(notificationsDto, "In-app notifications retrieved successfully"));
    }

    /**
     * GET /api/v1/notifications/in-app/unread-count
     * Đếm chưa đọc - AUTHENTICATED
     */
    @GetMapping("/in-app/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwtTokenService.extractUserId(jwt);
        log.debug("Fetching in-app unread count for userId={}", userId);

        long unreadCount = getInAppNotificationsUseCase.countUnread(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", unreadCount), "Unread count retrieved successfully"));
    }

    /**
     * PUT /api/v1/notifications/in-app/{id}/read
     * Đánh dấu đã đọc - AUTHENTICATED (CHỈ OWNER)
     */
    @PutMapping("/in-app/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        Long userId = jwtTokenService.extractUserId(jwt);
        log.info("Marking notification ID={} as read by userId={}", id, userId);

        markNotificationReadUseCase.execute(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/notifications/in-app/read-all
     * Đánh dấu tất cả đã đọc - AUTHENTICATED
     */
    @PutMapping("/in-app/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwtTokenService.extractUserId(jwt);
        log.info("Marking all in-app notifications as read for userId={}", userId);

        markNotificationReadUseCase.executeAll(userId);
        return ResponseEntity.noContent().build();
    }

    private EmailNotificationDto toEmailNotificationDto(EmailNotification e) {
        if (e == null) return null;
        return EmailNotificationDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .status(e.getStatus())
                .type(e.getType())
                .recipient(e.getRecipient() != null ? e.getRecipient().getValue() : null)
                .subject(e.getSubject())
                .template(e.getTemplate())
                .retryCount(e.getRetryCount())
                .createdAt(e.getCreatedAt())
                .sentAt(e.getSentAt())
                .lastRetryAt(e.getLastRetryAt())
                .errorMessage(e.getErrorMessage())
                .eventId(e.getEventId())
                .payloadJson(e.getPayloadJson())
                .build();
    }

    private InAppNotificationDto toInAppNotificationDto(InAppNotification n) {
        if (n == null) return null;
        return InAppNotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .channel(n.getChannel())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}

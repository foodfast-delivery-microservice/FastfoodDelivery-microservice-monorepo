package com.example.notificationservice.domain.port;

import com.example.notificationservice.application.dto.UserEmailResponse;

public interface UserServicePort {

    UserEmailResponse getUserEmailById(Long userId);

    boolean updateEmailDeliverability(Long userId, boolean undeliverable, java.time.LocalDateTime bouncedAt, int bounceIncrement);
}

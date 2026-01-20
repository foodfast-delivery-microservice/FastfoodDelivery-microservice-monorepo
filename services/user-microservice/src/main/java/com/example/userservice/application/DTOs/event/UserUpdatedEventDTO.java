package com.example.userservice.application.DTOs.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserUpdatedEventDTO {
    private Long userId;
    private String newUsername;
    private String newEmail;
}

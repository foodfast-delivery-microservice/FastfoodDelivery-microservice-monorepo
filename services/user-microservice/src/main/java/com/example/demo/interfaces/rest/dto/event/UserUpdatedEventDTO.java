package com.example.demo.interfaces.rest.dto.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserUpdatedEventDTO {
    private Long userId;
    private String newUsername;
    private String newEmail;
}

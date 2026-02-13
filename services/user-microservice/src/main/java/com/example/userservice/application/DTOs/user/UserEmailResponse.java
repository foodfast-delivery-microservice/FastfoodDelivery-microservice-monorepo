package com.example.userservice.application.DTOs.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailResponse {
    private Long id;
    private String fullName;
    private String email;
}

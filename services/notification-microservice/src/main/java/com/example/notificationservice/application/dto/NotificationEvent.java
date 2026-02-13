package com.example.notificationservice.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    
    @NotBlank(message = "Event type cannot be blank")
    private String eventType;
    
    @NotBlank(message = "Recipient email cannot be blank")
    @Email(message = "Recipient must be a valid email address")
    private String recipient;
    
    @NotBlank(message = "Template cannot be blank")
    private String template;
    
    private Map<String, Object> data;
}

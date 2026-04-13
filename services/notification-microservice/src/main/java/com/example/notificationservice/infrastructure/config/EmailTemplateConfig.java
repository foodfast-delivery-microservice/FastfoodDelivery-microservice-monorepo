package com.example.notificationservice.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for email template subjects.
 * Externalizes email subjects to properties file for easy customization.
 */
@Configuration
@ConfigurationProperties(prefix = "email.templates")
@Getter
@Setter
public class EmailTemplateConfig {

    private Map<String, String> subjects = new HashMap<>();

    public String getSubject(String templateName) {
        return subjects.getOrDefault(templateName, "Notification from FastFood Delivery");
    }
}

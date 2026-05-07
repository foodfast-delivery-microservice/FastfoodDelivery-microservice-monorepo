package com.example.userservice.infrastructure.service;

import com.example.userservice.application.service.DisposableEmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SimpleDisposableEmailValidator implements DisposableEmailValidator {

    private final boolean enabled;
    private final Set<String> blockedDomains;

    public SimpleDisposableEmailValidator(
            @Value("${app.email.disposable-check.enabled:true}") boolean enabled,
            @Value("${app.email.disposable-check.domains:10minutemail.com,guerrillamail.com,mailinator.com,temp-mail.org,trashmail.com}") String domains
    ) {
        this.enabled = enabled;
        this.blockedDomains = Arrays.stream(domains.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isDisposable(String email) {
        if (!enabled || email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1).toLowerCase();
        return blockedDomains.contains(domain);
    }
}

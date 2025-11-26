package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.SystemKPIResponse;
import com.example.order_service.application.usecase.GetSystemKPIUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final GetSystemKPIUseCase getSystemKPIUseCase;

    /**
     * GET /api/v1/admin/dashboard/kpis
     * Get system-wide KPIs for admin dashboard
     * Admin only
     */
    @GetMapping("/dashboard/kpis")
    public ResponseEntity<SystemKPIResponse> getSystemKPIs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Extract and validate admin role
        String role = jwt.getClaimAsString("role");
        if (!"ADMIN".equals(role)) {
            log.warn("Non-admin user attempted to access KPIs: role={}", role);
            return ResponseEntity.status(403).build();
        }

        log.info("Admin requesting system KPIs for date: {}", date);
        SystemKPIResponse response = getSystemKPIUseCase.execute(date);
        return ResponseEntity.ok(response);
    }
}

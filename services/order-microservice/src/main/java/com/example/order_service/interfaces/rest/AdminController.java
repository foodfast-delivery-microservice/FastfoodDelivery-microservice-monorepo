package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.RevenueByRestaurantResponse;
import com.example.order_service.application.dto.SystemKPIResponse;
import com.example.order_service.application.usecase.GetRevenueByRestaurantUseCase;
import com.example.order_service.application.usecase.GetSystemKPIUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final GetSystemKPIUseCase getSystemKPIUseCase;
    private final GetRevenueByRestaurantUseCase getRevenueByRestaurantUseCase;

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

    /**
     * GET /api/v1/admin/dashboard/revenue-by-restaurant
     * Get revenue by restaurant for pie chart
     * Admin only
     */
    @GetMapping("/dashboard/revenue-by-restaurant")
    public ResponseEntity<RevenueByRestaurantResponse> getRevenueByRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        // Extract and validate admin role
        String role = jwt.getClaimAsString("role");
        if (!"ADMIN".equals(role)) {
            log.warn("Non-admin user attempted to access revenue by restaurant: role={}", role);
            return ResponseEntity.status(403).build();
        }

        try {
            log.info("Admin requesting revenue by restaurant from {} to {}", fromDate, toDate);
            RevenueByRestaurantResponse response = getRevenueByRestaurantUseCase.execute(fromDate, toDate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting revenue by restaurant: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

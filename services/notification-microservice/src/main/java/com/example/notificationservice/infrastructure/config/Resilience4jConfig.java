package com.example.notificationservice.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j (Circuit Breaker and Rate Limiter).
 */
@Configuration
public class Resilience4jConfig {

    /**
     * Rate limiter configuration for email sending.
     * Limits emails per minute to prevent SMTP provider blocking.
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10) // Max 10 emails per period
                .limitRefreshPeriod(Duration.ofMinutes(1)) // Reset limit every minute
                .timeoutDuration(Duration.ofSeconds(5)) // Wait up to 5 seconds for permission
                .build();

        return RateLimiterRegistry.of(config);
    }
}

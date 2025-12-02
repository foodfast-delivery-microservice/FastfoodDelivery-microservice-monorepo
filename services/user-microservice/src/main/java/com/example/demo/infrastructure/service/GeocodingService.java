package com.example.demo.infrastructure.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Simple wrapper around Nominatim (OpenStreetMap) for forward geocoding.
 */
@Service
@Slf4j
public class GeocodingService {

    private final WebClient nominatimWebClient;

    public GeocodingService(WebClient nominatimWebClient) {
        this.nominatimWebClient = nominatimWebClient;
    }

    public Optional<GeoResult> geocode(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return Optional.empty();
        }

        try {
            log.info("Calling Nominatim to geocode address: {}", fullAddress);
            String searchQuery = fullAddress.contains(", Vietnam") ? fullAddress : fullAddress + ", Vietnam";
            
            GeoResult[] results = nominatimWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", searchQuery)
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .queryParam("addressdetails", "1")
                            .queryParam("countrycodes", "vn")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(GeoResult[].class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.error("Failed to geocode address '{}'. Reason: {}", fullAddress, ex.getMessage());
                        return Mono.just(new GeoResult[0]);
                    })
                    .block();

            if (results != null && results.length > 0) {
                GeoResult first = results[0];
                log.info("Geocoding success: {} => ({}, {})", first.displayName, first.lat, first.lon);
                return Optional.of(first);
            }
        } catch (Exception ex) {
            log.error("Unexpected error calling Nominatim", ex);
        }
        return Optional.empty();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeoResult {
        private BigDecimal lat;
        private BigDecimal lon;
        private String displayName;

        // Nominatim returns strings, map via setters
        @com.fasterxml.jackson.annotation.JsonProperty("lat")
        public void setLat(String lat) {
            this.lat = lat != null ? new BigDecimal(lat) : null;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("lon")
        public void setLon(String lon) {
            this.lon = lon != null ? new BigDecimal(lon) : null;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("display_name")
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}


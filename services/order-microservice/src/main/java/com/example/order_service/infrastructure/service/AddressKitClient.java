package com.example.order_service.infrastructure.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight client for Cas AddressKit (https://addresskit.cas.so/).
 *
 * - GET /{effectiveDate}/provinces/{provinceID}/communes
 * - POST /convert
 *
 * Dùng để:
 * - Lấy danh sách xã/phường (communes) chính thức theo dữ liệu Cục Thống Kê.
 * - Chuyển đổi địa chỉ cũ 3 cấp (Tỉnh - Huyện - Xã) sang địa chỉ mới 2 cấp.
 */
@Component
@Slf4j
public class AddressKitClient {

    private final WebClient addressKitWebClient;

    /**
     * Province ID của TP.HCM trong danh mục hành chính NSO (ví dụ: 79).
     * Có thể cấu hình qua application.properties: app.addresskit.hcm-province-id=79
     */
    private final String hcmProvinceId;

    /**
     * Cache đơn giản trong bộ nhớ cho danh sách communes theo key: effectiveDate + ":" + provinceId
     */
    private final Map<String, List<CommuneDto>> communesCache = new ConcurrentHashMap<>();

    /**
     * Cache danh sách tỉnh/thành theo effectiveDate (ví dụ "latest").
     */
    private final Map<String, List<ProvinceDto>> provinceCache = new ConcurrentHashMap<>();
    /**
     * Lấy danh sách tỉnh/thành theo hiệu lực.
     */
    public List<ProvinceDto> getProvinces(String effectiveDate) {
        if (provinceCache.containsKey(effectiveDate)) {
            return provinceCache.get(effectiveDate);
        }

        try {
            log.info("Calling AddressKit to fetch provinces, effectiveDate={}", effectiveDate);
            List<ProvinceDto> provinces = addressKitWebClient.get()
                    .uri("/{effectiveDate}/provinces", effectiveDate)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(ProvinceDto.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.error("Failed to fetch provinces from AddressKit. Reason: {}", ex.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();

            if (provinces == null) {
                provinces = Collections.emptyList();
            }

            provinceCache.put(effectiveDate, provinces);
            return provinces;
        } catch (Exception ex) {
            log.error("Unexpected error while calling AddressKit provinces API", ex);
            return Collections.emptyList();
        }
    }


    public AddressKitClient(
            WebClient addressKitWebClient,
            @Value("${app.addresskit.hcm-province-id:79}") String hcmProvinceId
    ) {
        this.addressKitWebClient = addressKitWebClient;
        this.hcmProvinceId = hcmProvinceId;
    }

    /**
     * Lấy danh sách communes (xã/phường) của một tỉnh tại thời điểm hiệu lực.
     * effectiveDate: "latest" hoặc "2025-07-01"
     */
    public List<CommuneDto> getCommunesForProvince(String effectiveDate, String provinceId) {
        String key = effectiveDate + ":" + provinceId;

        if (communesCache.containsKey(key)) {
            return communesCache.get(key);
        }

        try {
            log.info("Calling AddressKit to fetch communes for province={}, effectiveDate={}", provinceId, effectiveDate);

            List<CommuneDto> communes = addressKitWebClient.get()
                    .uri("/{effectiveDate}/provinces/{provinceId}/communes", effectiveDate, provinceId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(CommuneDto.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.error("Failed to fetch communes from AddressKit (effectiveDate={}, provinceId={}). Reason: {}",
                                effectiveDate, provinceId, ex.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();

            if (communes == null) {
                communes = Collections.emptyList();
            }

            log.info("Received {} communes from AddressKit for province {} (effectiveDate={})",
                    communes.size(), provinceId, effectiveDate);
            communesCache.put(key, communes);
            return communes;
        } catch (Exception ex) {
            log.error("Unexpected error while calling AddressKit communes API", ex);
            return Collections.emptyList();
        }
    }

    /**
     * Convenience helper for Ho Chi Minh City.
     */
    public List<CommuneDto> getHcmCommunes(String effectiveDate) {
        return getCommunesForProvince(effectiveDate, hcmProvinceId);
    }

    /**
     * Find a commune by code within a province.
     */
    public java.util.Optional<CommuneDto> findCommuneByCode(String effectiveDate, String provinceId, String communeCode) {
        return getCommunesForProvince(effectiveDate, provinceId).stream()
                .filter(c -> communeCode.equalsIgnoreCase(c.getCode()))
                .findFirst();
    }

    /**
     * Gọi API /convert để chuyển đổi địa chỉ cũ 3 cấp sang địa chỉ mới 2 cấp.
     * Input là tỉnh, huyện, xã (tên text). Output là địa chỉ đã chuẩn hóa (tên + mã).
     */
    public ConvertResponse convertLegacyAddress(String province, String district, String commune) {
        try {
            log.info("Calling AddressKit /convert for legacy address: province='{}', district='{}', commune='{}'",
                    province, district, commune);

            ConvertRequest request = ConvertRequest.builder()
                    .province(province)
                    .district(district)
                    .commune(commune)
                    .build();

            ConvertResponse response = addressKitWebClient.post()
                    .uri("/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ConvertResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.error("Failed to call AddressKit /convert. Reason: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response == null) {
                log.warn("AddressKit /convert returned null for legacy address");
            } else {
                log.info("AddressKit /convert result: provinceCode={}, communeCode={}, communeName={}",
                        response.getProvinceCode(), response.getCommuneCode(), response.getCommuneName());
            }

            return response;
        } catch (Exception ex) {
            log.error("Unexpected error while calling AddressKit /convert", ex);
            return null;
        }
    }

    // ===== DTOs cho AddressKit =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommuneDto {
        private String code;
        private String name;
        private String districtCode;
        private String districtName;
        private String provinceCode;
        private String provinceName;
        private String effectiveFrom;
        private String effectiveTo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProvinceDto {
        private String code;
        private String name;
        private String shortName;
        private String effectiveFrom;
        private String effectiveTo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConvertRequest {
        private String province;
        private String district;
        private String commune;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConvertResponse {
        private String provinceCode;
        private String provinceName;
        private String communeCode;
        private String communeName;
        private String districtName;
        private String note;
        private String effectiveDate;
    }
}



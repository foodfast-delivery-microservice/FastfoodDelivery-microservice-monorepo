package com.example.order_service.application.service;

import com.example.order_service.infrastructure.service.AddressKitClient;
import com.example.order_service.infrastructure.service.AddressKitClient.CommuneDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Chuẩn hóa địa chỉ hành chính (TP.HCM) bằng dữ liệu từ AddressKit.
 *
 * - Nhận vào city/district/ward dạng text (từ frontend + geocoding).
 * - Dùng AddressKit để map sang:
 *   + provinceCode / provinceName
 *   + communeCode / communeName (phường/xã mới)
 *   + normalizedDistrictName (tên quận/huyện theo AddressKit)
 *
 * Lưu ý: Service này không xử lý toạ độ (lat/lng) – phần đó do geocoder đảm nhiệm.
 */
@Service
@Slf4j
public class AdministrativeAddressNormalizer {

    private final AddressKitClient addressKitClient;

    public AdministrativeAddressNormalizer(AddressKitClient addressKitClient) {
        this.addressKitClient = addressKitClient;
    }

    /**
     * Chuẩn hóa địa chỉ cho TP.HCM. Nếu không thể chuẩn hóa (không match được),
     * trả về Optional.empty().
     */
    public Optional<NormalizedAddress> normalizeForHcm(String city, String district, String ward) {
        String cityNorm = normalize(city);
        if (cityNorm == null || (!cityNorm.contains("ho chi minh") && !cityNorm.contains("tp hcm"))) {
            // Không phải TP.HCM → hiện tại chỉ xử lý HCM
            log.debug("City '{}' is not recognized as Ho Chi Minh City. Skipping normalization.", city);
            return Optional.empty();
        }

        String wardNorm = normalize(ward);
        String districtNorm = normalize(district);

        // Lấy danh sách communes của TP.HCM ở thời điểm hiện tại
        List<CommuneDto> communes = addressKitClient.getHcmCommunes("latest");
        if (communes.isEmpty()) {
            log.warn("AddressKit returned empty communes list for HCM. Skipping normalization.");
            return Optional.empty();
        }

        // Lọc những communes có tên gần với ward
        List<ScoredCommune> candidates = communes.stream()
                .map(c -> scoreCommune(c, wardNorm, districtNorm))
                .filter(sc -> sc.score > 0.0)
                .sorted(Comparator.comparingDouble((ScoredCommune sc) -> sc.score).reversed())
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            log.warn("No suitable commune candidate found for ward='{}', district='{}'.", ward, district);
            return Optional.empty();
        }

        ScoredCommune best = candidates.get(0);
        CommuneDto commune = best.commune;

        log.info("Normalized address. Input ward='{}', district='{}' -> commune='{}' (code={}), district='{}', province='{}' (code={}), score={}",
                ward, district,
                commune.getName(), commune.getCode(),
                commune.getDistrictName(),
                commune.getProvinceName(), commune.getProvinceCode(),
                best.score);

        NormalizedAddress result = NormalizedAddress.builder()
                .provinceCode(commune.getProvinceCode())
                .provinceName(commune.getProvinceName())
                .communeCode(commune.getCode())
                .communeName(commune.getName())
                .normalizedDistrictName(commune.getDistrictName())
                .build();

        return Optional.of(result);
    }

    private ScoredCommune scoreCommune(CommuneDto commune, String wardNorm, String districtNorm) {
        String communeNameNorm = normalize(commune.getName());
        String communeDistrictNorm = normalize(commune.getDistrictName());

        double score = 0.0;

        // So khớp ward với commune name
        if (wardNorm != null && communeNameNorm != null) {
            if (wardNorm.equals(communeNameNorm)) {
                score += 1.0;
            } else if (communeNameNorm.contains(wardNorm) || wardNorm.contains(communeNameNorm)) {
                score += 0.7;
            } else {
                double sim = jaccardSimilarity(wardNorm, communeNameNorm);
                score += sim * 0.7;
            }
        }

        // So khớp district với commune.districtName (nếu có)
        if (districtNorm != null && communeDistrictNorm != null && !districtNorm.isEmpty()) {
            if (districtNorm.equals(communeDistrictNorm)) {
                score += 0.5;
            } else if (communeDistrictNorm.contains(districtNorm) || districtNorm.contains(communeDistrictNorm)) {
                score += 0.3;
            }
        }

        return new ScoredCommune(commune, score);
    }

    private String normalize(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return null;

        // Loại bỏ tiền tố hành chính phổ biến
        String lower = trimmed.toLowerCase(Locale.ROOT);
        lower = lower
                .replaceFirst("^(thanh pho|thành phố|tp\\.?\\s*)", "")
                .replaceFirst("^(quan|quận)\\s*", "")
                .replaceFirst("^(huyen|huyện)\\s*", "")
                .replaceFirst("^(xa|xã)\\s*", "")
                .replaceFirst("^(phuong|phường)\\s*", "");

        // Chuẩn hóa Unicode, bỏ dấu tiếng Việt
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return normalized.trim();
    }

    /**
     * Jaccard similarity trên tập ký tự (rất đơn giản, đủ dùng cho matching gần đúng).
     */
    private double jaccardSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        if (a.equals(b)) return 1.0;

        var setA = a.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        var setB = b.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());

        if (setA.isEmpty() || setB.isEmpty()) return 0.0;

        long intersectionSize = setA.stream().filter(setB::contains).count();
        long unionSize = setA.size() + setB.size() - intersectionSize;

        if (unionSize == 0) return 0.0;
        return (double) intersectionSize / (double) unionSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NormalizedAddress {
        private String provinceCode;
        private String provinceName;
        private String communeCode;
        private String communeName;
        private String normalizedDistrictName;
    }

    private static class ScoredCommune {
        private final CommuneDto commune;
        private final double score;

        private ScoredCommune(CommuneDto commune, double score) {
            this.commune = Objects.requireNonNull(commune);
            this.score = score;
        }
    }
}



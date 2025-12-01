package com.example.order_service.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddress {

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "ward", nullable = false, length = 100)
    private String ward;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /**
     * Administrative codes/names normalized from AddressKit (NSO data)
     * These fields are optional and used for analytics/joining with other systems.
     */
    @Column(name = "province_code", length = 20)
    private String provinceCode;

    @Column(name = "province_name", length = 100)
    private String provinceName;

    @Column(name = "commune_code", length = 20)
    private String communeCode;

    @Column(name = "commune_name", length = 100)
    private String communeName;

    /**
     * Optional: normalized district name (for cases where the new model still
     * distinguishes districts or for backward compatibility with legacy data).
     */
    @Column(name = "normalized_district_name", length = 100)
    private String normalizedDistrictName;

    @Column(name = "lat", precision = 10, scale = 7)
    private java.math.BigDecimal lat;

    @Column(name = "lng", precision = 10, scale = 7)
    private java.math.BigDecimal lng;

    public String getFullAddress() {
        return String.format("%s, %s, %s, %s", addressLine1, ward, district, city);
    }
}

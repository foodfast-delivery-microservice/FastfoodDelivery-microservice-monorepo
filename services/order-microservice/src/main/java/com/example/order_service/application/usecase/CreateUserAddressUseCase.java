package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.CreateUserAddressRequest;
import com.example.order_service.application.dto.UserAddressResponse;
import com.example.order_service.application.mapper.UserAddressMapper;
import com.example.order_service.domain.exception.AddressValidationException;
import com.example.order_service.domain.model.UserAddress;
import com.example.order_service.domain.repository.UserAddressRepository;
import com.example.order_service.infrastructure.service.AddressKitClient;
import com.example.order_service.infrastructure.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateUserAddressUseCase {

    private static final String EFFECTIVE_DATE_LATEST = "latest";

    private final UserAddressRepository userAddressRepository;
    private final AddressKitClient addressKitClient;
    private final GeocodingService geocodingService;

    public UserAddressResponse execute(Long userId, CreateUserAddressRequest request) {
        if (userId == null) {
            throw new AddressValidationException("Không xác định được userId");
        }
        if (request == null) {
            throw new AddressValidationException("Request không hợp lệ");
        }

        var communeDto = addressKitClient
                .findCommuneByCode(EFFECTIVE_DATE_LATEST, request.getProvinceCode(), request.getCommuneCode())
                .orElseThrow(() -> new AddressValidationException("Không tìm thấy xã/phường hợp lệ"));

        String fullAddress = buildFullAddress(request.getStreet(), communeDto);

        var geoResult = geocodingService.geocode(fullAddress);
        if (geoResult.isPresent() && geoResult.get().getDisplayName() != null) {
            fullAddress = geoResult.get().getDisplayName();
        }

        UserAddress userAddress = UserAddress.builder()
                .userId(userId)
                .street(request.getStreet())
                .provinceCode(communeDto.getProvinceCode())
                .provinceName(communeDto.getProvinceName())
                .communeCode(communeDto.getCode())
                .communeName(communeDto.getName())
                .districtName(communeDto.getDistrictName())
                .fullAddress(fullAddress)
                .note(request.getNote())
                .lat(geoResult.map(GeocodingService.GeoResult::getLat).orElse(null))
                .lng(geoResult.map(GeocodingService.GeoResult::getLon).orElse(null))
                .build();

        UserAddress saved = userAddressRepository.save(userAddress);
        log.info("Created user address id={} for userId={}", saved.getId(), userId);
        return UserAddressMapper.toResponse(saved);
    }

    private String buildFullAddress(String street, AddressKitClient.CommuneDto communeDto) {
        StringBuilder sb = new StringBuilder();
        sb.append(street.trim());
        if (communeDto.getName() != null) {
            sb.append(", ").append(communeDto.getName());
        }
        if (communeDto.getDistrictName() != null && !communeDto.getDistrictName().isBlank()) {
            sb.append(", ").append(communeDto.getDistrictName());
        }
        if (communeDto.getProvinceName() != null) {
            sb.append(", ").append(communeDto.getProvinceName());
        }
        sb.append(", Vietnam");
        return sb.toString();
    }
}




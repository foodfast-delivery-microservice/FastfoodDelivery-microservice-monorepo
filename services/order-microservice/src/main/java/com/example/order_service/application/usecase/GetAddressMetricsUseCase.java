package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.AddressMetricsResponse;
import com.example.order_service.domain.model.AddressSource;
import com.example.order_service.domain.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAddressMetricsUseCase {

    private final UserAddressRepository userAddressRepository;

    public AddressMetricsResponse execute() {
        long total = userAddressRepository.count();
        return AddressMetricsResponse.builder()
                .total(total)
                .geocodeOnly(userAddressRepository.countBySource(AddressSource.GEOCODE_ONLY))
                .userAdjust(userAddressRepository.countBySource(AddressSource.GEOCODE_USER_ADJUST))
                .driverAdjust(userAddressRepository.countBySource(AddressSource.DRIVER_ADJUST))
                .build();
    }
}




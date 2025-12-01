package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.UpdateAddressLocationRequest;
import com.example.order_service.application.dto.UserAddressResponse;
import com.example.order_service.application.mapper.UserAddressMapper;
import com.example.order_service.domain.exception.AddressValidationException;
import com.example.order_service.domain.model.AddressSource;
import com.example.order_service.domain.model.UserAddress;
import com.example.order_service.domain.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserAddressLocationUseCase {

    private final UserAddressRepository userAddressRepository;

    public UserAddressResponse execute(Long userId, Long addressId, UpdateAddressLocationRequest request) {
        if (userId == null) {
            throw new AddressValidationException("Không xác định được userId");
        }
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new AddressValidationException("Không tìm thấy địa chỉ"));

        if (!address.getUserId().equals(userId)) {
            throw new AddressValidationException("Bạn không có quyền chỉnh sửa địa chỉ này");
        }

        validateCoordinates(request.getLat(), request.getLng());

        address.setLat(request.getLat());
        address.setLng(request.getLng());
        address.setSource(request.getSource() != null ? request.getSource() : AddressSource.GEOCODE_USER_ADJUST);

        UserAddress updated = userAddressRepository.save(address);
        log.info("Updated location for address id={} by userId={}", addressId, userId);
        return UserAddressMapper.toResponse(updated);
    }

    public UserAddressResponse executeByDriver(Long addressId, UpdateAddressLocationRequest request) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new AddressValidationException("Không tìm thấy địa chỉ"));
        validateCoordinates(request.getLat(), request.getLng());
        address.setLat(request.getLat());
        address.setLng(request.getLng());
        address.setSource(AddressSource.DRIVER_ADJUST);
        UserAddress updated = userAddressRepository.save(address);
        log.info("Driver updated location for address id={}", addressId);
        return UserAddressMapper.toResponse(updated);
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            throw new AddressValidationException("Tọa độ không hợp lệ");
        }
        if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new AddressValidationException("Latitude phải nằm trong khoảng -90 đến 90");
        }
        if (lng.compareTo(BigDecimal.valueOf(-180)) < 0 || lng.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new AddressValidationException("Longitude phải nằm trong khoảng -180 đến 180");
        }
    }
}



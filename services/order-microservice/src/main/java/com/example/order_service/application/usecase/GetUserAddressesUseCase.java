package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.UserAddressResponse;
import com.example.order_service.application.mapper.UserAddressMapper;
import com.example.order_service.domain.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetUserAddressesUseCase {

    private final UserAddressRepository userAddressRepository;

    public List<UserAddressResponse> execute(Long userId) {
        return userAddressRepository.findByUserId(userId).stream()
                .map(UserAddressMapper::toResponse)
                .toList();
    }
}




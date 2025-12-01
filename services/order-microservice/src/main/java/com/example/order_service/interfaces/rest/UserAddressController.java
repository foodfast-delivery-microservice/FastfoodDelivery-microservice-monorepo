package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.AddressMetricsResponse;
import com.example.order_service.application.dto.CommuneResponse;
import com.example.order_service.application.dto.CreateUserAddressRequest;
import com.example.order_service.application.dto.ProvinceResponse;
import com.example.order_service.application.dto.UpdateAddressLocationRequest;
import com.example.order_service.application.dto.UserAddressResponse;
import com.example.order_service.application.usecase.CreateUserAddressUseCase;
import com.example.order_service.application.usecase.GetAdministrativeDivisionsUseCase;
import com.example.order_service.application.usecase.GetAddressMetricsUseCase;
import com.example.order_service.application.usecase.GetUserAddressesUseCase;
import com.example.order_service.application.usecase.UpdateUserAddressLocationUseCase;
import com.example.order_service.domain.exception.AddressValidationException;
import com.example.order_service.infrastructure.security.JwtTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Slf4j
public class UserAddressController {

    private final CreateUserAddressUseCase createUserAddressUseCase;
    private final UpdateUserAddressLocationUseCase updateUserAddressLocationUseCase;
    private final GetUserAddressesUseCase getUserAddressesUseCase;
    private final JwtTokenService jwtTokenService;
    private final GetAdministrativeDivisionsUseCase administrativeDivisionsUseCase;
    private final GetAddressMetricsUseCase getAddressMetricsUseCase;

    @PostMapping
    public ResponseEntity<UserAddressResponse> createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserAddressRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Creating address for userId={}", userId);
        UserAddressResponse response = createUserAddressUseCase.execute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserAddressResponse>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.info("Fetching addresses for userId={}", userId);
        return ResponseEntity.ok(getUserAddressesUseCase.execute(userId));
    }

    @PatchMapping("/{addressId}/location")
    public ResponseEntity<UserAddressResponse> updateLocation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressLocationRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Updating location for addressId={} by userId={}", addressId, userId);
        UserAddressResponse response = updateUserAddressLocationUseCase.execute(userId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{addressId}/driver-adjust")
    public ResponseEntity<UserAddressResponse> driverAdjustLocation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressLocationRequest request) {
        enforceDriverPrivileges(jwt);
        UserAddressResponse response = updateUserAddressLocationUseCase.executeByDriver(addressId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceResponse>> getProvinces() {
        return ResponseEntity.ok(administrativeDivisionsUseCase.getProvinces());
    }

    @GetMapping("/provinces/{provinceCode}/communes")
    public ResponseEntity<List<CommuneResponse>> getCommunes(@PathVariable String provinceCode) {
        return ResponseEntity.ok(administrativeDivisionsUseCase.getCommunes(provinceCode));
    }

    @GetMapping("/metrics")
    public ResponseEntity<AddressMetricsResponse> getMetrics(@AuthenticationPrincipal Jwt jwt) {
        String role = jwt != null ? jwt.getClaimAsString("role") : null;
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            throw new AddressValidationException("Chỉ admin mới được xem thống kê địa chỉ");
        }
        return ResponseEntity.ok(getAddressMetricsUseCase.execute());
    }

    private Long extractUserId(Jwt jwt) {
        Long userId = jwtTokenService.extractUserId(jwt);
        if (userId == null) {
            throw new AddressValidationException("Không xác định được userId từ token");
        }
        return userId;
    }

    private void enforceDriverPrivileges(Jwt jwt) {
        String role = jwt != null ? jwt.getClaimAsString("role") : null;
        if (role == null || (!role.equalsIgnoreCase("DRIVER")
                && !role.equalsIgnoreCase("ADMIN")
                && !role.equalsIgnoreCase("MERCHANT"))) {
            throw new AddressValidationException("Bạn không có quyền chỉnh sửa địa chỉ này");
        }
    }
}



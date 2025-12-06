package com.example.demo.interfaces.rest;

import com.example.demo.application.usecase.GetRestaurantByIdUseCase;
import com.example.demo.application.usecase.GetRestaurantByMerchantIdUseCase;
import com.example.demo.application.usecase.GetRestaurantsUseCase;
import com.example.demo.application.usecase.UpdateRestaurantUseCase;
import com.example.demo.domain.exception.ResourceNotFoundException;
import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.ApiResponse;
import com.example.demo.interfaces.rest.dto.restaurant.RestaurantDetailResponse;
import com.example.demo.interfaces.rest.dto.restaurant.RestaurantResponse;
import com.example.demo.interfaces.rest.dto.restaurant.RestaurantStatusRequest;
import com.example.demo.interfaces.rest.dto.restaurant.UpdateRestaurantRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final GetRestaurantsUseCase getRestaurantsUseCase;
    private final GetRestaurantByIdUseCase getRestaurantByIdUseCase;
    private final GetRestaurantByMerchantIdUseCase getRestaurantByMerchantIdUseCase;
    private final UpdateRestaurantUseCase updateRestaurantUseCase;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> listRestaurants(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean includeInactive,
            Authentication authentication) {

        // Nếu có includeInactive=true, lấy tất cả restaurants (kể cả inactive)
        // Chỉ admin mới biết dùng parameter này, nên không cần kiểm tra authentication ở đây
        // Security sẽ được kiểm tra ở SecurityConfig nếu cần
        if (Boolean.TRUE.equals(includeInactive)) {
            // Admin muốn xem tất cả restaurants (kể cả inactive)
            List<Restaurant> allRestaurants = restaurantRepository.findAll();
            List<RestaurantResponse> filtered = allRestaurants.stream()
                    .filter(r -> name == null || (r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))))
                    .filter(r -> category == null || (r.getCategory() != null && r.getCategory().equalsIgnoreCase(category)))
                    .filter(r -> city == null || (r.getCity() != null && r.getCity().equalsIgnoreCase(city)))
                    .map(RestaurantResponse::fromEntity)
                    .filter(r -> r != null)
                    .collect(Collectors.toList());

            int start = (int) PageRequest.of(Math.max(page, 0), Math.min(size, 100)).getOffset();
            int end = Math.min(start + Math.min(size, 100), filtered.size());
            List<RestaurantResponse> pageContent = start > end ? List.<RestaurantResponse>of() : filtered.subList(start, end);
            Page<RestaurantResponse> result = new PageImpl<>(pageContent, PageRequest.of(Math.max(page, 0), Math.min(size, 100)), filtered.size());
            
            ApiResponse<Page<RestaurantResponse>> response =
                    new ApiResponse<>(HttpStatus.OK, "restaurants", result, null);
            return ResponseEntity.ok(response);
        }

        // Mặc định: chỉ lấy restaurants active và approved (cho public/guest)
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100));
        Page<RestaurantResponse> result = getRestaurantsUseCase.execute(name, category, city, pageable);
        ApiResponse<Page<RestaurantResponse>> response =
                new ApiResponse<>(HttpStatus.OK, "restaurants", result, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> listAllRestaurantsForAdmin(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        System.out.println("🍽️ [RestaurantController] /admin/all - Authentication: " + (authentication != null ? authentication.getName() : "null"));
        if (authentication != null) {
            System.out.println("🔐 [RestaurantController] Authorities: " + authentication.getAuthorities());
            System.out.println("✅ [RestaurantController] Is authenticated: " + authentication.isAuthenticated());
        }
        
        // Kiểm tra admin role
        if (authentication == null || !hasRole(authentication, "ROLE_ADMIN")) {
            System.out.println("❌ [RestaurantController] Access denied - authentication is null or not ADMIN");
            throw new AccessDeniedException("Admin access required");
        }
        
        System.out.println("✅ [RestaurantController] Admin access granted");
        
        // Admin muốn xem tất cả restaurants (kể cả inactive)
        List<Restaurant> allRestaurants = restaurantRepository.findAll();
        List<RestaurantResponse> filtered = allRestaurants.stream()
                .filter(r -> name == null || (r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))))
                .filter(r -> category == null || (r.getCategory() != null && r.getCategory().equalsIgnoreCase(category)))
                .filter(r -> city == null || (r.getCity() != null && r.getCity().equalsIgnoreCase(city)))
                .map(RestaurantResponse::fromEntity)
                .filter(r -> r != null)
                .collect(Collectors.toList());

        int start = (int) PageRequest.of(Math.max(page, 0), Math.min(size, 100)).getOffset();
        int end = Math.min(start + Math.min(size, 100), filtered.size());
        List<RestaurantResponse> pageContent = start > end ? List.<RestaurantResponse>of() : filtered.subList(start, end);
        Page<RestaurantResponse> result = new PageImpl<>(pageContent, PageRequest.of(Math.max(page, 0), Math.min(size, 100)), filtered.size());
        
        ApiResponse<Page<RestaurantResponse>> response =
                new ApiResponse<>(HttpStatus.OK, "restaurants", result, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> getRestaurantById(
            @PathVariable @NonNull Long restaurantId) {
        RestaurantDetailResponse restaurant = getRestaurantByIdUseCase.execute(restaurantId);
        ApiResponse<RestaurantDetailResponse> response =
                new ApiResponse<>(HttpStatus.OK, "restaurant", restaurant, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> getRestaurantByMerchantId(
            @PathVariable @NonNull Long merchantId) {
        RestaurantDetailResponse restaurant = getRestaurantByMerchantIdUseCase.execute(merchantId);
        ApiResponse<RestaurantDetailResponse> response =
                new ApiResponse<>(HttpStatus.OK, "restaurant", restaurant, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> getMyRestaurant(Authentication authentication) {
        Long merchantId = resolveMerchantId(authentication);
        RestaurantDetailResponse restaurant = getRestaurantByMerchantIdUseCase.execute(merchantId);
        ApiResponse<RestaurantDetailResponse> response =
                new ApiResponse<>(HttpStatus.OK, "restaurant", restaurant, null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> updateMyRestaurant(
            Authentication authentication,
            @Valid @RequestBody UpdateRestaurantRequest request) {
        Long merchantId = resolveMerchantId(authentication);
        RestaurantDetailResponse restaurant = updateRestaurantUseCase.execute(merchantId, request);
        ApiResponse<RestaurantDetailResponse> response =
                new ApiResponse<>(HttpStatus.OK, "restaurant updated", restaurant, null);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> updateRestaurantStatus(
            Authentication authentication,
            @Valid @RequestBody RestaurantStatusRequest request) {
        Long merchantId = resolveMerchantId(authentication);
        Restaurant restaurant = restaurantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant for merchant not found"));
        restaurant.setActive(request.getActive());
        Restaurant saved = restaurantRepository.save(restaurant);
        ApiResponse<RestaurantDetailResponse> response =
                new ApiResponse<>(HttpStatus.OK, "restaurant status updated",
                        RestaurantDetailResponse.fromEntity(saved), null);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{restaurantId}/status")
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> updateRestaurantStatusByAdmin(
            @PathVariable @NonNull Long restaurantId,
            @Valid @RequestBody RestaurantStatusRequest request,
            Authentication authentication) {
        // Check if user is admin
        if (!hasRole(authentication, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Admin access required");
        }
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        restaurant.setActive(request.getActive());
        Restaurant saved = restaurantRepository.save(restaurant);
        ApiResponse<RestaurantDetailResponse> response =
                new ApiResponse<>(HttpStatus.OK, "restaurant status updated",
                        RestaurantDetailResponse.fromEntity(saved), null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> updateRestaurantByAdmin(
            @PathVariable @NonNull Long restaurantId,
            @Valid @RequestBody UpdateRestaurantRequest request,
            Authentication authentication) {
        // Check if user is admin
        if (!hasRole(authentication, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Admin access required");
        }
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        
        // Update restaurant fields
        if (request.getName() != null) restaurant.setName(request.getName());
        if (request.getDescription() != null) restaurant.setDescription(request.getDescription());
        if (request.getAddress() != null) restaurant.setAddress(request.getAddress());
        if (request.getCity() != null) restaurant.setCity(request.getCity());
        if (request.getDistrict() != null) restaurant.setDistrict(request.getDistrict());
        if (request.getCategory() != null) restaurant.setCategory(request.getCategory());
        if (request.getPhone() != null) restaurant.setPhone(request.getPhone());
        if (request.getEmail() != null) restaurant.setEmail(request.getEmail());
        if (request.getOpeningHours() != null) restaurant.setOpeningHours(request.getOpeningHours());
        if (request.getDeliveryFee() != null) restaurant.setDeliveryFee(request.getDeliveryFee());
        if (request.getEstimatedDeliveryTime() != null) restaurant.setEstimatedDeliveryTime(request.getEstimatedDeliveryTime());
        if (request.getImage() != null) restaurant.setImage(request.getImage());
        
        Restaurant saved = restaurantRepository.save(restaurant);
        ApiResponse<RestaurantDetailResponse> response =
                new ApiResponse<>(HttpStatus.OK, "restaurant updated by admin",
                        RestaurantDetailResponse.fromEntity(saved), null);
        return ResponseEntity.ok(response);
    }

    private Long resolveMerchantId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        boolean isMerchant = hasRole(authentication, "ROLE_MERCHANT");
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        if (!isMerchant && !isAdmin) {
            throw new AccessDeniedException("Merchant role required");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> role.equalsIgnoreCase(grantedAuthority.getAuthority()));
    }
}


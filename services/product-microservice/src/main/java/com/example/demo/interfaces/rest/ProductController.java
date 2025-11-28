package com.example.demo.interfaces.rest;

import com.example.demo.application.usecase.*;
import com.example.demo.domain.exception.AccessDeniedException;
import com.example.demo.domain.exception.InvalidIdException;
import com.example.demo.domain.exception.MissingMerchantIdException;
import com.example.demo.interfaces.common.ApiResponse;
import com.example.demo.interfaces.rest.dto.ProductPatch;
import com.example.demo.interfaces.rest.dto.ProductRequest;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import com.example.demo.interfaces.rest.dto.ProductValidationRequest;
import com.example.demo.interfaces.rest.dto.ProductValidationResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final DeleteProductByIdUseCase deleteProductByIdUseCase;
    private final GetAllProductsUserCase getAllProductsUserCase;
    private final GetProductsByCategoryUseCase getProductsByCategoryUseCase;
    private final ValidateProductsUseCase validateProductsUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final GetMerchantProductsUseCase getMerchantProductsUseCase;
    private final GetProductByIdUseCase getProductByIdUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProductRequest productRequest) {
        String role = extractRole(jwt);
        Long userId = extractUserId(jwt);

        if (isMerchant(role)) {
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing userId claim in token");
            }
            productRequest.setMerchantId(userId);
        } else if (isAdmin(role)) {
            if (productRequest.getMerchantId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "merchantId is required for admin-created products");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
        }

        ProductResponse created = createProductUseCase.create(productRequest);
        ApiResponse<ProductResponse> result = new ApiResponse<>(
                HttpStatus.CREATED,
                "created product",
                created,
                null);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody ProductPatch productPatch) {
        String role = extractRole(jwt);
        Long userId = extractUserId(jwt);
        boolean admin = isAdmin(role);

        if (!admin && userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing userId claim in token");
        }

        ProductResponse updated = updateProductUseCase.updateProduct(id, productPatch, admin ? null : userId, admin);

        ApiResponse<ProductResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "updated product",
                updated,
                null);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        String role = extractRole(jwt);
        Long userId = extractUserId(jwt);
        boolean admin = isAdmin(role);

        if (!admin && userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing userId claim in token");
        }

        deleteProductByIdUseCase.deleteProduct(id, admin ? null : userId, admin);
        ApiResponse<String> result = new ApiResponse<>(
                HttpStatus.ACCEPTED,
                "deleted product",
                null,
                null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);

    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam(value = "merchantId", required = false) Long merchantId) {

        ApiResponse<List<ProductResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "get all products",
                getAllProductsUserCase.getAllProducts(merchantId),
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ApiResponse<ProductResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "get product",
                getProductByIdUseCase.execute(id),
                null);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{category:[a-zA-Z_]+}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(@PathVariable String category) {
        ApiResponse<List<ProductResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "get products",
                getProductsByCategoryUseCase.getProductsByCategory(category),
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/merchants/me")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMerchantProducts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "merchantId", required = false) Long merchantIdParam,
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {
        String role = extractRole(jwt);
        Long userId = extractUserId(jwt);
        Long targetMerchantId;

        if (isMerchant(role)) {
            if (userId == null) {
                throw new InvalidIdException("Missing userId claim in token");
            }
            targetMerchantId = userId;
        } else if (isAdmin(role)) {
            if (merchantIdParam == null) {
                throw new MissingMerchantIdException();
            }
            targetMerchantId = merchantIdParam;
        } else {
            throw new AccessDeniedException();
        }

        List<ProductResponse> responses = getMerchantProductsUseCase.execute(targetMerchantId, includeInactive);

        ApiResponse<List<ProductResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "get merchant products",
                responses,
                null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<List<ProductValidationResponse>>> validateProducts(
            @Valid @RequestBody List<ProductValidationRequest> validationRequestList) {
        List<ProductValidationResponse> responseList = validateProductsUseCase.validate(validationRequestList);
        ApiResponse<List<ProductValidationResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "validate products",
                responseList,
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/ping")
    public String ping() {
        return "product-service OK";
    }

    private String extractRole(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("role") : null;
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object claimValue = jwt.getClaims().get("userId");
        if (claimValue instanceof Number number) {
            return number.longValue();
        }
        if (claimValue instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private boolean isMerchant(String role) {
        return "MERCHANT".equalsIgnoreCase(role);
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

}

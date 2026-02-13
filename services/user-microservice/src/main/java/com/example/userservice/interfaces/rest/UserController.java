package com.example.userservice.interfaces.rest;

import com.example.userservice.application.usecases.user.*;
import com.example.userservice.application.DTOs.user.ChangePasswordRequest;
import com.example.userservice.application.DTOs.user.CreateUserRequest;
import com.example.userservice.application.DTOs.user.CreateUserResponse;
import com.example.userservice.application.DTOs.user.UserContext;
import com.example.userservice.application.DTOs.user.UserEmailResponse;
import com.example.userservice.application.DTOs.user.UserPatchDTO;
import com.example.userservice.infrastructure.security.UserPrincipal;
import com.example.userservice.interfaces.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import com.example.userservice.domain.entities.User;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@RestController
public class UserController {
    // private final UserService userService;

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserByIdUseCase deleteUserByIdUseCase;
    private final GetAllUsersUseCase getAllUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse created = createUserUseCase.execute(request);
        ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                HttpStatus.CREATED,
                "created user",
                created,
                null);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long id,
            @RequestBody UserPatchDTO dto,
            Authentication authentication) {
        // Extract UserContext from Authentication
        UserContext userContext = extractUserContext(authentication);
        // Authorization is handled in UpdateUserUseCase
        User updated = updateUserUseCase.updateUser(id, dto, userContext);

        ApiResponse<User> result = new ApiResponse<>(
                HttpStatus.OK,
                "updated",
                updated,
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<User>> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        // Extract UserContext from Authentication
        UserContext userContext = extractUserContext(authentication);
        // Authorization is handled in ChangePasswordUseCase
        User updated = changePasswordUseCase.execute(id, request, userContext);
        ApiResponse<User> result = new ApiResponse<>(
                HttpStatus.OK,
                "changed password",
                updated,
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CreateUserResponse>>> getAllUsers() {

        ApiResponse<List<CreateUserResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "got all users",
                getAllUserUseCase.execute(),
                null);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreateUserResponse>> getUserById(@PathVariable Long id) {
        CreateUserResponse getUser = getUserByIdUseCase.execute(id);

        ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "get user",
                getUser,
                null);
        return ResponseEntity.ok(result);
    }

    /**
     * Validate user endpoint for Order Service
     * Allows USER role to validate if user exists and is active
     * GET /api/v1/users/{id}/validate
     */
    @GetMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<CreateUserResponse>> validateUser(@PathVariable Long id) {
        try {
            CreateUserResponse getUser = getUserByIdUseCase.execute(id);

            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.OK,
                    "user validated",
                    getUser,
                    null);
            return ResponseEntity.ok(result);
        } catch (com.example.userservice.domain.exception.InvalidId ex) {
            // User not found - return 404
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.NOT_FOUND,
                    ex.getMessage(),
                    null,
                    "INVALID_ID");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        } catch (Exception ex) {
            // Other errors - return 500
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error validating user: " + ex.getMessage(),
                    null,
                    "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Get user email endpoint for Notification Service
     * GET /api/v1/users/{id}/email
     * Returns minimal user information (id, fullName, email) for email notifications
     */
    @GetMapping("/{id}/email")
    public ResponseEntity<ApiResponse<UserEmailResponse>> getUserEmail(@PathVariable Long id) {
        try {
            CreateUserResponse user = getUserByIdUseCase.execute(id);
            
            UserEmailResponse emailResponse = UserEmailResponse.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .build();

            ApiResponse<UserEmailResponse> result = new ApiResponse<>(
                    HttpStatus.OK,
                    "user email retrieved",
                    emailResponse,
                    null);
            return ResponseEntity.ok(result);
        } catch (com.example.userservice.domain.exception.InvalidId ex) {
            // User not found - return 404
            ApiResponse<UserEmailResponse> result = new ApiResponse<>(
                    HttpStatus.NOT_FOUND,
                    ex.getMessage(),
                    null,
                    "INVALID_ID");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        } catch (Exception ex) {
            // Other errors - return 500
            ApiResponse<UserEmailResponse> result = new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error retrieving user email: " + ex.getMessage(),
                    null,
                    "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        deleteUserByIdUseCase.execute(id);
        ApiResponse<String> result = new ApiResponse<>(
                HttpStatus.NO_CONTENT,
                "deleted",
                null,
                null);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(result);
    }

    private final com.example.userservice.domain.repository.UserRepository userRepository;

    @Deprecated
    @GetMapping("/restaurants")
    public ResponseEntity<Void> getRestaurants() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(org.springframework.http.HttpHeaders.LOCATION, "/api/v1/restaurants")
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CreateUserResponse>> getMe(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CreateUserResponse response = CreateUserResponse.fromEntity(user);

        ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "get me",
                response,
                null);
        return ResponseEntity.ok(result);
    }

    /**
     * Extract UserContext from Spring Security Authentication object
     * This method isolates Spring Security dependencies in the controller layer
     */
    private UserContext extractUserContext(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("User is not authenticated");
        }

        String username = authentication.getName();
        Long userId = null;
        
        // Extract userId from principal
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            userId = userPrincipal.getUser().getId();
        }

        // Extract roles
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Check if admin
        boolean isAdmin = roles.stream()
                .anyMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role));

        return new UserContext(username, userId, roles, isAdmin);
    }

}

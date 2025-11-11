package com.example.demo.interfaces.rest;

import com.example.demo.application.usecase.*;
import com.example.demo.interfaces.rest.dto.user.ChangePasswordRequest;
import com.example.demo.interfaces.rest.dto.user.CreateUserRequest;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import com.example.demo.interfaces.rest.dto.user.UserPatchDTO;
import com.example.demo.interfaces.rest.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.domain.model.User;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@RestController
public class UserController {
	//private final UserService userService;

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserByIdUseCase  deleteUserByIdUseCase;
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
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id,@RequestBody UserPatchDTO dto){
        User updated = updateUserUseCase.updateUser(id,dto);

        ApiResponse<User> result = new ApiResponse<>(
                HttpStatus.OK,
                "updated",
                updated,
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<User>> changePassword(
            @PathVariable Long id,@RequestBody ChangePasswordRequest request) {
        User updated = changePasswordUseCase.execute(id, request);
        ApiResponse<User> result = new ApiResponse<>(
                HttpStatus.OK,
                "changed password",
                updated,
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CreateUserResponse>>> getAllUsers(){

        ApiResponse<List<CreateUserResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "got all users" ,
                getAllUserUseCase.execute(),
                null);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreateUserResponse>> getUserById(@PathVariable Long id){
        CreateUserResponse getUser = getUserByIdUseCase.execute(id);

        ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "get user",
                getUser,
                null
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Validate user endpoint for Order Service
     * Allows USER role to validate if user exists and is active
     * GET /api/v1/users/{id}/validate
     */
    @GetMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<CreateUserResponse>> validateUser(@PathVariable Long id){
        try {
            CreateUserResponse getUser = getUserByIdUseCase.execute(id);

            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.OK,
                    "user validated",
                    getUser,
                    null
            );
            return ResponseEntity.ok(result);
        } catch (com.example.demo.domain.exception.InvalidId ex) {
            // User not found - return 404
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.NOT_FOUND,
                    ex.getMessage(),
                    null,
                    "INVALID_ID"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        } catch (Exception ex) {
            // Other errors - return 500
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error validating user: " + ex.getMessage(),
                    null,
                    "INTERNAL_SERVER_ERROR"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id){
        deleteUserByIdUseCase.execute(id);
        ApiResponse<String> result = new ApiResponse<>(
                HttpStatus.NO_CONTENT,
                "deleted",
                null,
                null);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(result);
    }

}

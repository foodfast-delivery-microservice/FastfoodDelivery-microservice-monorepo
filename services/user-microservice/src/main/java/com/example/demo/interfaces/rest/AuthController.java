package com.example.demo.interfaces.rest;

import com.example.demo.application.usecase.LoginUseCase;
import com.example.demo.application.usecase.RegisterUseCase;
import com.example.demo.interfaces.rest.dto.ApiResponse;
import com.example.demo.interfaces.rest.dto.auth.LoginRequest;
import com.example.demo.interfaces.rest.dto.auth.LoginResponse;
import com.example.demo.interfaces.rest.dto.auth.RegisterRequest;

import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;;
    private final LoginUseCase loginUseCase;



    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CreateUserResponse>> register (@Valid @RequestBody RegisterRequest registerRequest) {
        // Gọi service
        CreateUserResponse response = registerUseCase.register(registerRequest);
        ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                HttpStatus.CREATED,
                "register successful",
                response,
                null);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login (@Valid @RequestBody LoginRequest loginRequest){
        LoginResponse loginResponse  = loginUseCase.login(loginRequest);
        ApiResponse<LoginResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "login success",
                loginResponse,
                null
        );
        return ResponseEntity.ok(result);
    }



}

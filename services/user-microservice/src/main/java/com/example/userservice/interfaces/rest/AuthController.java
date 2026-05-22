package com.example.userservice.interfaces.rest;

import com.example.userservice.application.usecases.auth.LoginUseCase;
import com.example.userservice.application.usecases.auth.RegisterUseCase;
import com.example.userservice.application.usecases.auth.RequestChangeEmailUseCase;
import com.example.userservice.application.usecases.auth.ResendEmailOtpUseCase;
import com.example.userservice.application.usecases.auth.VerifyEmailOtpUseCase;
import com.example.userservice.interfaces.common.ApiResponse;
import com.example.userservice.application.DTOs.auth.LoginRequest;
import com.example.userservice.application.DTOs.auth.LoginResponse;
import com.example.userservice.application.DTOs.auth.RegisterRequest;
import com.example.userservice.application.DTOs.auth.ChangeEmailRequest;
import com.example.userservice.application.DTOs.auth.ResendEmailOtpRequest;
import com.example.userservice.application.DTOs.auth.VerifyEmailOtpRequest;
import com.example.userservice.application.DTOs.auth.ForgotPasswordRequest;
import com.example.userservice.application.DTOs.auth.ResetPasswordRequest;
import com.example.userservice.application.DTOs.user.CreateUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RequestChangeEmailUseCase requestChangeEmailUseCase;
    private final ResendEmailOtpUseCase resendEmailOtpUseCase;
    private final VerifyEmailOtpUseCase verifyEmailOtpUseCase;

    private final com.example.userservice.application.usecases.auth.ForgotPasswordUseCase forgotPasswordUseCase;
    private final com.example.userservice.application.usecases.auth.ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "If email exists, a reset link has been sent.", null, null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Password reset successfully", null, null));
    }

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

    @PostMapping("/email/change")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            @Valid @RequestBody ChangeEmailRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED, "unauthorized", null, "UNAUTHORIZED"));
        }

        Long userId = null;
        if (authentication.getPrincipal() instanceof com.example.userservice.infrastructure.security.UserPrincipal principal) {
            userId = principal.getUser().getId();
        }

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED, "unauthorized", null, "UNAUTHORIZED"));
        }

        requestChangeEmailUseCase.requestChangeEmail(userId, request.getNewEmail());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "otp sent", null, null));
    }

    @PostMapping("/email/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        com.example.userservice.domain.entities.EmailVerificationOtp.OtpType type =
                com.example.userservice.domain.entities.EmailVerificationOtp.OtpType.valueOf(request.getType().trim().toUpperCase());
        verifyEmailOtpUseCase.verify(request.getEmail(), request.getOtp(), type);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "email verified", null, null));
    }

    @PostMapping("/email/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendEmailOtp(@Valid @RequestBody ResendEmailOtpRequest request) {
        com.example.userservice.domain.entities.EmailVerificationOtp.OtpType type =
                com.example.userservice.domain.entities.EmailVerificationOtp.OtpType.valueOf(request.getType().trim().toUpperCase());
        resendEmailOtpUseCase.resend(request.getEmail(), type);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "otp resent", null, null));
    }



}

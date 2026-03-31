package com.example.userservice;

import com.example.userservice.application.DTOs.auth.VerifyEmailOtpRequest;
import com.example.userservice.application.usecases.auth.VerifyEmailOtpUseCase;
import com.example.userservice.domain.entities.EmailVerificationOtp;
import com.example.userservice.domain.exception.OtpExpiredException;
import com.example.userservice.domain.exception.OtpInvalidException;
import com.example.userservice.domain.exception.OtpTooManyAttemptsException;
import com.example.userservice.interfaces.rest.AuthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
public class OTPVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VerifyEmailOtpUseCase verifyEmailOtpUseCase;

    @MockBean
    private com.example.userservice.application.usecases.auth.RegisterUseCase registerUseCase;

    @MockBean
    private com.example.userservice.application.usecases.auth.LoginUseCase loginUseCase;

    @MockBean
    private com.example.userservice.application.usecases.auth.RequestChangeEmailUseCase requestChangeEmailUseCase;

    @MockBean
    private com.example.userservice.application.usecases.auth.ResendEmailOtpUseCase resendEmailOtpUseCase;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void verifyOtp_signup_success() throws Exception {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest("test@test.com", "123456", "SIGNUP");

        doNothing().when(verifyEmailOtpUseCase).verify("test@test.com", "123456", EmailVerificationOtp.OtpType.SIGNUP);

        mockMvc.perform(post("/api/v1/auth/email/verify-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("email verified"));

        verify(verifyEmailOtpUseCase, times(1)).verify("test@test.com", "123456", EmailVerificationOtp.OtpType.SIGNUP);
    }

    @Test
    @WithMockUser
    void verifyOtp_changeEmail_success() throws Exception {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest("new@test.com", "654321", "CHANGE_EMAIL");

        doNothing().when(verifyEmailOtpUseCase).verify("new@test.com", "654321", EmailVerificationOtp.OtpType.CHANGE_EMAIL);

        mockMvc.perform(post("/api/v1/auth/email/verify-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("email verified"));

        verify(verifyEmailOtpUseCase, times(1)).verify("new@test.com", "654321", EmailVerificationOtp.OtpType.CHANGE_EMAIL);
    }

    @Test
    @WithMockUser
    void verifyOtp_invalidOtp_returns400() throws Exception {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest("test@test.com", "wrong", "SIGNUP");

        doThrow(new OtpInvalidException()).when(verifyEmailOtpUseCase).verify(anyString(), anyString(), any());

        // Assuming you have a GlobalExceptionHandler that maps OtpInvalidException to BAD_REQUEST (400)
        mockMvc.perform(post("/api/v1/auth/email/verify-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void verifyOtp_expiredOtp_returns400() throws Exception {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest("test@test.com", "123456", "SIGNUP");

        doThrow(new OtpExpiredException()).when(verifyEmailOtpUseCase).verify(anyString(), anyString(), any());

        mockMvc.perform(post("/api/v1/auth/email/verify-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void verifyOtp_tooManyAttempts_returns400() throws Exception {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest("test@test.com", "123456", "SIGNUP");

        doThrow(new OtpTooManyAttemptsException()).when(verifyEmailOtpUseCase).verify(anyString(), anyString(), any());

        mockMvc.perform(post("/api/v1/auth/email/verify-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

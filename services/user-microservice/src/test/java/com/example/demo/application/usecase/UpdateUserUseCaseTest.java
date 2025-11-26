package com.example.demo.application.usecase;

import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.messaging.EventPublisher;
import com.example.demo.infrastructure.messaging.event.MerchantActivatedEvent;
import com.example.demo.infrastructure.messaging.event.MerchantDeactivatedEvent;
import com.example.demo.interfaces.rest.dto.user.UserPatchDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ValidateUserAccessUseCase validateUserAccessUseCase;

    @InjectMocks
    private UpdateUserUseCase updateUserUseCase;

    private User merchantUser;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        merchantUser = new User();
        merchantUser.setId(1L);
        merchantUser.setUsername("merchant");
        merchantUser.setEmail("merchant@example.com");
        merchantUser.setRole(User.UserRole.MERCHANT);
        merchantUser.setActive(false);
        merchantUser.setApproved(true);

        adminAuth = new TestingAuthenticationToken("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Should publish MerchantActivatedEvent when merchant active toggles false -> true")
    void publishMerchantActivatedEventWhenMerchantIsReenabled() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPatchDTO patchDTO = new UserPatchDTO();
        patchDTO.setActive(true);

        // When
        updateUserUseCase.updateUser(1L, patchDTO, adminAuth);

        // Then
        ArgumentCaptor<MerchantActivatedEvent> eventCaptor = ArgumentCaptor.forClass(MerchantActivatedEvent.class);
        verify(eventPublisher).publishMerchantActivated(eventCaptor.capture());
        MerchantActivatedEvent event = eventCaptor.getValue();
        assertThat(event.getMerchantId()).isEqualTo(merchantUser.getId());
        assertThat(event.getTriggeredBy()).isEqualTo("admin");

        verify(eventPublisher, never()).publishMerchantDeactivated(any(MerchantDeactivatedEvent.class));
    }

    @Test
    @DisplayName("Should not publish MerchantActivatedEvent when active state does not change")
    void doNotPublishMerchantActivatedEventWhenActiveUnchanged() {
        // Given merchant already active
        merchantUser.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPatchDTO patchDTO = new UserPatchDTO();
        patchDTO.setActive(true); // idempotent update

        // When
        updateUserUseCase.updateUser(1L, patchDTO, adminAuth);

        // Then
        verify(eventPublisher, never()).publishMerchantActivated(any(MerchantActivatedEvent.class));
        verify(eventPublisher, never()).publishMerchantDeactivated(any(MerchantDeactivatedEvent.class));
    }
}


package com.example.userservice;

import com.example.userservice.domain.entities.OutboxEvent;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.domain.valueobjects.EventStatus;
import com.example.userservice.application.DTOs.user.UserContext;
import com.example.userservice.application.DTOs.user.UserPatchDTO;
import com.example.userservice.application.service.EventPayloadSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventPayloadSerializer eventPayloadSerializer;

    @Mock
    private com.example.userservice.application.usecases.user.ValidateUserAccessUseCase validateUserAccessUseCase;

    @InjectMocks
    private com.example.userservice.application.usecases.user.UpdateUserUseCase updateUserUseCase;

    private User merchantUser;
    private UserContext adminUserContext;

    @BeforeEach
    void setUp() {
        merchantUser = new User();
        merchantUser.setId(1L);
        merchantUser.setUsername("merchant");
        merchantUser.setEmail("merchant@example.com");
        merchantUser.setRole(User.UserRole.MERCHANT);
        merchantUser.setActive(false);
        merchantUser.setApproved(true);

        adminUserContext = new UserContext(
                "admin",
                999L,
                Set.of("ROLE_ADMIN"),
                true
        );

        // Mock EventPayloadSerializer to return JSON string
        when(eventPayloadSerializer.serialize(any())).thenReturn("{\"test\":\"data\"}");
    }

    @Test
    @DisplayName("Should create OutboxEvent for MerchantActivatedEvent when merchant active toggles false -> true")
    void createOutboxEventForMerchantActivatedWhenMerchantIsReenabled() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPatchDTO patchDTO = new UserPatchDTO();
        patchDTO.setActive(true);

        // When
        updateUserUseCase.updateUser(1L, patchDTO, adminUserContext);

        // Then - Verify OutboxEvent was created for MerchantActivated
        ArgumentCaptor<OutboxEvent> outboxEventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, atLeastOnce()).save(outboxEventCaptor.capture());
        
        // Check that MerchantActivated event was created
        boolean foundMerchantActivated = outboxEventCaptor.getAllValues().stream()
                .anyMatch(event -> "MerchantActivated".equals(event.getType()) 
                        && event.getStatus() == EventStatus.NEW);
        assertThat(foundMerchantActivated).isTrue();

        // Verify UserUpdated event was also created
        boolean foundUserUpdated = outboxEventCaptor.getAllValues().stream()
                .anyMatch(event -> "UserUpdated".equals(event.getType()));
        assertThat(foundUserUpdated).isTrue();

        // Verify EventPayloadSerializer was used
        verify(eventPayloadSerializer, atLeastOnce()).serialize(any());
    }

    @Test
    @DisplayName("Should not create OutboxEvent for MerchantActivatedEvent when active state does not change")
    void doNotCreateOutboxEventForMerchantActivatedWhenActiveUnchanged() {
        // Given merchant already active
        merchantUser.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPatchDTO patchDTO = new UserPatchDTO();
        patchDTO.setActive(true); // idempotent update

        // When
        updateUserUseCase.updateUser(1L, patchDTO, adminUserContext);

        // Then - Verify no MerchantActivated event was created
        ArgumentCaptor<OutboxEvent> outboxEventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, atLeastOnce()).save(outboxEventCaptor.capture());
        
        boolean foundMerchantActivated = outboxEventCaptor.getAllValues().stream()
                .anyMatch(event -> "MerchantActivated".equals(event.getType()));
        assertThat(foundMerchantActivated).isFalse();

        // UserUpdated event should still be created
        boolean foundUserUpdated = outboxEventCaptor.getAllValues().stream()
                .anyMatch(event -> "UserUpdated".equals(event.getType()));
        assertThat(foundUserUpdated).isTrue();
    }
}


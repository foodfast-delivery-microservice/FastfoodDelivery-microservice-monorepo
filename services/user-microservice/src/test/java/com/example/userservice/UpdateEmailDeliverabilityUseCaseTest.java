package com.example.userservice;

import com.example.userservice.application.DTOs.user.UpdateEmailDeliverabilityRequest;
import com.example.userservice.application.usecases.user.UpdateEmailDeliverabilityUseCase;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateEmailDeliverabilityUseCaseTest {

    @Test
    void execute_updatesDeliverabilityAndBounceFields() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UpdateEmailDeliverabilityUseCase useCase = new UpdateEmailDeliverabilityUseCase(userRepository);

        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setBounceCount(1);
        user.setEmailUndeliverable(false);

        Mockito.when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateEmailDeliverabilityRequest request = new UpdateEmailDeliverabilityRequest();
        request.setUndeliverable(true);
        request.setBounceIncrement(1);
        request.setBouncedAt(LocalDateTime.of(2026, 5, 7, 21, 30));

        User updated = useCase.execute(10L, request);

        assertThat(updated.isEmailUndeliverable()).isTrue();
        assertThat(updated.getBounceCount()).isEqualTo(2);
        assertThat(updated.getLastBounceAt()).isEqualTo(LocalDateTime.of(2026, 5, 7, 21, 30));
    }

    @Test
    void execute_userNotFound_throwsInvalidId() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UpdateEmailDeliverabilityUseCase useCase = new UpdateEmailDeliverabilityUseCase(userRepository);
        Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateEmailDeliverabilityRequest request = new UpdateEmailDeliverabilityRequest();
        request.setUndeliverable(true);

        assertThrows(com.example.userservice.domain.exception.InvalidId.class, () -> useCase.execute(999L, request));
    }
}

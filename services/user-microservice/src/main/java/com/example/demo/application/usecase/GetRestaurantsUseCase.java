package com.example.demo.application.usecase;

import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRestaurantsUseCase {
    private final UserRepository userRepository;

    public List<CreateUserResponse> execute() {
        List<User> merchants = userRepository.findByRoleAndActive(User.UserRole.MERCHANT, true);
        return merchants.stream()
                .map(CreateUserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

package com.example.demo.application.usecase;

import com.example.demo.domain.exception.AdminAccessDeniedException;
import com.example.demo.domain.exception.InvalidId;
import com.example.demo.domain.exception.InvalidRoleException;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.messaging.EventPublisher;
import com.example.demo.interfaces.rest.dto.event.UserUpdatedEventDTO;
import com.example.demo.interfaces.rest.dto.user.UserPatchDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
@Service
@RequiredArgsConstructor
public class UpdateUserUseCase {
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;


    @Transactional
    // user tự thay đổi thông tin của mình
    public User updateUser (Long id, UserPatchDTO userPatchDTO){
        User existingUser = userRepository.findById(id).orElseThrow(()-> new InvalidId(id));

        // only update when field was sent (not null)
        if (userPatchDTO.getUsername()!= null){
            existingUser.setUsername(userPatchDTO.getUsername());
        }
        if (userPatchDTO.getEmail()!= null){
            existingUser.setEmail(userPatchDTO.getEmail());
        }

        User updatedUser = userRepository.save(existingUser);


        // -- BƯỚC MỚI: BẮN RA SỰ KIỆN --
        UserUpdatedEventDTO eventDTO = UserUpdatedEventDTO.builder()
                .userId(updatedUser.getId())
                .newUsername(updatedUser.getUsername())
                .newEmail(updatedUser.getEmail())
                .build();

        eventPublisher.publishUserUpdated(eventDTO);

        // -- KẾT THÚC BƯỚC MỚI --

        return updatedUser;

    }

    // thay đổi role của user
    // only admin

    public User updateRoleUser (User currentUser, Long id, String newRole){
        // check if caller is admin
        if (!currentUser.getRole().equals(User.UserRole.ADMIN)){
            throw new AdminAccessDeniedException();
        }
        User targetUser = userRepository.findById(id)
                .orElseThrow(()-> new InvalidId(id));
        Set<String> allowedRole = Set.of("ADMIN", "USER");
        String roleUpper = newRole.toUpperCase();
        if (!allowedRole.contains(roleUpper)){
            throw new InvalidRoleException(roleUpper);
        }
        targetUser.setRole(User.UserRole.valueOf(roleUpper));
        return userRepository.save(targetUser);
    }

}

package com.example.demo.application.usecase;

import com.example.demo.domain.exception.*;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.messaging.EventPublisher;
import com.example.demo.interfaces.rest.dto.event.UserUpdatedEventDTO;
import com.example.demo.interfaces.rest.dto.user.UserPatchDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateUserUseCase {
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final ValidateUserAccessUseCase validateUserAccessUseCase;

    @Transactional
    // user tự thay đổi thông tin của mình
    public User updateUser(Long id, UserPatchDTO userPatchDTO, Authentication authentication) {
        // Validate: User can only update their own account (unless ADMIN)
        validateUserAccessUseCase.execute(id, authentication);

        User existingUser = userRepository.findById(id)
                .orElseThrow(()-> new InvalidId(id));

        // 1. CHECK USERNAME
        // only update when field was sent (not null)
        if (userPatchDTO.getUsername()!= null){
            // Chỉ check trùng nếu username MỚI khác username CŨ
            if(!userPatchDTO.getUsername().equals(existingUser.getUsername())){
                // nếu tên mới khác tên cũ thì kiểm tra tên mới có trùng với ai trong database không
                if (userRepository.existsByUsername(userPatchDTO.getUsername())){
                    throw new UsernameAlreadyExistException(userPatchDTO.getUsername());
                }
                // nếu không trùng set giá trị mới
                existingUser.setUsername(userPatchDTO.getUsername());
            }

        }

        // 2. CHECK EMAIL
        // only update when field was sent (not null)
        if (userPatchDTO.getEmail()!= null){
            if (!userPatchDTO.getEmail().equals(existingUser.getEmail())){
                if (userRepository.existsByEmail(userPatchDTO.getEmail())){
                    throw new EmailAlreadyExistException(userPatchDTO.getEmail());
                }
                existingUser.setEmail(userPatchDTO.getEmail());
            }
        }
        if (userPatchDTO.getApproved() != null) {
            existingUser.setApproved(userPatchDTO.getApproved());
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
        Set<String> allowedRole = Set.of("ADMIN", "USER", "MERCHANT");
        String roleUpper = newRole.toUpperCase();
        if (!allowedRole.contains(roleUpper)){
            throw new InvalidRoleException(roleUpper);
        }
        targetUser.setRole(User.UserRole.valueOf(roleUpper));
        return userRepository.save(targetUser);
    }

}

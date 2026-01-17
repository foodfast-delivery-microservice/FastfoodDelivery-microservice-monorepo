package com.example.userservice.application.usecases.user;

import com.example.userservice.domain.exception.InvalidId;
import com.example.userservice.domain.exception.MerchantDeletionNotAllowedException;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.UserRepository;

public class DeleteUserByIdUseCase {
    private final UserRepository userRepository;

    public DeleteUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String execute(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new InvalidId(id));

        // Ràng buộc: Merchant phải inactive (blocked) trước khi xóa
        if (user.getRole() == User.UserRole.MERCHANT && user.isActive()) {
            throw new MerchantDeletionNotAllowedException(
                    "Không thể xóa merchant đang hoạt động. Vui lòng block merchant trước khi xóa.");
        }

        userRepository.deleteById(id);
        return "User deleted successfully with id: " + id;
    }
}

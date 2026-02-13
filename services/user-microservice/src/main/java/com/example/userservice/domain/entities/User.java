package com.example.userservice.domain.entities;

import com.example.userservice.domain.port.PasswordEncoderPort;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pure domain entity representing a User.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private Long id;
    private String username;
    private String email;
    private String password;
    private UserRole role;
    private boolean approved = true;
    private boolean active = true;

    // Common Profile Fields
    private String fullName;
    private String phone;
    private String address;
    private String avatar;

    // Merchant Profile Fields
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantImage;
    private String openingHours;

    public enum UserRole {
        ADMIN,
        USER,
        MERCHANT
    }

    /**
     * Business logic: Change user password with validation
     * 
     * @param newPassword The new password to set
     * @param passwordEncoderPort The password encoder port to hash the password
     * @throws IllegalArgumentException if password is invalid
     */
    public void changePassword(String newPassword, PasswordEncoderPort passwordEncoderPort) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }
        // bỏ khoảng trắng đầu cuối
        newPassword = newPassword.trim();

        // kt độ mạnh mật khẩu bằng regex
        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long, contain upper and lower case letters, a number, and a special character.");
        }

        this.password = passwordEncoderPort.encode(newPassword);
    }

    /**
     * Business rule: Validate password strength
     * 
     * @param password The password to validate
     * @return true if password meets strength requirements
     */
    private boolean isStrongPassword(String password) {
        // Regex: ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
}

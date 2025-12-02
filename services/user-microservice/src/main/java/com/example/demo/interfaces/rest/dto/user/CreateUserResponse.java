package com.example.demo.interfaces.rest.dto.user;

import com.example.demo.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateUserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean approved;
    private boolean active;

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

    public static CreateUserResponse fromEntity(User user) {
        CreateUserResponse response = new CreateUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setApproved(user.isApproved());
        response.setActive(user.isActive());

        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setAvatar(user.getAvatar());

        response.setRestaurantName(user.getRestaurantName());
        response.setRestaurantAddress(user.getRestaurantAddress());
        response.setRestaurantImage(user.getRestaurantImage());
        response.setOpeningHours(user.getOpeningHours());

        return response;
    }

}

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


    public static CreateUserResponse fromEntity(User user){
        CreateUserResponse response  = new CreateUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        return response;
    }

}

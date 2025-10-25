package com.example.demo.interfaces.rest.dto.user;

import lombok.Data;

@Data
public class UserPatchDTO {
    private String username;
    private String email;
    private String role;


}

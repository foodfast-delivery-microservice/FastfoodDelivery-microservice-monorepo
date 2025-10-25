package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidId;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;



public class GetUserByIdUseCase {
    private final UserRepository userRepository;

    public GetUserByIdUseCase (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public CreateUserResponse execute (Long id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new  InvalidId(id)) ;
        return CreateUserResponse.fromEntity(user);
    }
}

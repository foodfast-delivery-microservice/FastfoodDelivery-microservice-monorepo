package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidId;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;

public class DeleteUserByIdUseCase {
    private final UserRepository userRepository;
    public DeleteUserByIdUseCase(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public String execute(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new InvalidId(id));
        userRepository.delete(user);
        return "User deleted successfully with id: "+id;
    }
}

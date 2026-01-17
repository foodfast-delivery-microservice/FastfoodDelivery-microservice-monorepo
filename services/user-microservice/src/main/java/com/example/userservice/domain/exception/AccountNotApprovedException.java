package com.example.userservice.domain.exception;

public class AccountNotApprovedException extends RuntimeException {

    public AccountNotApprovedException(Long userId) {
        super("Account is not approved for user: " + userId);
    }
}


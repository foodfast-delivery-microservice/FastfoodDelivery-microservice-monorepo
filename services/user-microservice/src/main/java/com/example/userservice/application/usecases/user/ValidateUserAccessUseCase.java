package com.example.userservice.application.usecases.user;

import com.example.userservice.application.DTOs.user.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * UseCase for validating user access permissions
 * Ensures users can only access their own data unless they are ADMIN
 */
@Service
@RequiredArgsConstructor
public class ValidateUserAccessUseCase {

    /**
     * Validate that the authenticated user has permission to access the target
     * user's data
     * 
     * @param targetUserId The ID of the user being accessed
     * @param userContext  The user context extracted from authentication
     * @throws AccessDeniedException if user doesn't have permission
     */
    public void execute(Long targetUserId, UserContext userContext) {
        if (userContext == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        // Check if user is ADMIN - ADMIN can access any user
        if (userContext.isAdmin()) {
            return; // ADMIN has full access
        }

        // For non-ADMIN users, verify they are accessing their own data
        if (!userContext.userId().equals(targetUserId)) {
            throw new AccessDeniedException(
                    "Access denied: You can only access your own account");
        }
    }
}

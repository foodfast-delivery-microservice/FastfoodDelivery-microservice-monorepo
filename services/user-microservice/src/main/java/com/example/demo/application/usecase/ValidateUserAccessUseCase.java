package com.example.demo.application.usecase;

import com.example.demo.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
     * @param targetUserId   The ID of the user being accessed
     * @param authentication The authentication object from SecurityContext
     * @throws AccessDeniedException if user doesn't have permission
     */
    public void execute(Long targetUserId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        // Check if user is ADMIN - ADMIN can access any user
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")
                        || auth.getAuthority().equals("ADMIN"));

        if (isAdmin) {
            return; // ADMIN has full access
        }

        // For non-ADMIN users, verify they are accessing their own data
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            Long authenticatedUserId = userPrincipal.getUser().getId();

            if (!authenticatedUserId.equals(targetUserId)) {
                throw new AccessDeniedException(
                        "Access denied: You can only access your own account");
            }
        } else {
            throw new AccessDeniedException("Invalid authentication principal");
        }
    }
}

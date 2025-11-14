package com.capstone_project.elderly_platform.utils;

import com.capstone_project.elderly_platform.configurations.CustomAccountDetail;
import com.capstone_project.elderly_platform.exceptions.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.UUID;

/**
 * Utility class for accessing security context information.
 * Provides helper methods to get current authenticated user details.
 */
public class SecurityUtils {

    /**
     * Gets the current authenticated user's CustomAccountDetail.
     *
     * @return CustomAccountDetail of the current user
     * @throws AuthenticationException if user is not authenticated or principal is
     *                                 not CustomAccountDetail
     */
    public static CustomAccountDetail getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomAccountDetail)) {
            throw new AuthenticationException("User is not authenticated");
        }

        return (CustomAccountDetail) authentication.getPrincipal();
    }

    /**
     * Gets the current authenticated user's account ID.
     *
     * @return UUID of the current user's account
     * @throws AuthenticationException if user is not authenticated
     */
    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Gets the current authenticated user's email.
     *
     * @return Email of the current user
     * @throws AuthenticationException if user is not authenticated
     */
    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    /**
     * Gets the current authenticated user's authorities (roles).
     *
     * @return Collection of GrantedAuthority
     * @throws AuthenticationException if user is not authenticated
     */
    public static Collection<? extends GrantedAuthority> getCurrentUserAuthorities() {
        return getCurrentUser().getAuthorities();
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param roleName The role name to check (e.g., "ROLE_ADMIN")
     * @return true if user has the role, false otherwise
     * @throws AuthenticationException if user is not authenticated
     */
    public static boolean hasRole(String roleName) {
        return getCurrentUserAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(roleName));
    }

    /**
     * Gets the current user's access token.
     *
     * @return Access token string
     * @throws AuthenticationException if user is not authenticated
     */
    public static String getCurrentUserAccessToken() {
        return getCurrentUser().getAccessToken();
    }

    /**
     * Gets the current user's refresh token.
     *
     * @return Refresh token string
     * @throws AuthenticationException if user is not authenticated
     */
    public static String getCurrentUserRefreshToken() {
        return getCurrentUser().getRefreshToken();
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomAccountDetail;
    }
}

// Example: Get current authenticated user information from
// SecurityContextHolder
// Option 1: Get full user details
// CustomAccountDetail currentUser = SecurityUtils.getCurrentUser();

// Option 2: Get specific information
// UUID currentUserId = SecurityUtils.getCurrentUserId();
// String currentUserEmail = SecurityUtils.getCurrentUserEmail();
// boolean isAdmin = SecurityUtils.hasRole("ROLE_ADMIN");

// Example usage:
// UUID caregiverAccountId = SecurityUtils.getCurrentUserId();
// Verify that the current user is the assigned caregiver
// if
// (!careService.getCaregiverProfile().getAccount().getAccountId().equals(caregiverAccountId))
// {
// throw new BadRequestException("Only the assigned caregiver can accept this
// service");
// }
package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.UpdateUserRequest;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.PagingResponse;
import com.capstone_project.elderly_platform.dtos.response.UserResponse;
import com.capstone_project.elderly_platform.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RequestMapping("/api/v1/users")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User account management operations. Only accessible by ADMIN role")
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "Lock user account", 
        description = "Lock a user account by setting enabled=false and nonLocked=false. Only accessible by ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}/lock")
    public ResponseEntity<ObjectResponse> lockUser(
            @Parameter(description = "Account ID of the user to lock")
            @PathVariable UUID accountId) {
        try {
            UserResponse userResponse = userService.lockUser(accountId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "User locked successfully", userResponse));
        } catch (Exception e) {
            log.error("Error locking user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to lock user: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Unlock user account", 
        description = "Unlock a user account by setting enabled=true and nonLocked=true. Only accessible by ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}/unlock")
    public ResponseEntity<ObjectResponse> unlockUser(
            @Parameter(description = "Account ID of the user to unlock")
            @PathVariable UUID accountId) {
        try {
            UserResponse userResponse = userService.unlockUser(accountId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "User unlocked successfully", userResponse));
        } catch (Exception e) {
            log.error("Error unlocking user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to unlock user: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Update user account", 
        description = "Update user account information (email, avatarUrl). Role cannot be updated. Only accessible by ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountId}")
    public ResponseEntity<ObjectResponse> updateUser(
            @Parameter(description = "Account ID of the user to update")
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse userResponse = userService.updateUser(accountId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "User updated successfully", userResponse));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to update user: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Get all users with filters", 
        description = "Get all users with pagination and filters (email search, locked status, date range). Returns fullName from profile. Only accessible by ADMIN role"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PagingResponse> getAllUsers(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "Search by email (partial match)")
            @RequestParam(required = false) String email,
            
            @Parameter(description = "Filter by locked status (true = locked, false = unlocked)")
            @RequestParam(required = false) Boolean isLocked,
            
            @Parameter(description = "Start date filter (optional). Format: yyyy-MM-ddTHH:mm:ss")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime startDate,
            
            @Parameter(description = "End date filter (optional). Format: yyyy-MM-ddTHH:mm:ss")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime endDate) {
        try {
            PagingResponse response = userService.getAllUsers(page, size, email, isLocked, startDate, endDate);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error getting users", e);
            PagingResponse errorResponse = PagingResponse.builder()
                    .code("Failed")
                    .message("Failed to get users: " + e.getMessage())
                    .currentPage(page)
                    .totalPages(0)
                    .elementPerPage(size)
                    .totalElements(0)
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}










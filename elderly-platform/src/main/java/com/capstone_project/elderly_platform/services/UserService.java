package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.UpdateUserRequest;
import com.capstone_project.elderly_platform.dtos.response.PagingResponse;
import com.capstone_project.elderly_platform.dtos.response.UserResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UserService {
    UserResponse lockUser(UUID accountId);
    
    UserResponse unlockUser(UUID accountId);
    
    UserResponse updateUser(UUID accountId, UpdateUserRequest request);
    
    PagingResponse getAllUsers(
            int currentPage,
            int pageSize,
            String email,
            Boolean isLocked,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}




package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {
    private Long totalRegisteredUsers;
    private Long totalUnverifiedUsers;
    private Long totalCaregivers;
    private Long totalCareSeekers;
    private Long unverifiedCaregivers;
    private Long unverifiedCareSeekers;
}

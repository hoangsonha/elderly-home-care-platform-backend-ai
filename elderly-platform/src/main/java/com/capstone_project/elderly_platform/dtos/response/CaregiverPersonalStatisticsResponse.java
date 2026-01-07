package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaregiverPersonalStatisticsResponse {
    private Long totalCareServicesThisMonth;
    private Double totalEarningsThisMonth;
    private Double overallRating;
    private Double taskCompletionRate; // Percentage (0-100)
}


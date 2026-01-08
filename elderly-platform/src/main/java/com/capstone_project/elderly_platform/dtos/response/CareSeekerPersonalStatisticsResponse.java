package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareSeekerPersonalStatisticsResponse {
    private Long totalElderlyProfiles;
    private Long totalCareServicesThisMonth;
    private Double totalSpendingThisMonth;
    private Long totalCompletedBookings;
    private Long totalInProgressServices;
}


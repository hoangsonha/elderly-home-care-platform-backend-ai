package com.capstone_project.elderly_platform.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackDashboardResponseDTO {
    // Tổng quan theo từng loại
    List<FeedbackOverviewDTO> overview;
    
    // Chi tiết cho SERVICE feedback
    FeedbackServiceDetailsDTO serviceDetails;
    
    // Top 5 caregivers có rating cao nhất
    List<TopCaregiverRatingDTO> topCaregivers;
}

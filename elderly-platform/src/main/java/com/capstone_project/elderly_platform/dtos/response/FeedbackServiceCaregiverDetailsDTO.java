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
public class FeedbackServiceCaregiverDetailsDTO {
    Double averageRating; // Rating trung bình
    Long totalFeedback; // Số lượng feedback
    
    // Chi tiết 4 tiêu chí
    List<DetailedCriteriaRatingDTO> detailedCriteria;
}

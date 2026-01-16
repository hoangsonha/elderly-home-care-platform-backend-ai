package com.capstone_project.elderly_platform.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DetailedCriteriaRatingDTO {
    String criteriaName; // professionalism, attitude, punctuality, quality
    Long totalFeedback; // Tổng số feedback có đánh giá tiêu chí này
    Double averageRating; // Rating trung bình cho tiêu chí này
}

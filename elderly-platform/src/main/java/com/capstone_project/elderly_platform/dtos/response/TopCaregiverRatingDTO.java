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
public class TopCaregiverRatingDTO {
    String caregiverId;
    String caregiverName;
    String caregiverEmail;
    Double overallRating; // Rating trung bình tổng thể
    Long totalFeedback; // Số lượng feedback
}

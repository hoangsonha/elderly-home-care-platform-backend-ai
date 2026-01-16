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
public class FeedbackOverviewDTO {
    String targetType; // SERVICE, SYSTEM, DISPUTE
    Long totalFeedback; // Tổng số feedback
    Double averageRating; // Đánh giá trung bình
}

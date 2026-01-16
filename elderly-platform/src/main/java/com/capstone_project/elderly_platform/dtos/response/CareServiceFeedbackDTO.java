package com.capstone_project.elderly_platform.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareServiceFeedbackDTO {
    String feedbackId;
    String accountId;
    String targetId;
    String targetType;
    Integer rating;
    DetailedRatingsResponseDTO detailedRatings;
    String comment;
    LocalDateTime submissionTime;
    List<String> attachmentUrls; // URLs of uploaded images
}

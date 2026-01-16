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
public class FeedbackResponseDTO {
    String feedbackId;
    String accountId;
    String accountEmail;
    String targetId;
    String targetType; // SERVICE, SYSTEM, DISPUTE
    Integer rating; // General rating (1-5)
    DetailedRatingsResponseDTO detailedRatings; // For SERVICE feedback only
    String comment;
    LocalDateTime submissionTime;
    List<AttachmentResourceResponseDTO> attachments;
}

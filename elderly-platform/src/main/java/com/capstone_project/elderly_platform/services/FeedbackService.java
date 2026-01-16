package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateFeedbackRequest;
import com.capstone_project.elderly_platform.dtos.response.FeedbackResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FeedbackService {
    FeedbackResponseDTO createFeedback(CreateFeedbackRequest request, List<MultipartFile> images);

    FeedbackResponseDTO getFeedbackById(UUID feedbackId);

    List<FeedbackResponseDTO> getFeedbacksByTarget(UUID targetId, EnumFeedbackTargetType targetType);

    List<FeedbackResponseDTO> getMyFeedbacks();

    List<FeedbackResponseDTO> getFeedbacksByTargetType(EnumFeedbackTargetType targetType);
}

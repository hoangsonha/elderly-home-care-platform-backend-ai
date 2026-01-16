package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateFeedbackRequest;
import com.capstone_project.elderly_platform.dtos.response.AttachmentResourceResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.DetailedRatingsResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.FeedbackResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumAttachmentEntityType;
import com.capstone_project.elderly_platform.enums.EnumAttachmentType;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.AttachmentResource;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.Feedback;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.AttachmentResourceRepository;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.FeedbackRepository;
import com.capstone_project.elderly_platform.services.externals.firebase.FirebaseStorageService;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AccountRepository accountRepository;
    private final AttachmentResourceRepository attachmentResourceRepository;
    private final CareServiceRepository careServiceRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public FeedbackResponseDTO createFeedback(CreateFeedbackRequest request, List<MultipartFile> images) {
        // Get current user
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(currentAccountId)
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy tài khoản"));

        // Validate target based on target type
        validateTarget(request.getTargetType(), request.getTargetId());

        // Check if user already submitted feedback for this target
        boolean alreadyExists = feedbackRepository.existsByAccountIdAndTargetIdAndTargetType(
                currentAccountId, request.getTargetId(), request.getTargetType());
        if (alreadyExists) {
            throw new BadRequestException("Bạn đã gửi feedback cho mục này rồi");
        }

        // Validate detailed ratings for SERVICE feedback
        String detailedRatingsJson = null;
        if (request.getTargetType() == EnumFeedbackTargetType.SERVICE) {
            if (request.getProfessionalism() == null || request.getAttitude() == null
                    || request.getPunctuality() == null || request.getQuality() == null) {
                throw new BadRequestException(
                        "Feedback cho service cần có đầy đủ đánh giá chi tiết: Chuyên môn, Thái độ, Đúng giờ, Chất lượng");
            }
            try {
                DetailedRatingsResponseDTO detailedRatings = DetailedRatingsResponseDTO.builder()
                        .professionalism(request.getProfessionalism())
                        .attitude(request.getAttitude())
                        .punctuality(request.getPunctuality())
                        .quality(request.getQuality())
                        .build();
                detailedRatingsJson = objectMapper.writeValueAsString(detailedRatings);
            } catch (Exception e) {
                log.error("Failed to serialize detailed ratings: {}", e.getMessage(), e);
                throw new BadRequestException("Không thể lưu đánh giá chi tiết");
            }
        }

        // Create feedback
        Feedback feedback = Feedback.builder()
                .account(account)
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .rating(request.getRating())
                .detailedRatings(detailedRatingsJson)
                .comment(request.getComment())
                .submissionTime(LocalDateTime.now())
                .build();

        Feedback savedFeedback = feedbackRepository.save(feedback);

        // Update caregiver ratings_reviews if feedback is for SERVICE
        if (request.getTargetType() == EnumFeedbackTargetType.SERVICE) {
            try {
                updateCaregiverRatingsReviews(request.getTargetId(), savedFeedback);
            } catch (Exception e) {
                log.error("Failed to update caregiver ratings_reviews for feedback {}: {}",
                        savedFeedback.getFeedbackId(), e.getMessage(), e);
                // Don't throw exception - feedback is already saved
            }
        }

        // Upload images and create attachment resources
        List<AttachmentResource> attachments = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            try {
                List<String> uploadedUrls = firebaseStorageService.uploadMultipleImages(images);
                for (int i = 0; i < uploadedUrls.size(); i++) {
                    String url = uploadedUrls.get(i);
                    // Skip error messages
                    if (url != null && !url.startsWith("Lỗi")) {
                        AttachmentResource attachment = AttachmentResource.builder()
                                .entityId(savedFeedback.getFeedbackId())
                                .entityType(EnumAttachmentEntityType.FEEDBACK)
                                .type(EnumAttachmentType.IMAGE)
                                .url(url)
                                .orderIndex(i)
                                .build();
                        attachments.add(attachment);
                    }
                }
                if (!attachments.isEmpty()) {
                    attachmentResourceRepository.saveAll(attachments);
                }
            } catch (Exception e) {
                log.error("Failed to upload images for feedback {}: {}", savedFeedback.getFeedbackId(),
                        e.getMessage(), e);
                // Don't throw exception - feedback is already saved
            }
        }

        return toDTO(savedFeedback);
    }

    @Override
    public FeedbackResponseDTO getFeedbackById(UUID feedbackId) {
        Feedback feedback = feedbackRepository.findByFeedbackIdAndDeletedIsFalse(feedbackId)
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy feedback"));
        return toDTO(feedback);
    }

    @Override
    public List<FeedbackResponseDTO> getFeedbacksByTarget(UUID targetId, EnumFeedbackTargetType targetType) {
        List<Feedback> feedbacks = feedbackRepository.findByTargetIdAndTargetTypeAndDeletedIsFalse(
                targetId, targetType, Sort.by(Sort.Direction.DESC, "submissionTime"));
        return feedbacks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<FeedbackResponseDTO> getMyFeedbacks() {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(currentAccountId)
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy tài khoản"));

        List<Feedback> feedbacks = feedbackRepository.findByAccountAndDeletedIsFalse(
                account, Sort.by(Sort.Direction.DESC, "submissionTime"));
        return feedbacks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<FeedbackResponseDTO> getFeedbacksByTargetType(EnumFeedbackTargetType targetType) {
        List<Feedback> feedbacks = feedbackRepository.findByTargetTypeAndDeletedIsFalse(
                targetType, Sort.by(Sort.Direction.DESC, "submissionTime"));
        return feedbacks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private void validateTarget(EnumFeedbackTargetType targetType, UUID targetId) {
        switch (targetType) {
            case SERVICE:
                // Validate care service exists and is completed
                CareService careService = careServiceRepository.findByCareServiceIdAndDeletedIsFalse(targetId);
                if (careService == null) {
                    throw new ElementNotFoundException("Không tìm thấy care service");
                }
                if (careService.getStatus() != EnumCareServiceStatusType.COMPLETED) {
                    throw new BadRequestException("Chỉ có thể feedback cho care service đã hoàn thành");
                }
                break;
            case SYSTEM:
                // No specific validation needed for system feedback
                break;
            case DISPUTE:
                // TODO: Validate dispute exists when dispute feature is implemented
                break;
            default:
                throw new BadRequestException("Loại feedback không hợp lệ");
        }
    }

    private FeedbackResponseDTO toDTO(Feedback feedback) {
        // Parse detailed ratings
        DetailedRatingsResponseDTO detailedRatings = null;
        if (feedback.getDetailedRatings() != null && !feedback.getDetailedRatings().isEmpty()) {
            try {
                detailedRatings = objectMapper.readValue(feedback.getDetailedRatings(),
                        DetailedRatingsResponseDTO.class);
            } catch (Exception e) {
                log.warn("Failed to parse detailed ratings for feedback {}: {}", feedback.getFeedbackId(),
                        e.getMessage());
            }
        }

        // Get attachments
        List<AttachmentResource> attachments = attachmentResourceRepository
                .findByEntityIdAndEntityTypeAndDeletedIsFalseOrderByOrderIndexAsc(
                        feedback.getFeedbackId(), EnumAttachmentEntityType.FEEDBACK);

        List<AttachmentResourceResponseDTO> attachmentDTOs = attachments.stream()
                .map(att -> AttachmentResourceResponseDTO.builder()
                        .attachmentResourceId(att.getAttachmentResourceId().toString())
                        .title(att.getTitle())
                        .type(att.getType().name())
                        .url(att.getUrl())
                        .description(att.getDescription())
                        .orderIndex(att.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return FeedbackResponseDTO.builder()
                .feedbackId(feedback.getFeedbackId().toString())
                .accountId(feedback.getAccount().getAccountId().toString())
                .accountEmail(feedback.getAccount().getEmail())
                .targetId(feedback.getTargetId().toString())
                .targetType(feedback.getTargetType().name())
                .rating(feedback.getRating())
                .detailedRatings(detailedRatings)
                .comment(feedback.getComment())
                .submissionTime(feedback.getSubmissionTime())
                .attachments(attachmentDTOs)
                .build();
    }

    /**
     * Update caregiver ratings_reviews in profileData after creating SERVICE feedback
     * 
     * @param careServiceId Care service ID
     * @param feedback      The feedback that was just created
     */
    private void updateCaregiverRatingsReviews(UUID careServiceId, Feedback feedback) {
        // Get care service to find caregiver
        CareService careService = careServiceRepository.findByCareServiceIdAndDeletedIsFalse(careServiceId);
        if (careService == null || careService.getCaregiverProfile() == null) {
            log.warn("Care service {} or caregiver profile not found for updating ratings", careServiceId);
            return;
        }

        CaregiverProfile caregiverProfile = careService.getCaregiverProfile();
        String profileDataJson = caregiverProfile.getProfileData();

        try {
            // Parse profileData
            Map<String, Object> profileDataMap = new HashMap<>();
            if (profileDataJson != null && !profileDataJson.isEmpty()) {
                profileDataMap = objectMapper.readValue(profileDataJson,
                        new TypeReference<Map<String, Object>>() {
                        });
            }

            // Get all SERVICE feedbacks for this caregiver
            List<Feedback> allServiceFeedbacks = feedbackRepository
                    .findByTargetTypeAndDeletedIsFalse(EnumFeedbackTargetType.SERVICE,
                            Sort.by(Sort.Direction.ASC, "submissionTime"));

            // Filter feedbacks for this caregiver's care services
            List<Feedback> caregiverServiceFeedbacks = new ArrayList<>();
            for (Feedback f : allServiceFeedbacks) {
                if (f.getTargetType() == EnumFeedbackTargetType.SERVICE) {
                    CareService cs = careServiceRepository.findByCareServiceIdAndDeletedIsFalse(f.getTargetId());
                    if (cs != null && cs.getCaregiverProfile() != null
                            && cs.getCaregiverProfile().getCaregiverProfileId()
                                    .equals(caregiverProfile.getCaregiverProfileId())) {
                        caregiverServiceFeedbacks.add(f);
                    }
                }
            }

            // Calculate overall_rating (average of all ratings)
            double totalRating = 0.0;
            int totalReviews = caregiverServiceFeedbacks.size();
            Map<String, Integer> ratingBreakdown = new HashMap<>();
            ratingBreakdown.put("5_star", 0);
            ratingBreakdown.put("4_star", 0);
            ratingBreakdown.put("3_star", 0);
            ratingBreakdown.put("2_star", 0);
            ratingBreakdown.put("1_star", 0);

            // Calculate detailed ratings averages
            double totalProfessionalism = 0.0;
            double totalAttitude = 0.0;
            double totalPunctuality = 0.0;
            double totalQuality = 0.0;
            int detailedRatingsCount = 0;

            for (Feedback f : caregiverServiceFeedbacks) {
                totalRating += f.getRating();

                // Update rating breakdown
                String starKey = f.getRating() + "_star";
                ratingBreakdown.put(starKey, ratingBreakdown.getOrDefault(starKey, 0) + 1);

                // Parse detailed ratings if available
                if (f.getDetailedRatings() != null && !f.getDetailedRatings().isEmpty()) {
                    try {
                        DetailedRatingsResponseDTO detailedRatings = objectMapper.readValue(
                                f.getDetailedRatings(), DetailedRatingsResponseDTO.class);
                        if (detailedRatings.getProfessionalism() != null) {
                            totalProfessionalism += detailedRatings.getProfessionalism();
                        }
                        if (detailedRatings.getAttitude() != null) {
                            totalAttitude += detailedRatings.getAttitude();
                        }
                        if (detailedRatings.getPunctuality() != null) {
                            totalPunctuality += detailedRatings.getPunctuality();
                        }
                        if (detailedRatings.getQuality() != null) {
                            totalQuality += detailedRatings.getQuality();
                        }
                        detailedRatingsCount++;
                    } catch (Exception e) {
                        log.warn("Failed to parse detailed ratings for feedback {}: {}", f.getFeedbackId(),
                                e.getMessage());
                    }
                }
            }

            double overallRating = totalReviews > 0 ? totalRating / totalReviews : 0.0;

            // Build ratings_reviews map
            Map<String, Object> ratingsReviewsMap = new HashMap<>();
            ratingsReviewsMap.put("overall_rating", Math.round(overallRating * 10.0) / 10.0); // Round to 1 decimal
            ratingsReviewsMap.put("total_reviews", totalReviews);
            ratingsReviewsMap.put("rating_breakdown", ratingBreakdown);

            // Add detailed ratings breakdown if available
            if (detailedRatingsCount > 0) {
                Map<String, Double> detailedRatingsBreakdown = new HashMap<>();
                detailedRatingsBreakdown.put("professionalism",
                        Math.round((totalProfessionalism / detailedRatingsCount) * 10.0) / 10.0);
                detailedRatingsBreakdown.put("attitude",
                        Math.round((totalAttitude / detailedRatingsCount) * 10.0) / 10.0);
                detailedRatingsBreakdown.put("punctuality",
                        Math.round((totalPunctuality / detailedRatingsCount) * 10.0) / 10.0);
                detailedRatingsBreakdown.put("quality",
                        Math.round((totalQuality / detailedRatingsCount) * 10.0) / 10.0);
                ratingsReviewsMap.put("detailed_ratings_breakdown", detailedRatingsBreakdown);
            }

            // Update profileData
            profileDataMap.put("ratings_reviews", ratingsReviewsMap);
            String updatedProfileDataJson = objectMapper.writeValueAsString(profileDataMap);
            caregiverProfile.setProfileData(updatedProfileDataJson);
            caregiverProfileRepository.save(caregiverProfile);

            log.info("Updated ratings_reviews for caregiver profile {}: overall_rating={}, total_reviews={}",
                    caregiverProfile.getCaregiverProfileId(), overallRating, totalReviews);

        } catch (Exception e) {
            log.error("Failed to update caregiver ratings_reviews: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update caregiver ratings_reviews", e);
        }
    }
}

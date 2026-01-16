package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.response.*;
import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import com.capstone_project.elderly_platform.enums.EnumRoleType;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.Feedback;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.FeedbackRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackDashboardServiceImpl implements FeedbackDashboardService {

    private final FeedbackRepository feedbackRepository;
    private final CareServiceRepository careServiceRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public FeedbackDashboardResponseDTO getFeedbackDashboard() {
        // 1. Overview theo từng loại (SYSTEM, SERVICE, DISPUTE)
        List<FeedbackOverviewDTO> overview = getOverviewByTargetType();

        // 2. Chi tiết cho SERVICE feedback
        FeedbackServiceDetailsDTO serviceDetails = getServiceDetails();

        // 3. Top 5 caregivers có rating cao nhất
        List<TopCaregiverRatingDTO> topCaregivers = getTop5Caregivers();

        return FeedbackDashboardResponseDTO.builder()
                .overview(overview)
                .serviceDetails(serviceDetails)
                .topCaregivers(topCaregivers)
                .build();
    }

    private List<FeedbackOverviewDTO> getOverviewByTargetType() {
        List<FeedbackOverviewDTO> overview = new ArrayList<>();

        for (EnumFeedbackTargetType targetType : EnumFeedbackTargetType.values()) {
            Long totalFeedback = feedbackRepository.countByTargetType(targetType);
            if (totalFeedback == null) {
                totalFeedback = 0L;
            }
            
            Double averageRating = feedbackRepository.getAverageRatingByTargetType(targetType);
            if (averageRating == null) {
                averageRating = 0.0;
            }

            overview.add(FeedbackOverviewDTO.builder()
                    .targetType(targetType.name())
                    .totalFeedback(totalFeedback)
                    .averageRating(averageRating)
                    .build());
        }

        return overview;
    }

    private FeedbackServiceDetailsDTO getServiceDetails() {
        // Lấy tất cả feedback có targetType = SERVICE
        List<Feedback> serviceFeedbacks = feedbackRepository
                .findByTargetTypeAndDeletedIsFalse(EnumFeedbackTargetType.SERVICE, null);

        // Chi tiết cho caregiver
        FeedbackServiceCaregiverDetailsDTO caregiverDetails = getCaregiverDetails(serviceFeedbacks);

        // Chi tiết cho care seeker
        FeedbackServiceCareSeekerDetailsDTO careSeekerDetails = getCareSeekerDetails(serviceFeedbacks);

        return FeedbackServiceDetailsDTO.builder()
                .caregiverDetails(caregiverDetails)
                .careSeekerDetails(careSeekerDetails)
                .build();
    }

    private FeedbackServiceCaregiverDetailsDTO getCaregiverDetails(List<Feedback> serviceFeedbacks) {
        // Lấy feedback từ care seeker đánh giá caregiver
        // Tức là feedback có account.role = ROLE_CARE_SEEKER
        List<Feedback> caregiverFeedbacks = serviceFeedbacks.stream()
                .filter(feedback -> feedback.getAccount() != null 
                        && feedback.getAccount().getRole() != null
                        && feedback.getAccount().getRole().getRoleName() == EnumRoleType.ROLE_CARE_SEEKER)
                .collect(Collectors.toList());

        long totalRating = 0;
        long totalFeedback = caregiverFeedbacks.size();

        for (Feedback feedback : caregiverFeedbacks) {
            totalRating += feedback.getRating();
        }

        // Tính toán chi tiết 4 tiêu chí từ feedback của care seeker
        List<DetailedCriteriaRatingDTO> detailedCriteria = calculateDetailedCriteria(caregiverFeedbacks);

        double averageRating = totalFeedback > 0 ? (double) totalRating / totalFeedback : 0.0;

        return FeedbackServiceCaregiverDetailsDTO.builder()
                .averageRating(averageRating)
                .totalFeedback(totalFeedback)
                .detailedCriteria(detailedCriteria)
                .build();
    }

    private FeedbackServiceCareSeekerDetailsDTO getCareSeekerDetails(List<Feedback> serviceFeedbacks) {
        // Lấy feedback từ caregiver đánh giá care seeker
        // Tức là feedback có account.role = ROLE_CAREGIVER
        List<Feedback> careSeekerFeedbacks = serviceFeedbacks.stream()
                .filter(feedback -> feedback.getAccount() != null 
                        && feedback.getAccount().getRole() != null
                        && feedback.getAccount().getRole().getRoleName() == EnumRoleType.ROLE_CAREGIVER)
                .collect(Collectors.toList());

        long totalRating = 0;
        long totalFeedback = careSeekerFeedbacks.size();

        for (Feedback feedback : careSeekerFeedbacks) {
            totalRating += feedback.getRating();
        }

        double averageRating = totalFeedback > 0 ? (double) totalRating / totalFeedback : 0.0;

        return FeedbackServiceCareSeekerDetailsDTO.builder()
                .totalFeedback(totalFeedback)
                .averageRating(averageRating)
                .build();
    }

    private List<DetailedCriteriaRatingDTO> calculateDetailedCriteria(List<Feedback> serviceFeedbacks) {
        Map<String, Long> totalRatingsByCriteria = new HashMap<>();
        Map<String, Long> countByCriteria = new HashMap<>();

        totalRatingsByCriteria.put("professionalism", 0L);
        totalRatingsByCriteria.put("attitude", 0L);
        totalRatingsByCriteria.put("punctuality", 0L);
        totalRatingsByCriteria.put("quality", 0L);

        countByCriteria.put("professionalism", 0L);
        countByCriteria.put("attitude", 0L);
        countByCriteria.put("punctuality", 0L);
        countByCriteria.put("quality", 0L);

        for (Feedback feedback : serviceFeedbacks) {
            if (feedback.getDetailedRatings() != null && !feedback.getDetailedRatings().isEmpty()) {
                try {
                    DetailedRatingsResponseDTO detailedRatings = objectMapper.readValue(
                            feedback.getDetailedRatings(), DetailedRatingsResponseDTO.class);

                    if (detailedRatings.getProfessionalism() != null) {
                        totalRatingsByCriteria.put("professionalism",
                                totalRatingsByCriteria.get("professionalism") + detailedRatings.getProfessionalism());
                        countByCriteria.put("professionalism", countByCriteria.get("professionalism") + 1);
                    }
                    if (detailedRatings.getAttitude() != null) {
                        totalRatingsByCriteria.put("attitude",
                                totalRatingsByCriteria.get("attitude") + detailedRatings.getAttitude());
                        countByCriteria.put("attitude", countByCriteria.get("attitude") + 1);
                    }
                    if (detailedRatings.getPunctuality() != null) {
                        totalRatingsByCriteria.put("punctuality",
                                totalRatingsByCriteria.get("punctuality") + detailedRatings.getPunctuality());
                        countByCriteria.put("punctuality", countByCriteria.get("punctuality") + 1);
                    }
                    if (detailedRatings.getQuality() != null) {
                        totalRatingsByCriteria.put("quality",
                                totalRatingsByCriteria.get("quality") + detailedRatings.getQuality());
                        countByCriteria.put("quality", countByCriteria.get("quality") + 1);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse detailed ratings for feedback {}: {}", feedback.getFeedbackId(),
                            e.getMessage());
                }
            }
        }

        List<DetailedCriteriaRatingDTO> criteriaList = new ArrayList<>();
        for (String criteria : Arrays.asList("professionalism", "attitude", "punctuality", "quality")) {
            long count = countByCriteria.get(criteria);
            double average = count > 0 ? (double) totalRatingsByCriteria.get(criteria) / count : 0.0;

            criteriaList.add(DetailedCriteriaRatingDTO.builder()
                    .criteriaName(criteria)
                    .totalFeedback(count)
                    .averageRating(average)
                    .build());
        }

        return criteriaList;
    }

    private List<TopCaregiverRatingDTO> getTop5Caregivers() {
        // Lấy tất cả feedback có targetType = SERVICE
        List<Feedback> serviceFeedbacks = feedbackRepository
                .findByTargetTypeAndDeletedIsFalse(EnumFeedbackTargetType.SERVICE, null);

        // Lấy tất cả care service IDs
        Set<UUID> careServiceIds = serviceFeedbacks.stream()
                .map(Feedback::getTargetId)
                .collect(Collectors.toSet());

        // Lấy tất cả care services
        Map<UUID, CareService> careServiceMap = careServiceRepository.findAllById(careServiceIds)
                .stream()
                .filter(cs -> !cs.isDeleted())
                .collect(Collectors.toMap(CareService::getCareServiceId, cs -> cs));

        // Nhóm feedback theo caregiver và tính rating trung bình
        Map<UUID, List<Feedback>> feedbacksByCaregiver = new HashMap<>();
        Map<UUID, CareService> caregiverServiceMap = new HashMap<>();

        for (Feedback feedback : serviceFeedbacks) {
            CareService careService = careServiceMap.get(feedback.getTargetId());
            if (careService != null && careService.getCaregiverProfile() != null) {
                UUID caregiverId = careService.getCaregiverProfile().getCaregiverProfileId();
                feedbacksByCaregiver.computeIfAbsent(caregiverId, k -> new ArrayList<>()).add(feedback);
                caregiverServiceMap.put(caregiverId, careService);
            }
        }

        // Tính rating trung bình cho mỗi caregiver
        List<TopCaregiverRatingDTO> topCaregivers = new ArrayList<>();

        for (Map.Entry<UUID, List<Feedback>> entry : feedbacksByCaregiver.entrySet()) {
            UUID caregiverId = entry.getKey();
            List<Feedback> feedbacks = entry.getValue();

            long totalRating = feedbacks.stream().mapToLong(Feedback::getRating).sum();
            double averageRating = feedbacks.size() > 0 ? (double) totalRating / feedbacks.size() : 0.0;

            CareService careService = caregiverServiceMap.get(caregiverId);
            if (careService != null && careService.getCaregiverProfile() != null) {
                String caregiverName = careService.getCaregiverProfile().getFullName();
                String caregiverEmail = careService.getCaregiverProfile().getAccount() != null
                        ? careService.getCaregiverProfile().getAccount().getEmail()
                        : null;

                topCaregivers.add(TopCaregiverRatingDTO.builder()
                        .caregiverId(caregiverId.toString())
                        .caregiverName(caregiverName)
                        .caregiverEmail(caregiverEmail)
                        .overallRating(averageRating)
                        .totalFeedback((long) feedbacks.size())
                        .build());
            }
        }

        // Sắp xếp theo rating giảm dần và lấy top 5
        return topCaregivers.stream()
                .sorted((a, b) -> Double.compare(b.getOverallRating(), a.getOverallRating()))
                .limit(5)
                .collect(Collectors.toList());
    }
}

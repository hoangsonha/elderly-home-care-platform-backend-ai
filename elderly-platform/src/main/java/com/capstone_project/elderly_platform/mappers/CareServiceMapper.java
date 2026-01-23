package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.CareServiceFeedbackDTO;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileDetailResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.DetailedRatingsResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.WorkScheduleResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumAttachmentEntityType;
import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import com.capstone_project.elderly_platform.pojos.AttachmentResource;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.Feedback;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.Qualification;
import com.capstone_project.elderly_platform.repositories.AttachmentResourceRepository;
import com.capstone_project.elderly_platform.repositories.FeedbackRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareServiceMapper {

        private final CareSeekerProfileMapper careSeekerProfileMapper;
        private final CaregiverProfileMapper caregiverProfileMapper;
        private final ElderlyProfileMapper elderlyProfileMapper;
        private final ServicePackageMapper servicePackageMapper;
        private final WorkScheduleMapper workScheduleMapper;
        private final FeedbackRepository feedbackRepository;
        private final AttachmentResourceRepository attachmentResourceRepository;
        private final ObjectMapper objectMapper;

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
        private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public CareServiceResponseDTO toDTO(CareService careService) {
                if (careService == null) {
                        return null;
                }

                // Map nested profiles
                CareSeekerProfileResponseDTO careSeekerProfileDTO = null;
                if (careService.getCareSeekerProfile() != null) {
                        careSeekerProfileDTO = careSeekerProfileMapper.toDTO(careService.getCareSeekerProfile());
                }

                CaregiverProfileDetailResponseDTO caregiverProfileDTO = null;
                if (careService.getCaregiverProfile() != null) {
                        caregiverProfileDTO = mapCaregiverProfileToDetailDTO(careService.getCaregiverProfile());
                }

                ElderlyProfileResponseDTO elderlyProfileDTO = null;
                if (careService.getElderlyProfile() != null) {
                        elderlyProfileDTO = elderlyProfileMapper.toDTO(careService.getElderlyProfile());
                }

                ServicePackageResponseDTO servicePackageDTO = null;
                if (careService.getServicePackage() != null) {
                        servicePackageDTO = servicePackageMapper.toDTO(careService.getServicePackage());
                }

                // Map work schedule
                WorkScheduleResponseDTO workScheduleDTO = null;
                if (careService.getWorkSchedule() != null && !careService.getWorkSchedule().isDeleted()) {
                        workScheduleDTO = workScheduleMapper.toDTO(careService.getWorkSchedule());
                }

                // Map feedback (if exists)
                CareServiceFeedbackDTO feedbackDTO = null;
                if (careService.getCareServiceId() != null) {
                        try {
                                List<Feedback> feedbacks = feedbackRepository
                                                .findByTargetIdAndTargetTypeAndDeletedIsFalse(
                                                                careService.getCareServiceId(),
                                                                EnumFeedbackTargetType.SERVICE,
                                                                Sort.by(Sort.Direction.DESC, "submissionTime"));
                                if (!feedbacks.isEmpty()) {
                                        Feedback feedback = feedbacks.get(0); // Get the most recent feedback
                                        feedbackDTO = mapFeedbackToDTO(feedback);
                                }
                        } catch (Exception e) {
                                log.warn("Failed to load feedback for care service {}: {}",
                                                careService.getCareServiceId(),
                                                e.getMessage());
                        }
                }

                return CareServiceResponseDTO.builder()
                                .careServiceId(careService.getCareServiceId() != null
                                                ? careService.getCareServiceId().toString()
                                                : null)
                                .careServiceSnapshot(careService.getCareServiceSnapshot()) // Keep as JSON string
                                .bookingCode(careService.getBookingCode())
                                .workDate(careService.getWorkDate() != null
                                                ? careService.getWorkDate().format(DATE_FORMATTER)
                                                : null)
                                .startTime(careService.getStartTime() != null
                                                ? careService.getStartTime().format(TIME_FORMATTER)
                                                : null)
                                .endTime(careService.getEndTime() != null
                                                ? careService.getEndTime().format(TIME_FORMATTER)
                                                : null)
                                .caregiverResponseDeadline(careService.getCaregiverResponseDeadline() != null
                                                ? careService.getCaregiverResponseDeadline().format(DATETIME_FORMATTER)
                                                : null)
                                .completedAt(careService.getCompletedAt() != null
                                                ? careService.getCompletedAt().format(DATETIME_FORMATTER)
                                                : null)
                                .status(careService.getStatus() != null
                                                ? careService.getStatus().name()
                                                : null)
                                .note(careService.getNote())
                                .systemFeePercentage(careService.getSystemFeePercentage())
                                .totalPrice(careService.getTotalPrice())
                                .caregiverEarnings(careService.getCaregiverEarnings())
                                .location(careService.getLocation()) // Keep as JSON string
                                .configVersion(careService.getConfigVersion()) // Keep as JSON string
                                .careSeekerProfile(careSeekerProfileDTO)
                                .elderlyProfile(elderlyProfileDTO)
                                .caregiverProfile(caregiverProfileDTO)
                                .servicePackage(servicePackageDTO)
                                .workSchedule(workScheduleDTO)
                                .feedback(feedbackDTO)
                                .build();
        }

        private CareServiceFeedbackDTO mapFeedbackToDTO(Feedback feedback) {
                if (feedback == null) {
                        return null;
                }

                // Parse detailed ratings
                DetailedRatingsResponseDTO detailedRatings = null;
                if (feedback.getDetailedRatings() != null && !feedback.getDetailedRatings().isEmpty()) {
                        try {
                                detailedRatings = objectMapper.readValue(feedback.getDetailedRatings(),
                                                DetailedRatingsResponseDTO.class);
                        } catch (Exception e) {
                                log.warn("Failed to parse detailed ratings for feedback {}: {}",
                                                feedback.getFeedbackId(),
                                                e.getMessage());
                        }
                }

                // Get attachment URLs
                List<String> attachmentUrls = null;
                if (feedback.getFeedbackId() != null) {
                        try {
                                List<AttachmentResource> attachments = attachmentResourceRepository
                                                .findByEntityIdAndEntityTypeAndDeletedIsFalseOrderByOrderIndexAsc(
                                                                feedback.getFeedbackId(),
                                                                EnumAttachmentEntityType.FEEDBACK);
                                if (attachments != null && !attachments.isEmpty()) {
                                        attachmentUrls = attachments.stream()
                                                        .map(AttachmentResource::getUrl)
                                                        .filter(url -> url != null && !url.isEmpty())
                                                        .collect(Collectors.toList());
                                }
                        } catch (Exception e) {
                                log.warn("Failed to load attachments for feedback {}: {}", feedback.getFeedbackId(),
                                                e.getMessage());
                        }
                }

                return CareServiceFeedbackDTO.builder()
                                .feedbackId(feedback.getFeedbackId() != null ? feedback.getFeedbackId().toString()
                                                : null)
                                .accountId(feedback.getAccount() != null && feedback.getAccount().getAccountId() != null
                                                ? feedback.getAccount().getAccountId().toString()
                                                : null)
                                .targetId(feedback.getTargetId() != null ? feedback.getTargetId().toString() : null)
                                .targetType(feedback.getTargetType() != null ? feedback.getTargetType().name() : null)
                                .rating(feedback.getRating())
                                .detailedRatings(detailedRatings)
                                .comment(feedback.getComment())
                                .submissionTime(feedback.getSubmissionTime())
                                .attachmentUrls(attachmentUrls)
                                .build();
        }

        private CaregiverProfileDetailResponseDTO mapCaregiverProfileToDetailDTO(CaregiverProfile profile) {
                if (profile == null) {
                        return null;
                }

                // Calculate age
                Integer age = null;
                if (profile.getBirthDate() != null) {
                        age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
                }

                // Map Account info
                String accountId = null;
                String email = null;
                String avatarUrl = null;
                Boolean enabled = null;
                Boolean nonLocked = null;

                if (profile.getAccount() != null) {
                        accountId = profile.getAccount().getAccountId() != null
                                        ? profile.getAccount().getAccountId().toString()
                                        : null;
                        email = profile.getAccount().getEmail();
                        avatarUrl = profile.getAccount().getAvatarUrl();
                        enabled = profile.getAccount().getEnabled();
                        nonLocked = profile.getAccount().getNonLocked();
                }

                // Map Qualifications (include all, even deleted ones)
                List<CaregiverProfileDetailResponseDTO.QualificationDetailDTO> qualifications = new ArrayList<>();
                if (profile.getQualifications() != null) {
                        qualifications = profile.getQualifications().stream()
                                        .map(this::mapQualificationToDetailDTO)
                                        .collect(Collectors.toList());
                }

                return CaregiverProfileDetailResponseDTO.builder()
                                .caregiverProfileId(profile.getCaregiverProfileId() != null
                                                ? profile.getCaregiverProfileId().toString()
                                                : null)
                                .fullName(profile.getFullName())
                                .phoneNumber(profile.getPhoneNumber())
                                .location(profile.getLocation())
                                .bio(profile.getBio())
                                .isVerified(profile.getIsVerified())
                                .status(profile.getStatus() != null ? profile.getStatus().name() : null)
                                .rejectionReason(profile.getRejectionReason())
                                .isNeededReviewCertificate(profile.getIsNeededReviewCertificate())
                                .acceptedAt(profile.getAcceptedAt() != null
                                                ? profile.getAcceptedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                : null)
                                .declinedAt(profile.getDeclinedAt() != null
                                                ? profile.getDeclinedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                : null)
                                .reviewedBy(profile.getReviewedBy() != null ? profile.getReviewedBy().toString() : null)
                                .birthDate(profile.getBirthDate() != null
                                                ? profile.getBirthDate().format(DATE_FORMATTER)
                                                : null)
                                .age(age)
                                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                                .profileData(profile.getProfileData())
                                .accountId(accountId)
                                .email(email)
                                .avatarUrl(avatarUrl)
                                .enabled(enabled)
                                .nonLocked(nonLocked)
                                .qualifications(qualifications)
                                // Statistics are null for nested objects (only needed for getMyCaregiverProfile)
                                .totalCompletedBookings(null)
                                .totalEarnings(null)
                                .taskCompletionRate(null)
                                .build();
        }

        private CaregiverProfileDetailResponseDTO.QualificationDetailDTO mapQualificationToDetailDTO(
                        Qualification qualification) {
                if (qualification == null) {
                        return null;
                }

                String qualificationTypeId = null;
                String qualificationTypeName = null;
                if (qualification.getQualificationType() != null) {
                        qualificationTypeId = qualification.getQualificationType().getQualificationTypeId() != null
                                        ? qualification.getQualificationType().getQualificationTypeId().toString()
                                        : null;
                        qualificationTypeName = qualification.getQualificationType().getTypeName();
                }

                return CaregiverProfileDetailResponseDTO.QualificationDetailDTO.builder()
                                .qualificationId(qualification.getQualificationId() != null
                                                ? qualification.getQualificationId().toString()
                                                : null)
                                .qualificationTypeId(qualificationTypeId)
                                .qualificationTypeName(qualificationTypeName)
                                .certificateNumber(qualification.getCertificateNumber())
                                .issuingOrganization(qualification.getIssuingOrganization())
                                .issueDate(qualification.getIssueDate() != null
                                                ? qualification.getIssueDate().format(DATE_FORMATTER)
                                                : null)
                                .expiryDate(qualification.getExpiryDate() != null
                                                ? qualification.getExpiryDate().format(DATE_FORMATTER)
                                                : null)
                                .certificateUrl(qualification.getCertificateUrl())
                                .isVerified(qualification.getIsVerified())
                                .status(qualification.getStatus() != null ? qualification.getStatus().name() : null)
                                .rejectionReason(qualification.getRejectionReason())
                                .acceptedAt(qualification.getAcceptedAt() != null
                                                ? qualification.getAcceptedAt()
                                                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                : null)
                                .declinedAt(qualification.getDeclinedAt() != null
                                                ? qualification.getDeclinedAt()
                                                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                : null)
                                .reviewedBy(qualification.getReviewedBy() != null
                                                ? qualification.getReviewedBy().toString()
                                                : null)
                                .notes(qualification.getNotes())
                                .deleted(qualification.isDeleted())
                                .build();
        }
}

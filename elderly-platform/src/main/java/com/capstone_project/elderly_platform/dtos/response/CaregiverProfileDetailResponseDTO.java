package com.capstone_project.elderly_platform.dtos.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.capstone_project.elderly_platform.utils.JsonStringDeserializer;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CaregiverProfileDetailResponseDTO {
    // Caregiver Profile Info
    String caregiverProfileId;
    String fullName;
    String phoneNumber;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String location;
    
    String bio;
    Boolean isVerified;
    String status; // PENDING, APPROVED, REJECTED, EXPIRED
    String rejectionReason;
    Boolean isNeededReviewCertificate;
    String acceptedAt;
    String declinedAt;
    String reviewedBy;
    String birthDate;
    Integer age;
    String gender;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String profileData;
    
    // Account Info
    String accountId;
    String email;
    String avatarUrl;
    Boolean enabled;
    Boolean nonLocked;
    
    // Qualifications
    List<QualificationDetailDTO> qualifications;
    
    // Statistics
    Long totalCompletedBookings;        // Tổng số lịch hẹn (care-service có status COMPLETED)
    Double totalEarnings;               // Tổng thu nhập (tổng total_caregiver_earnings từ tất cả PayoutBatches)
    Double taskCompletionRate;           // Tỉ lệ task hoàn thành (%) trong các care-service COMPLETED
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QualificationDetailDTO {
        String qualificationId;
        String qualificationTypeId;
        String qualificationTypeName;
        String certificateNumber;
        String issuingOrganization;
        String issueDate;
        String expiryDate;
        String certificateUrl;
        Boolean isVerified;
        String status; // PENDING, APPROVED, REJECTED, EXPIRED
        String rejectionReason;
        String acceptedAt;
        String declinedAt;
        String reviewedBy;
        String notes;
        Boolean deleted; // true nếu chứng chỉ đã bị soft delete
    }
}


package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumVerificationStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "qualifications")
public class Qualification extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "qualification_id")
    UUID qualificationId;

    @ManyToOne
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    CaregiverProfile caregiverProfile;

    @ManyToOne
    @JoinColumn(name = "qualification_type_id", nullable = false)
    QualificationType qualificationType;

    @Column(name = "certificate_number", length = 255)
    String certificateNumber;

    @Column(name = "issuing_organization", length = 255)
    String issuingOrganization;

    @Column(name = "issue_date")
    LocalDate issueDate;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "certificate_url", columnDefinition = "TEXT")
    String certificateUrl;

    @Column(name = "is_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    @Builder.Default
    Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(255) DEFAULT 'PENDING'")
    @Builder.Default
    EnumVerificationStatusType status = EnumVerificationStatusType.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;

    @Column(name = "accepted_at")
    LocalDateTime acceptedAt;

    @Column(name = "declined_at")
    LocalDateTime declinedAt;

    @Column(name = "reviewed_by")
    UUID reviewedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;
}
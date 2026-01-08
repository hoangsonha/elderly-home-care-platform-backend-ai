package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumGenderType;
import com.capstone_project.elderly_platform.enums.EnumVerificationStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "caregiver_profiles")
public class CaregiverProfile extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "caregiver_profile_id")
    UUID caregiverProfileId;

    String fullName;

    String phoneNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "location", columnDefinition = "jsonb")
    String location;

    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;

    Boolean isVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(255) DEFAULT 'PENDING'")
    @Builder.Default
    EnumVerificationStatusType status = EnumVerificationStatusType.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;

    @Column(name = "is_needed_review_certificate", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    @Builder.Default
    Boolean isNeededReviewCertificate = false;

    @Column(name = "accepted_at")
    LocalDateTime acceptedAt;

    @Column(name = "declined_at")
    LocalDateTime declinedAt;

    @Column(name = "reviewed_by")
    UUID reviewedBy;

    LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    EnumGenderType gender;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_data", columnDefinition = "jsonb")
    String profileData;

    @OneToOne
    @JoinColumn(name = "account_id")
    Account account;

    @OneToMany(mappedBy = "caregiverProfile")
    List<CareService> careServices;

    @OneToMany(mappedBy = "caregiverProfile")
    List<WorkSchedule> workSchedules;

    @OneToMany(mappedBy = "caregiverProfile")
    List<Payout> payouts;

    @OneToMany(mappedBy = "caregiverProfile")
    List<PayoutBatch> payoutBatches;

    @OneToMany(mappedBy = "caregiverProfile")
    List<Qualification> qualifications;

}

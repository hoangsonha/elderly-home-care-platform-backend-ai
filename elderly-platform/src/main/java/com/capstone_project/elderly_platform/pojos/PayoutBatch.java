package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumPayoutBatchStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "payout_batches")
public class PayoutBatch extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "payout_batch_id")
    UUID payoutBatchId;

    @Column(name = "batch_code", unique = true, nullable = false, length = 50)
    String batchCode;

    Integer payoutMonth;

    Integer payoutYear;

    Integer totalBookings;

    Double totalServiceAmount;

    Double totalSystemFeeAmount;

    @Enumerated(EnumType.STRING)
    EnumPayoutBatchStatusType status;

    Double totalCaregiverEarnings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transfer_reference_data", columnDefinition = "jsonb")
    String transferReferenceData;

    LocalDateTime transferredAt;

    LocalDate scheduledAt;

    LocalDate startDate;

    LocalDate endDate;

    @Column(name = "bank_account_number", length = 100)
    String bankAccountNumber;

    @Column(name = "bank_name", length = 50)
    String bankName;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @ManyToOne
    @JoinColumn(name = "caregiver_id")
    CaregiverProfile caregiverProfile;

    @OneToMany(mappedBy = "payoutBatch", cascade = CascadeType.ALL)
    List<Payout> payouts;
}

package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumPayoutStatusType;
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
@Table(name = "payouts")
public class Payout extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "payout_id")
    UUID payoutId;

    @Column(name = "payout_code", unique = true, nullable = false, length = 50)
    String payoutCode;

    Double caregiverEarnings;

    LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    EnumPayoutStatusType status;

    LocalDateTime includedAt;

    LocalDateTime paidAt;

    Double systemRevenue;

    Double systemFeePercentage;

    Double totalAmount;

    @OneToOne
    @JoinColumn(name = "care_service_id")
    CareService careService;

    @ManyToOne
    @JoinColumn(name = "caregiver_id")
    CaregiverProfile caregiverProfile;

    @ManyToOne
    @JoinColumn(name = "payout_batch_id")
    PayoutBatch payoutBatch;
}

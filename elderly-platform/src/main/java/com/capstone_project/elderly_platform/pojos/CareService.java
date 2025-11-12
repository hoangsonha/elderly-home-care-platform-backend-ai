package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "care_services")
public class CareService extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "care_service_id")
    UUID careServiceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "care_service_snapshot", columnDefinition = "jsonb")
    String careServiceSnapshot;     // Snapshot of the care service details at the time of booking including ServicePackage, Caregiver, Elderly, Seeker details

    @Column(name = "booking_code", unique = true, nullable = false, length = 50)
    String bookingCode;

    LocalDate workDate;

    LocalTime startTime;

    LocalTime endTime;

    LocalDateTime caregiverResponseDeadline;

    LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    EnumCareServiceStatusType status;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    Double systemFeePercentage;

    Double totalPrice;

    Double caregiverEarnings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "location", columnDefinition = "jsonb")
    String location;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_version", columnDefinition = "jsonb")
    String configVersion;  // JSON map of config keys and their values at booking time (all active configs)

    @ManyToOne
    @JoinColumn(name = "seeker_id")
    CareSeekerProfile careSeekerProfile;

    @ManyToOne
    @JoinColumn(name = "caregiver_id")
    CaregiverProfile caregiverProfile;

    @ManyToOne
    @JoinColumn(name = "elderly_id")
    ElderlyProfile elderlyProfile;

    @ManyToOne
    @JoinColumn(name = "service_package_id")
    ServicePackage servicePackage;

    @OneToOne(mappedBy = "careService")
    Payment payment;

    @OneToOne(mappedBy = "careService")
    PlatformRevenue platformRevenue;

    @OneToMany(mappedBy = "careService")
    List<CareServiceStatusLog> careServiceStatusLogs;

    @OneToOne(mappedBy = "careService")
    WorkSchedule workSchedule;

    @OneToOne(mappedBy = "careService")
    Payout payout;

}

package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumPaymentStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "payment_id")
    UUID paymentId;

    @Column(name = "payment_code", unique = true, nullable = false, length = 50)
    String paymentCode;

    @Column(name = "payment_method", length = 50)
    String paymentMethod;

    Double amount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response_data", columnDefinition = "jsonb")
    String gatewayResponseData;

    LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    EnumPaymentStatusType status;

    @ManyToOne
    @JoinColumn(name = "seeker_id")
    CareSeekerProfile seekerProfile;

    @OneToOne
    @JoinColumn(name = "care_service_id")
    CareService careService;

}

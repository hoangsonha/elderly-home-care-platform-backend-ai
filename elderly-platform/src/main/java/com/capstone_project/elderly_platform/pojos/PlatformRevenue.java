package com.capstone_project.elderly_platform.pojos;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "platform_revenues")
public class PlatformRevenue {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "flatform_revenue_id")
    UUID platformRevenueId;

    Double totalAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "other_cost", columnDefinition = "jsonb")
    String otherCost;

    Double netRevenue;

    @OneToOne
    @JoinColumn(name = "care_service_id")
    CareService careService;

}

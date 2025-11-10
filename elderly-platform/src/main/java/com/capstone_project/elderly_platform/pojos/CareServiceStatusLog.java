package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumActorType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "care_service_status_logs")
public class CareServiceStatusLog extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "care_service_status_log_id")
    UUID careServiceStatusLogId;

    @Enumerated(EnumType.STRING)
    EnumActorType changedBy;

    @Column(name = "old_status", length = 50)
    String oldStatus;

    @Column(name = "new_status", length = 50)
    String newStatus;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @ManyToOne
    @JoinColumn(name = "care_service_id")
    CareService careService;
}

package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
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
@Table(name = "service_tasks")
public class ServiceTask extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "service_task_id")
    UUID serviceTaskId;

    String taskName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    EnumActivationStatusType status;

    @ManyToOne
    @JoinColumn(name = "service_package_id")
    ServicePackage servicePackage;

}

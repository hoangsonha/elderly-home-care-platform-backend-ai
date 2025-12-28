package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumServicePackageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "service_packages")
public class ServicePackage extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "service_package_id")
    UUID servicePackageId;

    String packageName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    Integer durationHours;

    @Enumerated(EnumType.STRING)
    EnumServicePackageType packageType;

    Double price;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qualification", columnDefinition = "jsonb")
    String qualification;

    @Enumerated(EnumType.STRING)
    EnumActivationStatusType status;

    @OneToMany(mappedBy = "servicePackage")
    List<ServiceTask> serviceTasks;

    @OneToMany(mappedBy = "servicePackage")
    List<CareService> careServices;

}

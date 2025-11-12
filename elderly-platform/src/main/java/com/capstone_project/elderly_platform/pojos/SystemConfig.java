package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
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
@Table(name = "system_configs")
public class SystemConfig extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "system_config_id")
    UUID systemConfigId;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_key", nullable = false, length = 100)
    EnumSystemConfigKey configKey;

    @Column(name = "config_value", nullable = false, length = 255)
    String configValue;

    @Column(name = "version", nullable = false)
    @Builder.Default
    Integer version = 1;

    @Column(name = "active", nullable = false)
    @Builder.Default
    Boolean active = true;

    @Column(name = "changed_by_account_id")
    UUID changedByAccountId;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    String changeReason;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;
}

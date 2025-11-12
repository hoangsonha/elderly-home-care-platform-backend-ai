package com.capstone_project.elderly_platform.dtos.response;

import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemConfigResponse {
    UUID systemConfigId;
    EnumSystemConfigKey configKey;
    String configValue;
    Integer version;
    Boolean active;
    String description;
    UUID changedByAccountId;
    String changeReason;
}


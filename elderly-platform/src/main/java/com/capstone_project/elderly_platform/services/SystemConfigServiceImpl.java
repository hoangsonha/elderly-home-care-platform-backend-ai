package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.pojos.SystemConfig;
import com.capstone_project.elderly_platform.repositories.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public String getConfigValue(EnumSystemConfigKey configKey, String defaultValue) {
        return systemConfigRepository.findByConfigKeyAndActiveIsTrueAndDeletedIsFalse(configKey)
                .map(SystemConfig::getConfigValue)
                .orElseGet(() -> {
                    log.warn("Config key {} not found, using default value: {}", configKey, defaultValue);
                    return defaultValue;
                });
    }

    @Override
    public int getConfigValueAsInt(EnumSystemConfigKey configKey, int defaultValue) {
        String value = getConfigValue(configKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse config value for key {}: {}, using default: {}", configKey, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public double getConfigValueAsDouble(EnumSystemConfigKey configKey, double defaultValue) {
        String value = getConfigValue(configKey, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse config value for key {}: {}, using default: {}", configKey, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public SystemConfig getConfigWithVersion(EnumSystemConfigKey configKey) {
        return systemConfigRepository.findByConfigKeyAndActiveIsTrueAndDeletedIsFalse(configKey)
                .orElse(null);
    }

    @Override
    public Map<EnumSystemConfigKey, Integer> getAllConfigVersions() {
        // Lấy tất cả versions của configs đang active
        // Dùng để lưu vào CareService khi booking để track version đã dùng
        Map<EnumSystemConfigKey, Integer> versions = new HashMap<>();
        List<SystemConfig> activeConfigs = systemConfigRepository.findByActiveIsTrueAndDeletedIsFalse();
        for (SystemConfig config : activeConfigs) {
            versions.put(config.getConfigKey(), config.getVersion());
        }
        return versions;
    }

    @Override
    public Map<EnumSystemConfigKey, String> getAllActiveConfigs() {
        // Lấy tất cả config values đang active
        Map<EnumSystemConfigKey, String> configs = new HashMap<>();
        List<SystemConfig> activeConfigs = systemConfigRepository.findByActiveIsTrueAndDeletedIsFalse();
        for (SystemConfig config : activeConfigs) {
            configs.put(config.getConfigKey(), config.getConfigValue());
        }
        return configs;
    }

    @Override
    @Transactional
    public void updateConfigValue(EnumSystemConfigKey configKey, String value, UUID changedByAccountId,
            String changeReason) {
        // Get current active config
        SystemConfig currentActiveConfig = systemConfigRepository
                .findByConfigKeyAndActiveIsTrueAndDeletedIsFalse(configKey)
                .orElse(null);

        // Check if value actually changed
        if (currentActiveConfig != null && currentActiveConfig.getConfigValue().equals(value)) {
            log.info("Config key {} value unchanged, skipping update", configKey);
            return;
        }

        // Calculate next version
        int nextVersion = 1;
        if (currentActiveConfig != null) {
            // Get all configs with same key to find max version
            List<SystemConfig> allConfigs = systemConfigRepository
                    .findByConfigKeyAndDeletedIsFalseOrderByVersionDesc(configKey);
            if (!allConfigs.isEmpty()) {
                nextVersion = allConfigs.get(0).getVersion() + 1;
            }
            // Deactivate all existing configs with same key
            allConfigs.forEach(config -> config.setActive(false));
            systemConfigRepository.saveAll(allConfigs);
        }

        // Create new active config
        SystemConfig newConfig = SystemConfig.builder()
                .configKey(configKey)
                .configValue(value)
                .version(nextVersion)
                .active(true)
                .changedByAccountId(changedByAccountId)
                .description(currentActiveConfig != null ? currentActiveConfig.getDescription() : null)
                .build();
        systemConfigRepository.save(newConfig);

        log.info("Updated config key {} to version {}: {} -> {}",
                configKey, nextVersion,
                currentActiveConfig != null ? currentActiveConfig.getConfigValue() : "null",
                value);
    }
}

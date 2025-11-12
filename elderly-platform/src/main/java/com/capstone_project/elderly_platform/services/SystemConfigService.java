package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.pojos.SystemConfig;

import java.util.Map;
import java.util.UUID;

public interface SystemConfigService {
    String getConfigValue(EnumSystemConfigKey configKey, String defaultValue);
    int getConfigValueAsInt(EnumSystemConfigKey configKey, int defaultValue);
    double getConfigValueAsDouble(EnumSystemConfigKey configKey, double defaultValue);
    SystemConfig getConfigWithVersion(EnumSystemConfigKey configKey);
    
    /**
     * Lấy tất cả versions của các config keys đang active
     * Dùng để lưu vào CareService khi booking để track version đã dùng
     */
    Map<EnumSystemConfigKey, Integer> getAllConfigVersions();
    
    /**
     * Lấy tất cả config values đang active
     * Trả về Map với key là EnumSystemConfigKey và value là config value (String)
     */
    Map<EnumSystemConfigKey, String> getAllActiveConfigs();
    
    void updateConfigValue(EnumSystemConfigKey configKey, String value, UUID changedByAccountId, String changeReason);
}


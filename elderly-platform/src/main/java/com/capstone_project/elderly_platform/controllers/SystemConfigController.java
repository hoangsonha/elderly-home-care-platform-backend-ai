package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.configurations.CustomAccountDetail;
import com.capstone_project.elderly_platform.dtos.request.UpdateSystemConfigRequest;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.SystemConfigResponse;
import com.capstone_project.elderly_platform.enums.EnumRoleType;
import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.pojos.SystemConfig;
import com.capstone_project.elderly_platform.services.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * Get all active system configs
     */
    @Operation(summary = "Get all active system configs", description = "Retrieves all active system configuration values")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ObjectResponse> getAllActiveConfigs() {
        try {
            Map<EnumSystemConfigKey, String> configs = systemConfigService.getAllActiveConfigs();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Get all active configs successfully", configs));
        } catch (Exception e) {
            log.error("Error getting all active configs", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Get all active configs failed", null));
        }
    }

    /**
     * Get system config by key
     */
    @Operation(summary = "Get system config by key", description = "Retrieves system configuration by key")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{configKey}")
    public ResponseEntity<ObjectResponse> getConfigByKey(@PathVariable String configKey) {
        try {
            EnumSystemConfigKey key = EnumSystemConfigKey.valueOf(configKey);
            SystemConfig config = systemConfigService.getConfigWithVersion(key);
            if (config == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ObjectResponse("Failed", "Config not found", null));
            }
            SystemConfigResponse response = mapToResponse(config);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Get config successfully", response));
        } catch (IllegalArgumentException e) {
            log.error("Invalid config key: {}", configKey, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Invalid config key", null));
        } catch (Exception e) {
            log.error("Error getting config by key", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Get config failed", null));
        }
    }

    /**
     * Update system config value
     */
    @Operation(summary = "Update system config", description = "Updates system configuration value (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{configKey}")
    public ResponseEntity<ObjectResponse> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody UpdateSystemConfigRequest request) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomAccountDetail)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ObjectResponse("Failed", "Unauthorized", null));
            }

            CustomAccountDetail accountDetail = (CustomAccountDetail) authentication.getPrincipal();

            // Check if user is admin
            boolean isAdmin = accountDetail.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(EnumRoleType.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ObjectResponse("Failed", "Only admin can update config", null));
            }

            EnumSystemConfigKey key = EnumSystemConfigKey.valueOf(configKey);
            systemConfigService.updateConfigValue(
                    key,
                    request.getValue(),
                    accountDetail.getId(),
                    request.getChangeReason());

            // Get updated config
            SystemConfig updatedConfig = systemConfigService.getConfigWithVersion(key);
            SystemConfigResponse response = mapToResponse(updatedConfig);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Update config successfully", response));
        } catch (IllegalArgumentException e) {
            log.error("Invalid config key: {}", configKey, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Invalid config key", null));
        } catch (BadRequestException e) {
            log.error("Error updating config", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating config", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Update config failed", null));
        }
    }

    /**
     * Get all config keys (enum values)
     */
    @Operation(summary = "Get all config keys", description = "Retrieves all available system configuration keys")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/keys")
    public ResponseEntity<ObjectResponse> getAllConfigKeys() {
        try {
            List<String> keys = List.of(EnumSystemConfigKey.values()).stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Get all config keys successfully", keys));
        } catch (Exception e) {
            log.error("Error getting all config keys", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Get all config keys failed", null));
        }
    }

    private SystemConfigResponse mapToResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .systemConfigId(config.getSystemConfigId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .version(config.getVersion())
                .active(config.getActive())
                .description(config.getDescription())
                .changedByAccountId(config.getChangedByAccountId())
                .changeReason(config.getChangeReason())
                .build();
    }
}

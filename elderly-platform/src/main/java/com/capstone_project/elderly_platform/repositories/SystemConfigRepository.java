package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.pojos.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {
    Optional<SystemConfig> findByConfigKeyAndActiveIsTrueAndDeletedIsFalse(EnumSystemConfigKey configKey);

    java.util.List<SystemConfig> findByConfigKeyAndDeletedIsFalseOrderByVersionDesc(EnumSystemConfigKey configKey);

    java.util.List<SystemConfig> findByActiveIsTrueAndDeletedIsFalse();
}

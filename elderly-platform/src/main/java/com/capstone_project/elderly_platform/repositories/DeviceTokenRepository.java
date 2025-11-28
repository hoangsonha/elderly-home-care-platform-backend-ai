package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    
    List<DeviceToken> findByAccount_AccountIdAndIsActiveTrue(UUID accountId);
    
    Optional<DeviceToken> findByAccount_AccountIdAndFcmToken(UUID accountId, String fcmToken);
    
    Optional<DeviceToken> findByAccount_AccountIdAndDeviceType(UUID accountId, String deviceType);
    
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.account.accountId = :accountId AND dt.fcmToken = :fcmToken")
    void deactivateToken(UUID accountId, String fcmToken);
    
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.account.accountId = :accountId")
    void deactivateAllTokensByAccount(UUID accountId);
}


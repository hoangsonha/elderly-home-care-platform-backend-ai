package com.capstone_project.elderly_platform.pojos;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "device_tokens")
public class DeviceToken extends BaseEntity {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "device_token_id")
    UUID deviceTokenId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;
    
    @Column(name = "fcm_token", nullable = false, columnDefinition = "TEXT")
    String fcmToken;
    
    @Column(name = "device_type", nullable = false, length = 20)
    String deviceType; // ANDROID, IOS, WEB
    
    @Column(name = "device_name", length = 255)
    String deviceName;
    
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;
    
    @Column(name = "last_used_at")
    LocalDateTime lastUsedAt;
}


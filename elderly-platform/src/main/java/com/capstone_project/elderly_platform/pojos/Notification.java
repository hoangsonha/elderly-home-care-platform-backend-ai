package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumNotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notifications")
public class Notification extends BaseEntity {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "notification_id")
    UUID notificationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    Account recipient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    Account sender;
    
    @Column(name = "title", nullable = false)
    String title;
    
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    String body;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 100)
    EnumNotificationType notificationType;
    
    @Column(name = "related_entity_type", length = 100)
    String relatedEntityType;
    
    @Column(name = "related_entity_id")
    UUID relatedEntityId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    String data;
    
    @Column(name = "image_url", columnDefinition = "TEXT")
    String imageUrl;
    
    @Column(name = "is_read", nullable = false)
    Boolean isRead = false;
    
    @Column(name = "read_at")
    LocalDateTime readAt;
    
    @Column(name = "sent_at")
    LocalDateTime sentAt;
}


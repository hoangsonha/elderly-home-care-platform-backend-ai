package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    Page<Notification> findByRecipient_AccountIdAndDeletedFalseOrderByCreatedAtDesc(
        UUID recipientId, 
        Pageable pageable
    );
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.accountId = :recipientId AND n.isRead = false AND n.deleted = false")
    Long countUnreadByRecipient(UUID recipientId);
    
    @Query("SELECT n FROM Notification n WHERE n.recipient.accountId = :recipientId AND n.isRead = false AND n.deleted = false ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByRecipient(UUID recipientId, Pageable pageable);
}


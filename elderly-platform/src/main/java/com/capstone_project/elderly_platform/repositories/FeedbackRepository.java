package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.Feedback;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    // Find feedback by ID (not deleted)
    Optional<Feedback> findByFeedbackIdAndDeletedIsFalse(UUID feedbackId);

    // Find feedbacks by account
    List<Feedback> findByAccountAndDeletedIsFalse(Account account, Sort sort);

    // Find feedbacks by target (care service, dispute, etc.)
    List<Feedback> findByTargetIdAndTargetTypeAndDeletedIsFalse(
            UUID targetId, EnumFeedbackTargetType targetType, Sort sort);

    // Find feedbacks by target type
    List<Feedback> findByTargetTypeAndDeletedIsFalse(EnumFeedbackTargetType targetType, Sort sort);

    // Check if user already submitted feedback for a specific target
    @Query("SELECT COUNT(f) > 0 FROM Feedback f WHERE f.account.accountId = :accountId " +
            "AND f.targetId = :targetId AND f.targetType = :targetType AND f.deleted = false")
    boolean existsByAccountIdAndTargetIdAndTargetType(
            @Param("accountId") UUID accountId,
            @Param("targetId") UUID targetId,
            @Param("targetType") EnumFeedbackTargetType targetType);

    // Find feedback by account and target
    Optional<Feedback> findByAccount_AccountIdAndTargetIdAndTargetTypeAndDeletedIsFalse(
            UUID accountId, UUID targetId, EnumFeedbackTargetType targetType);

    // Get count by target type
    @Query("SELECT COUNT(f) FROM Feedback f " +
            "WHERE f.targetType = :targetType AND f.deleted = false")
    Long countByTargetType(@Param("targetType") EnumFeedbackTargetType targetType);

    // Get average rating by target type
    @Query("SELECT COALESCE(AVG(f.rating), 0) FROM Feedback f " +
            "WHERE f.targetType = :targetType AND f.deleted = false")
    Double getAverageRatingByTargetType(@Param("targetType") EnumFeedbackTargetType targetType);
}

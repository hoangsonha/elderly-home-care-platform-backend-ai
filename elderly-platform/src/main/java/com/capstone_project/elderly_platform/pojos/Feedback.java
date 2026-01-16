package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Table(name = "feedbacks")
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "feedback_id")
    UUID feedbackId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @Column(name = "target_id", nullable = false)
    UUID targetId; // ID of the target entity (care_service_id, dispute_id, etc.)

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    EnumFeedbackTargetType targetType; // SERVICE, SYSTEM, DISPUTE

    @Column(name = "rating", nullable = false)
    Integer rating; // General rating from 1 to 5

    // Detailed ratings for service feedback (stored as JSONB)
    // Format: {"professionalism": 5, "attitude": 4, "punctuality": 5, "quality": 4}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detailed_ratings", columnDefinition = "jsonb")
    String detailedRatings; // JSON string for detailed ratings

    @Column(name = "comment", columnDefinition = "TEXT")
    String comment; // Feedback comment/text

    @Column(name = "submission_time", nullable = false)
    LocalDateTime submissionTime;
}

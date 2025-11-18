package com.capstone_project.elderly_platform.pojos;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder // <- bắt buộc có để dùng builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "ratings")
public class Rating extends BaseEntity {

    @Id
    @GeneratedValue
    UUID ratingId;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;

    @ManyToOne
    @JoinColumn(name = "service_task_id")
    ServiceTask serviceTask;

    Integer score;

    @Column(columnDefinition = "TEXT")
    String comment;

    Boolean systemFeedback = false;

    Boolean complaintFeedback = false;
}

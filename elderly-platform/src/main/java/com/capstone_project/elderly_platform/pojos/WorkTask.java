package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType;
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
@Table(name = "work_tasks")
public class WorkTask extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "work_task_id")
    UUID workTaskId;

    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    EnumWorkTaskStatusType status;

    LocalDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "work_schedule_id")
    WorkSchedule workSchedule;

}

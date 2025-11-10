package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumWorkScheduleStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "work_schedules")
public class WorkSchedule extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "work_schedule_id")
    UUID workScheduleId;

    @Enumerated(EnumType.STRING)
    EnumWorkScheduleStatusType status;

    LocalDate workDate;

    LocalTime startTime;

    LocalTime endTime;

    LocalDateTime completedAt;

    Integer totalTasks;

    Integer completedTasks;

    @OneToOne
    @JoinColumn(name = "care_service_id")
    CareService careService;

    @ManyToOne
    @JoinColumn(name = "caregiver_id")
    CaregiverProfile caregiverProfile;

    @OneToMany(mappedBy = "workSchedule")
    List<WorkTask> workTasks;

}

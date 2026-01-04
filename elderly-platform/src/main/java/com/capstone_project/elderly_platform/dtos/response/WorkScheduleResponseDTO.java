package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for WorkSchedule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleResponseDTO {
    String workScheduleId;
    String status;
    String workDate;
    String startTime;
    String endTime;
    String completedAt;
    Integer totalTasks;
    Integer completedTasks;
    String checkInImageUrl;
    String checkOutImageUrl;
    List<WorkTaskResponseDTO> workTasks;
}



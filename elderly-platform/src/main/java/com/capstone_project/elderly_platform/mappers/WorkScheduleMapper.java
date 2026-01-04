package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.WorkScheduleResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.WorkTaskResponseDTO;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import com.capstone_project.elderly_platform.pojos.WorkTask;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkScheduleMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public WorkScheduleResponseDTO toDTO(WorkSchedule workSchedule) {
        if (workSchedule == null) {
            return null;
        }

        // Map work tasks
        List<WorkTaskResponseDTO> workTaskDTOs = null;
        if (workSchedule.getWorkTasks() != null) {
            workTaskDTOs = workSchedule.getWorkTasks().stream()
                    .filter(task -> !task.isDeleted())
                    .map(this::toTaskDTO)
                    .collect(Collectors.toList());
        }

        return WorkScheduleResponseDTO.builder()
                .workScheduleId(workSchedule.getWorkScheduleId() != null
                        ? workSchedule.getWorkScheduleId().toString()
                        : null)
                .status(workSchedule.getStatus() != null
                        ? workSchedule.getStatus().name()
                        : null)
                .workDate(workSchedule.getWorkDate() != null
                        ? workSchedule.getWorkDate().format(DATE_FORMATTER)
                        : null)
                .startTime(workSchedule.getStartTime() != null
                        ? workSchedule.getStartTime().format(TIME_FORMATTER)
                        : null)
                .endTime(workSchedule.getEndTime() != null
                        ? workSchedule.getEndTime().format(TIME_FORMATTER)
                        : null)
                .completedAt(workSchedule.getCompletedAt() != null
                        ? workSchedule.getCompletedAt().format(DATETIME_FORMATTER)
                        : null)
                .totalTasks(workSchedule.getTotalTasks())
                .completedTasks(workSchedule.getCompletedTasks())
                .checkInImageUrl(workSchedule.getCheckInImageUrl())
                .checkOutImageUrl(workSchedule.getCheckOutImageUrl())
                .workTasks(workTaskDTOs)
                .build();
    }

    private WorkTaskResponseDTO toTaskDTO(WorkTask workTask) {
        if (workTask == null) {
            return null;
        }

        return WorkTaskResponseDTO.builder()
                .workTaskId(workTask.getWorkTaskId() != null
                        ? workTask.getWorkTaskId().toString()
                        : null)
                .name(workTask.getName())
                .description(workTask.getDescription())
                .status(workTask.getStatus() != null
                        ? workTask.getStatus().name()
                        : null)
                .completedAt(workTask.getCompletedAt() != null
                        ? workTask.getCompletedAt().format(DATETIME_FORMATTER)
                        : null)
                .build();
    }
}



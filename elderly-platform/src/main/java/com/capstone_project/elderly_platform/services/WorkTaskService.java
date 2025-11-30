package com.capstone_project.elderly_platform.services;
import com.capstone_project.elderly_platform.dtos.request.CreateWorkTaskRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateWorkTaskStatusRequest;
import com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType;
import com.capstone_project.elderly_platform.pojos.WorkTask;

import java.util.List;
import java.util.UUID;
public interface WorkTaskService {
    WorkTask createTask(CreateWorkTaskRequest request);
    List<WorkTask> getTasksBySchedule(UUID scheduleId);
    WorkTask updateTaskStatus(UUID taskId, EnumWorkTaskStatusType status);
    WorkTask updateStatus(UUID taskId, UpdateWorkTaskStatusRequest request);
}

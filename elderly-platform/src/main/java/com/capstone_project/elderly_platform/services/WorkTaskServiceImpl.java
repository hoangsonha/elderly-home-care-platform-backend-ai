package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkTaskRequest;
import com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import com.capstone_project.elderly_platform.pojos.WorkTask;
import com.capstone_project.elderly_platform.repositories.WorkScheduleRepository;
import com.capstone_project.elderly_platform.repositories.WorkTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkTaskServiceImpl implements WorkTaskService {

    private final WorkTaskRepository workTaskRepo;
    private final WorkScheduleRepository scheduleRepo;

    @Override
    public WorkTask createTask(CreateWorkTaskRequest request) {

        // 1. Lấy WorkSchedule từ DB
        WorkSchedule schedule = scheduleRepo.findById(request.getWorkScheduleId())
                .orElseThrow(() -> new RuntimeException("WorkSchedule not found"));

        // 2. Tạo WorkTask mới
        WorkTask task = new WorkTask();
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setStatus(EnumWorkTaskStatusType.PENDING);
        task.setWorkSchedule(schedule);

        // 3. Cập nhật totalTasks của WorkSchedule
        if (schedule.getTotalTasks() == null) {
            schedule.setTotalTasks(0);
        }
        schedule.setTotalTasks(schedule.getTotalTasks() + 1);
        scheduleRepo.save(schedule);

        // 4. Lưu task vào DB và trả về
        return workTaskRepo.save(task);
    }


    @Override
    public List<WorkTask> getTasksBySchedule(UUID scheduleId) {
        return workTaskRepo.findByWorkSchedule_WorkScheduleId(scheduleId);
    }

    @Override
    public WorkTask updateTaskStatus(UUID taskId, EnumWorkTaskStatusType status) {
        WorkTask task = workTaskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setStatus(status);

        if (status == EnumWorkTaskStatusType.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }

        return workTaskRepo.save(task);
    }
}

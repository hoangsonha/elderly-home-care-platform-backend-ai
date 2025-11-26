package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkScheduleRequest;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;

import java.util.List;
import java.util.UUID;

public interface WorkScheduleService {
    WorkSchedule createSchedule(CreateWorkScheduleRequest request);
    List<WorkSchedule> getAll();
    WorkSchedule getById(UUID id);
    void delete(UUID id);
}
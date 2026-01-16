package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleByDateRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;

import java.time.LocalDate;
import java.util.Map;

public interface CaregiverScheduleService {

    CaregiverProfileResponseDTO updateFreeSchedule(UpdateFreeScheduleRequest request);

    CaregiverProfileResponseDTO updateFreeScheduleByDate(UpdateFreeScheduleByDateRequest request);

    Map<String, Object> getFreeSchedule();

    void initializeFreeScheduleIfNotExists();

    Map<String, Object> getFreeScheduleForDate(LocalDate date, java.util.UUID caregiverId);
}

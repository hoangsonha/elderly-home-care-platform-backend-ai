package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;

import java.util.Map;

public interface CaregiverScheduleService {
    /**
     * Update free schedule for current caregiver
     * @param request Update free schedule request
     * @return Updated caregiver profile
     */
    CaregiverProfileResponseDTO updateFreeSchedule(UpdateFreeScheduleRequest request);
    
    /**
     * Get free schedule for current caregiver
     * @return Free schedule map
     */
    Map<String, Object> getFreeSchedule();
    
    /**
     * Initialize free schedule if not exists for a caregiver profile
     * This is called automatically when needed
     */
    void initializeFreeScheduleIfNotExists();
}




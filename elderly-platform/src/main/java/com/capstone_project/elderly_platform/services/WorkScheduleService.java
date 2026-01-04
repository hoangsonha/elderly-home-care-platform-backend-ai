package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.EndWorkRequest;
import com.capstone_project.elderly_platform.dtos.request.StartWorkRequest;
import com.capstone_project.elderly_platform.dtos.request.ToggleWorkTaskRequest;
import com.capstone_project.elderly_platform.dtos.response.EndWorkResponse;
import com.capstone_project.elderly_platform.dtos.response.StartWorkResponse;
import com.capstone_project.elderly_platform.dtos.response.ToggleWorkTaskResponse;
import org.springframework.web.multipart.MultipartFile;

public interface WorkScheduleService {
    /**
     * Start work (Check In)
     * - Upload CI image
     * - Update care service status to IN_PROGRESS
     * - Save CI image URL to work schedule
     * - Update all tasks from PENDING to IN_PROGRESS
     */
    StartWorkResponse startWork(StartWorkRequest request, MultipartFile checkInImage);
    
    /**
     * End work (Check Out)
     * - Upload CO image
     * - Update care service status to WAITING_PAYMENT
     * - Save CO image URL to work schedule
     * - Update all IN_PROGRESS tasks to NOT_COMPLETED
     * - Create payment link and return QR code
     */
    EndWorkResponse endWork(EndWorkRequest request, MultipartFile checkOutImage);
    
    /**
     * Toggle work task status
     * - If IN_PROGRESS → DONE (set completedAt)
     * - If DONE → IN_PROGRESS (clear completedAt)
     */
    ToggleWorkTaskResponse toggleWorkTask(ToggleWorkTaskRequest request);
}


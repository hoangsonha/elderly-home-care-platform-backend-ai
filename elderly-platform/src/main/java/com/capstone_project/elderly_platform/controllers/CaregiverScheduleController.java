package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.CaregiverScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/api/v1/caregiver-schedule")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Caregiver Schedule", description = "Operations related to caregiver free schedule management")
public class CaregiverScheduleController {

    private final CaregiverScheduleService caregiverScheduleService;

    /**
     * Update free schedule for current caregiver
     *
     * @param request Update free schedule request
     * @return Updated caregiver profile
     */
    @Operation(summary = "Update free schedule", 
               description = "Update free schedule for current caregiver. " +
                           "If available_all_time = true, caregiver is available all the time. " +
                           "Otherwise, booked_slots contains list of booked time slots.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PutMapping("/free-schedule")
    public ResponseEntity<ObjectResponse> updateFreeSchedule(@Valid @RequestBody UpdateFreeScheduleRequest request) {
        try {
            CaregiverProfileResponseDTO profile = caregiverScheduleService.updateFreeSchedule(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Cập nhật lịch rảnh thành công", profile));
        } catch (ElementNotFoundException e) {
            log.error("Error updating free schedule", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error updating free schedule", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating free schedule", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi cập nhật lịch rảnh: " + e.getMessage(), null));
        }
    }

    /**
     * Get free schedule for current caregiver
     *
     * @return Free schedule data
     */
    @Operation(summary = "Get free schedule", 
               description = "Get free schedule for current caregiver. " +
                           "Returns available_all_time flag or booked_slots list.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @GetMapping("/free-schedule")
    public ResponseEntity<ObjectResponse> getFreeSchedule() {
        try {
            Map<String, Object> freeSchedule = caregiverScheduleService.getFreeSchedule();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Lấy lịch rảnh thành công", freeSchedule));
        } catch (ElementNotFoundException e) {
            log.error("Error getting free schedule", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting free schedule", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi lấy lịch rảnh: " + e.getMessage(), null));
        }
    }
}



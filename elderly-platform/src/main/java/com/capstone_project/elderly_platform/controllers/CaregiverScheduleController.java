package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleByDateRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileDetailResponseDTO;
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
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

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
    @Operation(summary = "Update free schedule", description = "Update free schedule for current caregiver. " +
            "If available_all_time = true, caregiver is available all the time. " +
            "Otherwise, booked_slots contains list of booked time slots.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PutMapping("/free-schedule")
    public ResponseEntity<ObjectResponse> updateFreeSchedule(@Valid @RequestBody UpdateFreeScheduleRequest request) {
        try {
            CaregiverProfileDetailResponseDTO profile = caregiverScheduleService.updateFreeSchedule(request);
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
     * Update free schedule for a specific date
     *
     * @param request Update free schedule by date request
     * @return Updated caregiver profile
     */
    @Operation(summary = "Update free schedule for a specific date", description = "Update free schedule for current caregiver for a specific date. " +
            "Booking slots (is_booking = true) cannot be modified and will be preserved. " +
            "Only manual slots (is_booking = false) can be updated.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PutMapping("/free-schedule/date")
    public ResponseEntity<ObjectResponse> updateFreeScheduleByDate(@Valid @RequestBody UpdateFreeScheduleByDateRequest request) {
        try {
            CaregiverProfileDetailResponseDTO profile = caregiverScheduleService.updateFreeScheduleByDate(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Cập nhật lịch rảnh cho ngày " + request.getDate() + " thành công", profile));
        } catch (ElementNotFoundException e) {
            log.error("Error updating free schedule by date", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error updating free schedule by date", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating free schedule by date", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi cập nhật lịch rảnh: " + e.getMessage(), null));
        }
    }

    /**
     * Get free schedule for current caregiver
     *
     * @return Free schedule data
     */
    @Operation(summary = "Get free schedule", description = "Get free schedule for current caregiver. " +
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

    /**
     * Get free schedule for a specific date
     *
     * @param date        The date to check availability (format: yyyy-MM-dd)
     * @param caregiverId Optional caregiver ID. If null and user is CAREGIVER,
     *                    returns own schedule.
     *                    If user is CARE_SEEKER, this parameter is required.
     * @return Free schedule information for the date
     */
    @Operation(summary = "Get free schedule for a specific date", description = "Get free schedule for a caregiver on a specific date. "
            +
            "CAREGIVER: If caregiverId is not provided, returns own schedule. " +
            "CARE_SEEKER: caregiverId is required. " +
            "Returns available_all_day flag and list of booked slots for that date.")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @GetMapping("/free-schedule/date")
    public ResponseEntity<ObjectResponse> getFreeScheduleForDate(
            @io.swagger.v3.oas.annotations.Parameter(description = "Date to check availability (format: yyyy-MM-dd)", required = true) @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @io.swagger.v3.oas.annotations.Parameter(description = "Caregiver ID (optional for CAREGIVER, required for CARE_SEEKER)", required = false) @RequestParam(value = "caregiverId", required = false) UUID caregiverId) {
        try {
            Map<String, Object> freeSchedule = caregiverScheduleService.getFreeScheduleForDate(date, caregiverId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Lấy lịch rảnh cho ngày " + date + " thành công",
                            freeSchedule));
        } catch (ElementNotFoundException e) {
            log.error("Error getting free schedule for date", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error getting free schedule for date", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting free schedule for date", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi lấy lịch rảnh: " + e.getMessage(), null));
        }
    }
}

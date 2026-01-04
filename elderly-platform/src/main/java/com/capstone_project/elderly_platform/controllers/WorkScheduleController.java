package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.EndWorkRequest;
import com.capstone_project.elderly_platform.dtos.request.StartWorkRequest;
import com.capstone_project.elderly_platform.dtos.request.ToggleWorkTaskRequest;
import com.capstone_project.elderly_platform.dtos.response.EndWorkResponse;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.StartWorkResponse;
import com.capstone_project.elderly_platform.dtos.response.ToggleWorkTaskResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.WorkScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/v1/work-schedules")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Work Schedule", description = "Operations related to work schedule management (CI-CO)")
public class WorkScheduleController {

    private final WorkScheduleService workScheduleService;

    /**
     * Start work (Check In)
     * - Upload CI image
     * - Update care service status to IN_PROGRESS
     */
    @Operation(summary = "Start work (Check In)", description = "Start work for a care service. Uploads CI image, updates status to IN_PROGRESS. "
            +
            "Care service must be in CAREGIVER_APPROVED status. Use multipart/form-data. " +
            "Note: In Swagger UI, for the 'request' part, you must manually set Content-Type to 'application/json'.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PostMapping(value = "/start-work", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> startWork(
            @Parameter(description = "JSON data containing care service ID. IMPORTANT: Set Content-Type to 'application/json' in Swagger UI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @RequestPart("request") @Valid StartWorkRequest request,
            @Parameter(description = "Check In image file (required)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart("checkInImage") MultipartFile checkInImage) {
        try {
            StartWorkResponse response = workScheduleService.startWork(request, checkInImage);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", response.getMessage(), response));
        } catch (ElementNotFoundException e) {
            log.error("Error starting work", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error starting work", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error starting work", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi bắt đầu làm việc: " + e.getMessage(), null));
        }
    }

    /**
     * End work (Check Out)
     * - Upload CO image
     * - Update care service status to WAITING_PAYMENT
     * - Create payment link and return QR code
     */
    @Operation(summary = "End work (Check Out)", description = "End work for a care service. Uploads CO image, updates status to WAITING_PAYMENT, "
            +
            "and creates payment link with QR code. Care service must be in IN_PROGRESS status. " +
            "Use multipart/form-data. Note: In Swagger UI, for the 'request' part, you must manually set Content-Type to 'application/json'.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PostMapping(value = "/end-work", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> endWork(
            @Parameter(description = "JSON data containing care service ID. IMPORTANT: Set Content-Type to 'application/json' in Swagger UI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @RequestPart("request") @Valid EndWorkRequest request,
            @Parameter(description = "Check Out image file (required)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart("checkOutImage") MultipartFile checkOutImage) {
        try {
            EndWorkResponse response = workScheduleService.endWork(request, checkOutImage);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", response.getMessage(), response));
        } catch (ElementNotFoundException e) {
            log.error("Error ending work", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error ending work", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error ending work", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi kết thúc làm việc: " + e.getMessage(), null));
        }
    }

    /**
     * Toggle work task status
     * - If IN_PROGRESS → DONE (set completedAt)
     * - If DONE → IN_PROGRESS (clear completedAt)
     */
    @Operation(summary = "Toggle work task status", description = "Toggle work task status between IN_PROGRESS and DONE. "
            +
            "If task is IN_PROGRESS, it will be marked as DONE. " +
            "If task is DONE, it will be changed back to IN_PROGRESS. " +
            "Work schedule must be in IN_PROGRESS status.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PostMapping("/toggle-task")
    public ResponseEntity<ObjectResponse> toggleWorkTask(@Valid @RequestBody ToggleWorkTaskRequest request) {
        try {
            ToggleWorkTaskResponse response = workScheduleService.toggleWorkTask(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", response.getMessage(), response));
        } catch (ElementNotFoundException e) {
            log.error("Error toggling work task", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error toggling work task", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error toggling work task", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi thay đổi trạng thái task: " + e.getMessage(), null));
        }
    }
}

package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkNoteRequest;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.WorkNoteResponseDTO;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.WorkNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/work-notes")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Work Note", description = "Operations related to work note management")
public class WorkNoteController {

    private final WorkNoteService workNoteService;

    /**
     * Create a new work note
     */
    @Operation(summary = "Create work note", description = "Create a new work note for a work schedule")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @PostMapping
    public ResponseEntity<ObjectResponse> createWorkNote(@Valid @RequestBody CreateWorkNoteRequest request) {
        try {
            WorkNoteResponseDTO response = workNoteService.createWorkNote(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ObjectResponse("Success", "Tạo ghi chú thành công", response));
        } catch (ElementNotFoundException e) {
            log.error("Error creating work note", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error creating work note", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating work note", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi tạo ghi chú: " + e.getMessage(), null));
        }
    }

    /**
     * Get all work notes by work schedule ID
     */
    @Operation(summary = "Get all work notes by work schedule", description = "Get all work notes for a specific work schedule")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @GetMapping("/work-schedule/{workScheduleId}")
    public ResponseEntity<ObjectResponse> getAllWorkNotesByWorkScheduleId(
            @Parameter(description = "Work schedule ID") @PathVariable UUID workScheduleId) {
        try {
            List<WorkNoteResponseDTO> response = workNoteService.getAllWorkNotesByWorkScheduleId(workScheduleId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Lấy danh sách ghi chú thành công", response));
        } catch (ElementNotFoundException e) {
            log.error("Error getting work notes", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting work notes", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Lỗi khi lấy danh sách ghi chú: " + e.getMessage(), null));
        }
    }
}

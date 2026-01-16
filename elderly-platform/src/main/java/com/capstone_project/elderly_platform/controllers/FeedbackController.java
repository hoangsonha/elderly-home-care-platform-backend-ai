package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CreateFeedbackRequest;
import com.capstone_project.elderly_platform.dtos.response.FeedbackResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.FeedbackService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/feedbacks")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feedback", description = "Operations related to feedback management")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Create feedback
     * 
     * @param request Feedback request data
     * @param images  Optional list of images
     * @return Created feedback
     */
    @Operation(summary = "Create feedback", description = "Create feedback for service, system, or dispute. "
            + "For SERVICE feedback: requires detailed ratings (professionalism, attitude, punctuality, quality). "
            + "For SYSTEM feedback: only requires general rating and comment. "
            + "For DISPUTE feedback: requires general rating, comment, and optional images.")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @PostMapping("")
    public ResponseEntity<ObjectResponse> createFeedback(
            @Valid @RequestPart("request") CreateFeedbackRequest request,
            @Parameter(description = "Optional list of images") @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            FeedbackResponseDTO feedback = feedbackService.createFeedback(request, images);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Tạo feedback thành công", feedback));
        } catch (BadRequestException e) {
            log.error("Error creating feedback", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (ElementNotFoundException e) {
            log.error("Error creating feedback", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating feedback", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Get feedback by ID
     * 
     * @param feedbackId Feedback ID
     * @return Feedback details
     */
    @Operation(summary = "Get feedback by ID", description = "Get feedback detail by feedback ID")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER') or hasRole('ADMIN')")
    @GetMapping("/{feedbackId}")
    public ResponseEntity<ObjectResponse> getFeedbackById(
            @Parameter(description = "Feedback ID", required = true) @PathVariable UUID feedbackId) {
        try {
            FeedbackResponseDTO feedback = feedbackService.getFeedbackById(feedbackId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Lấy feedback thành công", feedback));
        } catch (ElementNotFoundException e) {
            log.error("Error getting feedback by ID", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting feedback by ID", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Get feedbacks by target (care service, dispute, etc.)
     * 
     * @param targetId   Target ID (care_service_id, dispute_id, etc.)
     * @param targetType Target type (SERVICE, SYSTEM, DISPUTE)
     * @return List of feedbacks
     */
    @Operation(summary = "Get feedbacks by target", description = "Get all feedbacks for a specific target (care service, dispute, etc.)")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER') or hasRole('ADMIN')")
    @GetMapping("/target")
    public ResponseEntity<ObjectResponse> getFeedbacksByTarget(
            @Parameter(description = "Target ID", required = true) @RequestParam UUID targetId,
            @Parameter(description = "Target type (SERVICE, SYSTEM, DISPUTE)", required = true) @RequestParam EnumFeedbackTargetType targetType) {
        try {
            List<FeedbackResponseDTO> feedbacks = feedbackService.getFeedbacksByTarget(targetId, targetType);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success",
                            String.format("Lấy %d feedback thành công", feedbacks.size()), feedbacks));
        } catch (Exception e) {
            log.error("Error getting feedbacks by target", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Get my feedbacks
     * 
     * @return List of my feedbacks
     */
    @Operation(summary = "Get my feedbacks", description = "Get all feedbacks submitted by current user")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @GetMapping("/my-feedbacks")
    public ResponseEntity<ObjectResponse> getMyFeedbacks() {
        try {
            List<FeedbackResponseDTO> feedbacks = feedbackService.getMyFeedbacks();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success",
                            String.format("Lấy %d feedback thành công", feedbacks.size()), feedbacks));
        } catch (Exception e) {
            log.error("Error getting my feedbacks", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Get feedbacks by target type
     * 
     * @param targetType Target type (SERVICE, SYSTEM, DISPUTE)
     * @return List of feedbacks
     */
    @Operation(summary = "Get feedbacks by target type", description = "Get all feedbacks by target type (SERVICE, SYSTEM, DISPUTE)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/type/{targetType}")
    public ResponseEntity<ObjectResponse> getFeedbacksByTargetType(
            @Parameter(description = "Target type (SERVICE, SYSTEM, DISPUTE)", required = true) @PathVariable EnumFeedbackTargetType targetType) {
        try {
            List<FeedbackResponseDTO> feedbacks = feedbackService.getFeedbacksByTargetType(targetType);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success",
                            String.format("Lấy %d feedback thành công", feedbacks.size()), feedbacks));
        } catch (Exception e) {
            log.error("Error getting feedbacks by target type", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }
}

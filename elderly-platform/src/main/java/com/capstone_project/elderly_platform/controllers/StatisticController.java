package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.response.CareServiceStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CaregiverPersonalStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CaregiverStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerPersonalStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.UserStatisticsResponse;
import com.capstone_project.elderly_platform.services.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RequestMapping("/api/v1/statistics")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "Statistics operations. Only accessible by ADMIN role")
public class StatisticController {

    private final StatisticService statisticService;

    @Operation(summary = "Get user statistics", description = "Get total registered users and total unverified users within a date range. Only accessible by ADMIN role")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<ObjectResponse> getUserStatistics(
            @Parameter(description = "Start date (optional). Format: yyyy-MM-ddTHH:mm:ss") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (optional). Format: yyyy-MM-ddTHH:mm:ss") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            UserStatisticsResponse statistics = statisticService.getUserStatistics(startDate, endDate);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "User statistics retrieved successfully", statistics));
        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get user statistics: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get caregiver statistics", description = "Get total caregivers and pending verification caregivers. Only accessible by ADMIN role")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/caregivers")
    public ResponseEntity<ObjectResponse> getCaregiverStatistics() {
        try {
            CaregiverStatisticsResponse statistics = statisticService.getCaregiverStatistics();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Caregiver statistics retrieved successfully", statistics));
        } catch (Exception e) {
            log.error("Error getting caregiver statistics", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get caregiver statistics: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get care service statistics", description = "Get total care services and count by each status. Only accessible by ADMIN role")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/care-services")
    public ResponseEntity<ObjectResponse> getCareServiceStatistics() {
        try {
            CareServiceStatisticsResponse statistics = statisticService.getCareServiceStatistics();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Care service statistics retrieved successfully", statistics));
        } catch (Exception e) {
            log.error("Error getting care service statistics", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get care service statistics: " + e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Get caregiver personal statistics", description = "Get personal statistics for current caregiver including care services this month, earnings, rating, and task completion rate. Only accessible by CAREGIVER role")
    @PreAuthorize("hasRole('CAREGIVER')")
    @GetMapping("/caregiver/personal")
    public ResponseEntity<ObjectResponse> getCaregiverPersonalStatistics() {
        try {
            CaregiverPersonalStatisticsResponse statistics = statisticService.getCaregiverPersonalStatistics();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Caregiver personal statistics retrieved successfully",
                            statistics));
        } catch (Exception e) {
            log.error("Error getting caregiver personal statistics", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get caregiver personal statistics: " + e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Get care seeker personal statistics", description = "Get personal statistics for current care seeker including elderly profiles, care services this month, spending, completed bookings, and in progress services. Only accessible by CARE_SEEKER role")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @GetMapping("/care-seeker/personal")
    public ResponseEntity<ObjectResponse> getCareSeekerPersonalStatistics() {
        try {
            CareSeekerPersonalStatisticsResponse statistics = statisticService.getCareSeekerPersonalStatistics();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Care seeker personal statistics retrieved successfully",
                            statistics));
        } catch (Exception e) {
            log.error("Error getting care seeker personal statistics", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed",
                            "Failed to get care seeker personal statistics: " + e.getMessage(), null));
        }
    }
}

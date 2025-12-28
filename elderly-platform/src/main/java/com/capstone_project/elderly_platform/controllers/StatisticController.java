package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.response.CaregiverStatisticsResponse;
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
}





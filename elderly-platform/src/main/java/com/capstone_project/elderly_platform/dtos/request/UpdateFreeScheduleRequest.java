package com.capstone_project.elderly_platform.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request DTO for updating caregiver free schedule
 * 
 * Mobile Request Examples:
 * 
 * 1. Set available all time:
 * {
 *   "freeSchedule": {
 *     "availableAllTime": true
 *   }
 * }
 * 
 * 2. Set specific booked slots:
 * {
 *   "freeSchedule": {
 *     "availableAllTime": false,
 *     "bookedSlots": [
 *       {
 *         "date": "2025-12-01",
 *         "startTime": "09:00",
 *         "endTime": "12:00"
 *       },
 *       {
 *         "date": "2025-12-02",
 *         "startTime": "14:00",
 *         "endTime": "17:00"
 *       }
 *     ]
 *   }
 * }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateFreeScheduleRequest {
    
    @NotNull(message = "Free schedule data is required")
    @Valid
    @JsonProperty("free_schedule")
    FreeSchedule freeSchedule;
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FreeSchedule {
        /**
         * If true, caregiver is available all the time (rảnh toàn tập)
         * If false, use bookedSlots to specify unavailable times
         */
        @JsonProperty("available_all_time")
        Boolean availableAllTime;
        
        /**
         * List of booked time slots (only used when availableAllTime = false)
         * Format: date (yyyy-MM-dd), startTime and endTime (HH:mm)
         */
        @JsonProperty("booked_slots")
        List<BookedSlot> bookedSlots;
    }
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BookedSlot {
        /**
         * Date in format: yyyy-MM-dd (e.g., "2025-12-01")
         */
        @NotBlank(message = "Date is required")
        String date;
        
        /**
         * Start time in format: HH:mm (e.g., "09:00")
         */
        @NotBlank(message = "Start time is required")
        @JsonProperty("start_time")
        String startTime;
        
        /**
         * End time in format: HH:mm (e.g., "12:00")
         */
        @NotBlank(message = "End time is required")
        @JsonProperty("end_time")
        String endTime;
    }
}


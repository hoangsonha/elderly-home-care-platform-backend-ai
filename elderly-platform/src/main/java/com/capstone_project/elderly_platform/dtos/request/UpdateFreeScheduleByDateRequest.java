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
 * Request DTO for updating caregiver free schedule for a specific date
 * 
 * Mobile Request Example:
 * {
 *   "date": "2026-01-15",
 *   "booked_slots": [
 *     {
 *       "start_time": "09:00",
 *       "end_time": "12:00"
 *     },
 *     {
 *       "start_time": "14:00",
 *       "end_time": "17:00"
 *     }
 *   ]
 * }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateFreeScheduleByDateRequest {
    
    /**
     * Date in format: yyyy-MM-dd (e.g., "2026-01-15")
     */
    @NotBlank(message = "Date is required")
    String date;
    
    /**
     * List of booked time slots for this date
     * Note: Booking slots (is_booking = true) cannot be modified and will be preserved
     */
    @NotNull(message = "Booked slots list is required (can be empty array)")
    @JsonProperty("booked_slots")
    @Valid
    List<BookedSlot> bookedSlots;
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BookedSlot {
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

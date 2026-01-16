package com.capstone_project.elderly_platform.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.UUID;

/**
 * Utility class for managing caregiver free schedule in profileData
 */
@Slf4j
@Component
public class CaregiverScheduleUtils {

    private final ObjectMapper objectMapper;
    private static final String FREE_SCHEDULE_KEY = "free_schedule";
    private static final String AVAILABLE_ALL_TIME = "available_all_time";

    public CaregiverScheduleUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize free schedule to "available all time" if not exists
     * 
     * @param profileData Current profileData JSON string
     * @return Updated profileData JSON string with free_schedule initialized
     */
    public String initializeFreeScheduleIfNotExists(String profileData) {
        try {
            Map<String, Object> profileDataMap = parseProfileData(profileData);

            // Check if free_schedule already exists
            if (!profileDataMap.containsKey(FREE_SCHEDULE_KEY)) {
                Map<String, Object> freeScheduleMap = new HashMap<>();
                freeScheduleMap.put(AVAILABLE_ALL_TIME, true);
                profileDataMap.put(FREE_SCHEDULE_KEY, freeScheduleMap);

                return objectMapper.writeValueAsString(profileDataMap);
            }

            return profileData;
        } catch (Exception e) {
            log.error("Failed to initialize free schedule: {}", e.getMessage(), e);
            // Return original profileData if error occurs
            return profileData;
        }
    }

    /**
     * Update free schedule to exclude booked time slot
     * 
     * @param profileData Current profileData JSON string
     * @param workDate    Date of the booking
     * @param startTime   Start time of the booking
     * @param endTime     End time of the booking
     * @return Updated profileData JSON string with booked time excluded
     */
    public String excludeBookedTime(String profileData, LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        return excludeBookedTime(profileData, workDate, startTime, endTime, null);
    }

    /**
     * Update free schedule to exclude booked time slot (with care service ID to mark as booking)
     * 
     * @param profileData Current profileData JSON string
     * @param workDate    Date of the booking
     * @param startTime   Start time of the booking
     * @param endTime     End time of the booking
     * @param careServiceId Care service ID to mark this slot as a booking (null for manual slots)
     * @return Updated profileData JSON string with booked time excluded
     */
    public String excludeBookedTime(String profileData, LocalDate workDate, LocalTime startTime, LocalTime endTime, UUID careServiceId) {
        return excludeBookedTime(profileData, workDate, startTime, endTime, careServiceId, null);
    }

    /**
     * Update free schedule to exclude booked time slot (with care service ID and booking code to mark as booking)
     * 
     * @param profileData Current profileData JSON string
     * @param workDate    Date of the booking
     * @param startTime   Start time of the booking
     * @param endTime     End time of the booking
     * @param careServiceId Care service ID to mark this slot as a booking (null for manual slots)
     * @param bookingCode Booking code of the care service (null for manual slots)
     * @return Updated profileData JSON string with booked time excluded
     */
    public String excludeBookedTime(String profileData, LocalDate workDate, LocalTime startTime, LocalTime endTime, UUID careServiceId, String bookingCode) {
        try {
            Map<String, Object> profileDataMap = parseProfileData(profileData);

            // Initialize free_schedule if not exists
            if (!profileDataMap.containsKey(FREE_SCHEDULE_KEY)) {
                Map<String, Object> freeScheduleMap = new HashMap<>();
                freeScheduleMap.put(AVAILABLE_ALL_TIME, true);
                profileDataMap.put(FREE_SCHEDULE_KEY, freeScheduleMap);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> freeScheduleMap = (Map<String, Object>) profileDataMap.get(FREE_SCHEDULE_KEY);

            // If available_all_time is true, convert to specific schedule
            if (Boolean.TRUE.equals(freeScheduleMap.get(AVAILABLE_ALL_TIME))) {
                freeScheduleMap.remove(AVAILABLE_ALL_TIME);
                freeScheduleMap.put("booked_slots", new ArrayList<>());
            }

            // Get or create booked_slots list
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bookedSlots = (List<Map<String, Object>>) freeScheduleMap.computeIfAbsent(
                    "booked_slots", k -> new ArrayList<>());

            // Add new booked slot
            Map<String, Object> bookedSlot = new HashMap<>();
            bookedSlot.put("date", workDate.toString());
            bookedSlot.put("start_time", startTime.toString());
            bookedSlot.put("end_time", endTime.toString());
            
            // Mark as booking if careServiceId is provided
            if (careServiceId != null) {
                bookedSlot.put("is_booking", true);
                bookedSlot.put("care_service_id", careServiceId.toString());
                if (bookingCode != null) {
                    bookedSlot.put("booking_code", bookingCode);
                }
            } else {
                bookedSlot.put("is_booking", false);
            }

            bookedSlots.add(bookedSlot);
            freeScheduleMap.put("booked_slots", bookedSlots);

            profileDataMap.put(FREE_SCHEDULE_KEY, freeScheduleMap);

            return objectMapper.writeValueAsString(profileDataMap);
        } catch (Exception e) {
            log.error("Failed to exclude booked time: {}", e.getMessage(), e);
            // Return original profileData if error occurs
            return profileData;
        }
    }

    /**
     * Remove all booked slots for a specific date (both booking and manual slots)
     * Used by scheduled job to clean up old schedules
     * 
     * @param profileData Current profileData JSON string
     * @param date        Date to remove all booked slots
     * @return Updated profileData JSON string with all booked slots for the date removed
     */
    public String removeBookedSlotsByDate(String profileData, LocalDate date) {
        try {
            Map<String, Object> profileDataMap = parseProfileData(profileData);

            // If no free_schedule, nothing to remove
            if (!profileDataMap.containsKey(FREE_SCHEDULE_KEY)) {
                return profileData;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> freeScheduleMap = (Map<String, Object>) profileDataMap.get(FREE_SCHEDULE_KEY);

            // If available_all_time is true, nothing to remove
            if (Boolean.TRUE.equals(freeScheduleMap.get(AVAILABLE_ALL_TIME))) {
                return profileData;
            }

            // Get booked slots
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bookedSlots = (List<Map<String, Object>>) freeScheduleMap.get("booked_slots");

            if (bookedSlots == null || bookedSlots.isEmpty()) {
                return profileData;
            }

            // Remove ALL slots (both booking and manual) for the specified date
            String dateString = date.toString();
            int initialSize = bookedSlots.size();
            
            bookedSlots.removeIf(slot -> {
                String slotDate = (String) slot.get("date");
                return dateString.equals(slotDate);
            });

            int removedCount = initialSize - bookedSlots.size();
            
            if (removedCount > 0) {
                freeScheduleMap.put("booked_slots", bookedSlots);
                profileDataMap.put(FREE_SCHEDULE_KEY, freeScheduleMap);
                log.debug("Removed {} booked slot(s) for date {} (all slots removed)", removedCount, date);
                return objectMapper.writeValueAsString(profileDataMap);
            }

            return profileData;
        } catch (Exception e) {
            log.error("Failed to remove booked slots by date: {}", e.getMessage(), e);
            // Return original profileData if error occurs
            return profileData;
        }
    }

    /**
     * Remove booked time slot to restore free schedule
     * 
     * @param profileData Current profileData JSON string
     * @param workDate    Date of the booking to remove
     * @param startTime   Start time of the booking to remove
     * @param endTime     End time of the booking to remove
     * @return Updated profileData JSON string with booked time removed
     */
    public String removeBookedTime(String profileData, LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        try {
            Map<String, Object> profileDataMap = parseProfileData(profileData);

            // If no free_schedule, nothing to remove
            if (!profileDataMap.containsKey(FREE_SCHEDULE_KEY)) {
                return profileData;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> freeScheduleMap = (Map<String, Object>) profileDataMap.get(FREE_SCHEDULE_KEY);

            // If available_all_time is true, nothing to remove
            if (Boolean.TRUE.equals(freeScheduleMap.get(AVAILABLE_ALL_TIME))) {
                return profileData;
            }

            // Get booked slots
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bookedSlots = (List<Map<String, Object>>) freeScheduleMap.get("booked_slots");

            if (bookedSlots == null || bookedSlots.isEmpty()) {
                return profileData;
            }

            // Remove matching booked slot
            String dateString = workDate.toString();
            String startTimeStr = startTime.toString();
            String endTimeStr = endTime.toString();

            bookedSlots.removeIf(slot -> {
                String slotDate = (String) slot.get("date");
                String slotStartTime = (String) slot.get("start_time");
                String slotEndTime = (String) slot.get("end_time");

                return dateString.equals(slotDate) 
                    && startTimeStr.equals(slotStartTime) 
                    && endTimeStr.equals(slotEndTime);
            });

            freeScheduleMap.put("booked_slots", bookedSlots);
            profileDataMap.put(FREE_SCHEDULE_KEY, freeScheduleMap);

            return objectMapper.writeValueAsString(profileDataMap);
        } catch (Exception e) {
            log.error("Failed to remove booked time: {}", e.getMessage(), e);
            // Return original profileData if error occurs
            return profileData;
        }
    }

    /**
     * Update free schedule manually by caregiver
     * 
     * @param profileData  Current profileData JSON string
     * @param freeSchedule New free schedule data
     * @return Updated profileData JSON string
     */
    public String updateFreeSchedule(String profileData, Map<String, Object> freeSchedule) {
        try {
            Map<String, Object> profileDataMap = parseProfileData(profileData);
            profileDataMap.put(FREE_SCHEDULE_KEY, freeSchedule);
            return objectMapper.writeValueAsString(profileDataMap);
        } catch (Exception e) {
            log.error("Failed to update free schedule: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update free schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Get free schedule from profileData
     * 
     * @param profileData Current profileData JSON string
     * @return Free schedule map or null if not exists
     */
    public Map<String, Object> getFreeSchedule(String profileData) {
        try {
            Map<String, Object> profileDataMap = parseProfileData(profileData);
            @SuppressWarnings("unchecked")
            Map<String, Object> freeSchedule = (Map<String, Object>) profileDataMap.get(FREE_SCHEDULE_KEY);
            return freeSchedule;
        } catch (Exception e) {
            log.error("Failed to get free schedule: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if caregiver is available during the requested time slot
     * 
     * @param profileData Current profileData JSON string
     * @param workDate    Date of the booking
     * @param startTime   Start time of the booking
     * @param endTime     End time of the booking
     * @return true if caregiver is available, false if busy
     */
    public boolean isAvailable(String profileData, LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        try {
            Map<String, Object> profileDataMap = parseProfileData(profileData);

            // If no free_schedule, assume available all time
            if (!profileDataMap.containsKey(FREE_SCHEDULE_KEY)) {
                return true;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> freeScheduleMap = (Map<String, Object>) profileDataMap.get(FREE_SCHEDULE_KEY);

            // If available_all_time is true, caregiver is available
            if (Boolean.TRUE.equals(freeScheduleMap.get(AVAILABLE_ALL_TIME))) {
                return true;
            }

            // Get booked slots
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bookedSlots = (List<Map<String, Object>>) freeScheduleMap.get("booked_slots");

            if (bookedSlots == null || bookedSlots.isEmpty()) {
                return true;
            }

            // Check for conflicts with booked slots on the same date
            String dateString = workDate.toString();
            for (Map<String, Object> bookedSlot : bookedSlots) {
                String bookedDate = (String) bookedSlot.get("date");

                // Only check slots on the same date
                if (!dateString.equals(bookedDate)) {
                    continue;
                }

                String bookedStartTimeStr = (String) bookedSlot.get("start_time");
                String bookedEndTimeStr = (String) bookedSlot.get("end_time");

                if (bookedStartTimeStr == null || bookedEndTimeStr == null) {
                    continue;
                }

                LocalTime bookedStartTime = LocalTime.parse(bookedStartTimeStr);
                LocalTime bookedEndTime = LocalTime.parse(bookedEndTimeStr);

                // Check if time slots overlap
                // Overlap occurs if: startTime < bookedEndTime && endTime > bookedStartTime
                if (startTime.isBefore(bookedEndTime) && endTime.isAfter(bookedStartTime)) {
                    log.warn("Time conflict detected: Requested time [{}-{}] overlaps with booked time [{}-{}] on {}",
                            startTime, endTime, bookedStartTime, bookedEndTime, workDate);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to check availability: {}", e.getMessage(), e);
            // In case of error, assume available to avoid blocking bookings
            return true;
        }
    }

    /**
     * Parse profileData JSON string to Map
     * 
     * @param profileData JSON string
     * @return Map representation of profileData
     */
    private Map<String, Object> parseProfileData(String profileData) {
        if (profileData == null || profileData.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(profileData, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse profileData: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}

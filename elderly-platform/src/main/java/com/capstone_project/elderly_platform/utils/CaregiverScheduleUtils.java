package com.capstone_project.elderly_platform.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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
     * @param profileData Current profileData JSON string
     * @param workDate Date of the booking
     * @param startTime Start time of the booking
     * @param endTime End time of the booking
     * @return Updated profileData JSON string with booked time excluded
     */
    public String excludeBookedTime(String profileData, LocalDate workDate, LocalTime startTime, LocalTime endTime) {
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
     * Update free schedule manually by caregiver
     * @param profileData Current profileData JSON string
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
     * Parse profileData JSON string to Map
     * @param profileData JSON string
     * @return Map representation of profileData
     */
    private Map<String, Object> parseProfileData(String profileData) {
        if (profileData == null || profileData.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(profileData, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse profileData: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}



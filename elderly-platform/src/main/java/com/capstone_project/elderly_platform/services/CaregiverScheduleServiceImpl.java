package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.CaregiverProfileMapper;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.utils.CaregiverScheduleUtils;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaregiverScheduleServiceImpl implements CaregiverScheduleService {

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverProfileMapper caregiverProfileMapper;
    private final CaregiverScheduleUtils scheduleUtils;

    @Override
    @Transactional
    public CaregiverProfileResponseDTO updateFreeSchedule(UpdateFreeScheduleRequest request) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
        
        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Không tìm thấy hồ sơ người chăm sóc");
        }
        
        // Initialize free schedule if not exists
        String currentProfileData = caregiverProfile.getProfileData();
        currentProfileData = scheduleUtils.initializeFreeScheduleIfNotExists(currentProfileData);
        
        // Convert FreeSchedule DTO to Map
        Map<String, Object> freeScheduleMap = convertFreeScheduleToMap(request.getFreeSchedule());
        
        // Update free schedule
        String updatedProfileData = scheduleUtils.updateFreeSchedule(
                currentProfileData, 
                freeScheduleMap
        );
        
        caregiverProfile.setProfileData(updatedProfileData);
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        
        log.info("Updated free schedule for caregiver profile ID: {}", savedProfile.getCaregiverProfileId());
        
        return caregiverProfileMapper.toDTO(savedProfile);
    }

    @Override
    public Map<String, Object> getFreeSchedule() {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
        
        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Không tìm thấy hồ sơ người chăm sóc");
        }
        
        // Initialize free schedule if not exists
        String currentProfileData = caregiverProfile.getProfileData();
        currentProfileData = scheduleUtils.initializeFreeScheduleIfNotExists(currentProfileData);
        
        // If profileData was updated, save it
        if (!currentProfileData.equals(caregiverProfile.getProfileData())) {
            caregiverProfile.setProfileData(currentProfileData);
            caregiverProfileRepository.save(caregiverProfile);
        }
        
        Map<String, Object> freeSchedule = scheduleUtils.getFreeSchedule(currentProfileData);
        
        if (freeSchedule == null) {
            // Return default available all time
            return Map.of("available_all_time", true);
        }
        
        return freeSchedule;
    }

    @Override
    @Transactional
    public void initializeFreeScheduleIfNotExists() {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
        
        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Không tìm thấy hồ sơ người chăm sóc");
        }
        
        String currentProfileData = caregiverProfile.getProfileData();
        String updatedProfileData = scheduleUtils.initializeFreeScheduleIfNotExists(currentProfileData);
        
        if (!updatedProfileData.equals(currentProfileData)) {
            caregiverProfile.setProfileData(updatedProfileData);
            caregiverProfileRepository.save(caregiverProfile);
            log.info("Initialized free schedule for caregiver profile ID: {}", caregiverProfile.getCaregiverProfileId());
        }
    }
    
    /**
     * Convert FreeSchedule DTO to Map for storage in profileData
     */
    private Map<String, Object> convertFreeScheduleToMap(UpdateFreeScheduleRequest.FreeSchedule freeSchedule) {
        Map<String, Object> map = new HashMap<>();
        
        if (freeSchedule.getAvailableAllTime() != null) {
            map.put("available_all_time", freeSchedule.getAvailableAllTime());
        }
        
        if (freeSchedule.getBookedSlots() != null && !freeSchedule.getBookedSlots().isEmpty()) {
            List<Map<String, Object>> bookedSlotsList = new ArrayList<>();
            for (UpdateFreeScheduleRequest.BookedSlot slot : freeSchedule.getBookedSlots()) {
                Map<String, Object> slotMap = new HashMap<>();
                slotMap.put("date", slot.getDate());
                slotMap.put("start_time", slot.getStartTime());
                slotMap.put("end_time", slot.getEndTime());
                bookedSlotsList.add(slotMap);
            }
            map.put("booked_slots", bookedSlotsList);
        }
        
        return map;
    }

    @Override
    public Map<String, Object> getFreeScheduleForDate(LocalDate date, UUID caregiverId) {
        CaregiverProfile caregiverProfile;
        
        // If caregiverId is provided, use it
        if (caregiverId != null) {
            caregiverProfile = caregiverProfileRepository
                    .findByCaregiverProfileIdAndDeletedIsFalse(caregiverId);
            if (caregiverProfile == null) {
                throw new ElementNotFoundException("Không tìm thấy hồ sơ người chăm sóc với ID: " + caregiverId);
            }
        } else {
            // If caregiverId is null, check user role
            UUID currentAccountId = SecurityUtils.getCurrentUserId();
            
            // If user is CARE_SEEKER, caregiverId is required
            if (SecurityUtils.hasRole("ROLE_CARE_SEEKER")) {
                throw new BadRequestException("CARE_SEEKER phải truyền caregiverId để xem lịch rảnh của caregiver");
            }
            
            // If user is CAREGIVER, get their own profile
            caregiverProfile = caregiverProfileRepository
                    .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
            
            if (caregiverProfile == null) {
                throw new ElementNotFoundException("Không tìm thấy hồ sơ người chăm sóc");
            }
        }
        
        // Initialize free schedule if not exists
        String currentProfileData = caregiverProfile.getProfileData();
        currentProfileData = scheduleUtils.initializeFreeScheduleIfNotExists(currentProfileData);
        
        // If profileData was updated, save it
        if (!currentProfileData.equals(caregiverProfile.getProfileData())) {
            caregiverProfile.setProfileData(currentProfileData);
            caregiverProfileRepository.save(caregiverProfile);
        }
        
        Map<String, Object> freeSchedule = scheduleUtils.getFreeSchedule(currentProfileData);
        
        Map<String, Object> result = new HashMap<>();
        result.put("date", date.toString());
        
        // If free schedule is null or available_all_time is true, caregiver is available all day
        if (freeSchedule == null || Boolean.TRUE.equals(freeSchedule.get("available_all_time"))) {
            result.put("available_all_day", true);
            result.put("booked_slots", new ArrayList<>());
            return result;
        }
        
        // Get booked slots for the specific date
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allBookedSlots = (List<Map<String, Object>>) freeSchedule.get("booked_slots");
        
        if (allBookedSlots == null || allBookedSlots.isEmpty()) {
            // No booked slots, available all day
            result.put("available_all_day", true);
            result.put("booked_slots", new ArrayList<>());
            return result;
        }
        
        // Filter booked slots for the specific date
        String dateString = date.toString();
        List<Map<String, Object>> bookedSlotsForDate = allBookedSlots.stream()
                .filter(slot -> dateString.equals(slot.get("date")))
                .collect(Collectors.toList());
        
        // Merge overlapping slots for cleaner display
        List<Map<String, Object>> mergedSlots = mergeOverlappingSlots(bookedSlotsForDate);
        
        result.put("available_all_day", mergedSlots.isEmpty());
        result.put("booked_slots", mergedSlots);
        
        log.info("Retrieved free schedule for date {}: available_all_day={}, booked_slots_count={} (merged from {})", 
                date, mergedSlots.isEmpty(), mergedSlots.size(), bookedSlotsForDate.size());
        
        return result;
    }

    /**
     * Merge overlapping time slots for cleaner display
     * Example: [6h-10h, 9h-16h] → [6h-16h]
     * 
     * @param slots List of booked slots with date, start_time, end_time
     * @return Merged list of non-overlapping slots
     */
    private List<Map<String, Object>> mergeOverlappingSlots(List<Map<String, Object>> slots) {
        if (slots == null || slots.isEmpty()) {
            return new ArrayList<>();
        }

        // Parse and sort slots by start_time
        List<SlotInfo> slotInfos = new ArrayList<>();
        for (Map<String, Object> slot : slots) {
            try {
                String date = (String) slot.get("date");
                String startTimeStr = (String) slot.get("start_time");
                String endTimeStr = (String) slot.get("end_time");
                
                if (date == null || startTimeStr == null || endTimeStr == null) {
                    continue; // Skip invalid slots
                }
                
                LocalTime startTime = LocalTime.parse(startTimeStr);
                LocalTime endTime = LocalTime.parse(endTimeStr);
                
                slotInfos.add(new SlotInfo(date, startTime, endTime));
            } catch (Exception e) {
                log.warn("Failed to parse slot: {}", slot, e);
                // Skip invalid slots
            }
        }

        // Sort by start_time
        slotInfos.sort(Comparator.comparing(s -> s.startTime));

        // Merge overlapping slots
        List<SlotInfo> merged = new ArrayList<>();
        for (SlotInfo current : slotInfos) {
            if (merged.isEmpty()) {
                merged.add(current);
            } else {
                SlotInfo last = merged.get(merged.size() - 1);
                
                // If current slot overlaps or is adjacent to last slot, merge them
                // Overlap: current.startTime <= last.endTime
                if (!current.startTime.isAfter(last.endTime)) {
                    // Merge: extend last slot's endTime to max of both
                    if (current.endTime.isAfter(last.endTime)) {
                        last.endTime = current.endTime;
                    }
                } else {
                    // No overlap, add as new slot
                    merged.add(current);
                }
            }
        }

        // Convert back to Map format
        return merged.stream()
                .map(slot -> {
                    Map<String, Object> slotMap = new HashMap<>();
                    slotMap.put("date", slot.date);
                    slotMap.put("start_time", slot.startTime.toString());
                    slotMap.put("end_time", slot.endTime.toString());
                    return slotMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper class to represent a time slot for merging
     */
    private static class SlotInfo {
        String date;
        LocalTime startTime;
        LocalTime endTime;

        SlotInfo(String date, LocalTime startTime, LocalTime endTime) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}


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
import java.util.ArrayList;
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
        
        result.put("available_all_day", bookedSlotsForDate.isEmpty());
        result.put("booked_slots", bookedSlotsForDate);
        
        log.info("Retrieved free schedule for date {}: available_all_day={}, booked_slots_count={}", 
                date, bookedSlotsForDate.isEmpty(), bookedSlotsForDate.size());
        
        return result;
    }
}


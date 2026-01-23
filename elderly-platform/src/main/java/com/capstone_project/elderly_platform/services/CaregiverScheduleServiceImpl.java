package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleByDateRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateFreeScheduleRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileDetailResponseDTO;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.CaregiverProfileMapper;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.utils.CaregiverScheduleUtils;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
    private final CareServiceRepository careServiceRepository;
    private final ProfileService profileService;
    private final CaregiverScheduleUtils scheduleUtils;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CaregiverProfileDetailResponseDTO updateFreeSchedule(UpdateFreeScheduleRequest request) {
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
        
        // Validate date range: only allow setting schedule from today to +7 days
        validateDateRange(freeScheduleMap);
        
        // Check for conflicts with existing care services (current date to +7 days)
        checkConflictWithCareServices(caregiverProfile, freeScheduleMap);
        
        // Merge booking slots from current profileData into new schedule
        // This ensures booking slots (is_booking = true) are preserved when user updates schedule
        mergeBookingSlots(freeScheduleMap, currentProfileData);
        
        // Update free schedule
        String updatedProfileData = scheduleUtils.updateFreeSchedule(
                currentProfileData, 
                freeScheduleMap
        );
        
        caregiverProfile.setProfileData(updatedProfileData);
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        
        log.info("Updated free schedule for caregiver profile ID: {}", savedProfile.getCaregiverProfileId());
        
        // Return detail DTO
        return profileService.getCaregiverById(savedProfile.getCaregiverProfileId());
    }

    @Override
    @Transactional
    public CaregiverProfileDetailResponseDTO updateFreeScheduleByDate(UpdateFreeScheduleByDateRequest request) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
        
        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Không tìm thấy hồ sơ người chăm sóc");
        }
        
        // Parse date
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(request.getDate());
        } catch (Exception e) {
            throw new BadRequestException("Ngày không hợp lệ. Format: yyyy-MM-dd (ví dụ: 2026-01-15)");
        }
        
        // Validate date range: only allow setting schedule from today to +7 days
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(7);
        
        if (targetDate.isBefore(today)) {
            throw new BadRequestException(
                    String.format("Không thể cập nhật lịch rảnh cho ngày trong quá khứ. Ngày: %s. Chỉ có thể cập nhật từ hôm nay (%s) đến %s ngày sau (%s)",
                            targetDate, today, 7, maxDate));
        }
        
        if (targetDate.isAfter(maxDate)) {
            throw new BadRequestException(
                    String.format("Không thể cập nhật lịch rảnh cho ngày quá xa. Ngày: %s. Chỉ có thể cập nhật từ hôm nay (%s) đến %s ngày sau (%s)",
                            targetDate, today, 7, maxDate));
        }
        
        // Initialize free schedule if not exists
        String currentProfileData = caregiverProfile.getProfileData();
        currentProfileData = scheduleUtils.initializeFreeScheduleIfNotExists(currentProfileData);
        
        // Parse profileData directly to get all booked slots (more reliable)
        Map<String, Object> profileDataMap = parseProfileData(currentProfileData);
        @SuppressWarnings("unchecked")
        Map<String, Object> currentFreeSchedule = (Map<String, Object>) profileDataMap.get("free_schedule");
        
        // Get all booked slots from current schedule
        List<Map<String, Object>> allCurrentBookedSlots = new ArrayList<>();
        if (currentFreeSchedule != null && !Boolean.TRUE.equals(currentFreeSchedule.get("available_all_time"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> currentBookedSlots = (List<Map<String, Object>>) currentFreeSchedule.get("booked_slots");
            if (currentBookedSlots != null) {
                // Create deep copies to avoid modifying original
                for (Map<String, Object> slot : currentBookedSlots) {
                    allCurrentBookedSlots.add(new HashMap<>(slot));
                }
            }
        }
        
        log.debug("Current free schedule for date {}: total slots = {}, available_all_time = {}", 
                targetDate, allCurrentBookedSlots.size(), 
                currentFreeSchedule != null ? currentFreeSchedule.get("available_all_time") : "null");
        
        // Separate booking slots (is_booking = true) and manual slots (is_booking = false) for the target date
        String targetDateStr = targetDate.toString();
        List<Map<String, Object>> bookingSlotsForDate = new ArrayList<>(); // Slots from care service bookings
        List<Map<String, Object>> otherDateSlots = new ArrayList<>(); // Slots from other dates
        
        for (Map<String, Object> slot : allCurrentBookedSlots) {
            String slotDate = (String) slot.get("date");
            if (targetDateStr.equals(slotDate)) {
                Boolean isBooking = (Boolean) slot.get("is_booking");
                if (Boolean.TRUE.equals(isBooking)) {
                    // This is a booking slot - preserve it
                    bookingSlotsForDate.add(new HashMap<>(slot));
                }
                // Manual slots for this date will be replaced by new slots from request
            } else {
                // Keep slots from other dates
                otherDateSlots.add(new HashMap<>(slot));
            }
        }
        
        // Convert request booked_slots to manual slots (is_booking = false)
        List<Map<String, Object>> newManualSlots = new ArrayList<>();
        if (request.getBookedSlots() != null) {
            for (UpdateFreeScheduleByDateRequest.BookedSlot slot : request.getBookedSlots()) {
                Map<String, Object> slotMap = new HashMap<>();
                slotMap.put("date", targetDateStr);
                slotMap.put("start_time", slot.getStartTime());
                slotMap.put("end_time", slot.getEndTime());
                slotMap.put("is_booking", false); // Mark as manual slot
                newManualSlots.add(slotMap);
            }
        }
        
        // Merge: booking slots (preserved) + new manual slots
        List<Map<String, Object>> finalSlotsForDate = new ArrayList<>();
        finalSlotsForDate.addAll(bookingSlotsForDate);
        finalSlotsForDate.addAll(newManualSlots);
        
        // Combine with slots from other dates
        List<Map<String, Object>> allFinalSlots = new ArrayList<>();
        allFinalSlots.addAll(otherDateSlots);
        allFinalSlots.addAll(finalSlotsForDate);
        
        log.debug("Merged slots for date {}: booking slots = {}, new manual slots = {}, other date slots = {}, total final slots = {}", 
                targetDate, bookingSlotsForDate.size(), newManualSlots.size(), otherDateSlots.size(), allFinalSlots.size());
        
        // Create new free schedule map
        Map<String, Object> newFreeScheduleMap = new HashMap<>();
        if (allFinalSlots.isEmpty()) {
            // If no slots, check if we should set available_all_time
            // Only set available_all_time if there are no slots at all
            newFreeScheduleMap.put("available_all_time", true);
            log.debug("No slots found, setting available_all_time = true");
        } else {
            newFreeScheduleMap.put("available_all_time", false);
            newFreeScheduleMap.put("booked_slots", allFinalSlots);
            log.debug("Setting available_all_time = false with {} booked slots", allFinalSlots.size());
        }
        
        // Check for conflicts with existing care services for the new manual slots
        checkConflictWithCareServicesForDate(caregiverProfile, targetDate, newManualSlots);
        
        // Update free schedule
        String updatedProfileData = scheduleUtils.updateFreeSchedule(
                currentProfileData, 
                newFreeScheduleMap
        );
        
        log.debug("ProfileData before update length: {}, after update length: {}", 
                currentProfileData != null ? currentProfileData.length() : 0,
                updatedProfileData != null ? updatedProfileData.length() : 0);
        
        // Verify the update by parsing back
        Map<String, Object> verifySchedule = scheduleUtils.getFreeSchedule(updatedProfileData);
        if (verifySchedule != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> verifySlots = (List<Map<String, Object>>) verifySchedule.get("booked_slots");
            log.debug("Verification: free schedule has {} slots after update", 
                    verifySlots != null ? verifySlots.size() : 0);
        }
        
        caregiverProfile.setProfileData(updatedProfileData);
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        
        log.info("Updated free schedule for date {} for caregiver profile ID: {}. Preserved {} booking slot(s), added {} manual slot(s), total slots = {}", 
                targetDate, savedProfile.getCaregiverProfileId(), bookingSlotsForDate.size(), newManualSlots.size(), allFinalSlots.size());
        
        // Return detail DTO
        return profileService.getCaregiverById(savedProfile.getCaregiverProfileId());
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
        
        // Enrich booking_code for booking slots if missing (backward compatibility)
        enrichBookingCodes(freeSchedule, caregiverProfile);
        
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
     * All slots from user input are marked as is_booking = false (manual slots)
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
                slotMap.put("is_booking", false); // Mark as manual slot (not from booking)
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
        
        // Enrich booking_code for booking slots if missing (backward compatibility)
        enrichBookingCodesForSlots(mergedSlots, caregiverProfile);
        
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
     * Validate that booked slots are within allowed date range (today to +7 days)
     * 
     * @param freeScheduleMap New free schedule map
     * @throws BadRequestException if any slot is outside the allowed range
     */
    private void validateDateRange(Map<String, Object> freeScheduleMap) {
        // If available_all_time is true, no validation needed
        if (Boolean.TRUE.equals(freeScheduleMap.get("available_all_time"))) {
            return;
        }
        
        // Get booked slots from the new schedule
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bookedSlots = (List<Map<String, Object>>) freeScheduleMap.get("booked_slots");
        
        if (bookedSlots == null || bookedSlots.isEmpty()) {
            return; // No booked slots, no validation needed
        }
        
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(7);
        
        // Check each booked slot
        for (Map<String, Object> slot : bookedSlots) {
            String dateStr = (String) slot.get("date");
            
            if (dateStr == null) {
                continue; // Skip invalid slots (will be caught by other validations)
            }
            
            try {
                LocalDate slotDate = LocalDate.parse(dateStr);
                
                // Check if date is before today
                if (slotDate.isBefore(today)) {
                    throw new BadRequestException(
                            String.format("Không thể set lịch rảnh cho ngày trong quá khứ. Ngày: %s. Chỉ có thể set từ hôm nay (%s) đến %s ngày sau (%s)",
                                    slotDate, today, 7, maxDate));
                }
                
                // Check if date is after +7 days
                if (slotDate.isAfter(maxDate)) {
                    throw new BadRequestException(
                            String.format("Không thể set lịch rảnh cho ngày quá xa. Ngày: %s. Chỉ có thể set từ hôm nay (%s) đến %s ngày sau (%s)",
                                    slotDate, today, 7, maxDate));
                }
            } catch (Exception e) {
                if (e instanceof BadRequestException) {
                    throw e; // Re-throw BadRequestException
                }
                log.warn("Failed to parse date for validation: {}", dateStr, e);
                // Continue checking other slots
            }
        }
    }
    
    /**
     * Check if the new free schedule conflicts with existing care services
     * Only check care services from current date to +7 days
     * 
     * @param caregiverProfile Caregiver profile
     * @param freeScheduleMap New free schedule map
     * @throws BadRequestException if there's a conflict
     */
    private void checkConflictWithCareServices(CaregiverProfile caregiverProfile, Map<String, Object> freeScheduleMap) {
        // If available_all_time is true, no conflict check needed
        if (Boolean.TRUE.equals(freeScheduleMap.get("available_all_time"))) {
            return;
        }
        
        // Get booked slots from the new schedule
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bookedSlots = (List<Map<String, Object>>) freeScheduleMap.get("booked_slots");
        
        if (bookedSlots == null || bookedSlots.isEmpty()) {
            return; // No booked slots, no conflict
        }
        
        // Get date range: current date to +7 days
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(7);
        
        // Get all care services in this date range
        List<CareService> careServices = careServiceRepository
                .findByCaregiverProfileAndWorkDateBetweenAndDeletedIsFalse(
                        caregiverProfile, today, endDate);
        
        if (careServices == null || careServices.isEmpty()) {
            return; // No care services, no conflict
        }
        
        // Check each booked slot against care services
        for (Map<String, Object> slot : bookedSlots) {
            String dateStr = (String) slot.get("date");
            String startTimeStr = (String) slot.get("start_time");
            String endTimeStr = (String) slot.get("end_time");
            
            if (dateStr == null || startTimeStr == null || endTimeStr == null) {
                continue; // Skip invalid slots
            }
            
            try {
                LocalDate slotDate = LocalDate.parse(dateStr);
                
                // Only check slots within the date range (today to +7 days)
                if (slotDate.isBefore(today) || slotDate.isAfter(endDate)) {
                    continue; // Skip slots outside the range
                }
                
                LocalTime slotStartTime = LocalTime.parse(startTimeStr);
                LocalTime slotEndTime = LocalTime.parse(endTimeStr);
                
                // Check conflict with each care service
                for (CareService careService : careServices) {
                    if (careService.getWorkDate() == null || 
                        careService.getStartTime() == null || 
                        careService.getEndTime() == null) {
                        continue; // Skip invalid care services
                    }
                    
                    // Check if same date
                    if (!slotDate.equals(careService.getWorkDate())) {
                        continue;
                    }
                    
                    // Check if time slots overlap
                    if (isTimeOverlap(slotStartTime, slotEndTime, 
                                     careService.getStartTime(), careService.getEndTime())) {
                        throw new BadRequestException(
                                String.format("Không thể cập nhật lịch rảnh vì có conflict với care service đã đặt trước. " +
                                        "Ngày: %s, Thời gian: %s - %s. Care service booking code: %s. " +
                                        "Vui lòng không set lịch bận trong khoảng thời gian đã có booking.",
                                        slotDate, slotStartTime, slotEndTime, careService.getBookingCode()));
                    }
                }
            } catch (Exception e) {
                if (e instanceof BadRequestException) {
                    throw e; // Re-throw BadRequestException
                }
                log.warn("Failed to parse slot for conflict check: {}", slot, e);
                // Continue checking other slots
            }
        }
    }
    
    /**
     * Merge booking slots (is_booking = true) from current profileData into new freeScheduleMap
     * This ensures booking slots are preserved when user updates their schedule
     * 
     * @param freeScheduleMap New free schedule map from user request
     * @param currentProfileData Current profileData JSON string
     */
    private void mergeBookingSlots(Map<String, Object> freeScheduleMap, String currentProfileData) {
        try {
            // If available_all_time is true, no need to merge
            if (Boolean.TRUE.equals(freeScheduleMap.get("available_all_time"))) {
                return;
            }
            
            // Get current free schedule
            Map<String, Object> currentFreeSchedule = scheduleUtils.getFreeSchedule(currentProfileData);
            if (currentFreeSchedule == null) {
                return; // No current schedule to merge from
            }
            
            // Get booked slots from current schedule
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> currentBookedSlots = (List<Map<String, Object>>) currentFreeSchedule.get("booked_slots");
            
            if (currentBookedSlots == null || currentBookedSlots.isEmpty()) {
                return; // No current slots to merge
            }
            
            // Get new booked slots from user request
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> newBookedSlots = (List<Map<String, Object>>) freeScheduleMap.computeIfAbsent(
                    "booked_slots", k -> new ArrayList<>());
            
            // Extract booking slots (is_booking = true) from current schedule
            List<Map<String, Object>> bookingSlots = new ArrayList<>();
            for (Map<String, Object> slot : currentBookedSlots) {
                Boolean isBooking = (Boolean) slot.get("is_booking");
                if (Boolean.TRUE.equals(isBooking)) {
                    bookingSlots.add(new HashMap<>(slot)); // Create a copy
                }
            }
            
            // Add booking slots to new schedule (they won't conflict because we already checked)
            if (!bookingSlots.isEmpty()) {
                newBookedSlots.addAll(bookingSlots);
                freeScheduleMap.put("booked_slots", newBookedSlots);
                log.debug("Merged {} booking slot(s) into new schedule", bookingSlots.size());
            }
        } catch (Exception e) {
            log.warn("Failed to merge booking slots: {}", e.getMessage(), e);
            // Continue without merging - user's update will still work
        }
    }

    /**
     * Check if new manual slots conflict with existing care services for a specific date
     * 
     * @param caregiverProfile Caregiver profile
     * @param targetDate Target date
     * @param newManualSlots New manual slots to check
     * @throws BadRequestException if there's a conflict
     */
    private void checkConflictWithCareServicesForDate(CaregiverProfile caregiverProfile, LocalDate targetDate, List<Map<String, Object>> newManualSlots) {
        if (newManualSlots == null || newManualSlots.isEmpty()) {
            return; // No slots to check
        }
        
        // Get all care services for this date
        List<CareService> careServices = careServiceRepository
                .findByCaregiverProfileAndWorkDateAndDeletedIsFalse(caregiverProfile, targetDate, Sort.unsorted());
        
        if (careServices == null || careServices.isEmpty()) {
            return; // No care services, no conflict
        }
        
        // Check each new manual slot against care services
        for (Map<String, Object> slot : newManualSlots) {
            String startTimeStr = (String) slot.get("start_time");
            String endTimeStr = (String) slot.get("end_time");
            
            if (startTimeStr == null || endTimeStr == null) {
                continue; // Skip invalid slots
            }
            
            try {
                LocalTime slotStartTime = LocalTime.parse(startTimeStr);
                LocalTime slotEndTime = LocalTime.parse(endTimeStr);
                
                // Check conflict with each care service
                for (CareService careService : careServices) {
                    if (careService.getStartTime() == null || careService.getEndTime() == null) {
                        continue; // Skip invalid care services
                    }
                    
                    // Check if time slots overlap
                    if (isTimeOverlap(slotStartTime, slotEndTime, 
                                     careService.getStartTime(), careService.getEndTime())) {
                        throw new BadRequestException(
                                String.format("Không thể cập nhật lịch rảnh vì có conflict với care service đã đặt trước. " +
                                        "Ngày: %s, Thời gian: %s - %s. Care service booking code: %s. " +
                                        "Vui lòng không set lịch bận trong khoảng thời gian đã có booking.",
                                        targetDate, slotStartTime, slotEndTime, careService.getBookingCode()));
                    }
                }
            } catch (Exception e) {
                if (e instanceof BadRequestException) {
                    throw e; // Re-throw BadRequestException
                }
                log.warn("Failed to parse slot for conflict check: {}", slot, e);
                // Continue checking other slots
            }
        }
    }

    /**
     * Check if two time ranges overlap
     * 
     * @param start1 Start time of range 1
     * @param end1 End time of range 1
     * @param start2 Start time of range 2
     * @param end2 End time of range 2
     * @return true if ranges overlap, false otherwise
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        // Two ranges overlap if: start1 < end2 AND start2 < end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    /**
     * Enrich booking_code for booking slots in free schedule if missing (backward compatibility)
     * 
     * @param freeSchedule Free schedule map
     * @param caregiverProfile Caregiver profile
     */
    private void enrichBookingCodes(Map<String, Object> freeSchedule, CaregiverProfile caregiverProfile) {
        if (freeSchedule == null || Boolean.TRUE.equals(freeSchedule.get("available_all_time"))) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bookedSlots = (List<Map<String, Object>>) freeSchedule.get("booked_slots");
        
        if (bookedSlots == null || bookedSlots.isEmpty()) {
            return;
        }
        
        enrichBookingCodesForSlots(bookedSlots, caregiverProfile);
    }
    
    /**
     * Enrich booking_code for booking slots if missing (backward compatibility)
     * 
     * @param bookedSlots List of booked slots
     * @param caregiverProfile Caregiver profile
     */
    private void enrichBookingCodesForSlots(List<Map<String, Object>> bookedSlots, CaregiverProfile caregiverProfile) {
        if (bookedSlots == null || bookedSlots.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> slot : bookedSlots) {
            Boolean isBooking = (Boolean) slot.get("is_booking");
            if (Boolean.TRUE.equals(isBooking)) {
                // If booking_code is missing, try to get it from CareService
                if (!slot.containsKey("booking_code") || slot.get("booking_code") == null) {
                    String careServiceIdStr = (String) slot.get("care_service_id");
                    if (careServiceIdStr != null) {
                        try {
                            UUID careServiceId = UUID.fromString(careServiceIdStr);
                            CareService careService = careServiceRepository
                                    .findByCareServiceIdAndDeletedIsFalse(careServiceId);
                            if (careService != null && careService.getBookingCode() != null) {
                                slot.put("booking_code", careService.getBookingCode());
                                log.debug("Enriched booking_code {} for care service {}", 
                                        careService.getBookingCode(), careServiceId);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to enrich booking_code for care_service_id {}: {}", 
                                    careServiceIdStr, e.getMessage());
                        }
                    }
                }
            }
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


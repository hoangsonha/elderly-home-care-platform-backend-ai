package com.capstone_project.elderly_platform.services.externals.ai;

import com.capstone_project.elderly_platform.dtos.QualificationRequirements;
import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.enums.EnumVerificationStatusType;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.ElderlyProfile;
import com.capstone_project.elderly_platform.pojos.Qualification;
import com.capstone_project.elderly_platform.pojos.ServicePackage;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.WorkScheduleRepository;
import com.capstone_project.elderly_platform.repositories.WorkTaskRepository;
import com.capstone_project.elderly_platform.utils.CaregiverScheduleUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIMatchingConversionService {

    private final ObjectMapper objectMapper;
    private final CareServiceRepository careServiceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final WorkTaskRepository workTaskRepository;
    private final CaregiverScheduleUtils caregiverScheduleUtils;

    /**
     * Convert ElderlyProfile + ServicePackage + time info to requests.json format
     */
    public Map<String, Object> convertElderlyToRequestFormat(
            ElderlyProfile elderlyProfile,
            ServicePackage servicePackage,
            LocalDate workDate,
            LocalTime startTime,
            LocalTime endTime) {
        try {
            Map<String, Object> request = new HashMap<>();
            
            // Basic info
            request.put("id", "req_" + elderlyProfile.getElderlyProfileId().toString());
            request.put("seeker_name", elderlyProfile.getFullName() != null ? elderlyProfile.getFullName() : null);
            
            // Health status - always set, null if not available
            request.put("health_status", elderlyProfile.getHealthStatus() != null 
                    ? elderlyProfile.getHealthStatus().name().toLowerCase() 
                    : null);
            
            // Calculate elderly age - always set, null if not available
            if (elderlyProfile.getBirthDate() != null) {
                int age = Period.between(elderlyProfile.getBirthDate(), LocalDate.now()).getYears();
                request.put("elderly_age", age);
            } else {
                request.put("elderly_age", null);
            }
            
            // Parse location - always set, null if not available
            // Convert to format: {"lat": ..., "lon": ..., "address": ...}
            Map<String, Object> location = null;
            if (elderlyProfile.getLocation() != null && !elderlyProfile.getLocation().isEmpty()) {
                try {
                    Map<String, Object> locationMap = objectMapper.readValue(
                            elderlyProfile.getLocation(), 
                            new TypeReference<Map<String, Object>>() {});
                    // Convert to format: {"lat": ..., "lon": ..., "address": ...}
                    // Support both "latitude"/"longitude" and "lat"/"lon" formats
                    location = new HashMap<>();
                    Object lat = locationMap.get("lat");
                    Object lon = locationMap.get("lon");
                    if (lat == null) {
                        lat = locationMap.get("latitude");
                    }
                    if (lon == null) {
                        lon = locationMap.get("longitude");
                    }
                    location.put("lat", lat);
                    location.put("lon", lon);
                    location.put("address", locationMap.get("address"));
                } catch (Exception e) {
                    log.warn("Failed to parse location for elderly profile {}: {}", 
                            elderlyProfile.getElderlyProfileId(), e.getMessage());
                }
            }
            request.put("location", location);
            
            // Initialize optional fields to null first
            request.put("required_years_experience", null);
            request.put("overall_rating_range", null);
            request.put("caregiver_age_range", null);
            request.put("gender_preference", null);
            
            // Parse careRequirement for additional preferences
            if (elderlyProfile.getCareRequirement() != null && !elderlyProfile.getCareRequirement().isEmpty()) {
                try {
                    Map<String, Object> careReq = objectMapper.readValue(
                            elderlyProfile.getCareRequirement(), 
                            new TypeReference<Map<String, Object>>() {});
                    
                    // Extract preferences from careRequirement
                    if (careReq.containsKey("experience")) {
                        request.put("required_years_experience", careReq.get("experience"));
                    }
                    if (careReq.containsKey("rating")) {
                        Object ratingObj = careReq.get("rating");
                        List<Integer> ratingRange = new ArrayList<>();
                        
                        if (ratingObj instanceof List) {
                            // New format: List<Integer> [minRating, maxRating]
                            List<?> ratingList = (List<?>) ratingObj;
                            if (ratingList.size() >= 2) {
                                ratingRange.add(((Number) ratingList.get(0)).intValue());
                                ratingRange.add(((Number) ratingList.get(1)).intValue());
                            } else if (ratingList.size() == 1) {
                                ratingRange.add(((Number) ratingList.get(0)).intValue());
                                ratingRange.add(5);
                            } else {
                                ratingRange.add(0);
                                ratingRange.add(5);
                            }
                            request.put("overall_rating_range", ratingRange);
                        } else if (ratingObj instanceof Map) {
                            // Backward compatibility: Map with min/max
                            Map<String, Object> ratingMap = (Map<String, Object>) ratingObj;
                            if (ratingMap.containsKey("min")) {
                                ratingRange.add(((Number) ratingMap.get("min")).intValue());
                            } else {
                                ratingRange.add(0);
                            }
                            if (ratingMap.containsKey("max")) {
                                ratingRange.add(((Number) ratingMap.get("max")).intValue());
                            } else {
                                ratingRange.add(5);
                            }
                            request.put("overall_rating_range", ratingRange);
                        }
                    }
                    if (careReq.containsKey("age")) {
                        Object ageObj = careReq.get("age");
                        if (ageObj instanceof Map) {
                            Map<String, Object> ageMap = (Map<String, Object>) ageObj;
                            List<Integer> ageRange = new ArrayList<>();
                            if (ageMap.containsKey("min")) {
                                ageRange.add(((Number) ageMap.get("min")).intValue());
                            } else {
                                ageRange.add(18);
                            }
                            if (ageMap.containsKey("max")) {
                                ageRange.add(((Number) ageMap.get("max")).intValue());
                            } else {
                                ageRange.add(100);
                            }
                            request.put("caregiver_age_range", ageRange);
                        }
                    }
                    if (careReq.containsKey("gender")) {
                        request.put("gender_preference", careReq.get("gender"));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse careRequirement for elderly profile {}: {}", 
                            elderlyProfile.getElderlyProfileId(), e.getMessage());
                }
            }
            
            // Time slots
            Map<String, String> timeSlots = new HashMap<>();
            timeSlots.put("day", workDate.toString());
            timeSlots.put("start", startTime.toString());
            timeSlots.put("end", endTime.toString());
            request.put("time_slots", timeSlots);
            
            // Service package
            Map<String, Object> servicePackageMap = convertServicePackageToMap(servicePackage);
            request.put("service_package", servicePackageMap);
            
            return request;
        } catch (Exception e) {
            log.error("Failed to convert elderly profile to request format: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert elderly profile to request format", e);
        }
    }

    /**
     * Convert ServicePackage to map format
     */
    private Map<String, Object> convertServicePackageToMap(ServicePackage servicePackage) {
        Map<String, Object> map = new HashMap<>();
        map.put("servicePackageId", servicePackage.getServicePackageId().toString());
        map.put("packageName", servicePackage.getPackageName());
        map.put("description", servicePackage.getDescription());
        map.put("durationHours", servicePackage.getDurationHours());
        map.put("packageType", servicePackage.getPackageType() != null ? servicePackage.getPackageType().name() : null);
        map.put("price", servicePackage.getPrice());
        map.put("note", servicePackage.getNote());
        map.put("status", servicePackage.getStatus() != null ? servicePackage.getStatus().name() : null);
        
        // Parse qualification
        if (servicePackage.getQualification() != null && !servicePackage.getQualification().isEmpty()) {
            try {
                QualificationRequirements qual = objectMapper.readValue(
                        servicePackage.getQualification(), 
                        QualificationRequirements.class);
                Map<String, Object> qualMap = new HashMap<>();
                qualMap.put("skills", qual.getSkills());
                // Convert UUIDs to strings for certificate_groups
                if (qual.getCertificateGroups() != null) {
                    List<List<String>> certGroups = qual.getCertificateGroups().stream()
                            .map(group -> group.stream()
                                    .map(UUID::toString)
                                    .collect(Collectors.toList()))
                            .collect(Collectors.toList());
                    qualMap.put("certificate_groups", certGroups);
                }
                map.put("qualification", qualMap);
            } catch (Exception e) {
                log.warn("Failed to parse qualification for service package {}: {}", 
                        servicePackage.getServicePackageId(), e.getMessage());
            }
        }
        
        // Service tasks
        if (servicePackage.getServiceTasks() != null) {
            List<Map<String, Object>> tasks = servicePackage.getServiceTasks().stream()
                    .filter(task -> !task.isDeleted())
                    .map(task -> {
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("serviceTaskId", task.getServiceTaskId().toString());
                        taskMap.put("taskName", task.getTaskName());
                        taskMap.put("description", task.getDescription());
                        taskMap.put("status", task.getStatus() != null ? task.getStatus().name() : null);
                        return taskMap;
                    })
                    .collect(Collectors.toList());
            map.put("serviceTasks", tasks);
        }
        
        return map;
    }

    /**
     * Convert list of CaregiverProfile to caregivers.json format
     * Only filter by deleted and status (APPROVED)
     * Note: Availability filtering is handled by AI matching service
     */
    public List<Map<String, Object>> convertCaregiversToFormat(
            List<CaregiverProfile> caregivers,
            LocalDate workDate,
            LocalTime startTime,
            LocalTime endTime) {
        int totalCaregivers = caregivers.size();
        
        // Use AtomicInteger for counters that are modified in lambda
        java.util.concurrent.atomic.AtomicInteger filteredByStatus = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger filteredByDeleted = new java.util.concurrent.atomic.AtomicInteger(0);
        
        List<Map<String, Object>> result = caregivers.stream()
                .filter(cg -> {
                    // Check deleted first
                    if (cg.isDeleted()) {
                        filteredByDeleted.incrementAndGet();
                        return false;
                    }
                    
                    // Check status - only include APPROVED caregivers
                    if (cg.getStatus() != EnumVerificationStatusType.APPROVED) {
                        filteredByStatus.incrementAndGet();
                        log.debug("Caregiver {} filtered by status: {}", 
                                cg.getCaregiverProfileId(), cg.getStatus());
                        return false;
                    }
                    
                    // Note: Availability filtering is done by AI matching service
                    return true;
                })
                .map(this::convertCaregiverToMap)
                .collect(Collectors.toList());
        
        log.info("=== CAREGIVER FILTERING ===");
        log.info("Total caregivers from DB: {}", totalCaregivers);
        log.info("Filtered by deleted: {}", filteredByDeleted.get());
        log.info("Filtered by status (not APPROVED): {}", filteredByStatus.get());
        log.info("Remaining after filtering: {} (availability will be checked by AI service)", result.size());
        log.info("=== END CAREGIVER FILTERING ===");
        
        return result;
    }

    /**
     * Convert single CaregiverProfile to caregivers.json format
     */
    private Map<String, Object> convertCaregiverToMap(CaregiverProfile caregiver) {
        Map<String, Object> map = new HashMap<>();
        
        // Basic info - always set, null if not available
        map.put("caregiverProfileId", caregiver.getCaregiverProfileId() != null 
                ? caregiver.getCaregiverProfileId().toString() : null);
        map.put("fullName", caregiver.getFullName());
        map.put("phoneNumber", caregiver.getPhoneNumber());
        map.put("bio", caregiver.getBio());
        map.put("isVerified", caregiver.getIsVerified() != null ? caregiver.getIsVerified() : false);
        map.put("status", caregiver.getStatus() != null ? caregiver.getStatus().name() : null);
        map.put("rejectionReason", caregiver.getRejectionReason());
        map.put("isNeededReviewCertificate", caregiver.getIsNeededReviewCertificate() != null 
                ? caregiver.getIsNeededReviewCertificate() : false);
        
        // Always set these fields, null if not available
        map.put("acceptedAt", caregiver.getAcceptedAt() != null 
                ? caregiver.getAcceptedAt().toString() : null);
        map.put("declinedAt", caregiver.getDeclinedAt() != null 
                ? caregiver.getDeclinedAt().toString() : null);
        map.put("reviewedBy", caregiver.getReviewedBy() != null 
                ? caregiver.getReviewedBy().toString() : null);
        
        // Birth date and age - always set, null if not available
        if (caregiver.getBirthDate() != null) {
            map.put("birthDate", caregiver.getBirthDate().toString());
            // Calculate age
            int age = Period.between(caregiver.getBirthDate(), LocalDate.now()).getYears();
            map.put("age", age);
        } else {
            map.put("birthDate", null);
            map.put("age", null);
        }
        
        map.put("gender", caregiver.getGender() != null ? caregiver.getGender().name() : null);
        
        // Parse location - always set, null if not available
        // Convert to format: {"lat": ..., "lon": ..., "address": ..., "service_radius_km": ...}
        Map<String, Object> location = null;
        if (caregiver.getLocation() != null && !caregiver.getLocation().isEmpty()) {
            try {
                Map<String, Object> locationMap = objectMapper.readValue(
                        caregiver.getLocation(), 
                        new TypeReference<Map<String, Object>>() {});
                // Convert to format: {"lat": ..., "lon": ..., "address": ..., "service_radius_km": ...}
                location = new HashMap<>();
                // Support both "latitude"/"longitude" and "lat"/"lon" formats
                Object lat = locationMap.get("lat");
                Object lon = locationMap.get("lon");
                if (lat == null) {
                    lat = locationMap.get("latitude");
                }
                if (lon == null) {
                    lon = locationMap.get("longitude");
                }
                location.put("lat", lat);
                location.put("lon", lon);
                location.put("address", locationMap.get("address"));
                location.put("service_radius_km", locationMap.get("service_radius_km"));
            } catch (Exception e) {
                log.warn("Failed to parse location for caregiver {}: {}", 
                        caregiver.getCaregiverProfileId(), e.getMessage());
            }
        }
        map.put("location", location);
        
        // Parse profileData - always set, null if not available
        Map<String, Object> profileData = null;
        if (caregiver.getProfileData() != null && !caregiver.getProfileData().isEmpty()) {
            try {
                profileData = objectMapper.readValue(
                        caregiver.getProfileData(), 
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse profileData for caregiver {}: {}", 
                        caregiver.getCaregiverProfileId(), e.getMessage());
            }
        }
        map.put("profileData", profileData);
        
        // Account info - always set, null if not available
        map.put("accountId", caregiver.getAccount() != null && caregiver.getAccount().getAccountId() != null
                ? caregiver.getAccount().getAccountId().toString() : null);
        map.put("email", caregiver.getAccount() != null ? caregiver.getAccount().getEmail() : null);
        map.put("avatarUrl", caregiver.getAccount() != null ? caregiver.getAccount().getAvatarUrl() : null);
        map.put("enabled", caregiver.getAccount() != null && caregiver.getAccount().getEnabled() != null
                ? caregiver.getAccount().getEnabled() : null);
        map.put("nonLocked", caregiver.getAccount() != null && caregiver.getAccount().getNonLocked() != null
                ? caregiver.getAccount().getNonLocked() : null);
        
        // Qualifications - always set, empty array if not available
        List<Map<String, Object>> qualifications = new ArrayList<>();
        if (caregiver.getQualifications() != null) {
            qualifications = caregiver.getQualifications().stream()
                    .filter(q -> !q.isDeleted())
                    .map(this::convertQualificationToMap)
                    .collect(Collectors.toList());
        }
        map.put("qualifications", qualifications);
        
        // Calculate statistics
        calculateCaregiverStatistics(caregiver, map);
        
        return map;
    }

    /**
     * Convert Qualification to map format - always set all fields, null if not available
     */
    private Map<String, Object> convertQualificationToMap(Qualification qualification) {
        Map<String, Object> map = new HashMap<>();
        map.put("qualificationId", qualification.getQualificationId() != null 
                ? qualification.getQualificationId().toString() : null);
        map.put("qualificationTypeId", qualification.getQualificationType() != null 
                && qualification.getQualificationType().getQualificationTypeId() != null
                ? qualification.getQualificationType().getQualificationTypeId().toString() : null);
        map.put("qualificationTypeName", qualification.getQualificationType() != null 
                ? qualification.getQualificationType().getTypeName() : null);
        map.put("certificateNumber", qualification.getCertificateNumber());
        map.put("issuingOrganization", qualification.getIssuingOrganization());
        map.put("issueDate", qualification.getIssueDate() != null 
                ? qualification.getIssueDate().toString() : null);
        map.put("expiryDate", qualification.getExpiryDate() != null 
                ? qualification.getExpiryDate().toString() : null);
        map.put("certificateUrl", qualification.getCertificateUrl());
        map.put("isVerified", qualification.getIsVerified() != null ? qualification.getIsVerified() : false);
        map.put("status", qualification.getStatus() != null ? qualification.getStatus().name() : null);
        map.put("rejectionReason", qualification.getRejectionReason());
        map.put("acceptedAt", qualification.getAcceptedAt() != null 
                ? qualification.getAcceptedAt().toString() : null);
        map.put("declinedAt", qualification.getDeclinedAt() != null 
                ? qualification.getDeclinedAt().toString() : null);
        map.put("reviewedBy", qualification.getReviewedBy() != null 
                ? qualification.getReviewedBy().toString() : null);
        map.put("notes", qualification.getNotes());
        return map;
    }

    /**
     * Calculate caregiver statistics: totalCompletedBookings, taskCompletionRate, totalCancelOrDeclineBookingRate
     */
    private void calculateCaregiverStatistics(CaregiverProfile caregiver, Map<String, Object> map) {
        // Get all care services for this caregiver
        List<CareService> allCareServices = careServiceRepository
                .findByCaregiverProfileAndDeletedIsFalse(caregiver, org.springframework.data.domain.Sort.unsorted());
        
        // 1. Total completed bookings
        long totalCompletedBookings = allCareServices.stream()
                .filter(cs -> cs.getStatus() == EnumCareServiceStatusType.COMPLETED)
                .count();
        map.put("totalCompletedBookings", (int) totalCompletedBookings);
        
        // 2. Task completion rate
        double taskCompletionRate = calculateTaskCompletionRate(caregiver, allCareServices);
        map.put("taskCompletionRate", taskCompletionRate);
        
        // 3. Total cancel or decline booking rate
        long totalCancelled = allCareServices.stream()
                .filter(cs -> cs.getStatus() == EnumCareServiceStatusType.CANCELLED)
                .count();
        double cancelOrDeclineRate = allCareServices.isEmpty() ? 0.0 : 
                (double) totalCancelled / allCareServices.size() * 100.0;
        map.put("totalCancelOrDeclineBookingRate", cancelOrDeclineRate);
        
        // 4. Total earnings (from PayoutBatches - simplified, can be enhanced)
        map.put("totalEarnings", 0.0); // TODO: Calculate from PayoutBatches if needed
    }

    /**
     * Calculate task completion rate for caregiver
     */
    private double calculateTaskCompletionRate(CaregiverProfile caregiver, List<CareService> completedCareServices) {
        int totalTasks = 0;
        int doneTasks = 0;
        
        // Only count tasks from COMPLETED care services
        List<CareService> completed = completedCareServices.stream()
                .filter(cs -> cs.getStatus() == EnumCareServiceStatusType.COMPLETED)
                .collect(Collectors.toList());
        
        for (CareService careService : completed) {
            // Get work schedules for this care service
            List<com.capstone_project.elderly_platform.pojos.WorkSchedule> workSchedules = 
                    workScheduleRepository.findAll().stream()
                            .filter(ws -> !ws.isDeleted() 
                                    && ws.getCareService() != null
                                    && ws.getCareService().getCareServiceId().equals(careService.getCareServiceId()))
                            .collect(Collectors.toList());
            
            for (com.capstone_project.elderly_platform.pojos.WorkSchedule workSchedule : workSchedules) {
                if (workSchedule.getTotalTasks() != null) {
                    totalTasks += workSchedule.getTotalTasks();
                }
                
                // Get work tasks
                List<com.capstone_project.elderly_platform.pojos.WorkTask> workTasks = 
                        workTaskRepository.findAll().stream()
                                .filter(wt -> !wt.isDeleted() 
                                        && wt.getWorkSchedule() != null
                                        && wt.getWorkSchedule().getWorkScheduleId().equals(workSchedule.getWorkScheduleId()))
                                .collect(Collectors.toList());
                
                // Count DONE tasks
                for (com.capstone_project.elderly_platform.pojos.WorkTask workTask : workTasks) {
                    if (workTask.getStatus() == com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType.DONE) {
                        doneTasks++;
                    }
                }
            }
        }
        
        // Calculate percentage
        if (totalTasks > 0) {
            return (double) doneTasks / totalTasks * 100.0;
        }
        return 0.0;
    }
}

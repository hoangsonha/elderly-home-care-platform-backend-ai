package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CareServiceRequest;
import com.capstone_project.elderly_platform.dtos.request.LocationRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.enums.EnumServicePackageType;
import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.CareServiceMapper;
import com.capstone_project.elderly_platform.pojos.*;
import com.capstone_project.elderly_platform.repositories.*;
import com.capstone_project.elderly_platform.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareServiceServiceImpl implements CareServiceService {

    private final CareServiceRepository careServiceRepository;
    private final ElderlyProfileRepository elderlyProfileRepository;
    private final CareSeekerProfileRepository careSeekerProfileRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;
    private final CareServiceMapper careServiceMapper;

    @Override
    public CareServiceResponseDTO createCareService(CareServiceRequest request) {

        ElderlyProfile elderlyProfile = elderlyProfileRepository
                .findByElderlyProfileIdAndDeletedIsFalse(request.getElderlyProfileId());
        if (elderlyProfile == null) {
            throw new ElementNotFoundException("Elderly profile not found");
        }

        CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                .findByCareSeekerProfileIdAndDeletedIsFalse(request.getCareSeekerProfileId());
        if (careSeekerProfile == null) {
            throw new ElementNotFoundException("Care seeker profile not found");
        }

        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByCaregiverProfileIdAndDeletedIsFalse(request.getCaregiverProfileId());
        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found");
        }

        ServicePackage servicePackage = servicePackageRepository
                .findByServicePackageIdAndDeletedIsFalse(request.getServicePackageId());
        if (servicePackage == null) {
            throw new ElementNotFoundException("Service package not found");
        }

        // create snapshot
        CareServiceSnapshot snapshot = CareServiceSnapshot.builder()
                .elderlyProfile(createElderlyProfileSnapshot(elderlyProfile))
                .careSeekerProfile(createCareSeekerProfileSnapshot(careSeekerProfile))
                .caregiverProfile(createCaregiverProfileSnapshot(caregiverProfile))
                .servicePackage(createServicePackageSnapshot(servicePackage))
                .build();

        // Change snapshot to JSON string
        String careServiceSnapshotJson;
        try {
            careServiceSnapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize care service snapshot to JSON", e);
        }

        LocalDate workDate = request.getWorkDate();

        // Validate startHour và startMinute
        Integer startHour = request.getStartHour();
        Integer startMinute = request.getStartMinute();

        if (startHour == null) {
            throw new BadRequestException("Start hour is required");
        }
        if (startMinute == null) {
            throw new BadRequestException("Start minute is required");
        }
        if (startHour < 0 || startHour > 23) {
            throw new BadRequestException("Start hour must be between 0 and 23");
        }
        if (startMinute < 0 || startMinute > 59) {
            throw new BadRequestException("Start minute must be between 0 and 59");
        }

        LocalTime startTime = LocalTime.of(startHour, startMinute);

        if (servicePackage.getDurationHours() == null) {
            throw new BadRequestException("Service package duration hours is required");
        }
        LocalTime endTime = startTime.plusHours(servicePackage.getDurationHours());

        // get all active config values at booking time (snapshot) - use this for all
        // calculations
        Map<EnumSystemConfigKey, String> activeConfigs = systemConfigService.getAllActiveConfigs();

        // call function validate time of booking (using snapshot config)
        validateMinimumAdvanceBookingTime(workDate, startTime, servicePackage, activeConfigs);

        // calculate caregiver response deadline (using snapshot config)
        LocalDateTime caregiverResponseDeadline = calculateCaregiverResponseDeadline(workDate, startTime,
                activeConfigs);

        // serialize location to JSON
        String locationJson = null;
        if (request.getLocation() != null) {
            try {
                locationJson = objectMapper.writeValueAsString(request.getLocation());
            } catch (Exception e) {
                log.error("Failed to serialize location to JSON", e);
                throw new BadRequestException("Invalid location data");
            }
        }

        String configVersionJson;
        try {
            // Convert enum keys to strings for JSON serialization
            Map<String, String> configValueMap = new HashMap<>();
            activeConfigs.forEach((key, value) -> configValueMap.put(key.name(), value));
            configVersionJson = objectMapper.writeValueAsString(configValueMap);
        } catch (Exception e) {
            log.error("Failed to serialize config values to JSON", e);
            configVersionJson = null;
        }

        // create booking code
        String bookingCode = StringUtils.generateRandomAlphabetic(12);
        boolean existsCode = careServiceRepository.existsByBookingCode(bookingCode);
        while (existsCode) {
            bookingCode = StringUtils.generateRandomAlphabetic(12);
            existsCode = careServiceRepository.existsByBookingCode(bookingCode);
        }

        // Use config values from snapshot (activeConfigs) instead of querying DB again
        // to ensure consistency with the saved configVersionJson
        String systemFeeValue = activeConfigs.getOrDefault(
                EnumSystemConfigKey.SYSTEM_FEE_PERCENTAGE, "10.0");
        Double systemFeePercentage;
        try {
            systemFeePercentage = Double.parseDouble(systemFeeValue);
        } catch (NumberFormatException e) {
            log.error("Failed to parse system fee percentage: {}", systemFeeValue);
            systemFeePercentage = 10.0;
        }

        Double totalPrice = servicePackage.getPrice();
        Double caregiverEarnings = totalPrice - (totalPrice * systemFeePercentage / 100);

        CareService careService = CareService.builder()
                .careServiceSnapshot(careServiceSnapshotJson)
                .bookingCode(bookingCode)
                .workDate(workDate)
                .startTime(startTime)
                .endTime(endTime)
                .caregiverResponseDeadline(caregiverResponseDeadline)
                .status(EnumCareServiceStatusType.PENDING_CAREGIVER)
                .note(request.getNote())
                .systemFeePercentage(systemFeePercentage)
                .totalPrice(totalPrice)
                .caregiverEarnings(caregiverEarnings)
                .location(locationJson)
                .configVersion(configVersionJson)
                .careSeekerProfile(careSeekerProfile)
                .caregiverProfile(caregiverProfile)
                .elderlyProfile(elderlyProfile)
                .servicePackage(servicePackage)
                .build();

        return careServiceMapper.toDTO(careServiceRepository.save(careService));
    }

    // Validate time of booking
    private void validateMinimumAdvanceBookingTime(LocalDate workDate, LocalTime startTime,
            ServicePackage servicePackage, Map<EnumSystemConfigKey, String> activeConfigs) {
        if (servicePackage.getPackageType() == null) {
            throw new BadRequestException("Service package type is required");
        }

        int minimumAdvanceHours = getMinimumAdvanceHours(servicePackage.getPackageType(), activeConfigs);

        LocalDateTime bookingDateTime = LocalDateTime.of(workDate, startTime);
        LocalDateTime now = LocalDateTime.now();

        long hoursUntilBooking = java.time.Duration.between(now, bookingDateTime).toHours();

        if (hoursUntilBooking < minimumAdvanceHours) {
            throw new BadRequestException(
                    String.format("Must be booked at least %d hours in advance. Booking time: %s, Current time: %s",
                            minimumAdvanceHours,
                            bookingDateTime,
                            now));
        }
    }

    // take minimum advance hours from config snapshot based on package type
    private int getMinimumAdvanceHours(EnumServicePackageType packageType,
            Map<EnumSystemConfigKey, String> activeConfigs) {
        EnumSystemConfigKey configKey = switch (packageType) {
            case BASIC -> EnumSystemConfigKey.SERVICE_PACKAGE_MINIMUM_ADVANCE_HOURS_BASIC;
            case PROFESSIONAL -> EnumSystemConfigKey.SERVICE_PACKAGE_MINIMUM_ADVANCE_HOURS_PROFESSIONAL;
            case ADVANCED -> EnumSystemConfigKey.SERVICE_PACKAGE_MINIMUM_ADVANCE_HOURS_ADVANCED;
        };

        String defaultValue = switch (packageType) {
            case BASIC -> "12";
            case PROFESSIONAL -> "24";
            case ADVANCED -> "48";
        };

        String value = activeConfigs.getOrDefault(configKey, defaultValue);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse config value for key {}: {}", configKey, value);
            return Integer.parseInt(defaultValue);
        }
    }

    // Calculate caregiver response deadline based on advance booking days
    private LocalDateTime calculateCaregiverResponseDeadline(LocalDate workDate, LocalTime startTime,
            Map<EnumSystemConfigKey, String> activeConfigs) {
        LocalDateTime bookingDateTime = LocalDateTime.of(workDate, startTime);
        LocalDateTime now = LocalDateTime.now();

        // Calculate days until booking
        long daysUntilBooking = java.time.Duration.between(now, bookingDateTime).toDays();

        // Determine response deadline hours based on advance booking time (using
        // snapshot config)
        int responseDeadlineHours;
        String deadlineValue;
        if (daysUntilBooking >= 3) {
            deadlineValue = activeConfigs.getOrDefault(
                    EnumSystemConfigKey.CAREGIVER_RESPONSE_DEADLINE_3_DAYS_OR_MORE, "24");
        } else if (daysUntilBooking >= 1) {
            deadlineValue = activeConfigs.getOrDefault(
                    EnumSystemConfigKey.CAREGIVER_RESPONSE_DEADLINE_1_TO_2_DAYS, "12");
        } else {
            deadlineValue = activeConfigs.getOrDefault(
                    EnumSystemConfigKey.CAREGIVER_RESPONSE_DEADLINE_LESS_THAN_24H, "6");
        }

        try {
            responseDeadlineHours = Integer.parseInt(deadlineValue);
        } catch (NumberFormatException e) {
            log.error("Failed to parse response deadline value: {}", deadlineValue);
            responseDeadlineHours = daysUntilBooking >= 3 ? 24 : (daysUntilBooking >= 1 ? 12 : 6);
        }

        // Calculate deadline: current time + response deadline hours
        return now.plusHours(responseDeadlineHours);
    }

    private ElderlyProfileSnapshot createElderlyProfileSnapshot(ElderlyProfile elderlyProfile) {
        LocationSnapshot location = parseLocation(elderlyProfile.getLocation());
        return ElderlyProfileSnapshot.builder()
                .elderlyProfileId(elderlyProfile.getElderlyProfileId())
                .fullName(elderlyProfile.getFullName())
                .birthDate(elderlyProfile.getBirthDate())
                .location(location)
                .gender(elderlyProfile.getGender() != null ? elderlyProfile.getGender().name() : null)
                .avatarUrl(elderlyProfile.getAvatarUrl())
                .profileData(elderlyProfile.getProfileData())
                .careRequirement(elderlyProfile.getCareRequirement())
                .note(elderlyProfile.getNote())
                .healthNote(elderlyProfile.getHealthNote())
                .status(elderlyProfile.getStatus() != null ? elderlyProfile.getStatus().name() : null)
                .build();
    }

    private CareSeekerProfileSnapshot createCareSeekerProfileSnapshot(CareSeekerProfile careSeekerProfile) {
        LocationSnapshot location = parseLocation(careSeekerProfile.getLocation());
        return CareSeekerProfileSnapshot.builder()
                .careSeekerProfileId(careSeekerProfile.getCareSeekerProfileId())
                .fullName(careSeekerProfile.getFullName())
                .phoneNumber(careSeekerProfile.getPhoneNumber())
                .location(location)
                .birthDate(careSeekerProfile.getBirthDate())
                .gender(careSeekerProfile.getGender() != null ? careSeekerProfile.getGender().name() : null)
                .profileData(careSeekerProfile.getProfileData())
                .build();
    }

    private CaregiverProfileSnapshot createCaregiverProfileSnapshot(CaregiverProfile caregiverProfile) {
        LocationSnapshot location = parseLocation(caregiverProfile.getLocation());
        return CaregiverProfileSnapshot.builder()
                .caregiverProfileId(caregiverProfile.getCaregiverProfileId())
                .fullName(caregiverProfile.getFullName())
                .phoneNumber(caregiverProfile.getPhoneNumber())
                .location(location)
                .bio(caregiverProfile.getBio())
                .isVerified(caregiverProfile.getIsVerified())
                .birthDate(caregiverProfile.getBirthDate())
                .gender(caregiverProfile.getGender() != null ? caregiverProfile.getGender().name() : null)
                .profileData(caregiverProfile.getProfileData())
                .build();
    }

    private ServicePackageSnapshot createServicePackageSnapshot(ServicePackage servicePackage) {
        // Map service tasks to snapshot
        java.util.List<ServiceTaskSnapshot> taskSnapshots = null;
        if (servicePackage.getServiceTasks() != null && !servicePackage.getServiceTasks().isEmpty()) {
            taskSnapshots = servicePackage.getServiceTasks().stream()
                    .map(task -> ServiceTaskSnapshot.builder()
                            .serviceTaskId(task.getServiceTaskId())
                            .taskName(task.getTaskName())
                            .description(task.getDescription())
                            .status(task.getStatus() != null ? task.getStatus().name() : null)
                            .build())
                    .collect(java.util.stream.Collectors.toList());
        }

        return ServicePackageSnapshot.builder()
                .servicePackageId(servicePackage.getServicePackageId())
                .packageName(servicePackage.getPackageName())
                .description(servicePackage.getDescription())
                .durationHours(servicePackage.getDurationHours())
                .packageType(servicePackage.getPackageType() != null ? servicePackage.getPackageType().name() : null)
                .price(servicePackage.getPrice())
                .note(servicePackage.getNote())
                .serviceIncluded(null) // Not used, set to null
                .status(servicePackage.getStatus() != null ? servicePackage.getStatus().name() : null)
                .serviceTasks(taskSnapshots)
                .build();
    }

    private LocationSnapshot parseLocation(String locationJson) {
        if (locationJson == null || locationJson.trim().isEmpty()) {
            return null;
        }
        try {
            LocationRequest locationRequest = objectMapper.readValue(locationJson, LocationRequest.class);
            return LocationSnapshot.builder()
                    .address(locationRequest.getAddress())
                    .latitude(locationRequest.getLatitude())
                    .longitude(locationRequest.getLongitude())
                    .build();
        } catch (Exception e) {
            // return null if parsing fails or log errors
            return null;
        }
    }

    // Inner classes for snapshot
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CareServiceSnapshot {
        private ElderlyProfileSnapshot elderlyProfile;
        private CareSeekerProfileSnapshot careSeekerProfile;
        private CaregiverProfileSnapshot caregiverProfile;
        private ServicePackageSnapshot servicePackage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ElderlyProfileSnapshot {
        private UUID elderlyProfileId;
        private String fullName;
        private LocalDate birthDate;
        private LocationSnapshot location;
        private String gender;
        private String avatarUrl;
        private String profileData;
        private String careRequirement;
        private String note;
        private String healthNote;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CareSeekerProfileSnapshot {
        private UUID careSeekerProfileId;
        private String fullName;
        private String phoneNumber;
        private LocationSnapshot location;
        private LocalDate birthDate;
        private String gender;
        private String profileData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CaregiverProfileSnapshot {
        private UUID caregiverProfileId;
        private String fullName;
        private String phoneNumber;
        private LocationSnapshot location;
        private String bio;
        private Boolean isVerified;
        private LocalDate birthDate;
        private String gender;
        private String profileData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ServicePackageSnapshot {
        private UUID servicePackageId;
        private String packageName;
        private String description;
        private Integer durationHours;
        private String packageType;
        private Double price;
        private String note;
        private String serviceIncluded; // Not used, kept for backward compatibility
        private String status;
        private java.util.List<ServiceTaskSnapshot> serviceTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ServiceTaskSnapshot {
        private UUID serviceTaskId;
        private String taskName;
        private String description;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class LocationSnapshot {
        private String address;
        private Double latitude;
        private Double longitude;
    }

}

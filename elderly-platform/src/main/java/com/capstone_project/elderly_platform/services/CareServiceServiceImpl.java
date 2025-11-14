package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.configurations.CustomAccountDetail;
import com.capstone_project.elderly_platform.dtos.request.ConfirmationCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumActorType;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.enums.EnumServicePackageType;
import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.CareServiceMapper;
import com.capstone_project.elderly_platform.mappers.CareSeekerProfileMapper;
import com.capstone_project.elderly_platform.mappers.CaregiverProfileMapper;
import com.capstone_project.elderly_platform.mappers.ElderlyProfileMapper;
import com.capstone_project.elderly_platform.mappers.ServicePackageMapper;
import com.capstone_project.elderly_platform.pojos.*;
import com.capstone_project.elderly_platform.repositories.*;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import com.capstone_project.elderly_platform.utils.StringUtils;
import com.capstone_project.elderly_platform.services.ExpiredCareServiceQueueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CareServiceStatusLogRepository careServiceStatusLogRepository;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;
    private final CareServiceMapper careServiceMapper;
    private final ElderlyProfileMapper elderlyProfileMapper;
    private final CareSeekerProfileMapper careSeekerProfileMapper;
    private final CaregiverProfileMapper caregiverProfileMapper;
    private final ServicePackageMapper servicePackageMapper;
    private final NotificationService notificationService;
    private final ExpiredCareServiceQueueService expiredCareServiceQueueService;

    @Transactional
    @Override
    public CareServiceResponseDTO createCareService(CreateCareServiceRequest request) {
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

        // create snapshot using DTOs
        CareServiceSnapshot snapshot = CareServiceSnapshot.builder()
                .elderlyProfile(elderlyProfileMapper.toDTO(elderlyProfile))
                .careSeekerProfile(careSeekerProfileMapper.toDTO(careSeekerProfile))
                .caregiverProfile(caregiverProfileMapper.toDTO(caregiverProfile))
                .servicePackage(servicePackageMapper.toDTO(servicePackage))
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

        CareService savedCareService = careServiceRepository.save(careService);

        // Schedule expiration in Redis queue
        try {
            expiredCareServiceQueueService.scheduleExpiration(
                    savedCareService.getCareServiceId(),
                    caregiverResponseDeadline);
            log.info("Scheduled expiration for care service {} at {}",
                    savedCareService.getCareServiceId(), caregiverResponseDeadline);
        } catch (Exception e) {
            log.error("Failed to schedule expiration for care service {}: {}",
                    savedCareService.getCareServiceId(), e.getMessage(), e);
            // Don't throw exception - care service is already saved, expiration can be
            // handled manually
        }

        return careServiceMapper.toDTO(savedCareService);
    }

    @Override
    public CareServiceResponseDTO acceptCareServiceFromCaregiver(ConfirmationCareServiceRequest request) {
        CareService careService = careServiceRepository
                .findByCareServiceIdAndDeletedIsFalse(request.getCareServiceId());
        if (careService == null) {
            throw new ElementNotFoundException("Care service not found");
        }

        if (!careService.getStatus().equals(EnumCareServiceStatusType.PENDING_CAREGIVER)) {
            throw new BadRequestException("Care service status is not PENDING_CAREGIVER. Current status: "
                    + careService.getStatus());
        }

        // Cancel scheduled expiration from Redis queue
        expiredCareServiceQueueService.cancelExpiration(careService.getCareServiceId());

        CustomAccountDetail currentUser = SecurityUtils.getCurrentUser();
        UUID caregiverAccountId = currentUser.getId();

        CareServiceStatusLog careServiceStatusLog = CareServiceStatusLog.builder()
                .changedBy(EnumActorType.CAREGIVER)
                .careService(careService)
                .oldStatus(careService.getStatus())
                .newStatus(EnumCareServiceStatusType.CAREGIVER_APPROVED)
                .note("Accepted by caregiver with account ID: " + caregiverAccountId + " for care service ID: "
                        + careService.getCareServiceId())
                .build();

        careServiceStatusLogRepository.save(careServiceStatusLog);

        careService.setStatus(EnumCareServiceStatusType.CAREGIVER_APPROVED);
        CareService savedCareService = careServiceRepository.save(careService);

        // Send notification to both parties
        notificationService.sendCareServiceStatusChangeNotification(savedCareService,
                EnumCareServiceStatusType.CAREGIVER_APPROVED.name());

        return careServiceMapper.toDTO(savedCareService);
    }

    @Override
    public CareServiceResponseDTO declineCareService(ConfirmationCareServiceRequest request) {
        CareService careService = careServiceRepository
                .findByCareServiceIdAndDeletedIsFalse(request.getCareServiceId());
        if (careService == null) {
            throw new ElementNotFoundException("Care service not found");
        }

        // Cancel scheduled expiration from Redis queue
        expiredCareServiceQueueService.cancelExpiration(careService.getCareServiceId());

        CustomAccountDetail currentUser = SecurityUtils.getCurrentUser();
        UUID caregiverAccountId = currentUser.getId();

        if (!SecurityUtils.hasRole("ROLE_CAREGIVER") && !SecurityUtils.hasRole("ROLE_CARE_SEEKER")) {
            throw new BadRequestException("Only caregiver or care seeker can decline this service");
        }

        String title = SecurityUtils.hasRole("ROLE_CAREGIVER") ? "caregiver" : "care seeker";

        EnumActorType actorType = SecurityUtils.hasRole("ROLE_CAREGIVER") ? EnumActorType.CAREGIVER
                : EnumActorType.CARE_SEEKER;

        CareServiceStatusLog careServiceStatusLog = CareServiceStatusLog.builder()
                .changedBy(actorType)
                .careService(careService)
                .oldStatus(careService.getStatus())
                .newStatus(EnumCareServiceStatusType.CANCELLED)
                .note("Decline by " + title + " with account ID: " + caregiverAccountId + " for care service ID: "
                        + careService.getCareServiceId())
                .build();

        careServiceStatusLogRepository.save(careServiceStatusLog);

        careService.setStatus(EnumCareServiceStatusType.CANCELLED);
        CareService savedCareService = careServiceRepository.save(careService);

        // Send notification to both parties
        notificationService.sendCareServiceStatusChangeNotification(savedCareService,
                EnumCareServiceStatusType.CANCELLED.name());

        return careServiceMapper.toDTO(savedCareService);
    }

    /*
     * ------------------------------- Private method
     * ---------------------------------
     */

    // Note: checkAndExpireIfNeeded() method removed - expiration is now handled by
    // Redis queue worker
    // No need to check manually as worker processes expired care services
    // automatically

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

    // Inner class for snapshot - using DTOs instead of custom snapshot classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CareServiceSnapshot {
        private ElderlyProfileResponseDTO elderlyProfile;
        private CareSeekerProfileResponseDTO careSeekerProfile;
        private CaregiverProfileResponseDTO caregiverProfile;
        private ServicePackageResponseDTO servicePackage;
    }

}

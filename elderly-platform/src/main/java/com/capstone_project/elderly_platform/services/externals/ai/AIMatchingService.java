package com.capstone_project.elderly_platform.services.externals.ai;

import com.capstone_project.elderly_platform.dtos.request.externals.MatchCaregiverByElderlyRequest;
import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumServicePackageType;
import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.pojos.ElderlyProfile;
import com.capstone_project.elderly_platform.pojos.ServicePackage;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.ElderlyProfileRepository;
import com.capstone_project.elderly_platform.repositories.ServicePackageRepository;
import com.capstone_project.elderly_platform.repositories.ServiceTaskRepository;
import com.capstone_project.elderly_platform.services.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIMatchingService {

    private final RestTemplate restTemplate;
    private final AIMatchingConversionService aiMatchingConversionService;
    private final ElderlyProfileRepository elderlyProfileRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final SystemConfigService systemConfigService;

    @Value("${ai.matching.service.url:http://ai_matching:8000}")
    private String aiServiceUrl;

    /**
     * Match caregivers với request từ mobile (legacy - giữ lại để backward compatibility)
     */
    public Map<String, Object> matchCaregivers(Map<String, Object> request) {
        try {
            String url = aiServiceUrl + "/api/match-mobile";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, headers);

            log.info("Calling AI matching service: {}", url);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    httpEntity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            } else {
                log.error("AI matching service returned error status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get response from AI matching service");
            }

        } catch (Exception e) {
            log.error("Error calling AI matching service: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling AI matching service: " + e.getMessage(), e);
        }
    }

    /**
     * Match caregivers với caregivers list và request
     * Gửi cả caregivers list + request sang AI matching service
     */
    private Map<String, Object> matchCaregiversWithList(
            Map<String, Object> careRequest,
            List<Map<String, Object>> caregivers,
            Integer topN) {
        try {
            String url = aiServiceUrl + "/api/match-from-spring";

            // Build request payload
            Map<String, Object> payload = Map.of(
                    "care_request", careRequest,
                    "candidates", caregivers,
                    "top_n", topN != null ? topN : 10
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(payload, headers);

            log.info("Calling AI matching service with {} caregivers: {}", caregivers.size(), url);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    httpEntity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            } else {
                log.error("AI matching service returned error status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get response from AI matching service");
            }

        } catch (Exception e) {
            log.error("Error calling AI matching service: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling AI matching service: " + e.getMessage(), e);
        }
    }

    /**
     * Match caregivers với elderly profile ID - Main business logic
     * 
     * Flow:
     * 1. Lấy ElderlyProfile từ DB
     * 2. Lấy ServicePackage từ DB
     * 3. Validate thời gian booking (giống createCareService)
     * 4. Lấy tất cả caregivers từ DB
     * 5. Convert ElderlyProfile → requests.json format
     * 6. Convert CaregiverProfile list → caregivers.json format (filter by availability)
     * 7. Gửi cả 2 sang AI matching service
     */
    public Map<String, Object> matchCaregiversByElderly(MatchCaregiverByElderlyRequest request) {
        // 1. Lấy ElderlyProfile từ DB
        ElderlyProfile elderlyProfile = elderlyProfileRepository
                .findByElderlyProfileIdAndDeletedIsFalse(request.getElderlyProfileId());
        if (elderlyProfile == null) {
            throw new ElementNotFoundException("Không tìm thấy hồ sơ người cao tuổi với ID: " + request.getElderlyProfileId());
        }

        // 2. Lấy ServicePackage từ DB
        ServicePackage servicePackage = servicePackageRepository
                .findByServicePackageIdAndDeletedIsFalse(request.getServicePackageId());
        if (servicePackage == null) {
            throw new ElementNotFoundException("Không tìm thấy gói dịch vụ với ID: " + request.getServicePackageId());
        }

        // Check if service package is ACTIVE
        if (servicePackage.getStatus() != EnumActivationStatusType.ACTIVE) {
            throw new BadRequestException("Gói dịch vụ không ở trạng thái hoạt động");
        }

        // 3. Validate startHour và startMinute (giống createCareService)
        Integer startHour = request.getStartHour();
        Integer startMinute = request.getStartMinute();

        if (startHour == null) {
            throw new BadRequestException("Bạn phải cung cấp giờ bắt đầu");
        }
        if (startMinute == null) {
            throw new BadRequestException("Bạn phải cung cấp phút bắt đầu");
        }
        if (startHour < 0 || startHour > 23) {
            throw new BadRequestException("Giờ bắt đầu phải từ 0 đến 23");
        }
        if (startMinute < 0 || startMinute > 59) {
            throw new BadRequestException("Phút bắt đầu phải từ 0 đến 59");
        }

        LocalTime startTime = LocalTime.of(startHour, startMinute);

        if (servicePackage.getDurationHours() == null) {
            throw new BadRequestException("Cần có số giờ thời lượng của gói dịch vụ");
        }
        LocalTime endTime = startTime.plusHours(servicePackage.getDurationHours());

        // Validate không được làm việc qua ngày hôm sau
        if (endTime.isBefore(startTime)
                || (endTime.equals(LocalTime.MIDNIGHT) && !startTime.equals(LocalTime.MIDNIGHT))) {
            throw new BadRequestException(
                    String.format(
                            "Gói dịch vụ có thời lượng %d giờ không thể bắt đầu lúc %s vì sẽ làm việc qua ngày hôm sau. Vui lòng chọn thời gian bắt đầu sớm hơn hoặc chọn gói dịch vụ có thời lượng ngắn hơn.",
                            servicePackage.getDurationHours(), startTime));
        }

        // Validate minimum advance booking time (giống createCareService)
        Map<EnumSystemConfigKey, String> activeConfigs = systemConfigService.getAllActiveConfigs();
        validateMinimumAdvanceBookingTime(request.getWorkDate(), startTime, servicePackage, activeConfigs);

        // 4. Load service tasks cho service package
        List<com.capstone_project.elderly_platform.pojos.ServiceTask> tasks = serviceTaskRepository.findAll()
                .stream()
                .filter(task -> !task.isDeleted()
                        && task.getServicePackage() != null
                        && task.getServicePackage().getServicePackageId().equals(servicePackage.getServicePackageId()))
                .toList();
        servicePackage.setServiceTasks(tasks);

        // 5. Lấy tất cả caregivers từ DB
        List<com.capstone_project.elderly_platform.pojos.CaregiverProfile> allCaregivers = 
                caregiverProfileRepository.findByDeletedFalse();
        
        log.info("Total caregivers from DB (not deleted): {}", allCaregivers.size());

        // 6. Convert ElderlyProfile → requests.json format
        Map<String, Object> careRequest = aiMatchingConversionService.convertElderlyToRequestFormat(
                elderlyProfile,
                servicePackage,
                request.getWorkDate(),
                startTime,
                endTime);

        // 7. Convert CaregiverProfile list → caregivers.json format (filter by deleted and status only)
        // Note: Availability filtering is handled by AI matching service
        List<Map<String, Object>> caregivers = aiMatchingConversionService.convertCaregiversToFormat(
                allCaregivers,
                request.getWorkDate(),
                startTime,
                endTime);

        log.info("Sending {} caregivers to AI matching service", caregivers.size());

        // 8. Gửi cả caregivers list + request sang AI matching service
        return matchCaregiversWithList(
                careRequest,
                caregivers,
                request.getTopN() != null ? request.getTopN() : 10);
    }

    /**
     * Validate minimum advance booking time (giống createCareService)
     */
    private void validateMinimumAdvanceBookingTime(
            java.time.LocalDate workDate,
            LocalTime startTime,
            ServicePackage servicePackage,
            Map<EnumSystemConfigKey, String> activeConfigs) {
        
        if (servicePackage.getPackageType() == null) {
            throw new BadRequestException("Cần có loại gói dịch vụ");
        }

        int minimumAdvanceHours = getMinimumAdvanceHours(servicePackage.getPackageType(), activeConfigs);

        LocalDateTime bookingDateTime = LocalDateTime.of(workDate, startTime);
        LocalDateTime now = LocalDateTime.now();

        long hoursUntilBooking = java.time.Duration.between(now, bookingDateTime).toHours();

        if (hoursUntilBooking < minimumAdvanceHours) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            String formattedBookingDateTime = bookingDateTime.format(formatter);
            String formattedNow = now.format(formatter);
            throw new BadRequestException(
                    String.format(
                            "Bạn phải đặt lịch trước ít nhất %d giờ. Thời gian đặt lịch: %s, Thời gian hiện tại: %s",
                            minimumAdvanceHours,
                            formattedBookingDateTime,
                            formattedNow));
        }
    }

    /**
     * Get minimum advance hours from config snapshot based on package type (giống createCareService)
     */
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
}



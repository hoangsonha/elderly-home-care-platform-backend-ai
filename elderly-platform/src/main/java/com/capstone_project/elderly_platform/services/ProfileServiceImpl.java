package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateCareSeekerProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCareSeekerProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverQualificationsRequest;
import com.capstone_project.elderly_platform.dtos.request.CaregiverProfileVerificationRequest;
import com.capstone_project.elderly_platform.dtos.request.QualificationVerificationRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileDetailResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CaregiverVerificationResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileDetailResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumGenderType;
import com.capstone_project.elderly_platform.enums.EnumHealthStatusType;
import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.enums.EnumVerificationStatusType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.CaregiverProfileMapper;
import com.capstone_project.elderly_platform.mappers.CareSeekerProfileMapper;
import com.capstone_project.elderly_platform.mappers.ElderlyProfileMapper;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.ElderlyProfile;
import com.capstone_project.elderly_platform.pojos.Qualification;
import com.capstone_project.elderly_platform.pojos.QualificationType;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.CareSeekerProfileRepository;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.ElderlyProfileRepository;
import com.capstone_project.elderly_platform.repositories.PayoutBatchRepository;
import com.capstone_project.elderly_platform.repositories.QualificationRepository;
import com.capstone_project.elderly_platform.repositories.QualificationTypeRepository;
import com.capstone_project.elderly_platform.repositories.WorkScheduleRepository;
import com.capstone_project.elderly_platform.repositories.WorkTaskRepository;
import com.capstone_project.elderly_platform.services.externals.firebase.FirebaseStorageService;
import com.capstone_project.elderly_platform.utils.CaregiverScheduleUtils;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final ElderlyProfileRepository elderlyProfileRepository;
    private final CareSeekerProfileRepository careSeekerProfileRepository;
    private final AccountRepository accountRepository;
    private final QualificationRepository qualificationRepository;
    private final QualificationTypeRepository qualificationTypeRepository;
    private final CareServiceRepository careServiceRepository;
    private final PayoutBatchRepository payoutBatchRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final WorkTaskRepository workTaskRepository;
    private final CaregiverProfileMapper caregiverProfileMapper;
    private final CareSeekerProfileMapper careSeekerProfileMapper;
    private final ElderlyProfileMapper elderlyProfileMapper;
    private final FirebaseStorageService firebaseStorageService;
    private final CaregiverScheduleUtils caregiverScheduleUtils;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public List<CaregiverProfileResponseDTO> getAllCaregivers() {
        log.info("Getting all caregivers");
        return caregiverProfileRepository.findByDeletedFalse()
                .stream()
                .map(caregiverProfileMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ElderlyProfileResponseDTO> getElderlyProfilesByCurrentCareSeeker() {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Getting elderly profiles for care seeker with account ID: {}", currentAccountId);

        CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);

        if (careSeekerProfile == null) {
            throw new ElementNotFoundException("Care seeker profile not found for current user");
        }

        List<ElderlyProfileResponseDTO> elderlyProfiles = elderlyProfileRepository
                .findByCareSeekerProfile_CareSeekerProfileIdAndDeletedFalse(careSeekerProfile.getCareSeekerProfileId())
                .stream()
                .map(elderlyProfileMapper::toDTO)
                .collect(Collectors.toList());

        log.info("Found {} elderly profiles for care seeker {}", elderlyProfiles.size(), currentAccountId);
        return elderlyProfiles;
    }

    @Override
    @Transactional
    public ElderlyProfileResponseDTO createElderlyProfile(CreateElderlyProfileRequest request,
            MultipartFile avatarFile) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Creating elderly profile for care seeker with account ID: {}", currentAccountId);

        // Get care seeker profile
        CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);

        if (careSeekerProfile == null) {
            throw new ElementNotFoundException("Care seeker profile not found for current user");
        }

        // Upload avatar to Firebase if provided
        String avatarUrl = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                avatarUrl = firebaseStorageService.uploadSingleImages(avatarFile);
                log.info("Avatar uploaded successfully: {}", avatarUrl);
            } catch (Exception e) {
                log.error("Failed to upload avatar: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
            }
        }

        // Convert location to JSON string
        String locationJson = null;
        if (request.getLocation() != null) {
            try {
                Map<String, Object> locationMap = new HashMap<>();
                locationMap.put("address", request.getLocation().getAddress());
                locationMap.put("latitude", request.getLocation().getLatitude());
                locationMap.put("longitude", request.getLocation().getLongitude());
                locationJson = objectMapper.writeValueAsString(locationMap);
            } catch (Exception e) {
                log.error("Failed to convert location to JSON: {}", e.getMessage(), e);
                throw new BadRequestException("Invalid location data");
            }
        }

        // Build profileData JSON with all fields not in entity
        Map<String, Object> profileDataMap = new HashMap<>();

        // Create LocalDate from birthYear if provided (age will be calculated in
        // mapper)
        java.time.LocalDate birthDate = null;
        if (request.getBirthYear() != null) {
            // Create LocalDate from birthYear (set to January 1st of that year)
            birthDate = java.time.LocalDate.of(request.getBirthYear(), 1, 1);
            // Note: age is not stored in profileData, it will be calculated from birthDate
            // in mapper
        }

        if (request.getWeight() != null) {
            profileDataMap.put("weight", request.getWeight());
        }
        if (request.getHeight() != null) {
            profileDataMap.put("height", request.getHeight());
        }
        
        // Add medical conditions to profileData
        if (request.getMedicalConditions() != null) {
            Map<String, Object> medicalConditionsMap = new HashMap<>();
            if (request.getMedicalConditions().getUnderlyingDiseases() != null) {
                medicalConditionsMap.put("underlying_diseases", request.getMedicalConditions().getUnderlyingDiseases());
            }
            if (request.getMedicalConditions().getSpecialConditions() != null) {
                medicalConditionsMap.put("special_conditions", request.getMedicalConditions().getSpecialConditions());
            }
            if (request.getMedicalConditions().getAllergies() != null) {
                medicalConditionsMap.put("allergies", request.getMedicalConditions().getAllergies());
            }
            if (request.getMedicalConditions().getMedications() != null) {
                medicalConditionsMap.put("medications", request.getMedicalConditions().getMedications());
            }
            if (!medicalConditionsMap.isEmpty()) {
                profileDataMap.put("medical_conditions", medicalConditionsMap);
            }
        }
        
        // Add independence level to profileData
        if (request.getIndependenceLevel() != null && !request.getIndependenceLevel().isEmpty()) {
            Map<String, String> independenceLevelMap = new HashMap<>();
            for (CreateElderlyProfileRequest.IndependenceActivity activity : request.getIndependenceLevel()) {
                independenceLevelMap.put(activity.getActivity(), activity.getLevel());
            }
            profileDataMap.put("independence_level", independenceLevelMap);
        }
        
        if (request.getHobbies() != null) {
            profileDataMap.put("hobbies", request.getHobbies());
        }
        if (request.getFavoriteActivities() != null) {
            profileDataMap.put("favorite_activities", request.getFavoriteActivities());
        }

        if (request.getFavoriteFood() != null) {
            profileDataMap.put("favorite_food", request.getFavoriteFood());
        }

        if (request.getEmergencyContacts() != null && !request.getEmergencyContacts().isEmpty()) {
            List<Map<String, Object>> emergencyContactsList = request.getEmergencyContacts().stream()
                    .map(contact -> {
                        Map<String, Object> contactMap = new HashMap<>();
                        contactMap.put("name", contact.getName());
                        contactMap.put("relationship", contact.getRelationship());
                        contactMap.put("phone", contact.getPhone());
                        return contactMap;
                    })
                    .collect(Collectors.toList());
            profileDataMap.put("emergency_contacts", emergencyContactsList);
        }

        // Convert profileData to JSON string
        String profileDataJson = null;
        if (!profileDataMap.isEmpty()) {
            try {
                profileDataJson = objectMapper.writeValueAsString(profileDataMap);
            } catch (Exception e) {
                log.error("Failed to convert profileData to JSON: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to process profile data");
            }
        }

        // Build careRequirement JSON from careNeeds
        String careRequirementJson = null;
        if (request.getCareNeeds() != null) {
            try {
                Map<String, Object> careRequirementMap = new HashMap<>();
                if (request.getCareNeeds().getLevelOfCare() != null) {
                    careRequirementMap.put("level_of_care", request.getCareNeeds().getLevelOfCare());
                }
                if (request.getCareNeeds().getSkills() != null) {
                    Map<String, Object> skillsMap = new HashMap<>();
                    if (request.getCareNeeds().getSkills().getRequiredSkills() != null) {
                        skillsMap.put("kĩ năng bắt buộc", request.getCareNeeds().getSkills().getRequiredSkills());
                    }
                    if (request.getCareNeeds().getSkills().getPrioritySkills() != null) {
                        skillsMap.put("kĩ năng ưu tiên", request.getCareNeeds().getSkills().getPrioritySkills());
                    }
                    if (!skillsMap.isEmpty()) {
                        careRequirementMap.put("skills", skillsMap);
                    }
                }
                if (request.getCareNeeds().getAge() != null && !request.getCareNeeds().getAge().isEmpty()) {
                    careRequirementMap.put("age", request.getCareNeeds().getAge());
                }
                if (request.getCareNeeds().getGender() != null) {
                    careRequirementMap.put("gender", request.getCareNeeds().getGender());
                }
                if (request.getCareNeeds().getExperience() != null) {
                    careRequirementMap.put("experience", request.getCareNeeds().getExperience());
                }
                if (request.getCareNeeds().getRating() != null) {
                    careRequirementMap.put("rating", request.getCareNeeds().getRating());
                }
                if (!careRequirementMap.isEmpty()) {
                    careRequirementJson = objectMapper.writeValueAsString(careRequirementMap);
                }
            } catch (Exception e) {
                log.error("Failed to convert careRequirement to JSON: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to process care requirement data");
            }
        }

        // Convert gender string to enum
        EnumGenderType gender;
        try {
            gender = EnumGenderType.valueOf(request.getGender().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid gender: " + request.getGender());
        }

        // Convert healthStatus string to enum
        EnumHealthStatusType healthStatus = EnumHealthStatusType.GOOD; // Default value
        if (request.getHealthStatus() != null && !request.getHealthStatus().isEmpty()) {
            try {
                healthStatus = EnumHealthStatusType.valueOf(request.getHealthStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid health status: " + request.getHealthStatus() + ". Valid values: GOOD, WEAK, MODERATE");
            }
        }

        // Create ElderlyProfile entity
        ElderlyProfile elderlyProfile = ElderlyProfile.builder()
                .fullName(request.getName())
                .birthDate(birthDate)
                .location(locationJson)
                .gender(gender)
                .avatarUrl(avatarUrl)
                .profileData(profileDataJson)
                .careRequirement(careRequirementJson)
                .status(EnumActivationStatusType.ACTIVE)
                .healthStatus(healthStatus)
                .healthNote(request.getHealthNote())
                .note(request.getNote())
                .careSeekerProfile(careSeekerProfile)
                .build();

        // Save to database
        ElderlyProfile savedProfile = elderlyProfileRepository.save(elderlyProfile);
        log.info("Elderly profile created successfully with ID: {}", savedProfile.getElderlyProfileId());

        // Convert to DTO and return
        return elderlyProfileMapper.toDTO(savedProfile);
    }

    @Override
    @Transactional
    public CareSeekerProfileResponseDTO createCareSeekerProfile(CreateCareSeekerProfileRequest request,
            MultipartFile avatarFile) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Creating care seeker profile for account ID: {}", currentAccountId);

        // Get account
        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(currentAccountId)
                .orElseThrow(() -> new ElementNotFoundException("Account not found for current user"));

        // Check if profile already exists
        CareSeekerProfile existingProfile = careSeekerProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
        if (existingProfile != null) {
            throw new BadRequestException("Care seeker profile already exists for this account");
        }

        // Upload avatar to Firebase if provided
        String avatarUrl = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                avatarUrl = firebaseStorageService.uploadSingleImages(avatarFile);
                log.info("Avatar uploaded successfully: {}", avatarUrl);
                
                // Update avatarUrl in Account
                account.setAvatarUrl(avatarUrl);
                accountRepository.save(account);
            } catch (Exception e) {
                log.error("Failed to upload avatar: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
            }
        }

        // Convert location to JSON string
        String locationJson = null;
        if (request.getLocation() != null) {
            try {
                Map<String, Object> locationMap = new HashMap<>();
                locationMap.put("address", request.getLocation().getAddress());
                locationMap.put("latitude", request.getLocation().getLatitude());
                locationMap.put("longitude", request.getLocation().getLongitude());
                locationJson = objectMapper.writeValueAsString(locationMap);
            } catch (Exception e) {
                log.error("Failed to convert location to JSON: {}", e.getMessage(), e);
                throw new BadRequestException("Invalid location data");
            }
        }

        // Convert gender string to enum
        EnumGenderType gender;
        try {
            gender = EnumGenderType.valueOf(request.getGender().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid gender: " + request.getGender());
        }

        // Create LocalDate from birthYear (set to January 1st of that year)
        // Age will be calculated from birthDate in mapper
        java.time.LocalDate birthDate = null;
        if (request.getBirthYear() != null) {
            birthDate = java.time.LocalDate.of(request.getBirthYear(), 1, 1);
        }

        // Create CareSeekerProfile entity
        CareSeekerProfile careSeekerProfile = CareSeekerProfile.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhone())
                .birthDate(birthDate)
                .gender(gender)
                .location(locationJson)
                .account(account)
                .build();

        // Save to database
        CareSeekerProfile savedProfile = careSeekerProfileRepository.save(careSeekerProfile);
        log.info("Care seeker profile created successfully with ID: {}", savedProfile.getCareSeekerProfileId());

        // Convert to DTO and return
        return careSeekerProfileMapper.toDTO(savedProfile);
    }

    @Override
    @Transactional
    public CareSeekerProfileResponseDTO updateCareSeekerProfile(UpdateCareSeekerProfileRequest request,
            MultipartFile avatarFile) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Updating care seeker profile for account ID: {}", currentAccountId);

        // Get care seeker profile
        CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);

        if (careSeekerProfile == null) {
            throw new ElementNotFoundException("Care seeker profile not found for current user");
        }

        Account account = careSeekerProfile.getAccount();
        if (account == null) {
            throw new ElementNotFoundException("Account not found for care seeker profile");
        }

        // 1. Update basic fields
        if (request.getFullName() != null) {
            careSeekerProfile.setFullName(request.getFullName());
        }

        if (request.getBirthYear() != null) {
            careSeekerProfile.setBirthDate(LocalDate.of(request.getBirthYear(), 1, 1));
        }

        if (request.getGender() != null) {
            try {
                careSeekerProfile.setGender(EnumGenderType.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid gender: " + request.getGender());
            }
        }

        if (request.getPhone() != null) {
            careSeekerProfile.setPhoneNumber(request.getPhone());
        }

        // 2. Upload avatar and update Account
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String avatarUrl = firebaseStorageService.uploadSingleImages(avatarFile);
                log.info("Avatar uploaded successfully: {}", avatarUrl);
                account.setAvatarUrl(avatarUrl);
                accountRepository.save(account);
            } catch (Exception e) {
                log.error("Failed to upload avatar: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
            }
        }

        // 3. Update location JSON
        if (request.getLocation() != null) {
            try {
                Map<String, Object> locationMap = new HashMap<>();
                locationMap.put("address", request.getLocation().getAddress());
                locationMap.put("latitude", request.getLocation().getLatitude());
                locationMap.put("longitude", request.getLocation().getLongitude());
                careSeekerProfile.setLocation(objectMapper.writeValueAsString(locationMap));
            } catch (Exception e) {
                log.error("Failed to update location: {}", e.getMessage(), e);
                throw new BadRequestException("Invalid location data");
            }
        }

        // Save to database
        CareSeekerProfile savedProfile = careSeekerProfileRepository.save(careSeekerProfile);
        log.info("Care seeker profile updated successfully with ID: {}", savedProfile.getCareSeekerProfileId());

        // Convert to DTO and return
        return careSeekerProfileMapper.toDTO(savedProfile);
    }

    @Override
    @Transactional
    public CaregiverProfileResponseDTO createCaregiverProfile(UpdateCaregiverProfileRequest request,
            MultipartFile avatarFile,
            List<MultipartFile> credentialFiles,
            MultipartFile citizenIdFrontImage,
            MultipartFile citizenIdBackImage) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Creating caregiver profile for account ID: {}", currentAccountId);

        // Get account
        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(currentAccountId)
                .orElseThrow(() -> new ElementNotFoundException("Account not found for current user"));

        // Check if profile already exists (including deleted = false check)
        CaregiverProfile existingProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
        if (existingProfile != null) {
            log.warn("Attempt to create caregiver profile failed: Profile already exists for account ID: {}",
                    currentAccountId);
            throw new BadRequestException(
                    "Caregiver profile already exists for this account. Please use update API instead.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 0. Validate required CCCD/CMND fields
        if (request.getCitizenId() == null || request.getCitizenId().trim().isEmpty()) {
            throw new BadRequestException("Số CCCD/CMND là bắt buộc");
        }
        if (citizenIdFrontImage == null || citizenIdFrontImage.isEmpty()) {
            throw new BadRequestException("Ảnh mặt trước CCCD/CMND là bắt buộc");
        }
        if (citizenIdBackImage == null || citizenIdBackImage.isEmpty()) {
            throw new BadRequestException("Ảnh mặt sau CCCD/CMND là bắt buộc");
        }

        // 1. Upload avatar and update Account
        String avatarUrl = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                avatarUrl = firebaseStorageService.uploadSingleImages(avatarFile);
                log.info("Avatar uploaded successfully: {}", avatarUrl);
                account.setAvatarUrl(avatarUrl);
                accountRepository.save(account);
            } catch (Exception e) {
                log.error("Failed to upload avatar: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
            }
        }

        // 2. Convert gender string to enum
        EnumGenderType gender = null;
        if (request.getGender() != null) {
            try {
                gender = EnumGenderType.valueOf(request.getGender().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid gender: " + request.getGender());
            }
        }

        // 3. Create LocalDate from birthYear
        LocalDate birthDate = null;
        if (request.getBirthYear() != null) {
            birthDate = LocalDate.of(request.getBirthYear(), 1, 1);
        }

        // 4. Build location JSON (with service_radius_km)
        String locationJson = null;
        if (request.getLocation() != null || request.getServiceRadiusKm() != null) {
            try {
                Map<String, Object> locationMap = new HashMap<>();

                if (request.getLocation() != null) {
                    locationMap.put("address", request.getLocation().getAddress());
                    locationMap.put("latitude", request.getLocation().getLatitude());
                    locationMap.put("longitude", request.getLocation().getLongitude());
                }

                // Add service_radius_km
                if (request.getServiceRadiusKm() != null) {
                    locationMap.put("service_radius_km", request.getServiceRadiusKm());
                }

                locationJson = objectMapper.writeValueAsString(locationMap);
            } catch (Exception e) {
                log.error("Failed to convert location to JSON: {}", e.getMessage(), e);
                throw new BadRequestException("Invalid location data");
            }
        }

        // 5. Upload CCCD/CMND images and get URLs
        String citizenIdFrontImageUrl = null;
        String citizenIdBackImageUrl = null;
        
        if (citizenIdFrontImage != null && !citizenIdFrontImage.isEmpty()) {
            try {
                citizenIdFrontImageUrl = firebaseStorageService.uploadSingleImages(citizenIdFrontImage);
                log.info("CCCD/CMND front image uploaded successfully: {}", citizenIdFrontImageUrl);
            } catch (Exception e) {
                log.error("Failed to upload CCCD/CMND front image: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload CCCD/CMND front image: " + e.getMessage());
            }
        }
        
        if (citizenIdBackImage != null && !citizenIdBackImage.isEmpty()) {
            try {
                citizenIdBackImageUrl = firebaseStorageService.uploadSingleImages(citizenIdBackImage);
                log.info("CCCD/CMND back image uploaded successfully: {}", citizenIdBackImageUrl);
            } catch (Exception e) {
                log.error("Failed to upload CCCD/CMND back image: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload CCCD/CMND back image: " + e.getMessage());
            }
        }

        // 6. Build profileData JSON (years_experience, free_schedule,
        // max_hours_per_week, preferences, citizen_id)
        Map<String, Object> profileDataMap = new HashMap<>();

        if (request.getYearsExperience() != null) {
            profileDataMap.put("years_experience", request.getYearsExperience());
        }
        
        // Add citizen ID information
        if (request.getCitizenId() != null && !request.getCitizenId().trim().isEmpty()) {
            profileDataMap.put("citizen_id", request.getCitizenId());
        }
        if (citizenIdFrontImageUrl != null) {
            profileDataMap.put("citizen_id_front_image_url", citizenIdFrontImageUrl);
        }
        if (citizenIdBackImageUrl != null) {
            profileDataMap.put("citizen_id_back_image_url", citizenIdBackImageUrl);
        }

        // Handle free_schedule
        if (request.getFreeSchedule() != null) {
            Map<String, Object> freeScheduleMap = new HashMap<>();
            if (request.getFreeSchedule().getAvailableAllTime() != null) {
                freeScheduleMap.put("available_all_time", request.getFreeSchedule().getAvailableAllTime());
            }

            if (request.getFreeSchedule().getBookedSlots() != null) {
                List<Map<String, Object>> bookedSlotsList = new ArrayList<>();
                for (UpdateCaregiverProfileRequest.BookedSlotRequest slot : request.getFreeSchedule()
                        .getBookedSlots()) {
                    // Validate slot data
                    if (slot.getDate() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
                        throw new BadRequestException("Booked slot phải có đầy đủ date, start_time và end_time");
                    }

                    // Parse times to validate
                    LocalTime startTime;
                    LocalTime endTime;
                    try {
                        startTime = LocalTime.parse(slot.getStartTime());
                        endTime = LocalTime.parse(slot.getEndTime());
                    } catch (Exception e) {
                        throw new BadRequestException("start_time và end_time phải có format HH:mm (ví dụ: 08:00, 14:30)");
                    }

                    // Validate start_time < end_time
                    if (!startTime.isBefore(endTime)) {
                        throw new BadRequestException(
                                String.format("start_time (%s) phải nhỏ hơn end_time (%s) cho slot ngày %s",
                                        slot.getStartTime(), slot.getEndTime(), slot.getDate()));
                    }

                    Map<String, Object> slotMap = new HashMap<>();
                    slotMap.put("date", slot.getDate());
                    slotMap.put("start_time", slot.getStartTime());
                    slotMap.put("end_time", slot.getEndTime());
                    bookedSlotsList.add(slotMap);
                }
                freeScheduleMap.put("booked_slots", bookedSlotsList);
            }

            if (!freeScheduleMap.isEmpty()) {
                profileDataMap.put("free_schedule", freeScheduleMap);
            }
        }

        // Validate and add max_hours_per_week
        if (request.getMaxHoursPerWeek() != null) {
            int maxAllowedHours = systemConfigService.getConfigValueAsInt(
                    EnumSystemConfigKey.CAREGIVER_MAX_HOURS_PER_WEEK, 48);
            if (request.getMaxHoursPerWeek() > maxAllowedHours) {
                throw new BadRequestException("Max hours per week cannot exceed " + maxAllowedHours);
            }
            profileDataMap.put("max_hours_per_week", request.getMaxHoursPerWeek());
        }

        // Handle preferences
        if (request.getPreferences() != null) {
            Map<String, Object> preferencesMap = new HashMap<>();

            if (request.getPreferences().getPreferredHealthStatus() != null) {
                preferencesMap.put("preferred_health_status", request.getPreferences().getPreferredHealthStatus());
            }

            if (request.getPreferences().getElderlyAgePreference() != null) {
                Map<String, Object> ageRangeMap = new HashMap<>();
                if (request.getPreferences().getElderlyAgePreference().getMinAge() != null) {
                    ageRangeMap.put("min_age", request.getPreferences().getElderlyAgePreference().getMinAge());
                }
                if (request.getPreferences().getElderlyAgePreference().getMaxAge() != null) {
                    ageRangeMap.put("max_age", request.getPreferences().getElderlyAgePreference().getMaxAge());
                }
                if (!ageRangeMap.isEmpty()) {
                    preferencesMap.put("elderly_age_preference", ageRangeMap);
                }
            }

            if (!preferencesMap.isEmpty()) {
                profileDataMap.put("preferences", preferencesMap);
            }
        }

        // Add default ratings_reviews for new caregiver profile
        Map<String, Object> ratingsReviewsMap = new HashMap<>();
        ratingsReviewsMap.put("overall_rating", 0);
        ratingsReviewsMap.put("total_reviews", 0);

        Map<String, Integer> ratingBreakdown = new HashMap<>();
        ratingBreakdown.put("5_star", 0);
        ratingBreakdown.put("4_star", 0);
        ratingBreakdown.put("3_star", 0);
        ratingBreakdown.put("2_star", 0);
        ratingBreakdown.put("1_star", 0);
        ratingsReviewsMap.put("rating_breakdown", ratingBreakdown);

        // Add default detailed_ratings_breakdown
        Map<String, Double> detailedRatingsBreakdown = new HashMap<>();
        detailedRatingsBreakdown.put("professionalism", 0.0);
        detailedRatingsBreakdown.put("attitude", 0.0);
        detailedRatingsBreakdown.put("punctuality", 0.0);
        detailedRatingsBreakdown.put("quality", 0.0);
        ratingsReviewsMap.put("detailed_ratings_breakdown", detailedRatingsBreakdown);

        profileDataMap.put("ratings_reviews", ratingsReviewsMap);

        String profileDataJson = null;
        if (!profileDataMap.isEmpty()) {
            try {
                profileDataJson = objectMapper.writeValueAsString(profileDataMap);
            } catch (Exception e) {
                log.error("Failed to convert profileData to JSON: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to process profile data");
            }
        }

        // 7. Create CaregiverProfile entity
        // Set is_needed_review_certificate = true if there are credentials
        boolean hasCredentials = request.getCredentials() != null && !request.getCredentials().isEmpty();

        CaregiverProfile caregiverProfile = CaregiverProfile.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhone())
                .birthDate(birthDate)
                .gender(gender)
                .location(locationJson)
                .bio(request.getBio())
                .isVerified(false) // Default false
                .status(EnumVerificationStatusType.PENDING) // Default PENDING
                .isNeededReviewCertificate(hasCredentials) // true if has credentials
                .profileData(profileDataJson)
                .account(account)
                .build();

        caregiverProfile.setCreatedAt(now);
        caregiverProfile.setUpdatedAt(now);
        caregiverProfile.setDeleted(false);

        // Save caregiver profile first
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        log.info("Caregiver profile created successfully with ID: {}", savedProfile.getCaregiverProfileId());

        // 8. Handle credentials
        if (request.getCredentials() != null && !request.getCredentials().isEmpty()) {
            // Validate credential files
            if (credentialFiles == null || credentialFiles.size() != request.getCredentials().size()) {
                throw new BadRequestException("Number of credential files must match number of credentials. Expected: "
                        + request.getCredentials().size() + ", got: "
                        + (credentialFiles != null ? credentialFiles.size() : 0));
            }

            // Create qualifications
            for (int i = 0; i < request.getCredentials().size(); i++) {
                UpdateCaregiverProfileRequest.CredentialRequest credRequest = request.getCredentials().get(i);
                MultipartFile credentialFile = credentialFiles.get(i);

                // Validate qualification type
                QualificationType qualificationType = qualificationTypeRepository
                        .findByQualificationTypeIdAndDeletedIsFalse(credRequest.getQualificationTypeId());
                if (qualificationType == null) {
                    throw new BadRequestException(
                            "Qualification type not found: " + credRequest.getQualificationTypeId());
                }

                // Upload certificate file (required for each credential)
                String certificateUrl = null;
                if (credentialFile != null && !credentialFile.isEmpty()) {
                    try {
                        certificateUrl = firebaseStorageService.uploadFile(credentialFile);
                        log.info("Credential file uploaded successfully: {}", certificateUrl);
                    } catch (Exception e) {
                        log.error("Failed to upload credential file: {}", e.getMessage(), e);
                        throw new BadRequestException("Failed to upload credential file: " + e.getMessage());
                    }
                } else {
                    throw new BadRequestException("Credential file is required for credential at index " + i);
                }

                // Create Qualification
                Qualification qualification = Qualification.builder()
                        .caregiverProfile(savedProfile)
                        .qualificationType(qualificationType)
                        .certificateNumber(credRequest.getCertificateNumber())
                        .issuingOrganization(credRequest.getIssuingOrganization())
                        .issueDate(credRequest.getIssueDate())
                        .expiryDate(credRequest.getExpiryDate())
                        .certificateUrl(certificateUrl)
                        .isVerified(false) // Default false
                        .status(EnumVerificationStatusType.PENDING) // Default PENDING
                        .notes(credRequest.getNotes())
                        .build();

                qualification.setCreatedAt(now);
                qualification.setUpdatedAt(now);
                qualification.setDeleted(false);

                qualificationRepository.save(qualification);
            }
        }

        return caregiverProfileMapper.toDTO(savedProfile);
    }

    @Override
    @Transactional
    public CaregiverProfileResponseDTO updateCaregiverProfile(UpdateCaregiverProfileRequest request,
            MultipartFile avatarFile,
            List<MultipartFile> credentialFiles,
            MultipartFile citizenIdFrontImage,
            MultipartFile citizenIdBackImage) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Updating caregiver profile for account ID: {}", currentAccountId);

        // Get caregiver profile
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);

        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found for current user");
        }

        Account account = caregiverProfile.getAccount();
        if (account == null) {
            throw new ElementNotFoundException("Account not found for caregiver profile");
        }

        // 1. Update basic fields
        if (request.getFullName() != null) {
            caregiverProfile.setFullName(request.getFullName());
        }

        if (request.getBirthYear() != null) {
            caregiverProfile.setBirthDate(LocalDate.of(request.getBirthYear(), 1, 1));
        }

        if (request.getGender() != null) {
            try {
                caregiverProfile.setGender(EnumGenderType.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid gender: " + request.getGender());
            }
        }

        if (request.getPhone() != null) {
            caregiverProfile.setPhoneNumber(request.getPhone());
        }

        if (request.getBio() != null) {
            caregiverProfile.setBio(request.getBio());
        }

        // 2. Upload avatar and update Account
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String avatarUrl = firebaseStorageService.uploadSingleImages(avatarFile);
                log.info("Avatar uploaded successfully: {}", avatarUrl);
                account.setAvatarUrl(avatarUrl);
                accountRepository.save(account);
            } catch (Exception e) {
                log.error("Failed to upload avatar: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
            }
        }

        // 3. Update location JSON (with service_radius_km)
        if (request.getLocation() != null || request.getServiceRadiusKm() != null) {
            try {
                Map<String, Object> locationMap = new HashMap<>();

                // Parse existing location if exists
                if (caregiverProfile.getLocation() != null && !caregiverProfile.getLocation().isEmpty()) {
                    locationMap = objectMapper.readValue(caregiverProfile.getLocation(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                            });
                }

                // Update with new values
                if (request.getLocation() != null) {
                    if (request.getLocation().getAddress() != null) {
                        locationMap.put("address", request.getLocation().getAddress());
                    }
                    if (request.getLocation().getLatitude() != null) {
                        locationMap.put("latitude", request.getLocation().getLatitude());
                    }
                    if (request.getLocation().getLongitude() != null) {
                        locationMap.put("longitude", request.getLocation().getLongitude());
                    }
                }

                // Add service_radius_km
                if (request.getServiceRadiusKm() != null) {
                    locationMap.put("service_radius_km", request.getServiceRadiusKm());
                }

                caregiverProfile.setLocation(objectMapper.writeValueAsString(locationMap));
            } catch (Exception e) {
                log.error("Failed to update location: {}", e.getMessage(), e);
                throw new BadRequestException("Invalid location data");
            }
        }

        // 4. Upload CCCD/CMND images and get URLs
        String citizenIdFrontImageUrl = null;
        String citizenIdBackImageUrl = null;
        
        if (citizenIdFrontImage != null && !citizenIdFrontImage.isEmpty()) {
            try {
                citizenIdFrontImageUrl = firebaseStorageService.uploadSingleImages(citizenIdFrontImage);
                log.info("CCCD/CMND front image uploaded successfully: {}", citizenIdFrontImageUrl);
            } catch (Exception e) {
                log.error("Failed to upload CCCD/CMND front image: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload CCCD/CMND front image: " + e.getMessage());
            }
        }
        
        if (citizenIdBackImage != null && !citizenIdBackImage.isEmpty()) {
            try {
                citizenIdBackImageUrl = firebaseStorageService.uploadSingleImages(citizenIdBackImage);
                log.info("CCCD/CMND back image uploaded successfully: {}", citizenIdBackImageUrl);
            } catch (Exception e) {
                log.error("Failed to upload CCCD/CMND back image: {}", e.getMessage(), e);
                throw new BadRequestException("Failed to upload CCCD/CMND back image: " + e.getMessage());
            }
        }

        // 5. Update profileData JSON (years_experience, free_schedule,
        // max_hours_per_week, preferences, citizen_id)
        String currentProfileData = caregiverProfile.getProfileData();
        currentProfileData = caregiverScheduleUtils.initializeFreeScheduleIfNotExists(currentProfileData);

        try {
            Map<String, Object> profileDataMap = new HashMap<>();

            // Parse existing profileData if exists
            if (currentProfileData != null && !currentProfileData.isEmpty()) {
                profileDataMap = objectMapper.readValue(currentProfileData,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
            }

            // Update years_experience
            if (request.getYearsExperience() != null) {
                profileDataMap.put("years_experience", request.getYearsExperience());
            }
            
            // Update citizen ID information
            if (request.getCitizenId() != null && !request.getCitizenId().trim().isEmpty()) {
                profileDataMap.put("citizen_id", request.getCitizenId());
            }
            if (citizenIdFrontImageUrl != null) {
                profileDataMap.put("citizen_id_front_image_url", citizenIdFrontImageUrl);
            }
            if (citizenIdBackImageUrl != null) {
                profileDataMap.put("citizen_id_back_image_url", citizenIdBackImageUrl);
            }

            // Update free_schedule
            if (request.getFreeSchedule() != null) {
                Map<String, Object> freeScheduleMap = new HashMap<>();
                if (request.getFreeSchedule().getAvailableAllTime() != null) {
                    freeScheduleMap.put("available_all_time", request.getFreeSchedule().getAvailableAllTime());
                }

                if (request.getFreeSchedule().getBookedSlots() != null) {
                    List<Map<String, Object>> bookedSlotsList = new ArrayList<>();
                    for (UpdateCaregiverProfileRequest.BookedSlotRequest slot : request.getFreeSchedule()
                            .getBookedSlots()) {
                        // Validate slot data
                        if (slot.getDate() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
                            throw new BadRequestException("Booked slot phải có đầy đủ date, start_time và end_time");
                        }

                        // Parse times to validate
                        LocalTime startTime;
                        LocalTime endTime;
                        try {
                            startTime = LocalTime.parse(slot.getStartTime());
                            endTime = LocalTime.parse(slot.getEndTime());
                        } catch (Exception e) {
                            throw new BadRequestException("start_time và end_time phải có format HH:mm (ví dụ: 08:00, 14:30)");
                        }

                        // Validate start_time < end_time
                        if (!startTime.isBefore(endTime)) {
                            throw new BadRequestException(
                                    String.format("start_time (%s) phải nhỏ hơn end_time (%s) cho slot ngày %s",
                                            slot.getStartTime(), slot.getEndTime(), slot.getDate()));
                        }

                        Map<String, Object> slotMap = new HashMap<>();
                        slotMap.put("date", slot.getDate());
                        slotMap.put("start_time", slot.getStartTime());
                        slotMap.put("end_time", slot.getEndTime());
                        bookedSlotsList.add(slotMap);
                    }
                    freeScheduleMap.put("booked_slots", bookedSlotsList);
                }

                if (!freeScheduleMap.isEmpty()) {
                    profileDataMap.put("free_schedule", freeScheduleMap);
                }
            }

            // Update max_hours_per_week (validate against config)
            if (request.getMaxHoursPerWeek() != null) {
                int maxAllowedHours = systemConfigService.getConfigValueAsInt(
                        EnumSystemConfigKey.CAREGIVER_MAX_HOURS_PER_WEEK, 48);
                if (request.getMaxHoursPerWeek() > maxAllowedHours) {
                    throw new BadRequestException("Max hours per week cannot exceed " + maxAllowedHours);
                }
                profileDataMap.put("max_hours_per_week", request.getMaxHoursPerWeek());
            }

            // Update preferences
            if (request.getPreferences() != null) {
                Map<String, Object> preferencesMap = new HashMap<>();

                if (request.getPreferences().getPreferredHealthStatus() != null) {
                    preferencesMap.put("preferred_health_status", request.getPreferences().getPreferredHealthStatus());
                }

                if (request.getPreferences().getElderlyAgePreference() != null) {
                    Map<String, Object> ageRangeMap = new HashMap<>();
                    if (request.getPreferences().getElderlyAgePreference().getMinAge() != null) {
                        ageRangeMap.put("min_age", request.getPreferences().getElderlyAgePreference().getMinAge());
                    }
                    if (request.getPreferences().getElderlyAgePreference().getMaxAge() != null) {
                        ageRangeMap.put("max_age", request.getPreferences().getElderlyAgePreference().getMaxAge());
                    }
                    if (!ageRangeMap.isEmpty()) {
                        preferencesMap.put("elderly_age_preference", ageRangeMap);
                    }
                }

                if (!preferencesMap.isEmpty()) {
                    profileDataMap.put("preferences", preferencesMap);
                }
            }

            // Save updated profileData
            caregiverProfile.setProfileData(objectMapper.writeValueAsString(profileDataMap));
        } catch (Exception e) {
            log.error("Failed to update profileData: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update profile data: " + e.getMessage());
        }

        // Note: Credentials are now handled by separate API: PUT /api/v1/caregivers/qualifications

        // Save caregiver profile
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        log.info("Caregiver profile updated successfully with ID: {}", savedProfile.getCaregiverProfileId());

        return caregiverProfileMapper.toDTO(savedProfile);
    }

    @Override
    @Transactional
    public CaregiverProfileResponseDTO updateCaregiverQualifications(UpdateCaregiverQualificationsRequest request,
            List<MultipartFile> credentialFiles) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Updating caregiver qualifications for account ID: {}", currentAccountId);

        // Get caregiver profile
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);

        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found for current user");
        }

        if (request.getQualifications() == null || request.getQualifications().isEmpty()) {
            throw new BadRequestException("Danh sách chứng chỉ không được để trống");
        }

        // Validate credential files
        if (credentialFiles == null || credentialFiles.size() != request.getQualifications().size()) {
            throw new BadRequestException("Number of credential files must match number of qualifications. Expected: "
                    + request.getQualifications().size() + ", got: "
                    + (credentialFiles != null ? credentialFiles.size() : 0));
        }

        LocalDateTime now = LocalDateTime.now();

        // Delete existing qualifications (soft delete)
        List<Qualification> existingQualifications = qualificationRepository
                .findByCaregiverProfile_CaregiverProfileIdAndDeletedIsFalse(
                        caregiverProfile.getCaregiverProfileId());

        for (Qualification existing : existingQualifications) {
            existing.setDeleted(true);
            existing.setUpdatedAt(now);
            qualificationRepository.save(existing);
        }

        // Create new qualifications
        for (int i = 0; i < request.getQualifications().size(); i++) {
            UpdateCaregiverQualificationsRequest.QualificationRequest qualRequest = request.getQualifications().get(i);
            MultipartFile credentialFile = credentialFiles.get(i);

            // Validate qualification type
            QualificationType qualificationType = qualificationTypeRepository
                    .findByQualificationTypeIdAndDeletedIsFalse(qualRequest.getQualificationTypeId());
            if (qualificationType == null) {
                throw new BadRequestException(
                        "Qualification type not found: " + qualRequest.getQualificationTypeId());
            }

            // Upload certificate file (required for each credential)
            String certificateUrl = null;
            if (credentialFile != null && !credentialFile.isEmpty()) {
                try {
                    certificateUrl = firebaseStorageService.uploadFile(credentialFile);
                    log.info("Credential file uploaded successfully: {}", certificateUrl);
                } catch (Exception e) {
                    log.error("Failed to upload credential file: {}", e.getMessage(), e);
                    throw new BadRequestException("Failed to upload credential file: " + e.getMessage());
                }
            } else {
                throw new BadRequestException("Credential file is required for qualification at index " + i);
            }

            // Create Qualification
            Qualification qualification = Qualification.builder()
                    .caregiverProfile(caregiverProfile)
                    .qualificationType(qualificationType)
                    .certificateNumber(qualRequest.getCertificateNumber())
                    .issuingOrganization(qualRequest.getIssuingOrganization())
                    .issueDate(qualRequest.getIssueDate())
                    .expiryDate(qualRequest.getExpiryDate())
                    .certificateUrl(certificateUrl)
                    .isVerified(false) // Default false
                    .status(EnumVerificationStatusType.PENDING) // Default PENDING
                    .notes(qualRequest.getNotes())
                    .build();

            // Set is_needed_review_certificate = true when new credential is added
            caregiverProfile.setIsNeededReviewCertificate(true);

            qualification.setCreatedAt(now);
            qualification.setUpdatedAt(now);
            qualification.setDeleted(false);

            qualificationRepository.save(qualification);
        }

        // Save caregiver profile
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        log.info("Caregiver qualifications updated successfully with ID: {}", savedProfile.getCaregiverProfileId());

        return caregiverProfileMapper.toDTO(savedProfile);
    }

    @Override
    public List<CaregiverVerificationResponseDTO> getPendingVerificationCaregivers() {
        log.info("Getting all caregivers pending verification");

        List<CaregiverProfile> pendingCaregivers = caregiverProfileRepository.findByIsVerifiedFalseAndDeletedFalse();

        return pendingCaregivers.stream()
                .map(this::mapToVerificationDTO)
                .collect(Collectors.toList());
    }

    private CaregiverVerificationResponseDTO mapToVerificationDTO(CaregiverProfile profile) {
        if (profile == null) {
            return null;
        }

        // Calculate age
        Integer age = null;
        if (profile.getBirthDate() != null) {
            age = java.time.Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
        }

        // Map Account info
        String accountId = null;
        String email = null;
        String avatarUrl = null;
        Boolean enabled = null;
        Boolean nonLocked = null;

        if (profile.getAccount() != null) {
            Account account = profile.getAccount();
            accountId = account.getAccountId() != null ? account.getAccountId().toString() : null;
            email = account.getEmail();
            avatarUrl = account.getAvatarUrl();
            enabled = account.getEnabled();
            nonLocked = account.getNonLocked();
        }

        // Map Qualifications
        List<CaregiverVerificationResponseDTO.QualificationResponseDTO> qualifications = new ArrayList<>();
        if (profile.getQualifications() != null) {
            qualifications = profile.getQualifications().stream()
                    .filter(q -> !q.isDeleted())
                    .map(this::mapQualificationToDTO)
                    .collect(Collectors.toList());
        }

        return CaregiverVerificationResponseDTO.builder()
                .caregiverProfileId(profile.getCaregiverProfileId() != null
                        ? profile.getCaregiverProfileId().toString()
                        : null)
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .location(profile.getLocation())
                .bio(profile.getBio())
                .isVerified(profile.getIsVerified())
                .status(profile.getStatus() != null ? profile.getStatus().name() : null)
                .rejectionReason(profile.getRejectionReason())
                .isNeededReviewCertificate(profile.getIsNeededReviewCertificate())
                .acceptedAt(profile.getAcceptedAt() != null
                        ? profile.getAcceptedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .declinedAt(profile.getDeclinedAt() != null
                        ? profile.getDeclinedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .reviewedBy(profile.getReviewedBy() != null ? profile.getReviewedBy().toString() : null)
                .birthDate(profile.getBirthDate() != null
                        ? profile.getBirthDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .age(age)
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .profileData(profile.getProfileData())
                .accountId(accountId)
                .email(email)
                .avatarUrl(avatarUrl)
                .enabled(enabled)
                .nonLocked(nonLocked)
                .qualifications(qualifications)
                .build();
    }

    private CaregiverVerificationResponseDTO.QualificationResponseDTO mapQualificationToDTO(
            Qualification qualification) {
        if (qualification == null) {
            return null;
        }

        String qualificationTypeId = null;
        String qualificationTypeName = null;
        if (qualification.getQualificationType() != null) {
            qualificationTypeId = qualification.getQualificationType().getQualificationTypeId() != null
                    ? qualification.getQualificationType().getQualificationTypeId().toString()
                    : null;
            qualificationTypeName = qualification.getQualificationType().getTypeName();
        }

        return CaregiverVerificationResponseDTO.QualificationResponseDTO.builder()
                .qualificationId(qualification.getQualificationId() != null
                        ? qualification.getQualificationId().toString()
                        : null)
                .qualificationTypeId(qualificationTypeId)
                .qualificationTypeName(qualificationTypeName)
                .certificateNumber(qualification.getCertificateNumber())
                .issuingOrganization(qualification.getIssuingOrganization())
                .issueDate(qualification.getIssueDate() != null
                        ? qualification.getIssueDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .expiryDate(qualification.getExpiryDate() != null
                        ? qualification.getExpiryDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .certificateUrl(qualification.getCertificateUrl())
                .isVerified(qualification.getIsVerified())
                .status(qualification.getStatus() != null ? qualification.getStatus().name() : null)
                .rejectionReason(qualification.getRejectionReason())
                .acceptedAt(qualification.getAcceptedAt() != null
                        ? qualification.getAcceptedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .declinedAt(qualification.getDeclinedAt() != null
                        ? qualification.getDeclinedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .reviewedBy(qualification.getReviewedBy() != null ? qualification.getReviewedBy().toString() : null)
                .notes(qualification.getNotes())
                .build();
    }

    @Override
    @Transactional
    public CaregiverVerificationResponseDTO verifyCaregiverProfileStatus(UUID caregiverProfileId,
            CaregiverProfileVerificationRequest request) {
        log.info("Verifying caregiver profile status {} with action: {}", caregiverProfileId, request.getAction());

        // Find caregiver profile
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByCaregiverProfileIdAndDeletedIsFalse(caregiverProfileId);

        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found");
        }

        // Validate action
        String action = request.getAction();
        if (action == null || action.trim().isEmpty()) {
            throw new BadRequestException("Action is required. Must be 'APPROVE' or 'REJECT'");
        }

        action = action.toUpperCase().trim();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new BadRequestException("Invalid action. Must be 'APPROVE' or 'REJECT'");
        }

        // Validate rejectionReason for REJECT
        if ("REJECT".equals(action)) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required when rejecting caregiver profile");
            }
        }

        // Get current admin user ID
        UUID reviewedBy = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        // Update status and isVerified
        if ("APPROVE".equals(action)) {
            caregiverProfile.setIsVerified(true);
            caregiverProfile.setStatus(EnumVerificationStatusType.APPROVED);
            caregiverProfile.setRejectionReason(null);
            caregiverProfile.setAcceptedAt(now);
            caregiverProfile.setDeclinedAt(null);
            caregiverProfile.setReviewedBy(reviewedBy);
            log.info("Caregiver profile {} approved by admin {}", caregiverProfileId, reviewedBy);
        } else {
            caregiverProfile.setIsVerified(false);
            caregiverProfile.setStatus(EnumVerificationStatusType.REJECTED);
            caregiverProfile.setRejectionReason(request.getRejectionReason());
            caregiverProfile.setAcceptedAt(null);
            caregiverProfile.setDeclinedAt(now);
            caregiverProfile.setReviewedBy(reviewedBy);
            log.info("Caregiver profile {} rejected by admin {} with reason: {}", caregiverProfileId, reviewedBy,
                    request.getRejectionReason());
        }

        // Update timestamp
        caregiverProfile.setUpdatedAt(now);

        // Save caregiver profile
        caregiverProfileRepository.save(caregiverProfile);
        log.info("Caregiver profile {} verification status updated successfully", caregiverProfileId);

        // Fetch again with account and qualifications to ensure all data is loaded
        CaregiverProfile updatedProfile = caregiverProfileRepository
                .findByCaregiverProfileIdWithAccountAndQualifications(caregiverProfileId);

        if (updatedProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found after update");
        }

        // Return updated profile with all information
        return mapToVerificationDTO(updatedProfile);
    }

    @Override
    @Transactional
    public CaregiverVerificationResponseDTO verifyQualification(UUID qualificationId,
            QualificationVerificationRequest request) {
        log.info("Verifying qualification {} with action: {}", qualificationId, request.getAction());

        // Find qualification
        Qualification qualification = qualificationRepository
                .findByQualificationIdAndDeletedIsFalse(qualificationId);

        if (qualification == null) {
            throw new ElementNotFoundException("Qualification not found");
        }

        // Validate action
        String action = request.getAction();
        if (action == null || action.trim().isEmpty()) {
            throw new BadRequestException("Action is required. Must be 'APPROVE' or 'REJECT'");
        }

        action = action.toUpperCase().trim();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new BadRequestException("Invalid action. Must be 'APPROVE' or 'REJECT'");
        }

        // Validate rejectionReason for REJECT
        if ("REJECT".equals(action)) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required when rejecting qualification");
            }
        }

        // Get current admin user ID
        UUID reviewedBy = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        // Update qualification status
        if ("APPROVE".equals(action)) {
            qualification.setStatus(EnumVerificationStatusType.APPROVED);
            qualification.setIsVerified(true);
            qualification.setRejectionReason(null);
            qualification.setAcceptedAt(now);
            qualification.setDeclinedAt(null);
            qualification.setReviewedBy(reviewedBy);
            log.info("Qualification {} approved by admin {}", qualificationId, reviewedBy);
        } else {
            qualification.setStatus(EnumVerificationStatusType.REJECTED);
            qualification.setIsVerified(false);
            qualification.setRejectionReason(request.getRejectionReason());
            qualification.setAcceptedAt(null);
            qualification.setDeclinedAt(now);
            qualification.setReviewedBy(reviewedBy);
            log.info("Qualification {} rejected by admin {} with reason: {}", qualificationId, reviewedBy,
                    request.getRejectionReason());
        }

        // Update timestamp
        qualification.setUpdatedAt(now);

        // Save qualification
        qualificationRepository.save(qualification);
        log.info("Qualification {} verification status updated successfully", qualificationId);

        // Check if all qualifications are reviewed, if yes, set
        // is_needed_review_certificate = false
        CaregiverProfile caregiverProfile = qualification.getCaregiverProfile();
        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found for qualification");
        }

        UUID caregiverProfileId = caregiverProfile.getCaregiverProfileId();
        List<Qualification> allQualifications = qualificationRepository
                .findByCaregiverProfile_CaregiverProfileIdAndDeletedIsFalse(caregiverProfileId);

        // Check if there are any PENDING qualifications
        boolean hasPendingQualifications = allQualifications.stream()
                .anyMatch(q -> q.getStatus() == EnumVerificationStatusType.PENDING);

        if (!hasPendingQualifications) {
            caregiverProfile.setIsNeededReviewCertificate(false);
            caregiverProfile.setUpdatedAt(LocalDateTime.now());
            caregiverProfileRepository.save(caregiverProfile);
            log.info("All qualifications reviewed for caregiver profile {}, set is_needed_review_certificate = false",
                    caregiverProfileId);
        }

        // Fetch caregiver profile again with account and qualifications to ensure all
        // data is loaded
        CaregiverProfile updatedProfile = caregiverProfileRepository
                .findByCaregiverProfileIdWithAccountAndQualifications(caregiverProfileId);

        if (updatedProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found after update");
        }

        // Return updated profile with all information
        return mapToVerificationDTO(updatedProfile);
    }

    @Override
    public CaregiverProfileDetailResponseDTO getMyCaregiverProfile() {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Getting caregiver profile for account ID: {}", currentAccountId);

        // Fetch caregiver profile with account and qualifications
        CaregiverProfile caregiverProfile = caregiverProfileRepository
                .findByAccountIdWithAccountAndQualifications(currentAccountId);

        if (caregiverProfile == null) {
            throw new ElementNotFoundException("Caregiver profile not found for current user");
        }

        return mapToCaregiverProfileDetailDTO(caregiverProfile);
    }

    @Override
    public CareSeekerProfileDetailResponseDTO getMyCareSeekerProfile() {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Getting care seeker profile for account ID: {}", currentAccountId);

        // Fetch care seeker profile with account and elderly profiles
        CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                .findByAccountIdWithAccountAndElderlyProfiles(currentAccountId);

        if (careSeekerProfile == null) {
            throw new ElementNotFoundException("Care seeker profile not found for current user");
        }

        return mapToCareSeekerProfileDetailDTO(careSeekerProfile);
    }

    private CaregiverProfileDetailResponseDTO mapToCaregiverProfileDetailDTO(CaregiverProfile profile) {
        if (profile == null) {
            return null;
        }

        // Calculate age
        Integer age = null;
        if (profile.getBirthDate() != null) {
            age = java.time.Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
        }

        // Map Account info
        String accountId = null;
        String email = null;
        String avatarUrl = null;
        Boolean enabled = null;
        Boolean nonLocked = null;

        if (profile.getAccount() != null) {
            Account account = profile.getAccount();
            accountId = account.getAccountId() != null ? account.getAccountId().toString() : null;
            email = account.getEmail();
            avatarUrl = account.getAvatarUrl();
            enabled = account.getEnabled();
            nonLocked = account.getNonLocked();
        }

        // Map Qualifications
        List<CaregiverProfileDetailResponseDTO.QualificationDetailDTO> qualifications = new ArrayList<>();
        if (profile.getQualifications() != null) {
            qualifications = profile.getQualifications().stream()
                    .filter(q -> !q.isDeleted())
                    .map(this::mapQualificationToDetailDTO)
                    .collect(Collectors.toList());
        }

        // Calculate statistics
        // 1. Total completed bookings (care-services with status COMPLETED)
        List<com.capstone_project.elderly_platform.pojos.CareService> completedCareServices = 
                careServiceRepository.findByCaregiverProfileAndStatusAndDeletedIsFalse(
                        profile, 
                        com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType.COMPLETED,
                        org.springframework.data.domain.Sort.unsorted());
        Long totalCompletedBookings = (long) completedCareServices.size();

        // 2. Total earnings (sum of total_caregiver_earnings from all PayoutBatches)
        Double totalEarnings = 0.0;
        List<com.capstone_project.elderly_platform.pojos.PayoutBatch> payoutBatches = 
                payoutBatchRepository.findAll().stream()
                        .filter(pb -> !pb.isDeleted() 
                                && pb.getCaregiverProfile() != null
                                && pb.getCaregiverProfile().getCaregiverProfileId()
                                        .equals(profile.getCaregiverProfileId()))
                        .collect(Collectors.toList());
        for (com.capstone_project.elderly_platform.pojos.PayoutBatch pb : payoutBatches) {
            if (pb.getTotalCaregiverEarnings() != null) {
                totalEarnings += pb.getTotalCaregiverEarnings();
            }
        }

        // 3. Task completion rate (% of DONE tasks in COMPLETED care-services)
        Double taskCompletionRate = 0.0;
        int totalTasks = 0;
        int doneTasks = 0;
        
        for (com.capstone_project.elderly_platform.pojos.CareService careService : completedCareServices) {
            // Get work schedules for this care service
            List<com.capstone_project.elderly_platform.pojos.WorkSchedule> workSchedules = 
                    workScheduleRepository.findAll().stream()
                            .filter(ws -> !ws.isDeleted() 
                                    && ws.getCareService() != null
                                    && ws.getCareService().getCareServiceId()
                                            .equals(careService.getCareServiceId()))
                            .collect(Collectors.toList());
            
            for (com.capstone_project.elderly_platform.pojos.WorkSchedule workSchedule : workSchedules) {
                // Add total_tasks from work schedule
                if (workSchedule.getTotalTasks() != null) {
                    totalTasks += workSchedule.getTotalTasks();
                }
                
                // Get work tasks for this work schedule
                List<com.capstone_project.elderly_platform.pojos.WorkTask> workTasks = 
                        workTaskRepository.findAll().stream()
                                .filter(wt -> !wt.isDeleted() 
                                        && wt.getWorkSchedule() != null
                                        && wt.getWorkSchedule().getWorkScheduleId()
                                                .equals(workSchedule.getWorkScheduleId()))
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
            taskCompletionRate = (doneTasks * 100.0) / totalTasks;
        }

        return CaregiverProfileDetailResponseDTO.builder()
                .caregiverProfileId(profile.getCaregiverProfileId() != null
                        ? profile.getCaregiverProfileId().toString()
                        : null)
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .location(profile.getLocation())
                .bio(profile.getBio())
                .isVerified(profile.getIsVerified())
                .status(profile.getStatus() != null ? profile.getStatus().name() : null)
                .rejectionReason(profile.getRejectionReason())
                .isNeededReviewCertificate(profile.getIsNeededReviewCertificate())
                .acceptedAt(profile.getAcceptedAt() != null
                        ? profile.getAcceptedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .declinedAt(profile.getDeclinedAt() != null
                        ? profile.getDeclinedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .reviewedBy(profile.getReviewedBy() != null ? profile.getReviewedBy().toString() : null)
                .birthDate(profile.getBirthDate() != null
                        ? profile.getBirthDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .age(age)
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .profileData(profile.getProfileData())
                .accountId(accountId)
                .email(email)
                .avatarUrl(avatarUrl)
                .enabled(enabled)
                .nonLocked(nonLocked)
                .qualifications(qualifications)
                .totalCompletedBookings(totalCompletedBookings)
                .totalEarnings(totalEarnings)
                .taskCompletionRate(taskCompletionRate)
                .build();
    }

    private CaregiverProfileDetailResponseDTO.QualificationDetailDTO mapQualificationToDetailDTO(Qualification qualification) {
        if (qualification == null) {
            return null;
        }

        String qualificationTypeId = null;
        String qualificationTypeName = null;
        if (qualification.getQualificationType() != null) {
            qualificationTypeId = qualification.getQualificationType().getQualificationTypeId() != null
                    ? qualification.getQualificationType().getQualificationTypeId().toString()
                    : null;
            qualificationTypeName = qualification.getQualificationType().getTypeName();
        }

        return CaregiverProfileDetailResponseDTO.QualificationDetailDTO.builder()
                .qualificationId(qualification.getQualificationId() != null
                        ? qualification.getQualificationId().toString()
                        : null)
                .qualificationTypeId(qualificationTypeId)
                .qualificationTypeName(qualificationTypeName)
                .certificateNumber(qualification.getCertificateNumber())
                .issuingOrganization(qualification.getIssuingOrganization())
                .issueDate(qualification.getIssueDate() != null
                        ? qualification.getIssueDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .expiryDate(qualification.getExpiryDate() != null
                        ? qualification.getExpiryDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .certificateUrl(qualification.getCertificateUrl())
                .isVerified(qualification.getIsVerified())
                .status(qualification.getStatus() != null ? qualification.getStatus().name() : null)
                .rejectionReason(qualification.getRejectionReason())
                .acceptedAt(qualification.getAcceptedAt() != null
                        ? qualification.getAcceptedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .declinedAt(qualification.getDeclinedAt() != null
                        ? qualification.getDeclinedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .reviewedBy(qualification.getReviewedBy() != null ? qualification.getReviewedBy().toString() : null)
                .notes(qualification.getNotes())
                .build();
    }

    private CareSeekerProfileDetailResponseDTO mapToCareSeekerProfileDetailDTO(CareSeekerProfile profile) {
        if (profile == null) {
            return null;
        }

        // Calculate age
        Integer age = null;
        if (profile.getBirthDate() != null) {
            age = java.time.Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
        }

        // Map Account info
        String accountId = null;
        String email = null;
        String avatarUrl = null;
        Boolean enabled = null;
        Boolean nonLocked = null;

        if (profile.getAccount() != null) {
            Account account = profile.getAccount();
            accountId = account.getAccountId() != null ? account.getAccountId().toString() : null;
            email = account.getEmail();
            avatarUrl = account.getAvatarUrl();
            enabled = account.getEnabled();
            nonLocked = account.getNonLocked();
        }

        // Map Elderly Profiles
        List<ElderlyProfileResponseDTO> elderlyProfiles = new ArrayList<>();
        if (profile.getElderlyProfiles() != null) {
            elderlyProfiles = profile.getElderlyProfiles().stream()
                    .filter(e -> !e.isDeleted())
                    .map(elderlyProfileMapper::toDTO)
                    .collect(Collectors.toList());
        }

        // Calculate statistics
        // 1. Total elderly profiles
        Long totalElderlyProfiles = (long) elderlyProfiles.size();

        // 2. Total completed bookings (care-services with status COMPLETED)
        List<com.capstone_project.elderly_platform.pojos.CareService> completedCareServices = 
                careServiceRepository.findByCareSeekerProfileAndStatusAndDeletedIsFalse(
                        profile, 
                        com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType.COMPLETED,
                        org.springframework.data.domain.Sort.unsorted());
        Long totalCompletedBookings = (long) completedCareServices.size();

        return CareSeekerProfileDetailResponseDTO.builder()
                .careSeekerProfileId(profile.getCareSeekerProfileId() != null
                        ? profile.getCareSeekerProfileId().toString()
                        : null)
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .location(profile.getLocation())
                .birthDate(profile.getBirthDate() != null
                        ? profile.getBirthDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .age(age)
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .profileData(profile.getProfileData())
                .accountId(accountId)
                .email(email)
                .avatarUrl(avatarUrl)
                .enabled(enabled)
                .nonLocked(nonLocked)
                .elderlyProfiles(elderlyProfiles)
                .totalElderlyProfiles(totalElderlyProfiles)
                .totalCompletedBookings(totalCompletedBookings)
                .build();
    }
}

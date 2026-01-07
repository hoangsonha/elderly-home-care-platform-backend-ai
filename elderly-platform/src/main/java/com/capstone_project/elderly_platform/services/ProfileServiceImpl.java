package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateCareSeekerProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverProfileRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumGenderType;
import com.capstone_project.elderly_platform.enums.EnumHealthStatusType;
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
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.ElderlyProfileRepository;
import com.capstone_project.elderly_platform.repositories.QualificationRepository;
import com.capstone_project.elderly_platform.repositories.QualificationTypeRepository;
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
    private final CaregiverProfileMapper caregiverProfileMapper;
    private final CareSeekerProfileMapper careSeekerProfileMapper;
    private final ElderlyProfileMapper elderlyProfileMapper;
    private final FirebaseStorageService firebaseStorageService;
    private final CaregiverScheduleUtils caregiverScheduleUtils;
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

        // Create LocalDate from birthYear if provided (age will be calculated in mapper)
        java.time.LocalDate birthDate = null;
        if (request.getBirthYear() != null) {
            // Create LocalDate from birthYear (set to January 1st of that year)
            birthDate = java.time.LocalDate.of(request.getBirthYear(), 1, 1);
            // Note: age is not stored in profileData, it will be calculated from birthDate in mapper
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
                throw new BadRequestException("Invalid health status: " + request.getHealthStatus() + ". Valid values: GOOD, WEAK, MODERATE");
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
    public CaregiverProfileResponseDTO createCaregiverProfile(UpdateCaregiverProfileRequest request,
                                                              MultipartFile avatarFile,
                                                              List<MultipartFile> credentialFiles) {
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        log.info("Creating caregiver profile for account ID: {}", currentAccountId);

        // Get account
        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(currentAccountId)
                .orElseThrow(() -> new ElementNotFoundException("Account not found for current user"));

        // Check if profile already exists (including deleted = false check)
        CaregiverProfile existingProfile = caregiverProfileRepository
                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
        if (existingProfile != null) {
            log.warn("Attempt to create caregiver profile failed: Profile already exists for account ID: {}", currentAccountId);
            throw new BadRequestException("Caregiver profile already exists for this account. Please use update API instead.");
        }

        LocalDateTime now = LocalDateTime.now();

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

        // 5. Build profileData JSON (years_experience, free_schedule, max_hours_per_week, preferences)
        Map<String, Object> profileDataMap = new HashMap<>();
        
        if (request.getYearsExperience() != null) {
            profileDataMap.put("years_experience", request.getYearsExperience());
        }
        
        // Handle free_schedule
        if (request.getFreeSchedule() != null) {
            Map<String, Object> freeScheduleMap = new HashMap<>();
            if (request.getFreeSchedule().getAvailableAllTime() != null) {
                freeScheduleMap.put("available_all_time", request.getFreeSchedule().getAvailableAllTime());
            }
            
            if (request.getFreeSchedule().getBookedSlots() != null) {
                List<Map<String, Object>> bookedSlotsList = new ArrayList<>();
                for (UpdateCaregiverProfileRequest.BookedSlotRequest slot : request.getFreeSchedule().getBookedSlots()) {
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
            if (request.getMaxHoursPerWeek() > 48) {
                throw new BadRequestException("Max hours per week cannot exceed 48");
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

        // 6. Create CaregiverProfile entity
        CaregiverProfile caregiverProfile = CaregiverProfile.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhone())
                .birthDate(birthDate)
                .gender(gender)
                .location(locationJson)
                .bio(request.getBio())
                .isVerified(false) // Default false
                .profileData(profileDataJson)
                .account(account)
                .build();
        
        caregiverProfile.setCreatedAt(now);
        caregiverProfile.setUpdatedAt(now);
        caregiverProfile.setDeleted(false);

        // Save caregiver profile first
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        log.info("Caregiver profile created successfully with ID: {}", savedProfile.getCaregiverProfileId());

        // 7. Handle credentials
        if (request.getCredentials() != null && !request.getCredentials().isEmpty()) {
            // Validate credential files
            if (credentialFiles == null || credentialFiles.size() != request.getCredentials().size()) {
                throw new BadRequestException("Number of credential files must match number of credentials. Expected: " 
                        + request.getCredentials().size() + ", got: " + (credentialFiles != null ? credentialFiles.size() : 0));
            }
            
            // Create qualifications
            for (int i = 0; i < request.getCredentials().size(); i++) {
                UpdateCaregiverProfileRequest.CredentialRequest credRequest = request.getCredentials().get(i);
                MultipartFile credentialFile = credentialFiles.get(i);
                
                // Validate qualification type
                QualificationType qualificationType = qualificationTypeRepository
                        .findByQualificationTypeIdAndDeletedIsFalse(credRequest.getQualificationTypeId());
                if (qualificationType == null) {
                    throw new BadRequestException("Qualification type not found: " + credRequest.getQualificationTypeId());
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
                                                               List<MultipartFile> credentialFiles) {
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

        LocalDateTime now = LocalDateTime.now();

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
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
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

        // 4. Update profileData JSON (years_experience, free_schedule, max_hours_per_week, preferences)
        String currentProfileData = caregiverProfile.getProfileData();
        currentProfileData = caregiverScheduleUtils.initializeFreeScheduleIfNotExists(currentProfileData);
        
        try {
            Map<String, Object> profileDataMap = new HashMap<>();
            
            // Parse existing profileData if exists
            if (currentProfileData != null && !currentProfileData.isEmpty()) {
                profileDataMap = objectMapper.readValue(currentProfileData, 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
            
            // Update years_experience
            if (request.getYearsExperience() != null) {
                profileDataMap.put("years_experience", request.getYearsExperience());
            }
            
            // Update free_schedule
            if (request.getFreeSchedule() != null) {
                Map<String, Object> freeScheduleMap = new HashMap<>();
                if (request.getFreeSchedule().getAvailableAllTime() != null) {
                    freeScheduleMap.put("available_all_time", request.getFreeSchedule().getAvailableAllTime());
                }
                
                if (request.getFreeSchedule().getBookedSlots() != null) {
                    List<Map<String, Object>> bookedSlotsList = new ArrayList<>();
                    for (UpdateCaregiverProfileRequest.BookedSlotRequest slot : request.getFreeSchedule().getBookedSlots()) {
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
            
            // Update max_hours_per_week (validate <= 48)
            if (request.getMaxHoursPerWeek() != null) {
                if (request.getMaxHoursPerWeek() > 48) {
                    throw new BadRequestException("Max hours per week cannot exceed 48");
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

        // 5. Handle credentials
        if (request.getCredentials() != null && !request.getCredentials().isEmpty()) {
            // Validate credential files
            if (credentialFiles == null || credentialFiles.size() != request.getCredentials().size()) {
                throw new BadRequestException("Number of credential files must match number of credentials. Expected: " 
                        + request.getCredentials().size() + ", got: " + (credentialFiles != null ? credentialFiles.size() : 0));
            }
            
            // Delete existing qualifications (soft delete)
            List<Qualification> existingQualifications = qualificationRepository
                    .findByCaregiverProfile_CaregiverProfileIdAndDeletedIsFalse(caregiverProfile.getCaregiverProfileId());
            
            for (Qualification existing : existingQualifications) {
                existing.setDeleted(true);
                existing.setUpdatedAt(now);
                qualificationRepository.save(existing);
            }
            
            // Create new qualifications
            for (int i = 0; i < request.getCredentials().size(); i++) {
                UpdateCaregiverProfileRequest.CredentialRequest credRequest = request.getCredentials().get(i);
                MultipartFile credentialFile = credentialFiles.get(i);
                
                // Validate qualification type
                QualificationType qualificationType = qualificationTypeRepository
                        .findByQualificationTypeIdAndDeletedIsFalse(credRequest.getQualificationTypeId());
                if (qualificationType == null) {
                    throw new BadRequestException("Qualification type not found: " + credRequest.getQualificationTypeId());
                }
                
                // Upload certificate file (required for each credential)
                String certificateUrl = null;
                if (credentialFile != null && !credentialFile.isEmpty()) {
                    try {
                        // Use uploadFile for any file type (image or document)
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
                        .caregiverProfile(caregiverProfile)
                        .qualificationType(qualificationType)
                        .certificateNumber(credRequest.getCertificateNumber())
                        .issuingOrganization(credRequest.getIssuingOrganization())
                        .issueDate(credRequest.getIssueDate())
                        .expiryDate(credRequest.getExpiryDate())
                        .certificateUrl(certificateUrl)
                        .isVerified(false) // Default false, not from request
                        .notes(credRequest.getNotes())
                        .build();
                
                qualification.setCreatedAt(now);
                qualification.setUpdatedAt(now);
                qualification.setDeleted(false);
                
                qualificationRepository.save(qualification);
            }
        }

        // Save caregiver profile
        CaregiverProfile savedProfile = caregiverProfileRepository.save(caregiverProfile);
        log.info("Caregiver profile updated successfully with ID: {}", savedProfile.getCaregiverProfileId());

        return caregiverProfileMapper.toDTO(savedProfile);
    }
}

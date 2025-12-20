package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumGenderType;
import com.capstone_project.elderly_platform.enums.EnumHealthStatusType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.CaregiverProfileMapper;
import com.capstone_project.elderly_platform.mappers.ElderlyProfileMapper;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import com.capstone_project.elderly_platform.pojos.ElderlyProfile;
import com.capstone_project.elderly_platform.repositories.CareSeekerProfileRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.ElderlyProfileRepository;
import com.capstone_project.elderly_platform.services.externals.firebase.FirebaseStorageService;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
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
    private final CaregiverProfileMapper caregiverProfileMapper;
    private final ElderlyProfileMapper elderlyProfileMapper;
    private final FirebaseStorageService firebaseStorageService;
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

        // Calculate age from date of birth
        if (request.getDateOfBirth() != null) {
            int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
            profileDataMap.put("age", age);
        } else if (request.getAge() != null) {
            profileDataMap.put("age", request.getAge());
        }

        if (request.getBloodType() != null) {
            profileDataMap.put("blood_type", request.getBloodType());
        }
        if (request.getWeight() != null) {
            profileDataMap.put("weight", request.getWeight());
        }
        if (request.getHeight() != null) {
            profileDataMap.put("height", request.getHeight());
        }
        if (request.getUnderlyingDiseases() != null) {
            profileDataMap.put("underlying_diseases", request.getUnderlyingDiseases());
        }
        if (request.getSpecialConditions() != null) {
            profileDataMap.put("special_conditions", request.getSpecialConditions());
        }
        if (request.getAllergies() != null) {
            profileDataMap.put("allergies", request.getAllergies());
        }
        if (request.getMedications() != null) {
            profileDataMap.put("medications", request.getMedications());
        }
        if (request.getIndependenceLevel() != null) {
            profileDataMap.put("independence_level", request.getIndependenceLevel());
        }
        if (request.getCareNeeds() != null) {
            profileDataMap.put("care_needs", request.getCareNeeds());
        }
        if (request.getHobbies() != null) {
            profileDataMap.put("hobbies", request.getHobbies());
        }
        if (request.getFavoriteActivities() != null) {
            profileDataMap.put("favorite_activities", request.getFavoriteActivities());
        }
        if (request.getMusicPreference() != null) {
            profileDataMap.put("music_preference", request.getMusicPreference());
        }
        if (request.getTvShows() != null) {
            profileDataMap.put("tv_shows", request.getTvShows());
        }
        if (request.getFoodPreferences() != null) {
            profileDataMap.put("food_preferences", request.getFoodPreferences());
        }
        if (request.getLivingEnvironment() != null) {
            Map<String, Object> livingEnvMap = new HashMap<>();
            livingEnvMap.put("houseType", request.getLivingEnvironment().getHouseType());
            livingEnvMap.put("livingWith", request.getLivingEnvironment().getLivingWith());
            livingEnvMap.put("accessibility", request.getLivingEnvironment().getAccessibility());
            profileDataMap.put("living_environment", livingEnvMap);
        }
        if (request.getEmergencyContact() != null) {
            Map<String, Object> emergencyContactMap = new HashMap<>();
            emergencyContactMap.put("name", request.getEmergencyContact().getName());
            emergencyContactMap.put("relationship", request.getEmergencyContact().getRelationship());
            emergencyContactMap.put("phone", request.getEmergencyContact().getPhone());
            profileDataMap.put("emergency_contact", emergencyContactMap);
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

        // Convert gender string to enum
        EnumGenderType gender;
        try {
            gender = EnumGenderType.valueOf(request.getGender().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid gender: " + request.getGender());
        }

        // Create ElderlyProfile entity
        ElderlyProfile elderlyProfile = ElderlyProfile.builder()
                .fullName(request.getName())
                .phoneNumber(request.getPhone())
                .birthDate(request.getDateOfBirth())
                .location(locationJson)
                .gender(gender)
                .avatarUrl(avatarUrl)
                .profileData(profileDataJson)
                .status(EnumActivationStatusType.ACTIVE)
                .healthStatus(EnumHealthStatusType.GOOD) // Default value
                .careSeekerProfile(careSeekerProfile)
                .build();

        // Save to database
        ElderlyProfile savedProfile = elderlyProfileRepository.save(elderlyProfile);
        log.info("Elderly profile created successfully with ID: {}", savedProfile.getElderlyProfileId());

        // Convert to DTO and return
        return elderlyProfileMapper.toDTO(savedProfile);
    }
}

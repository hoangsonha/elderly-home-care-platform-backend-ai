package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.CaregiverProfileMapper;
import com.capstone_project.elderly_platform.mappers.ElderlyProfileMapper;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import com.capstone_project.elderly_platform.repositories.CareSeekerProfileRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.ElderlyProfileRepository;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
}

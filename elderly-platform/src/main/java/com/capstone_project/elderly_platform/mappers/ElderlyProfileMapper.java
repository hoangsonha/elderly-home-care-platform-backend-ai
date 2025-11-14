package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.pojos.ElderlyProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Component
public class ElderlyProfileMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Calculate age from birthDate to current date
     */
    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public ElderlyProfileResponseDTO toDTO(ElderlyProfile profile) {
        if (profile == null) {
            return null;
        }

        return ElderlyProfileResponseDTO.builder()
                .elderlyProfileId(profile.getElderlyProfileId() != null
                        ? profile.getElderlyProfileId().toString()
                        : null)
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .birthDate(profile.getBirthDate() != null
                        ? profile.getBirthDate().format(DATE_FORMATTER)
                        : null)
                .age(calculateAge(profile.getBirthDate()))
                .location(profile.getLocation()) // Keep as JSON string
                .gender(profile.getGender() != null
                        ? profile.getGender().name()
                        : null)
                .avatarUrl(profile.getAvatarUrl())
                .profileData(profile.getProfileData()) // Keep as JSON string
                .careRequirement(profile.getCareRequirement()) // Keep as JSON string
                .note(profile.getNote())
                .healthNote(profile.getHealthNote())
                .healthStatus(profile.getHealthStatus() != null ? profile.getHealthStatus().name() : null)
                .status(profile.getStatus() != null
                        ? profile.getStatus().name()
                        : null)
                .build();
    }
}


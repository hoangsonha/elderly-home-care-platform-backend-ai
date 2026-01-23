package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Component
public class CareSeekerProfileMapper {

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

    public CareSeekerProfileResponseDTO toDTO(CareSeekerProfile profile) {
        if (profile == null) {
            return null;
        }

        // Get avatar and accountId from Account
        String avatarUrl = null;
        String accountId = null;
        if (profile.getAccount() != null) {
            avatarUrl = profile.getAccount().getAvatarUrl();
            accountId = profile.getAccount().getAccountId() != null
                    ? profile.getAccount().getAccountId().toString()
                    : null;
        }

        return CareSeekerProfileResponseDTO.builder()
                .careSeekerProfileId(profile.getCareSeekerProfileId() != null
                        ? profile.getCareSeekerProfileId().toString()
                        : null)
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .location(profile.getLocation()) // Keep as JSON string
                .birthDate(profile.getBirthDate() != null
                        ? profile.getBirthDate().format(DATE_FORMATTER)
                        : null)
                .age(calculateAge(profile.getBirthDate()))
                .gender(profile.getGender() != null
                        ? profile.getGender().name()
                        : null)
                .accountId(accountId)
                .avatarUrl(avatarUrl)
                .profileData(profile.getProfileData()) // Keep as JSON string
                .build();
    }
}



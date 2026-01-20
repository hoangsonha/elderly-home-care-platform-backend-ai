package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Component
public class CaregiverProfileMapper {

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

    public CaregiverProfileResponseDTO toDTO(CaregiverProfile profile) {
        if (profile == null) {
            return null;
        }

        // Get account info from Account
        String avatarUrl = null;
        String accountId = null;
        if (profile.getAccount() != null) {
            avatarUrl = profile.getAccount().getAvatarUrl();
            accountId = profile.getAccount().getAccountId() != null
                    ? profile.getAccount().getAccountId().toString()
                    : null;
        }

        return CaregiverProfileResponseDTO.builder()
                .caregiverProfileId(profile.getCaregiverProfileId() != null
                        ? profile.getCaregiverProfileId().toString()
                        : null)
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .location(profile.getLocation()) // Keep as JSON string
                .bio(profile.getBio())
                .isVerified(profile.getIsVerified())
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


package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CaregiverProfileResponseDTO {
    String caregiverProfileId;
    String fullName;
    String phoneNumber;
    String location;
    String bio;
    Boolean isVerified;
    String birthDate;
    Integer age;
    String gender;
    String avatarUrl;
    String profileData;
}

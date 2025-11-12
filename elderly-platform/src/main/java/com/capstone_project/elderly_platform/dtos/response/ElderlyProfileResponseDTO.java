package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElderlyProfileResponseDTO {
    String elderlyProfileId;
    String fullName;
    String phoneNumber;
    String birthDate;
    Integer age;
    String location;
    String gender;
    String avatarUrl;
    String profileData;
    String careRequirement;
    String note;
    String healthNote;
    String status;
}

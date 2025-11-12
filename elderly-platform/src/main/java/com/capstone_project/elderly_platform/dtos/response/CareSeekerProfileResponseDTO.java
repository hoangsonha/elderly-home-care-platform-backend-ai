package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareSeekerProfileResponseDTO {
    String careSeekerProfileId;
    String fullName;
    String phoneNumber;
    String location;
    String birthDate;
    Integer age;
    String gender;
    String avatarUrl;
    String profileData;

}

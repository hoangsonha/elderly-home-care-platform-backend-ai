package com.capstone_project.elderly_platform.dtos.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.capstone_project.elderly_platform.utils.JsonStringDeserializer;
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
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String location;
    
    String bio;
    Boolean isVerified;
    String birthDate;
    Integer age;
    String gender;
    String avatarUrl;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String profileData;
}

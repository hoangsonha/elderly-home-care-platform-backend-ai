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
public class CareSeekerProfileResponseDTO {
    String careSeekerProfileId;
    String fullName;
    String phoneNumber;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String location;
    
    String birthDate;
    Integer age;
    String gender;
    String accountId; // Account ID (UUID) - dùng cho chat API
    String avatarUrl;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String profileData;

}

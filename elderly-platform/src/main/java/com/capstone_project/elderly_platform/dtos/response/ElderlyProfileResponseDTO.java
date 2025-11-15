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
public class ElderlyProfileResponseDTO {
    String elderlyProfileId;
    String fullName;
    String phoneNumber;
    String birthDate;
    Integer age;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String location;
    
    String gender;
    String avatarUrl;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String profileData;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String careRequirement;
    
    String note;
    String healthStatus;
    String healthNote;
    String status;
}

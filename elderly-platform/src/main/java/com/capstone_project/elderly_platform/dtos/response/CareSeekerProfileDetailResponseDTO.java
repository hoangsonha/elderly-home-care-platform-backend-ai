package com.capstone_project.elderly_platform.dtos.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.capstone_project.elderly_platform.utils.JsonStringDeserializer;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareSeekerProfileDetailResponseDTO {
    // Care Seeker Profile Info
    String careSeekerProfileId;
    String fullName;
    String phoneNumber;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String location;
    
    String birthDate;
    Integer age;
    String gender;
    
    @JsonRawValue
    @JsonDeserialize(using = JsonStringDeserializer.class)
    String profileData;
    
    // Account Info
    String accountId;
    String email;
    String avatarUrl;
    Boolean enabled;
    Boolean nonLocked;
    
    // Elderly Profiles
    List<ElderlyProfileResponseDTO> elderlyProfiles;
    
    // Statistics
    Long totalElderlyProfiles;          // Tổng số elderly profiles
    Long totalCompletedBookings;         // Tổng số lịch hẹn (care-service có status COMPLETED)
}


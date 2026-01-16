package com.capstone_project.elderly_platform.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCareSeekerProfileRequest {
    
    @NotBlank(message = "Full name is required")
    @JsonProperty("full_name")
    String fullName;
    
    @NotNull(message = "Birth year is required")
    @JsonProperty("birth_year")
    Integer birthYear;
    
    @NotBlank(message = "Gender is required")
    String gender;
    
    @NotNull(message = "Location is required")
    @Valid
    LocationRequest location;
    
    String phone;
}

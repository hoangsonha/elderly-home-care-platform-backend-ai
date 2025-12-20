package com.capstone_project.elderly_platform.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateElderlyProfileRequest {
    
    @NotBlank(message = "Name is required")
    String name;
    
    Integer age;
    
    @NotBlank(message = "Gender is required")
    String gender;
    
    @NotNull(message = "Date of birth is required")
    @JsonProperty("date_of_birth")
    LocalDate dateOfBirth;
    
    @NotBlank(message = "Phone is required")
    String phone;
    
    @NotNull(message = "Location is required")
    LocationRequest location;
    
    @JsonProperty("blood_type")
    String bloodType;
    
    Double weight;
    
    Double height;
    
    @JsonProperty("underlying_diseases")
    List<String> underlyingDiseases;
    
    @JsonProperty("special_conditions")
    List<String> specialConditions;
    
    List<String> allergies;
    
    List<MedicationRequest> medications;
    
    @JsonProperty("independence_level")
    Map<String, String> independenceLevel;
    
    @JsonProperty("care_needs")
    List<String> careNeeds;
    
    List<String> hobbies;
    
    @JsonProperty("favorite_activities")
    List<String> favoriteActivities;
    
    @JsonProperty("music_preference")
    String musicPreference;
    
    @JsonProperty("tv_shows")
    List<String> tvShows;
    
    @JsonProperty("food_preferences")
    List<String> foodPreferences;
    
    @JsonProperty("living_environment")
    LivingEnvironmentRequest livingEnvironment;
    
    @JsonProperty("emergency_contact")
    EmergencyContactRequest emergencyContact;
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MedicationRequest {
        String name;
        String dosage;
        String frequency;
    }
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LivingEnvironmentRequest {
        @JsonProperty("houseType")
        String houseType;
        
        @JsonProperty("livingWith")
        List<String> livingWith;
        
        List<String> accessibility;
    }
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class EmergencyContactRequest {
        String name;
        String relationship;
        String phone;
    }
}


package com.capstone_project.elderly_platform.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCaregiverRequest {
    @JsonProperty("seeker_name")
    private String seekerName;

    @JsonProperty("care_level")
    private Integer careLevel;

    @JsonProperty("health_status")
    private String healthStatus;

    @JsonProperty("elderly_age")
    private Integer elderlyAge;

    @JsonProperty("caregiver_age_range")
    private List<Integer> caregiverAgeRange;

    @JsonProperty("gender_preference")
    private String genderPreference;

    @JsonProperty("required_years_experience")
    private Integer requiredYearsExperience;

    @JsonProperty("overall_rating_range")
    private List<Double> overallRatingRange;

    private List<String> personality;

    private List<String> attitude;

    private Skills skills;

    @JsonProperty("time_slots")
    private List<TimeSlot> timeSlots;

    private Location location;

    @JsonProperty("budget_per_hour")
    private Integer budgetPerHour;

    @JsonProperty("top_n")
    private Integer topN;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skills {
        @JsonProperty("required_skills")
        private List<String> requiredSkills;

        @JsonProperty("priority_skills")
        private List<String> prioritySkills;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private String day;
        private String start;
        private String end;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Double lat;
        private Double lon;
        private String address;
    }
}
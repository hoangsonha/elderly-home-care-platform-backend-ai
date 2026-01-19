package com.capstone_project.elderly_platform.dtos.request.externals;

import com.capstone_project.elderly_platform.dtos.QualificationRequirements;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCaregiverRequest {
    @JsonProperty("seeker_name")
    private String seekerName;

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

    @JsonProperty("time_slots")
    private TimeSlot timeSlots; // Có thể là object hoặc array, nhưng trong request thường là object

    private Location location;

    @JsonProperty("service_package")
    private ServicePackageInfo servicePackage;

    @JsonProperty("top_n")
    private Integer topN;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicePackageInfo {
        @JsonProperty("servicePackageId")
        private UUID servicePackageId;

        @JsonProperty("packageName")
        private String packageName;

        @JsonProperty("description")
        private String description;

        @JsonProperty("durationHours")
        private Integer durationHours;

        @JsonProperty("packageType")
        private String packageType;

        @JsonProperty("price")
        private Double price;

        @JsonProperty("note")
        private String note;

        @JsonProperty("qualification")
        private QualificationRequirements qualification;

        @JsonProperty("status")
        private String status;

        @JsonProperty("serviceTasks")
        private List<ServiceTaskInfo> serviceTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceTaskInfo {
        @JsonProperty("serviceTaskId")
        private UUID serviceTaskId;

        @JsonProperty("taskName")
        private String taskName;

        @JsonProperty("description")
        private String description;

        @JsonProperty("status")
        private String status;
    }
}
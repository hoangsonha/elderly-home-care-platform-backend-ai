package com.capstone_project.elderly_platform.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaregiverRecommendationDTO {
    private Integer rank;

    @JsonProperty("caregiver_id")
    private String caregiverId;

    private String name;

    private Integer age;

    private String gender;

    private Double rating;

    @JsonProperty("total_reviews")
    private Integer totalReviews;

    @JsonProperty("years_experience")
    private Integer yearsExperience;

    @JsonProperty("price_per_hour")
    private Integer pricePerHour;

    @JsonProperty("distance_km")
    private Double distanceKm;

    private String distance;

    private String avatar;

    private String experience;

    @JsonProperty("isVerified")
    private Boolean isVerified;

    @JsonProperty("match_score")
    private Double matchScore;

    @JsonProperty("match_percentage")
    private String matchPercentage;

    @JsonProperty("score_breakdown")
    private ScoreBreakdownDTO scoreBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdownDTO {
        private Double credential;
        private Double skills;
        private Double distance;
        private Double rating;
        private Double experience;
        private Double price;
        private Double trust;
    }
}




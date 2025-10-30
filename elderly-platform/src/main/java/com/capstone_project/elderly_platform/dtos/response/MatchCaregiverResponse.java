package com.capstone_project.elderly_platform.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCaregiverResponse {
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("care_level")
    private Integer careLevel;

    @JsonProperty("seeker_name")
    private String seekerName;

    private Map<String, Object> location;

    @JsonProperty("total_matches")
    private Integer totalMatches;

    private List<CaregiverRecommendationDTO> recommendations;
}
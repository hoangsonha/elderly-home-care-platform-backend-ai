package com.capstone_project.elderly_platform.dtos.request.externals;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCaregiverByElderlyRequest {
    @JsonProperty("elderly_profile_id")
    private UUID elderlyProfileId;

    @JsonProperty("service_package_id")
    private UUID servicePackageId;

    @JsonProperty("work_date")
    private LocalDate workDate;

    @JsonProperty("start_hour")
    private Integer startHour;

    @JsonProperty("start_minute")
    private Integer startMinute;

    @JsonProperty("top_n")
    private Integer topN;
}

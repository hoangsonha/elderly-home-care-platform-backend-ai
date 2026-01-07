package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareServiceStatisticsResponse {
    private Long totalCareServices;
    private Map<String, Long> countByStatus; // Map với key là status name, value là count
}




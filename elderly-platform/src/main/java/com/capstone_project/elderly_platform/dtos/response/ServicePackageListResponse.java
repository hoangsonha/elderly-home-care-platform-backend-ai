package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageListResponse {
    private Long totalPackages;
    private Long totalActivePackages;
    private Long totalBookings;
    private Double totalRevenue;
    private List<ServicePackageResponseDTO> packages;
}








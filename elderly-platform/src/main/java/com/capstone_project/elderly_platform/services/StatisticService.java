package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.response.CareServiceStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CaregiverPersonalStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CaregiverStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerPersonalStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.UserStatisticsResponse;

import java.time.LocalDateTime;

public interface StatisticService {
    UserStatisticsResponse getUserStatistics(LocalDateTime startDate, LocalDateTime endDate);
    
    CaregiverStatisticsResponse getCaregiverStatistics();
    
    CareServiceStatisticsResponse getCareServiceStatistics();
    
    CaregiverPersonalStatisticsResponse getCaregiverPersonalStatistics();
    
    CareSeekerPersonalStatisticsResponse getCareSeekerPersonalStatistics();
}





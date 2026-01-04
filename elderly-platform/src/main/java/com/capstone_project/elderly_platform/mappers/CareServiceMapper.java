package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.WorkScheduleResponseDTO;
import com.capstone_project.elderly_platform.pojos.CareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class CareServiceMapper {

        private final CareSeekerProfileMapper careSeekerProfileMapper;
        private final CaregiverProfileMapper caregiverProfileMapper;
        private final ElderlyProfileMapper elderlyProfileMapper;
        private final ServicePackageMapper servicePackageMapper;
        private final WorkScheduleMapper workScheduleMapper;

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
        private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public CareServiceResponseDTO toDTO(CareService careService) {
                if (careService == null) {
                        return null;
                }

                // Map nested profiles
                CareSeekerProfileResponseDTO careSeekerProfileDTO = null;
                if (careService.getCareSeekerProfile() != null) {
                        careSeekerProfileDTO = careSeekerProfileMapper.toDTO(careService.getCareSeekerProfile());
                }

                CaregiverProfileResponseDTO caregiverProfileDTO = null;
                if (careService.getCaregiverProfile() != null) {
                        caregiverProfileDTO = caregiverProfileMapper.toDTO(careService.getCaregiverProfile());
                }

                ElderlyProfileResponseDTO elderlyProfileDTO = null;
                if (careService.getElderlyProfile() != null) {
                        elderlyProfileDTO = elderlyProfileMapper.toDTO(careService.getElderlyProfile());
                }

                ServicePackageResponseDTO servicePackageDTO = null;
                if (careService.getServicePackage() != null) {
                        servicePackageDTO = servicePackageMapper.toDTO(careService.getServicePackage());
                }

                // Map work schedule
                WorkScheduleResponseDTO workScheduleDTO = null;
                if (careService.getWorkSchedule() != null && !careService.getWorkSchedule().isDeleted()) {
                        workScheduleDTO = workScheduleMapper.toDTO(careService.getWorkSchedule());
                }

                return CareServiceResponseDTO.builder()
                                .careServiceId(careService.getCareServiceId() != null
                                                ? careService.getCareServiceId().toString()
                                                : null)
                                .careServiceSnapshot(careService.getCareServiceSnapshot()) // Keep as JSON string
                                .bookingCode(careService.getBookingCode())
                                .workDate(careService.getWorkDate() != null
                                                ? careService.getWorkDate().format(DATE_FORMATTER)
                                                : null)
                                .startTime(careService.getStartTime() != null
                                                ? careService.getStartTime().format(TIME_FORMATTER)
                                                : null)
                                .endTime(careService.getEndTime() != null
                                                ? careService.getEndTime().format(TIME_FORMATTER)
                                                : null)
                                .caregiverResponseDeadline(careService.getCaregiverResponseDeadline() != null
                                                ? careService.getCaregiverResponseDeadline().format(DATETIME_FORMATTER)
                                                : null)
                                .completedAt(careService.getCompletedAt() != null
                                                ? careService.getCompletedAt().format(DATETIME_FORMATTER)
                                                : null)
                                .status(careService.getStatus() != null
                                                ? careService.getStatus().name()
                                                : null)
                                .note(careService.getNote())
                                .systemFeePercentage(careService.getSystemFeePercentage())
                                .totalPrice(careService.getTotalPrice())
                                .caregiverEarnings(careService.getCaregiverEarnings())
                                .location(careService.getLocation()) // Keep as JSON string
                                .configVersion(careService.getConfigVersion()) // Keep as JSON string
                                .careSeekerProfile(careSeekerProfileDTO)
                                .elderlyProfile(elderlyProfileDTO)
                                .caregiverProfile(caregiverProfileDTO)
                                .servicePackage(servicePackageDTO)
                                .workSchedule(workScheduleDTO)
                                .build();
        }
}

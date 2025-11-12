package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareServiceResponseDTO {
    String careServiceId;
    String careServiceSnapshot;
    String bookingCode;
    String workDate;
    String startTime;
    String endTime;
    String caregiverResponseDeadline;
    String completedAt;
    String status;
    String note;
    Double systemFeePercentage;
    Double totalPrice;
    Double caregiverEarnings;
    String location;
    String configVersion;
    CareSeekerProfileResponseDTO careSeekerProfile;
    ElderlyProfileResponseDTO elderlyProfile;
    CaregiverProfileResponseDTO caregiverProfile;
    ServicePackageResponseDTO servicePackage;
}

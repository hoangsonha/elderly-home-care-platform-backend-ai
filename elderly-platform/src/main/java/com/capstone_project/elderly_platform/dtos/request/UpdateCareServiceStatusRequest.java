package com.capstone_project.elderly_platform.dtos.request;

import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import lombok.Data;

@Data
public class UpdateCareServiceStatusRequest {
    EnumCareServiceStatusType status;
    String note;
}

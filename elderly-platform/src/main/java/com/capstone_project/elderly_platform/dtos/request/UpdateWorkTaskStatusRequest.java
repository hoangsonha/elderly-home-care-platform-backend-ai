package com.capstone_project.elderly_platform.dtos.request;

import com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkTaskStatusRequest {
    private EnumWorkTaskStatusType status;
}

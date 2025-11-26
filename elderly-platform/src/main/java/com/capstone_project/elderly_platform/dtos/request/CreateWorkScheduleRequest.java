package com.capstone_project.elderly_platform.dtos.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;


@Data
public class CreateWorkScheduleRequest {
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private UUID caregiverId;
    private UUID careServiceId;
}
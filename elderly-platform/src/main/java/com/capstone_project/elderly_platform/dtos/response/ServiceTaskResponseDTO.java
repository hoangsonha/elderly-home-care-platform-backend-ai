package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceTaskResponseDTO {
    String serviceTaskId;
    String taskName;
    String description;
    String status;
}

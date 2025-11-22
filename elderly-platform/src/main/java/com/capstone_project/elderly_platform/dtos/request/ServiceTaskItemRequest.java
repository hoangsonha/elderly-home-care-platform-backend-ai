package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceTaskItemRequest {
    
    UUID serviceTaskId;  // Optional: nếu có = update, nếu null = create
    
    @NotBlank(message = "Task name is required")
    String taskName;
    
    String description;
}


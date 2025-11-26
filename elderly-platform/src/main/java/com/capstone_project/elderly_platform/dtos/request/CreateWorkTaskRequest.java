package com.capstone_project.elderly_platform.dtos.request;

import java.util.UUID;

public class CreateWorkTaskRequest {
    private String name;
    private String description;
    private UUID workScheduleId;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getWorkScheduleId() {
        return workScheduleId;
    }
    public void setWorkScheduleId(UUID workScheduleId) {
        this.workScheduleId = workScheduleId;
    }
}

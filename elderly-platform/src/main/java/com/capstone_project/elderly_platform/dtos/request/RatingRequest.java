package com.capstone_project.elderly_platform.dtos.request;

import lombok.Data;

import java.util.UUID;

@Data
public class RatingRequest {
    private UUID accountId;
    private UUID serviceTaskId; // nếu rating theo ServiceTask
    private Integer score;
    private String comment;
    private boolean systemFeedback;
    private boolean complaintFeedback;
}

package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkNoteRequest;
import com.capstone_project.elderly_platform.dtos.response.WorkNoteResponseDTO;

import java.util.List;
import java.util.UUID;

public interface WorkNoteService {
    /**
     * Create a new work note
     */
    WorkNoteResponseDTO createWorkNote(CreateWorkNoteRequest request);
    
    /**
     * Get all work notes by work schedule ID
     */
    List<WorkNoteResponseDTO> getAllWorkNotesByWorkScheduleId(UUID workScheduleId);
}

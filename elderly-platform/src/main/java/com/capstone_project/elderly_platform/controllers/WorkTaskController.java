package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkTaskRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateWorkTaskStatusRequest;
import com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType;
import com.capstone_project.elderly_platform.pojos.WorkTask;
import com.capstone_project.elderly_platform.services.WorkTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-tasks")
@RequiredArgsConstructor
public class WorkTaskController {


    private final WorkTaskService workTaskService;


    @PostMapping
    public ResponseEntity<WorkTask> create(@RequestBody CreateWorkTaskRequest request) {
        return ResponseEntity.ok(workTaskService.createTask(request));
    }


    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<List<WorkTask>> getBySchedule(@PathVariable UUID scheduleId) {
        return ResponseEntity.ok(workTaskService.getTasksBySchedule(scheduleId));
    }


//    @PutMapping("/{id}/status")
//    public ResponseEntity<WorkTask> updateStatus(
//            @PathVariable UUID id,
//            @RequestParam EnumWorkTaskStatusType status
//    ) {
//        return ResponseEntity.ok(workTaskService.updateTaskStatus(id, status));
//    }
@PutMapping("/{taskId}/status")
public WorkTask updateStatus(
        @PathVariable UUID taskId,
        @RequestBody UpdateWorkTaskStatusRequest request
) {
    return workTaskService.updateStatus(taskId, request);
}
}
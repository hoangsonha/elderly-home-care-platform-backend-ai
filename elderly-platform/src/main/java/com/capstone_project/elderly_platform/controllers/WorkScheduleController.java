package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkScheduleRequest;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import com.capstone_project.elderly_platform.services.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {


    private final WorkScheduleService workScheduleService;


    @PostMapping
    public ResponseEntity<WorkSchedule> create(@RequestBody CreateWorkScheduleRequest request) {
        return ResponseEntity.ok(workScheduleService.createSchedule(request));
    }


    @GetMapping
    public ResponseEntity<List<WorkSchedule>> getAll() {
        return ResponseEntity.ok(workScheduleService.getAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<WorkSchedule> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workScheduleService.getById(id));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkScheduleRequest;
import com.capstone_project.elderly_platform.enums.EnumWorkScheduleStatusType;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class WorkScheduleServiceImpl implements WorkScheduleService{
    private final WorkScheduleRepository workScheduleRepo;
    private final CaregiverProfileRepository caregiverRepo;
    private final CareServiceRepository careServiceRepo;
    @Override
    public WorkSchedule createSchedule(CreateWorkScheduleRequest request) {
        CaregiverProfile caregiver = caregiverRepo.findById(request.getCaregiverId())
                .orElseThrow(() -> new RuntimeException("Caregiver not found"));


        CareService service = careServiceRepo.findById(request.getCareServiceId())
                .orElseThrow(() -> new RuntimeException("CareService not found"));


        WorkSchedule schedule = WorkSchedule.builder()
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(EnumWorkScheduleStatusType.SCHEDULED)
                .caregiverProfile(caregiver)
                .careService(service)
                .totalTasks(0)
                .completedTasks(0)
                .build();


        return workScheduleRepo.save(schedule);
    }

    @Override
    public List<WorkSchedule> getAll() {
        return workScheduleRepo.findAll();
    }

    @Override
    public WorkSchedule getById(UUID id) {
        return workScheduleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
    }

    @Override
    public void delete(UUID id) {
        workScheduleRepo.deleteById(id);
    }
}

package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateServicePackageRequest;
import com.capstone_project.elderly_platform.dtos.request.ServiceTaskItemRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateServicePackageRequest;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageListResponse;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageUsageResponse;
import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.mappers.ServicePackageMapper;
import com.capstone_project.elderly_platform.pojos.ServicePackage;
import com.capstone_project.elderly_platform.pojos.ServiceTask;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.ServicePackageRepository;
import com.capstone_project.elderly_platform.repositories.ServiceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicePackageServiceImpl implements ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final CareServiceRepository careServiceRepository;
    private final ServicePackageMapper servicePackageMapper;

    @Transactional
    @Override
    public ServicePackageResponseDTO createServicePackage(CreateServicePackageRequest request) {
        log.info("Creating service package with name: {}", request.getPackageName());

        ServicePackage servicePackage = ServicePackage.builder()
                .packageName(request.getPackageName())
                .description(request.getDescription())
                .durationHours(request.getDurationHours())
                .packageType(request.getPackageType())
                .price(request.getPrice())
                .note(request.getNote())
                .serviceIncluded(null) // Set to null for now, not used
                .status(EnumActivationStatusType.ACTIVE)
                .build();

        // Set timestamps manually
        LocalDateTime now = LocalDateTime.now();
        servicePackage.setCreatedAt(now);
        servicePackage.setUpdatedAt(now);
        servicePackage.setDeleted(false);

        ServicePackage savedPackage = servicePackageRepository.save(servicePackage);
        log.info("Service package created successfully with ID: {}", savedPackage.getServicePackageId());

        // Create service tasks if provided
        if (request.getServiceTasks() != null && !request.getServiceTasks().isEmpty()) {
            log.info("Creating {} service tasks for package ID: {}",
                    request.getServiceTasks().size(), savedPackage.getServicePackageId());

            List<ServiceTask> serviceTasks = new ArrayList<>();
            for (ServiceTaskItemRequest taskRequest : request.getServiceTasks()) {
                ServiceTask serviceTask = ServiceTask.builder()
                        .taskName(taskRequest.getTaskName())
                        .description(taskRequest.getDescription())
                        .status(EnumActivationStatusType.ACTIVE)
                        .servicePackage(savedPackage)
                        .build();
                serviceTasks.add(serviceTask);
            }

            List<ServiceTask> savedTasks = serviceTaskRepository.saveAll(serviceTasks);
            savedPackage.setServiceTasks(savedTasks);
            log.info("Service tasks created successfully for package ID: {}", savedPackage.getServicePackageId());
        } else {
            // No tasks provided, set empty list
            savedPackage.setServiceTasks(new ArrayList<>());
        }

        return servicePackageMapper.toDTO(savedPackage);
    }

    @Transactional
    @Override
    public ServicePackageResponseDTO updateServicePackage(UUID id, UpdateServicePackageRequest request) {
        log.info("Updating service package with ID: {}", id);

        ServicePackage servicePackage = servicePackageRepository.findByServicePackageIdAndDeletedIsFalse(id);
        if (servicePackage == null) {
            throw new ElementNotFoundException("Service package not found with ID: " + id);
        }

        if (request.getPackageName() != null) {
            servicePackage.setPackageName(request.getPackageName());
        }
        if (request.getDescription() != null) {
            servicePackage.setDescription(request.getDescription());
        }
        if (request.getDurationHours() != null) {
            servicePackage.setDurationHours(request.getDurationHours());
        }
        if (request.getPackageType() != null) {
            servicePackage.setPackageType(request.getPackageType());
        }
        if (request.getPrice() != null) {
            servicePackage.setPrice(request.getPrice());
        }
        if (request.getNote() != null) {
            servicePackage.setNote(request.getNote());
        }
        if (request.getStatus() != null) {
            servicePackage.setStatus(request.getStatus());
        }

        // Update timestamp manually
        servicePackage.setUpdatedAt(LocalDateTime.now());

        ServicePackage updatedPackage = servicePackageRepository.save(servicePackage);
        log.info("Service package updated successfully with ID: {}", id);

        // Handle service tasks if provided (Create/Update/Delete logic)
        if (request.getServiceTasks() != null) {
            handleServiceTasksUpdate(updatedPackage, request.getServiceTasks());
        }

        // Refresh to get latest tasks
        ServicePackage refreshedPackage = servicePackageRepository.findByServicePackageIdAndDeletedIsFalse(id);

        // Explicitly load tasks (force lazy loading)
        if (refreshedPackage != null) {
            List<ServiceTask> tasks = serviceTaskRepository.findAll()
                    .stream()
                    .filter(task -> !task.isDeleted()
                            && task.getServicePackage() != null
                            && task.getServicePackage().getServicePackageId().equals(id))
                    .collect(Collectors.toList());
            refreshedPackage.setServiceTasks(tasks);
        }

        return servicePackageMapper.toDTO(refreshedPackage);
    }

    @Transactional(readOnly = true)
    @Override
    public ServicePackageResponseDTO getServicePackageById(UUID id) {
        log.info("Getting service package with ID: {}", id);

        ServicePackage servicePackage = servicePackageRepository.findByServicePackageIdAndDeletedIsFalse(id);
        if (servicePackage == null) {
            throw new ElementNotFoundException("Service package not found with ID: " + id);
        }

        // Explicitly load tasks
        List<ServiceTask> tasks = serviceTaskRepository.findAll()
                .stream()
                .filter(task -> !task.isDeleted()
                        && task.getServicePackage() != null
                        && task.getServicePackage().getServicePackageId().equals(id))
                .collect(Collectors.toList());
        servicePackage.setServiceTasks(tasks);

        return servicePackageMapper.toDTO(servicePackage);
    }

    @Transactional(readOnly = true)
    @Override
    public ServicePackageListResponse getAllServicePackages() {
        log.info("Getting all service packages with statistics");

        List<ServicePackage> packages = servicePackageRepository.findAll()
                .stream()
                .filter(pkg -> !pkg.isDeleted())
                .collect(Collectors.toList());

        // Load tasks for each package
        for (ServicePackage pkg : packages) {
            List<ServiceTask> tasks = serviceTaskRepository.findAll()
                    .stream()
                    .filter(task -> !task.isDeleted()
                            && task.getServicePackage() != null
                            && task.getServicePackage().getServicePackageId().equals(pkg.getServicePackageId()))
                    .collect(Collectors.toList());
            pkg.setServiceTasks(tasks);
        }

        // Map to DTOs and add care service count
        List<ServicePackageResponseDTO> packageDTOs = packages.stream()
                .map(pkg -> {
                    ServicePackageResponseDTO dto = servicePackageMapper.toDTO(pkg);
                    // Count care services for this package
                    Long totalCareServices = careServiceRepository.countByServicePackageIdAndDeletedFalse(
                            pkg.getServicePackageId());
                    dto.setTotalCareServices(totalCareServices);
                    return dto;
                })
                .collect(Collectors.toList());

        // Calculate statistics
        Long totalPackages = (long) packages.size();
        Long totalActivePackages = packages.stream()
                .filter(pkg -> pkg.getStatus() == EnumActivationStatusType.ACTIVE)
                .count();
        Long totalBookings = careServiceRepository.countTotalBookings();
        Double totalRevenue = careServiceRepository.sumTotalRevenue();
        
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }

        log.info("Statistics - Total packages: {}, Active packages: {}, Total bookings: {}, Total revenue: {}",
                totalPackages, totalActivePackages, totalBookings, totalRevenue);

        return ServicePackageListResponse.builder()
                .totalPackages(totalPackages)
                .totalActivePackages(totalActivePackages)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .packages(packageDTOs)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ServicePackageResponseDTO> getAllActiveServicePackages() {
        log.info("Getting all active service packages");

        List<ServicePackage> packages = servicePackageRepository.findAll()
                .stream()
                .filter(pkg -> !pkg.isDeleted() && pkg.getStatus() == EnumActivationStatusType.ACTIVE)
                .collect(Collectors.toList());

        // Load tasks for each package
        for (ServicePackage pkg : packages) {
            List<ServiceTask> tasks = serviceTaskRepository.findAll()
                    .stream()
                    .filter(task -> !task.isDeleted()
                            && task.getServicePackage() != null
                            && task.getServicePackage().getServicePackageId().equals(pkg.getServicePackageId()))
                    .collect(Collectors.toList());
            pkg.setServiceTasks(tasks);
        }

        return packages.stream()
                .map(servicePackageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void deleteServicePackage(UUID id) {
        log.info("Soft deleting service package with ID: {}", id);

        ServicePackage servicePackage = servicePackageRepository.findByServicePackageIdAndDeletedIsFalse(id);
        if (servicePackage == null) {
            throw new ElementNotFoundException("Service package not found with ID: " + id);
        }

        LocalDateTime now = LocalDateTime.now();
        servicePackage.setDeleted(true);
        servicePackage.setStatus(EnumActivationStatusType.INACTIVE);
        servicePackage.setDeletedAt(now);
        servicePackage.setUpdatedAt(now);
        servicePackageRepository.save(servicePackage);

        log.info("Service package soft deleted successfully with ID: {}", id);
    }

    @Transactional
    @Override
    public void restoreServicePackage(UUID id) {
        log.info("Restoring service package with ID: {}", id);

        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Service package not found with ID: " + id));

        if (!servicePackage.isDeleted()) {
            log.warn("Service package with ID {} is not deleted", id);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        servicePackage.setDeleted(false);
        servicePackage.setStatus(EnumActivationStatusType.ACTIVE);
        servicePackage.setUnDeletedAt(now);
        servicePackage.setUpdatedAt(now);
        servicePackageRepository.save(servicePackage);

        log.info("Service package restored successfully with ID: {}", id);
    }

    /**
     * Handle service tasks update with Create/Update/Delete logic
     * - Tasks with ID in request: UPDATE
     * - Tasks without ID in request: CREATE
     * - Tasks in DB but not in request: SOFT DELETE
     */
    private void handleServiceTasksUpdate(ServicePackage servicePackage, List<ServiceTaskItemRequest> taskRequests) {
        log.info("Handling service tasks update for package ID: {}", servicePackage.getServicePackageId());

        // 1. Get existing tasks from DB
        List<ServiceTask> existingTasks = serviceTaskRepository.findAll()
                .stream()
                .filter(task -> !task.isDeleted()
                        && task.getServicePackage() != null
                        && task.getServicePackage().getServicePackageId().equals(servicePackage.getServicePackageId()))
                .collect(Collectors.toList());

        // 2. Collect IDs from request
        List<UUID> requestTaskIds = taskRequests.stream()
                .map(ServiceTaskItemRequest::getServiceTaskId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        // 3. SOFT DELETE tasks that are NOT in request
        for (ServiceTask existingTask : existingTasks) {
            if (!requestTaskIds.contains(existingTask.getServiceTaskId())) {
                log.info("Soft deleting task ID: {}", existingTask.getServiceTaskId());
                existingTask.setDeleted(true);
                existingTask.setDeletedAt(LocalDateTime.now());
                serviceTaskRepository.save(existingTask);
            }
        }

        // 4. CREATE or UPDATE tasks from request
        for (ServiceTaskItemRequest taskRequest : taskRequests) {
            if (taskRequest.getServiceTaskId() != null) {
                // UPDATE existing task
                ServiceTask existingTask = serviceTaskRepository
                        .findByServiceTaskIdAndDeletedIsFalse(taskRequest.getServiceTaskId())
                        .orElse(null);

                if (existingTask != null) {
                    log.info("Updating task ID: {}", taskRequest.getServiceTaskId());
                    existingTask.setTaskName(taskRequest.getTaskName());
                    existingTask.setDescription(taskRequest.getDescription());
                    serviceTaskRepository.save(existingTask);
                } else {
                    log.warn("Task ID {} not found or deleted, skipping update", taskRequest.getServiceTaskId());
                }
            } else {
                // CREATE new task
                log.info("Creating new task: {}", taskRequest.getTaskName());
                ServiceTask newTask = ServiceTask.builder()
                        .taskName(taskRequest.getTaskName())
                        .description(taskRequest.getDescription())
                        .status(EnumActivationStatusType.ACTIVE)
                        .servicePackage(servicePackage)
                        .build();
                serviceTaskRepository.save(newTask);
            }
        }

        log.info("Service tasks update completed for package ID: {}", servicePackage.getServicePackageId());
    }

    @Transactional(readOnly = true)
    @Override
    public ServicePackageUsageResponse getServicePackageUsage(UUID id) {
        log.info("Getting care service usage for service package with ID: {}", id);

        ServicePackage servicePackage = servicePackageRepository.findByServicePackageIdAndDeletedIsFalse(id);
        if (servicePackage == null) {
            throw new ElementNotFoundException("Service package not found with ID: " + id);
        }

        Long totalCareServices = careServiceRepository.countByServicePackageIdAndDeletedFalse(id);
        
        log.info("Service package {} has {} care services", id, totalCareServices);

        return ServicePackageUsageResponse.builder()
                .servicePackageId(servicePackage.getServicePackageId())
                .packageName(servicePackage.getPackageName())
                .totalCareServices(totalCareServices)
                .build();
    }
}

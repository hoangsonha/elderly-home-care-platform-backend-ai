package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.EndWorkRequest;
import com.capstone_project.elderly_platform.dtos.request.StartWorkRequest;
import com.capstone_project.elderly_platform.dtos.request.ToggleWorkTaskRequest;
import com.capstone_project.elderly_platform.dtos.request.externals.CreatePaymentLinkRequestBody;
import com.capstone_project.elderly_platform.dtos.response.EndWorkResponse;
import com.capstone_project.elderly_platform.dtos.response.StartWorkResponse;
import com.capstone_project.elderly_platform.dtos.response.ToggleWorkTaskResponse;
import com.capstone_project.elderly_platform.dtos.response.externals.PaymentLinkWithQRCodeResponse;
import com.capstone_project.elderly_platform.enums.EnumActorType;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.enums.EnumWorkScheduleStatusType;
import com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CareServiceStatusLog;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import com.capstone_project.elderly_platform.pojos.WorkTask;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CareServiceStatusLogRepository;
import com.capstone_project.elderly_platform.repositories.WorkScheduleRepository;
import com.capstone_project.elderly_platform.repositories.WorkTaskRepository;
import com.capstone_project.elderly_platform.services.externals.firebase.FirebaseStorageService;
import com.capstone_project.elderly_platform.services.externals.payos.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private final CareServiceRepository careServiceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final WorkTaskRepository workTaskRepository;
    private final CareServiceStatusLogRepository careServiceStatusLogRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final PayOSService payOSService;

    @Override
    @Transactional
    public StartWorkResponse startWork(StartWorkRequest request, MultipartFile checkInImage) {
        log.info("Starting work for care service ID: {}", request.getCareServiceId());

        // Validate and get care service
        CareService careService = careServiceRepository
                .findByCareServiceIdAndDeletedIsFalse(request.getCareServiceId());
        
        if (careService == null) {
            throw new ElementNotFoundException("Không tìm thấy dịch vụ chăm sóc");
        }

        // Validate status - must be CAREGIVER_APPROVED
        if (careService.getStatus() != EnumCareServiceStatusType.CAREGIVER_APPROVED) {
            throw new BadRequestException(
                    "Chỉ có thể bắt đầu làm việc khi dịch vụ ở trạng thái CAREGIVER_APPROVED. Trạng thái hiện tại: " 
                    + careService.getStatus());
        }

        // Validate image
        if (checkInImage == null || checkInImage.isEmpty()) {
            throw new BadRequestException("Vui lòng upload ảnh Check In (CI)");
        }

        // Get work schedule
        WorkSchedule workSchedule = workScheduleRepository.findAll()
                .stream()
                .filter(ws -> !ws.isDeleted() 
                        && ws.getCareService() != null 
                        && ws.getCareService().getCareServiceId().equals(careService.getCareServiceId()))
                .findFirst()
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy lịch làm việc cho dịch vụ này"));

        // Upload CI image to Firebase
        String checkInImageUrl;
        try {
            checkInImageUrl = firebaseStorageService.uploadSingleImages(checkInImage);
            if (checkInImageUrl == null) {
                throw new BadRequestException("Không thể upload ảnh Check In");
            }
            log.info("Uploaded CI image to Firebase: {}", checkInImageUrl);
        } catch (Exception e) {
            log.error("Failed to upload CI image: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi upload ảnh Check In: " + e.getMessage());
        }

        // Update work schedule with CI image and start time
        workSchedule.setCheckInImageUrl(checkInImageUrl);
        workSchedule.setStatus(EnumWorkScheduleStatusType.IN_PROGRESS);
        workSchedule.setStartTime(java.time.LocalTime.now()); // Set start time when work begins
        workScheduleRepository.save(workSchedule);

        // Update all tasks from PENDING to IN_PROGRESS
        if (workSchedule.getWorkTasks() != null) {
            for (WorkTask task : workSchedule.getWorkTasks()) {
                if (!task.isDeleted() && task.getStatus() == EnumWorkTaskStatusType.PENDING) {
                    task.setStatus(EnumWorkTaskStatusType.IN_PROGRESS);
                    workTaskRepository.save(task);
                }
            }
            log.info("Updated {} tasks from PENDING to IN_PROGRESS", 
                    workSchedule.getWorkTasks().stream()
                            .filter(t -> !t.isDeleted() && t.getStatus() == EnumWorkTaskStatusType.IN_PROGRESS)
                            .count());
        }

        // Update care service status to IN_PROGRESS
        EnumCareServiceStatusType oldStatus = careService.getStatus();
        careService.setStatus(EnumCareServiceStatusType.IN_PROGRESS);
        CareService savedCareService = careServiceRepository.save(careService);

        // Create status log
        CareServiceStatusLog statusLog = CareServiceStatusLog.builder()
                .changedBy(EnumActorType.CAREGIVER)
                .careService(savedCareService)
                .oldStatus(oldStatus)
                .newStatus(EnumCareServiceStatusType.IN_PROGRESS)
                .note("Bắt đầu làm việc - Check In tại " + java.time.LocalDateTime.now())
                .build();
        careServiceStatusLogRepository.save(statusLog);

        log.info("Work started successfully for care service {}", savedCareService.getCareServiceId());

        return StartWorkResponse.builder()
                .careServiceId(savedCareService.getCareServiceId())
                .status(savedCareService.getStatus().name())
                .checkInImageUrl(checkInImageUrl)
                .message("Bắt đầu làm việc thành công")
                .build();
    }

    @Override
    @Transactional
    public EndWorkResponse endWork(EndWorkRequest request, MultipartFile checkOutImage) {
        log.info("Ending work for care service ID: {}", request.getCareServiceId());

        // Validate and get care service
        CareService careService = careServiceRepository
                .findByCareServiceIdAndDeletedIsFalse(request.getCareServiceId());
        
        if (careService == null) {
            throw new ElementNotFoundException("Không tìm thấy dịch vụ chăm sóc");
        }

        // Validate status - must be IN_PROGRESS
        if (careService.getStatus() != EnumCareServiceStatusType.IN_PROGRESS) {
            throw new BadRequestException(
                    "Chỉ có thể kết thúc làm việc khi dịch vụ ở trạng thái IN_PROGRESS. Trạng thái hiện tại: " 
                    + careService.getStatus());
        }

        // Validate image
        if (checkOutImage == null || checkOutImage.isEmpty()) {
            throw new BadRequestException("Vui lòng upload ảnh Check Out (CO)");
        }

        // Get work schedule
        WorkSchedule workSchedule = workScheduleRepository.findAll()
                .stream()
                .filter(ws -> !ws.isDeleted() 
                        && ws.getCareService() != null 
                        && ws.getCareService().getCareServiceId().equals(careService.getCareServiceId()))
                .findFirst()
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy lịch làm việc cho dịch vụ này"));

        // Upload CO image to Firebase
        String checkOutImageUrl;
        try {
            checkOutImageUrl = firebaseStorageService.uploadSingleImages(checkOutImage);
            if (checkOutImageUrl == null) {
                throw new BadRequestException("Không thể upload ảnh Check Out");
            }
            log.info("Uploaded CO image to Firebase: {}", checkOutImageUrl);
        } catch (Exception e) {
            log.error("Failed to upload CO image: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi upload ảnh Check Out: " + e.getMessage());
        }

        // Update work schedule with CO image and end time
        workSchedule.setCheckOutImageUrl(checkOutImageUrl);
        workSchedule.setStatus(EnumWorkScheduleStatusType.COMPLETED);
        workSchedule.setEndTime(java.time.LocalTime.now()); // Set end time when work ends
        workSchedule.setCompletedAt(java.time.LocalDateTime.now());
        workScheduleRepository.save(workSchedule);

        // Update all IN_PROGRESS tasks to NOT_COMPLETED
        if (workSchedule.getWorkTasks() != null) {
            int notCompletedCount = 0;
            for (WorkTask task : workSchedule.getWorkTasks()) {
                if (!task.isDeleted() && task.getStatus() == EnumWorkTaskStatusType.IN_PROGRESS) {
                    task.setStatus(EnumWorkTaskStatusType.NOT_COMPLETED);
                    workTaskRepository.save(task);
                    notCompletedCount++;
                }
            }
            log.info("Updated {} tasks from IN_PROGRESS to NOT_COMPLETED", notCompletedCount);
        }

        // Update care service status to WAITING_PAYMENT
        EnumCareServiceStatusType oldStatus = careService.getStatus();
        careService.setStatus(EnumCareServiceStatusType.WAITING_PAYMENT);
        CareService savedCareService = careServiceRepository.save(careService);

        // Create status log
        CareServiceStatusLog statusLog = CareServiceStatusLog.builder()
                .changedBy(EnumActorType.CAREGIVER)
                .careService(savedCareService)
                .oldStatus(oldStatus)
                .newStatus(EnumCareServiceStatusType.WAITING_PAYMENT)
                .note("Kết thúc làm việc - Check Out tại " + java.time.LocalDateTime.now())
                .build();
        careServiceStatusLogRepository.save(statusLog);

        // Create payment link and QR code
        PaymentLinkWithQRCodeResponse paymentResponse;
        try {
            CreatePaymentLinkRequestBody paymentRequest = new CreatePaymentLinkRequestBody();
            paymentRequest.setCareServiceId(savedCareService.getCareServiceId());
            paymentResponse = payOSService.createPaymentLink(paymentRequest);
            
            if (paymentResponse == null) {
                throw new BadRequestException("Không thể tạo payment link");
            }
            log.info("Created payment link for care service {}", savedCareService.getCareServiceId());
        } catch (Exception e) {
            log.error("Failed to create payment link: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi tạo payment link: " + e.getMessage());
        }

        log.info("Work ended successfully for care service {}", savedCareService.getCareServiceId());

        return EndWorkResponse.builder()
                .careServiceId(savedCareService.getCareServiceId())
                .status(savedCareService.getStatus().name())
                .checkOutImageUrl(checkOutImageUrl)
                .qrCodeBase64(paymentResponse.getQrCodeBase64())
                .checkoutUrl(paymentResponse.getCheckoutUrl())
                .orderCode(paymentResponse.getOrderCode())
                .amount(paymentResponse.getAmount())
                .description(paymentResponse.getDescription())
                .productName(paymentResponse.getProductName())
                .paymentId(paymentResponse.getPaymentId())
                .message("Kết thúc làm việc thành công. Vui lòng quét mã QR để thanh toán.")
                .build();
    }

    @Override
    @Transactional
    public ToggleWorkTaskResponse toggleWorkTask(ToggleWorkTaskRequest request) {
        log.info("Toggling work task status for task ID: {}", request.getWorkTaskId());

        // Get work task
        WorkTask workTask = workTaskRepository
                .findByWorkTaskIdAndDeletedIsFalse(request.getWorkTaskId())
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy task"));

        // Validate work schedule status - must be IN_PROGRESS
        WorkSchedule workSchedule = workTask.getWorkSchedule();
        if (workSchedule == null || workSchedule.isDeleted()) {
            throw new ElementNotFoundException("Không tìm thấy lịch làm việc cho task này");
        }

        if (workSchedule.getStatus() != EnumWorkScheduleStatusType.IN_PROGRESS) {
            throw new BadRequestException(
                    "Chỉ có thể thay đổi trạng thái task khi đang làm việc. Trạng thái hiện tại: " 
                    + workSchedule.getStatus());
        }

        // Toggle status: IN_PROGRESS ↔ DONE
        EnumWorkTaskStatusType oldStatus = workTask.getStatus();
        EnumWorkTaskStatusType newStatus;
        String message;

        if (oldStatus == EnumWorkTaskStatusType.IN_PROGRESS) {
            // IN_PROGRESS → DONE
            newStatus = EnumWorkTaskStatusType.DONE;
            workTask.setCompletedAt(java.time.LocalDateTime.now());
            message = "Task đã được đánh dấu hoàn thành";
        } else if (oldStatus == EnumWorkTaskStatusType.DONE) {
            // DONE → IN_PROGRESS
            newStatus = EnumWorkTaskStatusType.IN_PROGRESS;
            workTask.setCompletedAt(null);
            message = "Task đã được đánh dấu chưa hoàn thành";
        } else {
            throw new BadRequestException(
                    "Chỉ có thể toggle task khi ở trạng thái IN_PROGRESS hoặc DONE. Trạng thái hiện tại: " 
                    + oldStatus);
        }

        workTask.setStatus(newStatus);
        WorkTask savedTask = workTaskRepository.save(workTask);

        log.info("Task {} status changed from {} to {}", 
                savedTask.getWorkTaskId(), oldStatus, newStatus);

        return ToggleWorkTaskResponse.builder()
                .workTaskId(savedTask.getWorkTaskId())
                .status(savedTask.getStatus().name())
                .message(message)
                .build();
    }
}


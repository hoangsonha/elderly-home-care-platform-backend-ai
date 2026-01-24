package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateWorkNoteRequest;
import com.capstone_project.elderly_platform.dtos.response.WorkNoteResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumNotificationType;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.WorkNote;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.WorkNoteRepository;
import com.capstone_project.elderly_platform.repositories.WorkScheduleRepository;
import com.capstone_project.elderly_platform.services.externals.firebase.PushNotificationService;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkNoteServiceImpl implements WorkNoteService {

    private final WorkNoteRepository workNoteRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AccountRepository accountRepository;
    private final PushNotificationService pushNotificationService;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    @Transactional
    public WorkNoteResponseDTO createWorkNote(CreateWorkNoteRequest request) {
        log.info("Creating work note for work schedule ID: {}", request.getWorkScheduleId());

        // Validate and get work schedule with related entities
        WorkSchedule workSchedule = workScheduleRepository
                .findByWorkScheduleIdAndDeletedIsFalse(request.getWorkScheduleId())
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy lịch làm việc"));

        // Get current user account
        UUID currentAccountId = SecurityUtils.getCurrentUserId();
        Account createdByAccount = accountRepository
                .findByAccountIdAndDeletedIsFalse(currentAccountId)
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy tài khoản"));

        // Create work note
        WorkNote workNote = WorkNote.builder()
                .content(request.getContent())
                .workSchedule(workSchedule)
                .createdBy(createdByAccount)
                .build();

        WorkNote savedWorkNote = workNoteRepository.save(workNote);
        log.info("Created work note with ID: {}", savedWorkNote.getWorkNoteId());

        // Send notification to the other party
        try {
            sendWorkNoteNotification(workSchedule, createdByAccount, savedWorkNote);
        } catch (Exception e) {
            log.error("Failed to send notification for work note {}: {}", 
                    savedWorkNote.getWorkNoteId(), e.getMessage(), e);
            // Don't throw exception - work note is already created
        }

        return toDTO(savedWorkNote);
    }

    @Override
    public List<WorkNoteResponseDTO> getAllWorkNotesByWorkScheduleId(UUID workScheduleId) {
        log.info("Getting all work notes for work schedule ID: {}", workScheduleId);

        // Validate work schedule exists
        workScheduleRepository
                .findByWorkScheduleIdAndDeletedIsFalse(workScheduleId)
                .orElseThrow(() -> new ElementNotFoundException("Không tìm thấy lịch làm việc"));

        // Get all work notes
        List<WorkNote> workNotes = workNoteRepository
                .findByWorkSchedule_WorkScheduleIdAndDeletedIsFalse(workScheduleId);

        return workNotes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private WorkNoteResponseDTO toDTO(WorkNote workNote) {
        // Get fullName from profile
        String createdByFullName = null;
        Account createdBy = workNote.getCreatedBy();
        if (createdBy != null) {
            if (createdBy.getCareSeekerProfile() != null && !createdBy.getCareSeekerProfile().isDeleted()) {
                createdByFullName = createdBy.getCareSeekerProfile().getFullName();
            } else if (createdBy.getCaregiverProfile() != null && !createdBy.getCaregiverProfile().isDeleted()) {
                createdByFullName = createdBy.getCaregiverProfile().getFullName();
            }
        }

        return WorkNoteResponseDTO.builder()
                .workNoteId(workNote.getWorkNoteId().toString())
                .content(workNote.getContent())
                .createdByAccountId(createdBy != null ? createdBy.getAccountId().toString() : null)
                .createdByFullName(createdByFullName)
                .createdAt(workNote.getCreatedAt() != null 
                        ? workNote.getCreatedAt().format(DATETIME_FORMATTER) 
                        : null)
                .updatedAt(workNote.getUpdatedAt() != null 
                        ? workNote.getUpdatedAt().format(DATETIME_FORMATTER) 
                        : null)
                .build();
    }

    private void sendWorkNoteNotification(WorkSchedule workSchedule, Account senderAccount, WorkNote workNote) {
        UUID senderAccountId = senderAccount.getAccountId();
        UUID recipientAccountId = null;
        String senderName = null;
        String recipientName = null;

        // Get sender name
        if (senderAccount.getCaregiverProfile() != null && !senderAccount.getCaregiverProfile().isDeleted()) {
            senderName = senderAccount.getCaregiverProfile().getFullName();
        } else if (senderAccount.getCareSeekerProfile() != null && !senderAccount.getCareSeekerProfile().isDeleted()) {
            senderName = senderAccount.getCareSeekerProfile().getFullName();
        }

        // Determine recipient: if sender is caregiver, notify seeker, and vice versa
        if (workSchedule.getCareService() != null) {
            // Get caregiver account ID
            UUID caregiverAccountId = null;
            if (workSchedule.getCaregiverProfile() != null 
                    && workSchedule.getCaregiverProfile().getAccount() != null) {
                caregiverAccountId = workSchedule.getCaregiverProfile().getAccount().getAccountId();
            }

            // Get seeker account ID
            UUID seekerAccountId = null;
            if (workSchedule.getCareService().getCareSeekerProfile() != null
                    && workSchedule.getCareService().getCareSeekerProfile().getAccount() != null) {
                seekerAccountId = workSchedule.getCareService().getCareSeekerProfile().getAccount().getAccountId();
            }

            // Determine recipient
            if (senderAccountId.equals(caregiverAccountId)) {
                // Sender is caregiver, notify seeker
                recipientAccountId = seekerAccountId;
                if (workSchedule.getCareService().getCareSeekerProfile() != null) {
                    recipientName = workSchedule.getCareService().getCareSeekerProfile().getFullName();
                }
            } else if (senderAccountId.equals(seekerAccountId)) {
                // Sender is seeker, notify caregiver
                recipientAccountId = caregiverAccountId;
                if (workSchedule.getCaregiverProfile() != null) {
                    recipientName = workSchedule.getCaregiverProfile().getFullName();
                }
            }
        }

        // Send notification if recipient is found
        if (recipientAccountId != null && !recipientAccountId.equals(senderAccountId)) {
            String title = senderName != null 
                    ? String.format("%s đã tạo ghi chú mới", senderName)
                    : "Có ghi chú mới";

            // Truncate content for notification body (max 100 chars)
            String contentPreview = workNote.getContent();
            if (contentPreview != null && contentPreview.length() > 100) {
                contentPreview = contentPreview.substring(0, 97) + "...";
            }

            String body = contentPreview != null 
                    ? contentPreview
                    : "Có một ghi chú mới được tạo cho lịch làm việc này.";

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("workNoteId", workNote.getWorkNoteId().toString());
            notificationData.put("workScheduleId", workSchedule.getWorkScheduleId().toString());
            if (workSchedule.getCareService() != null) {
                notificationData.put("careServiceId", workSchedule.getCareService().getCareServiceId().toString());
                if (workSchedule.getCareService().getBookingCode() != null) {
                    notificationData.put("bookingCode", workSchedule.getCareService().getBookingCode());
                }
            }

            pushNotificationService.sendNotification(
                    recipientAccountId,
                    senderAccountId,
                    title,
                    body,
                    EnumNotificationType.WORK_SCHEDULE_UPDATED,
                    "WORK_NOTE",
                    workNote.getWorkNoteId(),
                    notificationData,
                    null
            );

            log.info("Notification sent to {} for work note {} created by {}", 
                    recipientName != null ? recipientName : recipientAccountId, 
                    workNote.getWorkNoteId(), 
                    senderName != null ? senderName : senderAccountId);
        } else {
            log.warn("Could not determine recipient for work note notification. Sender: {}, WorkSchedule: {}", 
                    senderAccountId, workSchedule.getWorkScheduleId());
        }
    }
}

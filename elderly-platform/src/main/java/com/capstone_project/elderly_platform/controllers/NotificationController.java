package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.configurations.CustomAccountDetail;
import com.capstone_project.elderly_platform.dtos.request.NotificationTokenRequest;
import com.capstone_project.elderly_platform.dtos.response.DeviceTokenResponse;
import com.capstone_project.elderly_platform.dtos.response.NotificationResponse;
import com.capstone_project.elderly_platform.services.externals.firebase.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification Management APIs")
public class NotificationController {

    private final PushNotificationService pushNotificationService;


    @PostMapping("/tokens")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @Operation(summary = "Register device token for push notifications")
    public ResponseEntity<DeviceTokenResponse> registerToken(
            @RequestBody @Valid NotificationTokenRequest request,
            @AuthenticationPrincipal CustomAccountDetail accountDetail) {
        UUID accountId = accountDetail.getId();
        DeviceTokenResponse response = pushNotificationService
                .registerDeviceToken(accountId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/tokens")
    @Operation(summary = "Remove device token (logout)")
    public ResponseEntity<Void> removeToken(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomAccountDetail accountDetail) {
        UUID accountId = accountDetail.getId();
        String fcmToken = request.get("fcmToken");
        pushNotificationService.removeDeviceToken(accountId, fcmToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get notifications list")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomAccountDetail accountDetail,
            Pageable pageable) {
        UUID accountId = accountDetail.getId();
        Page<NotificationResponse> notifications = pushNotificationService
                .getNotifications(accountId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomAccountDetail accountDetail) {
        UUID accountId = accountDetail.getId();
        Long count = pushNotificationService.countUnread(accountId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomAccountDetail accountDetail) {
        UUID accountId = accountDetail.getId();
        pushNotificationService.markAsRead(accountId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/mark-all-as-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomAccountDetail accountDetail) {
        UUID accountId = accountDetail.getId();
        pushNotificationService.markAllAsRead(accountId);
        return ResponseEntity.noContent().build();
    }
}

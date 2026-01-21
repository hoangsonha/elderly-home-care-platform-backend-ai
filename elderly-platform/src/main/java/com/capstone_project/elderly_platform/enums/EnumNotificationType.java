package com.capstone_project.elderly_platform.enums;

public enum EnumNotificationType {
    // Care Service Related
    NEW_CARE_SERVICE_REQUEST,
    CARE_SERVICE_ACCEPTED,
    CARE_SERVICE_REJECTED,
    CARE_SERVICE_COMPLETED,
    CARE_SERVICE_CANCELLED,

    // Work Schedule Related
    NEW_WORK_SCHEDULE,
    WORK_SCHEDULE_UPDATED,
    WORK_SCHEDULE_REMINDER,

    // Payment Related
    PAYMENT_REQUIRED,
    PAYMENT_RECEIVED,
    PAYMENT_COMPLETED,

    // Rating & Feedback
    NEW_RATING,
    NEW_FEEDBACK,

    // Chat Related
    CHAT_MESSAGE,

    // System
    SYSTEM_ANNOUNCEMENT,
    ACCOUNT_VERIFIED
}

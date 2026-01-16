package com.capstone_project.elderly_platform.dtos.request;

import com.capstone_project.elderly_platform.enums.EnumFeedbackTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateFeedbackRequest {

    @NotNull(message = "Target type is required")
    EnumFeedbackTargetType targetType; // SERVICE, SYSTEM, DISPUTE

    @NotNull(message = "Target ID is required")
    UUID targetId; // care_service_id, dispute_id, etc.

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    Integer rating; // General rating from 1 to 5

    // Detailed ratings for SERVICE feedback only
    // Chuyên môn, Thái độ, Đúng giờ, Chất lượng
    @Min(value = 1, message = "Professionalism rating must be at least 1")
    @Max(value = 5, message = "Professionalism rating must be at most 5")
    Integer professionalism; // Chuyên môn (1-5)

    @Min(value = 1, message = "Attitude rating must be at least 1")
    @Max(value = 5, message = "Attitude rating must be at most 5")
    Integer attitude; // Thái độ (1-5)

    @Min(value = 1, message = "Punctuality rating must be at least 1")
    @Max(value = 5, message = "Punctuality rating must be at most 5")
    Integer punctuality; // Đúng giờ (1-5)

    @Min(value = 1, message = "Quality rating must be at least 1")
    @Max(value = 5, message = "Quality rating must be at most 5")
    Integer quality; // Chất lượng (1-5)

    String comment; // Feedback comment/text
}

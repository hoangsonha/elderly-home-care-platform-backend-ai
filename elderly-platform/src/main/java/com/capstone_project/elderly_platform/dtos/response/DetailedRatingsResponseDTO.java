package com.capstone_project.elderly_platform.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DetailedRatingsResponseDTO {
    Integer professionalism; // Chuyên môn
    Integer attitude; // Thái độ
    Integer punctuality; // Đúng giờ
    Integer quality; // Chất lượng
}

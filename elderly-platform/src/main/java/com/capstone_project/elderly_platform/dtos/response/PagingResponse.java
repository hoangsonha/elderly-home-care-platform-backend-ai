package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagingResponse {
    String code;
    String message;
    int currentPage;
    int totalPages;
    int elementPerPage;
    long totalElements;
    Object data;
}

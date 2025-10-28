package com.capstone_project.elderly_platform.dtos.request;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class SearchAircraftCodeRequest {
    String code;
}

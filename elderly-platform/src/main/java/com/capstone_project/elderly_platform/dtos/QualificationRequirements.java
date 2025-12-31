package com.capstone_project.elderly_platform.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QualificationRequirements {

    List<String> skills;

    @JsonProperty("certificate_groups")
    List<List<UUID>> certificateGroups;
}





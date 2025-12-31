package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.response.QualificationTypeResponseDTO;

import java.util.List;

public interface QualificationTypeService {

    List<QualificationTypeResponseDTO> getAllActiveQualificationTypes();

    List<QualificationTypeResponseDTO> getAllQualificationTypes();
}





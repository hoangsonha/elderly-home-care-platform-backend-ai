package com.capstone_project.elderly_platform.services.externals.firebase;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FirebaseStorageService {
    String uploadSingleImages(MultipartFile imageFile);

    String uploadSingleVideo(MultipartFile videoFile);

    List<String> uploadMultipleImages(List<MultipartFile> imageFiles);

    List<String> uploadMultipleVideos(List<MultipartFile> videoFiles);

    String uploadFile(MultipartFile file);
}

package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.services.externals.firebase.FirebaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/v1/files")
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "API for testing file uploads to Firebase Storage")
public class FileUploadController {

    private final FirebaseStorageService firebaseStorageService;

    @Operation(summary = "Upload single image", description = "Upload a single image file to Firebase Storage")
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> uploadSingleImage(
            @RequestPart("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ObjectResponse("Failed", "File is empty", null));
            }

            String imageUrl = firebaseStorageService.uploadSingleImages(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("contentType", file.getContentType());
            result.put("url", imageUrl);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Image uploaded successfully", result));
        } catch (Exception e) {
            log.error("Error uploading image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ObjectResponse("Failed", "Upload failed: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Upload single video", description = "Upload a single video file to Firebase Storage")
    @PostMapping(value = "/upload/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> uploadSingleVideo(
            @RequestPart("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ObjectResponse("Failed", "File is empty", null));
            }

            String videoUrl = firebaseStorageService.uploadSingleVideo(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("contentType", file.getContentType());
            result.put("url", videoUrl);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Video uploaded successfully", result));
        } catch (Exception e) {
            log.error("Error uploading video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ObjectResponse("Failed", "Upload failed: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Upload multiple images", description = "Upload multiple image files to Firebase Storage")
    @PostMapping(value = "/upload/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> uploadMultipleImages(
            @RequestPart("files") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ObjectResponse("Failed", "No files provided", null));
            }

            List<String> imageUrls = firebaseStorageService.uploadMultipleImages(files);
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalFiles", files.size());
            result.put("uploadedCount", imageUrls.size());
            result.put("urls", imageUrls);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", 
                            "Uploaded " + imageUrls.size() + " out of " + files.size() + " images", 
                            result));
        } catch (Exception e) {
            log.error("Error uploading multiple images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ObjectResponse("Failed", "Upload failed: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Upload multiple videos", description = "Upload multiple video files to Firebase Storage")
    @PostMapping(value = "/upload/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> uploadMultipleVideos(
            @RequestPart("files") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ObjectResponse("Failed", "No files provided", null));
            }

            List<String> videoUrls = firebaseStorageService.uploadMultipleVideos(files);
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalFiles", files.size());
            result.put("uploadedCount", videoUrls.size());
            result.put("urls", videoUrls);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", 
                            "Uploaded " + videoUrls.size() + " out of " + files.size() + " videos", 
                            result));
        } catch (Exception e) {
            log.error("Error uploading multiple videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ObjectResponse("Failed", "Upload failed: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Upload any file", description = "Upload any file type to Firebase Storage (auto-detect type)")
    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> uploadFile(
            @RequestPart("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ObjectResponse("Failed", "File is empty", null));
            }

            String fileUrl = firebaseStorageService.uploadFile(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("contentType", file.getContentType());
            result.put("url", fileUrl);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "File uploaded successfully", result));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ObjectResponse("Failed", "Upload failed: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Test endpoint", description = "Test if the file upload API is working")
    @GetMapping("/test")
    public ResponseEntity<ObjectResponse> testEndpoint() {
        Map<String, String> info = new HashMap<>();
        info.put("status", "API is running");
        info.put("service", "Firebase Storage Upload");
        info.put("version", "1.0");
        
        return ResponseEntity.ok(new ObjectResponse("Success", "File upload API is working", info));
    }
}






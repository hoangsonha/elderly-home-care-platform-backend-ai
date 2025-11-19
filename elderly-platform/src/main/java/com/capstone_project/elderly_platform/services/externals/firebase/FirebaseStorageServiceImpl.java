package com.capstone_project.elderly_platform.services.externals.firebase;

import com.capstone_project.elderly_platform.enums.EnumUploadType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    // Firebase

    @Value("${firebase.bucket.name}")
    private String bucketName;

    @Value("${firebase.get.stream}")
    private String fileConfigFirebase;

    @Value("${firebase.get.url}")
    private String urlFirebase;

    @Value("${firebase.get.folder}")
    private String folderContainImage;

    @Override
    public String uploadSingleVideo(MultipartFile videoFile) {
        try {
            String extension = getExtension(videoFile.getOriginalFilename());
            if (!isVideo(extension))
                throw new BadRequestException("Chỉ cho phép upload video");

            String fileName = UUID.randomUUID() + extension;
            File convertedFile = convertToFile(videoFile, fileName);
            String url = uploadToFirebase(convertedFile, fileName, videoFile.getContentType()); // <-- lấy đúng
                                                                                                // content-type
            convertedFile.delete();
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return "Video upload thất bại";
        }
    }

    @Override
    public String uploadSingleImages(MultipartFile imageFile) {
        try {
            String extension = getExtension(imageFile.getOriginalFilename());
            if (!isImage(extension))
                throw new BadRequestException("Must be an image");

            String fileName = UUID.randomUUID() + extension;
            File convertedFile = convertToFile(imageFile, fileName);
            String url = uploadToFirebase(convertedFile, fileName, imageFile.getContentType()); // <-- lấy đúng
                                                                                                // content-type
            convertedFile.delete();
            return url;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<String> uploadMultipleImages(List<MultipartFile> imageFiles) {
        List<String> uploadedUrls = new ArrayList<>();

        if (imageFiles == null || imageFiles.isEmpty()) {
            return uploadedUrls;
        }

        for (MultipartFile imageFile : imageFiles) {
            try {
                String extension = getExtension(imageFile.getOriginalFilename());
                if (!isImage(extension)) {
                    uploadedUrls.add("Lỗi: " + imageFile.getOriginalFilename() + " không phải là ảnh");
                    continue;
                }

                String fileName = UUID.randomUUID() + extension;
                File convertedFile = convertToFile(imageFile, fileName);
                String url = uploadToFirebase(convertedFile, fileName, imageFile.getContentType());
                convertedFile.delete();
                uploadedUrls.add(url);
            } catch (Exception e) {
                e.printStackTrace();
                uploadedUrls
                        .add("Lỗi upload: " + (imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename()
                                : "file không xác định"));
            }
        }

        return uploadedUrls;
    }

    @Override
    public List<String> uploadMultipleVideos(List<MultipartFile> videoFiles) {
        List<String> uploadedUrls = new ArrayList<>();

        if (videoFiles == null || videoFiles.isEmpty()) {
            return uploadedUrls;
        }

        for (MultipartFile videoFile : videoFiles) {
            try {
                String extension = getExtension(videoFile.getOriginalFilename());
                if (!isVideo(extension)) {
                    uploadedUrls.add("Lỗi: " + videoFile.getOriginalFilename() + " không phải là video");
                    continue;
                }

                String fileName = UUID.randomUUID() + extension;
                File convertedFile = convertToFile(videoFile, fileName);
                String url = uploadToFirebase(convertedFile, fileName, videoFile.getContentType());
                convertedFile.delete();
                uploadedUrls.add(url);
            } catch (Exception e) {
                e.printStackTrace();
                uploadedUrls
                        .add("Lỗi upload: " + (videoFile.getOriginalFilename() != null ? videoFile.getOriginalFilename()
                                : "file không xác định"));
            }
        }

        return uploadedUrls;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + getExtension(file.getOriginalFilename());
            File convertedFile = convertToFile(file, fileName);
            String url = uploadToFirebase(convertedFile, fileName, file.getContentType());
            convertedFile.delete();
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return "File upload thất bại: "
                    + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "file không xác định");
        }
    }

    private File convertToFile(MultipartFile multipartFile, String fileName) throws IOException {
        // File tempFile = new File(fileName);
        File tempFile = File.createTempFile("upload_", "_" + fileName);// create newFile ưith String of fileName (random
                                                                       // String + "extension") and save to Current
                                                                       // Working Directory or Java Virtual Machine
                                                                       // (JVM)
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }
        return tempFile;
    }

    private String uploadToFirebase(File file, String fileName, String contentType) throws IOException {
        String folder = folderContainImage + "/" + fileName;
        BlobId blobId = BlobId.of(bucketName, folder);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        Credentials credentials = getFirebaseCredentials();
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        storage.create(blobInfo, Files.readAllBytes(file.toPath()));

        return String.format(urlFirebase, URLEncoder.encode(folder, StandardCharsets.UTF_8));
    }

    /**
     * Lấy Firebase credentials theo thứ tự ưu tiên:
     * 1. Google Cloud default credentials (nếu chạy trên GCP với service account)
     * 2. FIREBASE_KEY_BASE64 - Base64 encoded JSON key từ biến môi trường (bảo mật
     * nhất)
     * 3. FIREBASE_KEY_PATH - Đường dẫn đến file key từ biến môi trường
     * 4. File key trong resources (fallback cho local dev)
     */
    private Credentials getFirebaseCredentials() throws IOException {
        // 1. Thử dùng Google Cloud default credentials (tự động nếu chạy trên GCP)
        try {
            Credentials defaultCredentials = GoogleCredentials.getApplicationDefault();
            System.out.println("*** Đang dùng Google Cloud Application Default Credentials");
            return defaultCredentials;
        } catch (IOException e) {
            // Không có default credentials, tiếp tục các phương án khác
            System.out.println("*** Không tìm thấy Application Default Credentials, thử các phương án khác...");
        }

        // 2. Thử lấy từ FIREBASE_KEY_BASE64 (Base64 encoded JSON)
        String keyBase64 = System.getenv("FIREBASE_KEY_BASE64");
        if (keyBase64 != null && !keyBase64.isBlank()) {
            try {
                System.out.println("*** Đang dùng FIREBASE_KEY_BASE64 từ biến môi trường");
                byte[] decodedBytes = Base64.getDecoder().decode(keyBase64);
                InputStream keyStream = new ByteArrayInputStream(decodedBytes);
                return GoogleCredentials.fromStream(keyStream);
            } catch (Exception e) {
                System.out.println("*** Lỗi decode FIREBASE_KEY_BASE64: " + e.getMessage());
                throw new ElementNotFoundException("Không thể decode FIREBASE_KEY_BASE64: " + e.getMessage());
            }
        }

        // 3. Thử lấy từ FIREBASE_KEY_PATH (đường dẫn file)
        String envPath = System.getenv("FIREBASE_KEY_PATH");
        if (envPath != null && !envPath.isBlank()) {
            try {
                System.out.println("*** Đang dùng FIREBASE_KEY_PATH từ biến môi trường: " + envPath);
                File keyFile = new File(envPath);
                if (!keyFile.exists()) {
                    throw new ElementNotFoundException("File key không tồn tại tại: " + envPath);
                }
                InputStream keyStream = new FileInputStream(keyFile);
                return GoogleCredentials.fromStream(keyStream);
            } catch (Exception e) {
                System.out.println("*** Lỗi đọc file từ FIREBASE_KEY_PATH: " + e.getMessage());
                throw new ElementNotFoundException("Không thể đọc file key từ FIREBASE_KEY_PATH: " + envPath);
            }
        }

        // 4. Fallback: Thử lấy từ resources (local dev)
        String fallbackPathInResources = "keys/key_firebase.json";
        System.out.println("*** Không có biến môi trường, dùng file trong resources: " + fallbackPathInResources);
        InputStream keyStream = FirebaseStorageServiceImpl.class.getClassLoader()
                .getResourceAsStream(fallbackPathInResources);

        if (keyStream == null) {
            throw new ElementNotFoundException(
                    "Không tìm thấy Firebase credentials. Vui lòng thiết lập một trong các cách sau:\n" +
                            "1. Chạy trên Google Cloud với service account\n" +
                            "2. Set biến môi trường FIREBASE_KEY_BASE64 (Base64 encoded JSON)\n" +
                            "3. Set biến môi trường FIREBASE_KEY_PATH (đường dẫn đến file key)\n" +
                            "4. Đặt file key_firebase.json trong resources/keys/");
        }

        return GoogleCredentials.fromStream(keyStream);
    }

    private EnumUploadType detectType(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        if (contentType == null && fileName != null) {
            // Fallback to extension-based detection if contentType is null
            String extension = getExtension(fileName);
            if (isImage(extension))
                return EnumUploadType.IMAGE;
            if (isVideo(extension))
                return EnumUploadType.VIDEO;
            if (isAudio(extension))
                return EnumUploadType.AUDIO;
            if (isPdf(extension))
                return EnumUploadType.PDF;
            if (isDocument(extension))
                return EnumUploadType.DOCUMENT;
            return EnumUploadType.OTHER;
        }

        if (contentType == null)
            return EnumUploadType.OTHER;

        if (contentType.startsWith("image"))
            return EnumUploadType.IMAGE;
        if (contentType.startsWith("video"))
            return EnumUploadType.VIDEO;
        if (contentType.startsWith("audio"))
            return EnumUploadType.AUDIO;
        if (contentType.equals("application/pdf"))
            return EnumUploadType.PDF;
        if (contentType.contains("document") || contentType.contains("msword") ||
                contentType.contains("spreadsheet") || contentType.contains("presentation") ||
                contentType.contains("text"))
            return EnumUploadType.DOCUMENT;
        return EnumUploadType.OTHER;
    }

    private String storeFile(MultipartFile file) {
        return file.getOriginalFilename();
    }

    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    private boolean isImage(String extension) {
        return extension.matches("(?i)\\.(jpg|jpeg|png|gif|bmp|webp|svg)$");
    }

    private boolean isVideo(String extension) {
        return extension.matches("(?i)\\.(mp4|mov|avi|mkv|wmv|flv|webm)$");
    }

    private boolean isAudio(String extension) {
        return extension.matches("(?i)\\.(mp3|wav|flac|aac|ogg|m4a|wma)$");
    }

    private boolean isPdf(String extension) {
        return extension.matches("(?i)\\.(pdf)$");
    }

    private boolean isDocument(String extension) {
        return extension.matches("(?i)\\.(doc|docx|xls|xlsx|ppt|pptx|txt|rtf|odt|ods|odp)$");
    }

}

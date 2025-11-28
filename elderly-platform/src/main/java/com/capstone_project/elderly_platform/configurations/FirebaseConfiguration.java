package com.capstone_project.elderly_platform.configurations;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Unified Firebase Configuration for both Cloud Messaging and Storage
 * Loads Firebase credentials once and creates all necessary beans
 */
@Configuration
@Slf4j
public class FirebaseConfiguration {

    @Value("${firebase.credentials.path:keys/key_firebase.json}")
    private String credentialsPath;

    @Value("${firebase.app.name:elderly-platform-app}")
    private String firebaseAppName;

    /**
     * Load Firebase credentials with multiple fallback options:
     * 1. FIREBASE_KEY_BASE64 environment variable (most secure for production)
     * 2. FIREBASE_CREDENTIALS_PATH environment variable (path to key file)
     * 3. File in classpath (for local development)
     */
    @Bean
    public GoogleCredentials firebaseCredentials() throws IOException {
        log.info("Loading Firebase credentials...");

        // Option 1: Try FIREBASE_KEY_BASE64 (Base64 encoded JSON)
        String keyBase64 = System.getenv("FIREBASE_KEY_BASE64");
        if (keyBase64 != null && !keyBase64.isBlank()) {
            log.info("Using FIREBASE_KEY_BASE64 from environment variable");
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(keyBase64);
                InputStream keyStream = new ByteArrayInputStream(decodedBytes);
                return GoogleCredentials.fromStream(keyStream);
            } catch (Exception e) {
                log.error("Failed to decode FIREBASE_KEY_BASE64: {}", e.getMessage());
                throw new IOException("Cannot decode FIREBASE_KEY_BASE64", e);
            }
        }

        // Option 2: Try FIREBASE_CREDENTIALS_PATH (file path from environment)
        String envPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
        if (envPath != null && !envPath.isBlank()) {
            log.info("Using FIREBASE_CREDENTIALS_PATH from environment: {}", envPath);
            try {
                File keyFile = new File(envPath);
                if (!keyFile.exists()) {
                    throw new IOException("Firebase key file not found at: " + envPath);
                }
                return GoogleCredentials.fromStream(new FileInputStream(keyFile));
            } catch (Exception e) {
                log.error("Failed to load from FIREBASE_CREDENTIALS_PATH: {}", e.getMessage());
                throw new IOException("Cannot read Firebase key from: " + envPath, e);
            }
        }

        // Option 3: Load from classpath (local development)
        log.info("Using Firebase key from classpath: {}", credentialsPath);
        try {
            InputStream keyStream = new ClassPathResource(credentialsPath).getInputStream();
            return GoogleCredentials.fromStream(keyStream);
        } catch (IOException e) {
            log.error("Firebase credentials not found at: src/main/resources/{}", credentialsPath);
            log.error("Please ensure the Firebase key file exists or set environment variables:");
            log.error("  - FIREBASE_KEY_BASE64: Base64 encoded JSON key (recommended for production)");
            log.error("  - FIREBASE_CREDENTIALS_PATH: Absolute path to JSON key file");
            log.error("  - Or place key_firebase.json in: src/main/resources/keys/");
            throw new IOException("Firebase credentials not found. Please configure credentials.", e);
        }
    }

    /**
     * Initialize Firebase App with credentials
     */
    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
        log.info("Initializing FirebaseApp...");

        // Check if app already exists
        try {
            FirebaseApp existingApp = FirebaseApp.getInstance(firebaseAppName);
            log.info("FirebaseApp '{}' already initialized", firebaseAppName);
            return existingApp;
        } catch (IllegalStateException e) {
            // App doesn't exist, create new one
            log.info("Creating new FirebaseApp: {}", firebaseAppName);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options, firebaseAppName);
        log.info("FirebaseApp '{}' initialized successfully", firebaseAppName);

        return app;
    }

    /**
     * Firebase Cloud Messaging Bean for sending push notifications
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        log.info("Creating FirebaseMessaging bean...");
        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
        log.info("FirebaseMessaging bean created successfully");
        return messaging;
    }

    /**
     * Firebase Storage Bean for uploading/downloading files
     */
    @Bean
    public Storage firebaseStorage(GoogleCredentials credentials) {
        log.info("Creating Firebase Storage bean...");
        Storage storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
        log.info("Firebase Storage bean created successfully");
        return storage;
    }
}

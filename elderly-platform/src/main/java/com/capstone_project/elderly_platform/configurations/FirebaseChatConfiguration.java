package com.capstone_project.elderly_platform.configurations;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
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
 * Firebase Configuration for Firestore Chat (Project Mới)
 * Tách riêng với FirebaseConfiguration (dùng cho Storage và FCM - Project Cũ)
 */
@Configuration
@Slf4j
public class FirebaseChatConfiguration {

    @Value("${firebase.chat.credentials.path:keys/key_firebase_chat.json}")
    private String chatCredentialsPath;

    @Value("${firebase.chat.app.name:chat-firebase-app}")
    private String chatAppName;

    /**
     * Load Firebase credentials cho Firestore (Project Mới)
     * Support multiple fallback options giống FirebaseConfiguration
     */
    @Bean("chatFirebaseCredentials")
    public GoogleCredentials chatFirebaseCredentials() throws IOException {
        log.info("Loading Firebase Chat credentials...");

        // Option 1: Try FIREBASE_CHAT_KEY_BASE64 (Base64 encoded JSON)
        String keyBase64 = System.getenv("FIREBASE_CHAT_KEY_BASE64");
        if (keyBase64 != null && !keyBase64.isBlank()) {
            log.info("Using FIREBASE_CHAT_KEY_BASE64 from environment variable");
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(keyBase64);
                InputStream keyStream = new ByteArrayInputStream(decodedBytes);
                return GoogleCredentials.fromStream(keyStream);
            } catch (Exception e) {
                log.error("Failed to decode FIREBASE_CHAT_KEY_BASE64: {}", e.getMessage());
                throw new IOException("Cannot decode FIREBASE_CHAT_KEY_BASE64", e);
            }
        }

        // Option 2: Try FIREBASE_CHAT_CREDENTIALS_PATH (file path from environment)
        String envPath = System.getenv("FIREBASE_CHAT_CREDENTIALS_PATH");
        if (envPath != null && !envPath.isBlank()) {
            log.info("Using FIREBASE_CHAT_CREDENTIALS_PATH from environment: {}", envPath);
            try {
                File keyFile = new File(envPath);
                if (!keyFile.exists()) {
                    throw new IOException("Firebase Chat key file not found at: " + envPath);
                }
                return GoogleCredentials.fromStream(new FileInputStream(keyFile));
            } catch (Exception e) {
                log.error("Failed to load from FIREBASE_CHAT_CREDENTIALS_PATH: {}", e.getMessage());
                throw new IOException("Cannot read Firebase Chat key from: " + envPath, e);
            }
        }

        // Option 3: Load from classpath (local development)
        log.info("Using Firebase Chat key from classpath: {}", chatCredentialsPath);
        try {
            InputStream keyStream = new ClassPathResource(chatCredentialsPath).getInputStream();
            return GoogleCredentials.fromStream(keyStream);
        } catch (IOException e) {
            log.error("Firebase Chat credentials not found at: src/main/resources/{}", chatCredentialsPath);
            log.error("Please ensure the Firebase Chat key file exists or set environment variables:");
            log.error("  - FIREBASE_CHAT_KEY_BASE64: Base64 encoded JSON key (recommended for production)");
            log.error("  - FIREBASE_CHAT_CREDENTIALS_PATH: Absolute path to JSON key file");
            log.error("  - Or place key_firebase_chat.json in: src/main/resources/keys/");
            throw new IOException("Firebase Chat credentials not found. Please configure credentials.", e);
        }
    }

    /**
     * Initialize Firebase App cho Firestore (Project Mới)
     * Tách riêng với FirebaseApp của Storage/FCM
     */
    @Bean("chatFirebaseApp")
    public FirebaseApp chatFirebaseApp(GoogleCredentials chatFirebaseCredentials) throws IOException {
        log.info("Initializing Firebase Chat App...");

        // Check if app already exists
        try {
            FirebaseApp existingApp = FirebaseApp.getInstance(chatAppName);
            log.info("Firebase Chat App '{}' already initialized", chatAppName);
            return existingApp;
        } catch (IllegalStateException e) {
            // App doesn't exist, create new one
            log.info("Creating new Firebase Chat App: {}", chatAppName);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(chatFirebaseCredentials)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options, chatAppName);
        log.info("Firebase Chat App '{}' initialized successfully", chatAppName);

        return app;
    }

    /**
     * Firestore Bean cho Chat
     * Dùng FirebaseApp riêng (project mới)
     */
    @Bean
    public Firestore firestore(FirebaseApp chatFirebaseApp) {
        log.info("Creating Firestore bean for chat...");
        Firestore firestore = FirestoreClient.getFirestore(chatFirebaseApp);
        log.info("Firestore bean created successfully");
        return firestore;
    }

    /**
     * Firebase Cloud Messaging Bean cho FCM (Project Mới)
     * Dùng project mới để gửi FCM notifications
     * Tách riêng với FirebaseMessaging của FirebaseConfiguration (project cũ)
     */
    @Bean("chatFirebaseMessaging")
    public com.google.firebase.messaging.FirebaseMessaging chatFirebaseMessaging(FirebaseApp chatFirebaseApp) {
        log.info("Creating FirebaseMessaging bean for FCM (project mới)...");
        com.google.firebase.messaging.FirebaseMessaging messaging = 
            com.google.firebase.messaging.FirebaseMessaging.getInstance(chatFirebaseApp);
        log.info("FirebaseMessaging bean (project mới) created successfully");
        return messaging;
    }
}

package com.yourname.securedrive.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Service class for setting up and managing Google Drive API connections.
 * Handles authentication and provides a configured Drive service instance.
 */
public class DriveService {
    
    private static final Logger logger = LoggerFactory.getLogger(DriveService.class);
    private static final String APPLICATION_NAME = "SecureDriveUploader";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";
    
    /**
     * Global instance of the scopes required by this application.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    
    /**
     * Creates an authorized Credential object.
     * 
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        logger.info("Loading Google Drive credentials");
        
        // Load client secrets
        Path credentialsPath = Paths.get(CREDENTIALS_FILE_PATH);
        if (!Files.exists(credentialsPath)) {
            throw new FileNotFoundException("Credentials file not found: " + CREDENTIALS_FILE_PATH);
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, 
            new FileReader(credentialsPath.toFile()));
        
        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        
        logger.info("Starting OAuth2 authorization flow");
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        
        logger.info("Successfully authorized with Google Drive");
        return credential;
    }
    
    /**
     * Creates and returns a configured Google Drive service instance.
     * 
     * @return A configured Drive service instance
     * @throws IOException if credentials or network operations fail
     * @throws GeneralSecurityException if security operations fail
     */
    public static Drive getDriveService() throws IOException, GeneralSecurityException {
        logger.info("Initializing Google Drive service");
        
        // Build a new authorized API client service
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        
        logger.info("Google Drive service initialized successfully");
        return service;
    }
    
    /**
     * Checks if the application has valid credentials for Google Drive access.
     * 
     * @return true if credentials are available and valid
     */
    public static boolean hasValidCredentials() {
        try {
            // Check if credentials file exists
            Path credentialsPath = Paths.get(CREDENTIALS_FILE_PATH);
            if (!Files.exists(credentialsPath)) {
                logger.warn("Credentials file not found: {}", CREDENTIALS_FILE_PATH);
                return false;
            }
            
            // Check if tokens directory exists (indicates previous authorization)
            Path tokensPath = Paths.get(TOKENS_DIRECTORY_PATH);
            if (!Files.exists(tokensPath)) {
                logger.info("No previous authorization found");
                return false;
            }
            
            // Try to create a service instance to validate credentials
            getDriveService();
            return true;
            
        } catch (Exception e) {
            logger.warn("Invalid or expired credentials: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Clears stored authentication tokens, forcing re-authorization on next use.
     */
    public static void clearCredentials() {
        try {
            Path tokensPath = Paths.get(TOKENS_DIRECTORY_PATH);
            if (Files.exists(tokensPath)) {
                Files.walk(tokensPath)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete token file: {}", path, e);
                        }
                    });
                logger.info("Cleared stored authentication tokens");
            }
        } catch (IOException e) {
            logger.error("Error clearing credentials", e);
        }
    }
    
    /**
     * Gets the path to the credentials file.
     * 
     * @return Path to the credentials.json file
     */
    public static Path getCredentialsFilePath() {
        return Paths.get(CREDENTIALS_FILE_PATH);
    }
    
    /**
     * Gets the path to the tokens directory.
     * 
     * @return Path to the tokens directory
     */
    public static Path getTokensDirectoryPath() {
        return Paths.get(TOKENS_DIRECTORY_PATH);
    }
}

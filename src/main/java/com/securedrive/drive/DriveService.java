package com.securedrive.drive;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Service class for Google Drive API integration.
 * Handles authentication, authorization, and service initialization.
 */
public class DriveService {
    
    private static final Logger logger = LoggerFactory.getLogger(DriveService.class);
    private static final String APPLICATION_NAME = "SecureDriveUploader";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = ".securedrive/tokens";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    
    /**
     * Global instance of the scopes required by this application.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    
    private Drive driveService;
    private boolean isInitialized = false;
    
    /**
     * Constructor for DriveService.
     */
    public DriveService() {
        logger.info("DriveService created");
    }
    
    /**
     * Initialize the Google Drive service.
     * This method handles authentication and service setup.
     * 
     * @throws Exception If initialization fails
     */
    public void initialize() throws Exception {
        if (isInitialized) {
            logger.debug("DriveService already initialized");
            return;
        }
        
        try {
            logger.info("Initializing Google Drive service");
            
            // Build a new authorized API client service
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            
            this.driveService = service;
            this.isInitialized = true;
            
            logger.info("Google Drive service initialized successfully");
            
        } catch (GeneralSecurityException e) {
            logger.error("Security error during Drive service initialization: {}", e.getMessage());
            throw new Exception("Security error: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("IO error during Drive service initialization: {}", e.getMessage());
            throw new Exception("IO error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during Drive service initialization: {}", e.getMessage());
            throw new Exception("Initialization failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Authenticate with Google Drive (convenience method for GUI).
     * This method initializes the service and performs authentication.
     * 
     * @return True if authentication successful, false otherwise
     */
    public boolean authenticate() {
        try {
            initialize();
            return testConnection();
        } catch (Exception e) {
            logger.error("Authentication failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the authenticated user's email address.
     * 
     * @return User email address, or null if not available
     */
    public String getUserEmail() {
        try {
            if (!isInitialized) {
                return null;
            }
            
            Drive.About.Get aboutRequest = driveService.about().get()
                .setFields("user(emailAddress)");
            
            com.google.api.services.drive.model.About about = aboutRequest.execute();
            com.google.api.services.drive.model.User user = about.getUser();
            
            return user.getEmailAddress();
            
        } catch (Exception e) {
            logger.error("Failed to get user email: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Sign out from Google Drive (clear authentication).
     */
    public void signOut() {
        logger.info("Signing out from Google Drive");
        
        this.driveService = null;
        this.isInitialized = false;
        
        // Clear stored tokens
        try {
            Path tokensPath = Paths.get(System.getProperty("user.home"), TOKENS_DIRECTORY_PATH);
            if (tokensPath.toFile().exists()) {
                deleteDirectory(tokensPath.toFile());
                logger.info("Cleared stored authentication tokens");
            }
        } catch (Exception e) {
            logger.warn("Failed to clear stored tokens: {}", e.getMessage());
        }
    }
    
    /**
     * Get the Google Drive service instance.
     * 
     * @return The Drive service instance
     * @throws IllegalStateException If service is not initialized
     */
    public Drive getDriveService() {
        if (!isInitialized || driveService == null) {
            throw new IllegalStateException("Drive service not initialized. Call initialize() first.");
        }
        return driveService;
    }
    
    /**
     * Check if the service is initialized.
     * 
     * @return True if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Creates an authorized Credential object.
     * 
     * @param HTTP_TRANSPORT The network HTTP Transport
     * @return An authorized Credential object
     * @throws IOException If the credentials.json file cannot be found
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets
        InputStream in = DriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        
        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        
        logger.info("Starting OAuth2 authorization flow");
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        
        logger.info("OAuth2 authorization completed successfully");
        return credential;
    }
    
    /**
     * Test the connection to Google Drive.
     * 
     * @return True if connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            if (!isInitialized) {
                logger.warn("Service not initialized, cannot test connection");
                return false;
            }
            
            // Try to list files to test connection
            Drive.Files.List request = driveService.files().list()
                .setPageSize(1)
                .setFields("nextPageToken, files(id, name)");
            
            request.execute();
            
            logger.info("Google Drive connection test successful");
            return true;
            
        } catch (Exception e) {
            logger.error("Google Drive connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get information about the authenticated user.
     * 
     * @return User information string
     * @throws Exception If user info cannot be retrieved
     */
    public String getUserInfo() throws Exception {
        if (!isInitialized) {
            throw new IllegalStateException("Service not initialized");
        }
        
        try {
            // Get user info from the about endpoint
            Drive.About.Get aboutRequest = driveService.about().get()
                .setFields("user(displayName,emailAddress)");
            
            com.google.api.services.drive.model.About about = aboutRequest.execute();
            com.google.api.services.drive.model.User user = about.getUser();
            
            return String.format("User: %s (%s)", 
                               user.getDisplayName(), user.getEmailAddress());
            
        } catch (Exception e) {
            logger.error("Failed to get user info: {}", e.getMessage());
            throw new Exception("Could not retrieve user information: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the available storage quota information.
     * 
     * @return Storage quota information string
     * @throws Exception If quota info cannot be retrieved
     */
    public String getStorageQuota() throws Exception {
        if (!isInitialized) {
            throw new IllegalStateException("Service not initialized");
        }
        
        try {
            // Get storage quota from the about endpoint
            Drive.About.Get aboutRequest = driveService.about().get()
                .setFields("storageQuota");
            
            com.google.api.services.drive.model.About about = aboutRequest.execute();
            Object quota = about.getStorageQuota();
            
            if (quota == null) {
                return "Storage quota information not available";
            }
            
            // Try to get quota information using reflection or return basic info
            return "Storage quota information available (details may vary by API version)";
            
        } catch (Exception e) {
            logger.error("Failed to get storage quota: {}", e.getMessage());
            return "Storage quota information not available: " + e.getMessage();
        }
    }
    
    /**
     * Format bytes into human-readable format.
     * 
     * @param bytes The number of bytes
     * @return Formatted string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes < 1024L * 1024 * 1024 * 1024) {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else {
            return String.format("%.1f TB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Reset the service (clear authentication and reinitialize).
     * 
     * @throws Exception If reset fails
     */
    public void reset() throws Exception {
        logger.info("Resetting Google Drive service");
        
        this.driveService = null;
        this.isInitialized = false;
        
        // Clear stored tokens
        try {
            Path tokensPath = Paths.get(System.getProperty("user.home"), TOKENS_DIRECTORY_PATH);
            if (tokensPath.toFile().exists()) {
                deleteDirectory(tokensPath.toFile());
                logger.info("Cleared stored authentication tokens");
            }
        } catch (Exception e) {
            logger.warn("Failed to clear stored tokens: {}", e.getMessage());
        }
        
        // Reinitialize
        initialize();
    }
    
    /**
     * Recursively delete a directory and its contents.
     * 
     * @param directory The directory to delete
     */
    private void deleteDirectory(java.io.File directory) {
        if (directory.exists()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}

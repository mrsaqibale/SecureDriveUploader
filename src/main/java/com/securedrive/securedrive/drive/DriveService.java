package com.securedrive.securedrive.drive;

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
 * Handles authentication, credential management, and Drive service initialization.
 */
public class DriveService {
    
    private static final Logger logger = LoggerFactory.getLogger(DriveService.class);
    
    private static final String APPLICATION_NAME = "SecureDriveUploader";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = ".securedrive/tokens";
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";
    
    /**
     * Global instance of the scopes required by this application.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    
    private Drive drive;
    private Credential credential;
    
    /**
     * Constructor for DriveService.
     * Initializes the Google Drive service with authentication.
     * 
     * @throws IOException if service initialization fails
     */
    public DriveService() throws IOException {
        initializeDriveService();
        logger.debug("DriveService initialized");
    }
    
    /**
     * Gets the initialized Drive service instance.
     * 
     * @return the Drive service
     */
    public Drive getDrive() {
        return drive;
    }
    
    /**
     * Gets the current credential.
     * 
     * @return the credential
     */
    public Credential getCredential() {
        return credential;
    }
    
    /**
     * Initializes the Google Drive service.
     * 
     * @throws IOException if initialization fails
     */
    private void initializeDriveService() throws IOException {
        try {
            // Build a new authorized API client service
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            
            // Load client secrets
            GoogleClientSecrets clientSecrets = loadClientSecrets();
            
            // Build flow and trigger user authorization request
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();
            
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            
            // Build the Drive service
            drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            
            logger.info("Google Drive service initialized successfully");
            
        } catch (GeneralSecurityException e) {
            logger.error("Security error initializing Drive service", e);
            throw new IOException("Failed to initialize Drive service due to security error", e);
        } catch (IOException e) {
            logger.error("IO error initializing Drive service", e);
            throw e;
        }
    }
    
    /**
     * Loads client secrets from the credentials file.
     * 
     * @return the client secrets
     * @throws IOException if loading fails
     */
    private GoogleClientSecrets loadClientSecrets() throws IOException {
        Path credentialsPath = Paths.get(CREDENTIALS_FILE_PATH);
        
        if (!Files.exists(credentialsPath)) {
            throw new IOException("Credentials file not found: " + CREDENTIALS_FILE_PATH + 
                    "\nPlease download your OAuth 2.0 client credentials from the Google Cloud Console.");
        }
        
        try (InputStream in = Files.newInputStream(credentialsPath)) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            
            if (clientSecrets.getDetails().getClientId() == null || 
                clientSecrets.getDetails().getClientSecret() == null) {
                throw new IOException("Invalid credentials file format");
            }
            
            logger.debug("Client secrets loaded successfully");
            return clientSecrets;
        }
    }
    
    /**
     * Checks if the service is properly authenticated.
     * 
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        try {
            if (drive == null || credential == null) {
                return false;
            }
            
            // Try to get user info to verify authentication
            drive.about().get().setFields("user").execute();
            return true;
        } catch (IOException e) {
            logger.warn("Authentication check failed", e);
            return false;
        }
    }
    
    /**
     * Refreshes the authentication token if needed.
     * 
     * @return true if refresh was successful, false otherwise
     */
    public boolean refreshToken() {
        try {
            if (credential != null && credential.getRefreshToken() != null) {
                credential.refreshToken();
                logger.info("Authentication token refreshed successfully");
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to refresh authentication token", e);
        }
        return false;
    }
    
    /**
     * Revokes the current authentication token.
     * 
     * @return true if revocation was successful, false otherwise
     */
    public boolean revokeToken() {
        try {
            if (credential != null) {
                credential.revoke();
                logger.info("Authentication token revoked successfully");
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to revoke authentication token", e);
        }
        return false;
    }
    
    /**
     * Clears stored tokens and forces re-authentication.
     * 
     * @return true if tokens were cleared successfully, false otherwise
     */
    public boolean clearStoredTokens() {
        try {
            Path tokensPath = Paths.get(TOKENS_DIRECTORY_PATH);
            if (Files.exists(tokensPath)) {
                deleteDirectory(tokensPath.toFile());
                logger.info("Stored tokens cleared successfully");
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to clear stored tokens", e);
        }
        return false;
    }
    
    /**
     * Gets the application name.
     * 
     * @return the application name
     */
    public String getApplicationName() {
        return APPLICATION_NAME;
    }
    
    /**
     * Gets the tokens directory path.
     * 
     * @return the tokens directory path
     */
    public String getTokensDirectoryPath() {
        return TOKENS_DIRECTORY_PATH;
    }
    
    /**
     * Gets the credentials file path.
     * 
     * @return the credentials file path
     */
    public String getCredentialsFilePath() {
        return CREDENTIALS_FILE_PATH;
    }
    
    /**
     * Gets the required scopes.
     * 
     * @return the list of required scopes
     */
    public List<String> getScopes() {
        return Collections.unmodifiableList(SCOPES);
    }
    
    /**
     * Recursively deletes a directory and all its contents.
     * 
     * @param directory the directory to delete
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteDirectory(java.io.File directory) {
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
        }
        return directory.delete();
    }
    
    /**
     * Creates a sample credentials file template.
     * 
     * @return true if template was created successfully, false otherwise
     */
    public boolean createCredentialsTemplate() {
        try {
            Path credentialsPath = Paths.get(CREDENTIALS_FILE_PATH);
            
            if (Files.exists(credentialsPath)) {
                logger.warn("Credentials file already exists");
                return false;
            }
            
            String template = "{\n" +
                    "  \"installed\": {\n" +
                    "    \"client_id\": \"YOUR_CLIENT_ID.apps.googleusercontent.com\",\n" +
                    "    \"project_id\": \"your-project-id\",\n" +
                    "    \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "    \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "    \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "    \"client_secret\": \"YOUR_CLIENT_SECRET\",\n" +
                    "    \"redirect_uris\": [\"http://localhost:8888\"]\n" +
                    "  }\n" +
                    "}";
            
            Files.write(credentialsPath, template.getBytes());
            logger.info("Credentials template created at: {}", credentialsPath);
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to create credentials template", e);
            return false;
        }
    }
}

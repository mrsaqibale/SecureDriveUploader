package com.securedrive.securedrive.drive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Handles uploading encrypted files to Google Drive.
 * Provides functionality to upload, list, and manage files in Google Drive.
 */
public class DriveUploader {
    
    private static final Logger logger = LoggerFactory.getLogger(DriveUploader.class);
    
    private static final String FOLDER_NAME = "SecureDriveUploader";
    private static final String FOLDER_DESCRIPTION = "Encrypted files uploaded by SecureDriveUploader";
    private static final String MIME_TYPE_APPLICATION_OCTET = "application/octet-stream";
    
    private final DriveService driveService;
    private String appFolderId;
    
    /**
     * Constructor for DriveUploader.
     * 
     * @throws IOException if Drive service initialization fails
     */
    public DriveUploader() throws IOException {
        this.driveService = new DriveService();
        initializeAppFolder();
        logger.debug("DriveUploader initialized");
    }
    
    /**
     * Uploads a file to Google Drive.
     * 
     * @param file the file to upload
     * @return the file ID of the uploaded file
     * @throws IOException if upload fails
     */
    public String uploadFile(java.io.File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist or is null");
        }
        
        logger.info("Starting upload of file: {}", file.getName());
        
        // Create file metadata
        File fileMetadata = new File();
        fileMetadata.setName(file.getName());
        fileMetadata.setParents(Collections.singletonList(appFolderId));
        fileMetadata.setDescription("Encrypted file uploaded by SecureDriveUploader");
        
        // Set file content
        FileContent mediaContent = new FileContent(MIME_TYPE_APPLICATION_OCTET, file);
        
        // Upload the file
        Drive drive = driveService.getDrive();
        File uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id,name,size,createdTime,modifiedTime")
                .execute();
        
        logger.info("File uploaded successfully: {} (ID: {})", file.getName(), uploadedFile.getId());
        return uploadedFile.getId();
    }
    
    /**
     * Lists all files in the application folder.
     * 
     * @return list of files in the app folder
     * @throws IOException if listing fails
     */
    public List<File> listFiles() throws IOException {
        logger.debug("Listing files in app folder");
        
        Drive drive = driveService.getDrive();
        FileList result = drive.files().list()
                .setQ("'" + appFolderId + "' in parents and trashed=false")
                .setFields("files(id,name,size,createdTime,modifiedTime,description)")
                .execute();
        
        List<File> files = result.getFiles();
        logger.debug("Found {} files in app folder", files.size());
        
        return files;
    }
    
    /**
     * Downloads a file from Google Drive.
     * 
     * @param fileId the ID of the file to download
     * @param outputFile the file to save the downloaded content to
     * @throws IOException if download fails
     */
    public void downloadFile(String fileId, java.io.File outputFile) throws IOException {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("File ID cannot be null or empty");
        }
        
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
        
        logger.info("Downloading file with ID: {} to {}", fileId, outputFile.getName());
        
        Drive drive = driveService.getDrive();
        drive.files().get(fileId).executeMediaAndDownloadTo(new java.io.FileOutputStream(outputFile));
        
        logger.info("File downloaded successfully: {}", outputFile.getName());
    }
    
    /**
     * Deletes a file from Google Drive.
     * 
     * @param fileId the ID of the file to delete
     * @throws IOException if deletion fails
     */
    public void deleteFile(String fileId) throws IOException {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("File ID cannot be null or empty");
        }
        
        logger.info("Deleting file with ID: {}", fileId);
        
        Drive drive = driveService.getDrive();
        drive.files().delete(fileId).execute();
        
        logger.info("File deleted successfully: {}", fileId);
    }
    
    /**
     * Gets file information from Google Drive.
     * 
     * @param fileId the ID of the file
     * @return file metadata
     * @throws IOException if retrieval fails
     */
    public File getFileInfo(String fileId) throws IOException {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("File ID cannot be null or empty");
        }
        
        logger.debug("Getting file info for ID: {}", fileId);
        
        Drive drive = driveService.getDrive();
        File file = drive.files().get(fileId)
                .setFields("id,name,size,createdTime,modifiedTime,description,parents")
                .execute();
        
        return file;
    }
    
    /**
     * Searches for files by name in the application folder.
     * 
     * @param fileName the name to search for
     * @return list of matching files
     * @throws IOException if search fails
     */
    public List<File> searchFiles(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        
        logger.debug("Searching for files with name: {}", fileName);
        
        Drive drive = driveService.getDrive();
        String query = "'" + appFolderId + "' in parents and name contains '" + fileName + "' and trashed=false";
        
        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id,name,size,createdTime,modifiedTime,description)")
                .execute();
        
        List<File> files = result.getFiles();
        logger.debug("Found {} files matching search criteria", files.size());
        
        return files;
    }
    
    /**
     * Gets the application folder ID.
     * 
     * @return the folder ID
     */
    public String getAppFolderId() {
        return appFolderId;
    }
    
    /**
     * Initializes the application folder in Google Drive.
     * Creates the folder if it doesn't exist.
     * 
     * @throws IOException if folder creation/retrieval fails
     */
    private void initializeAppFolder() throws IOException {
        logger.debug("Initializing application folder");
        
        // First, try to find existing folder
        appFolderId = findAppFolder();
        
        if (appFolderId == null) {
            // Create new folder if it doesn't exist
            appFolderId = createAppFolder();
        }
        
        logger.info("Application folder initialized with ID: {}", appFolderId);
    }
    
    /**
     * Finds the existing application folder.
     * 
     * @return the folder ID if found, null otherwise
     * @throws IOException if search fails
     */
    private String findAppFolder() throws IOException {
        Drive drive = driveService.getDrive();
        String query = "name='" + FOLDER_NAME + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
        
        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id,name)")
                .execute();
        
        List<File> folders = result.getFiles();
        if (!folders.isEmpty()) {
            return folders.get(0).getId();
        }
        
        return null;
    }
    
    /**
     * Creates a new application folder.
     * 
     * @return the created folder ID
     * @throws IOException if folder creation fails
     */
    private String createAppFolder() throws IOException {
        logger.info("Creating new application folder: {}", FOLDER_NAME);
        
        File folderMetadata = new File();
        folderMetadata.setName(FOLDER_NAME);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setDescription(FOLDER_DESCRIPTION);
        
        Drive drive = driveService.getDrive();
        File folder = drive.files().create(folderMetadata)
                .setFields("id,name")
                .execute();
        
        logger.info("Application folder created with ID: {}", folder.getId());
        return folder.getId();
    }
    
    /**
     * Checks if the Drive service is properly authenticated.
     * 
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        try {
            Drive drive = driveService.getDrive();
            // Try to get user info to verify authentication
            drive.about().get().setFields("user").execute();
            return true;
        } catch (IOException e) {
            logger.warn("Authentication check failed", e);
            return false;
        }
    }
    
    /**
     * Gets the Drive service instance.
     * 
     * @return the Drive service
     */
    public DriveService getDriveService() {
        return driveService;
    }
}

package com.securedrive.drive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.securedrive.drive.DriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Handles file uploads to Google Drive.
 * Provides methods for uploading files with progress tracking and error handling.
 */
public class DriveUploader {
    
    private static final Logger logger = LoggerFactory.getLogger(DriveUploader.class);
    private static final String DEFAULT_FOLDER_NAME = "SecureDriveUploader";
    private static final String MIME_TYPE_APPLICATION_OCTET = "application/octet-stream";
    
    private final DriveService driveService;
    private String uploadFolderId;
    
    /**
     * Constructor for DriveUploader.
     * Initializes the Google Drive service.
     */
    public DriveUploader() {
        this.driveService = new DriveService();
        this.uploadFolderId = null;
        logger.info("DriveUploader initialized");
    }
    
    /**
     * Upload a file to Google Drive.
     * 
     * @param file The file to upload
     * @return The Google Drive file ID of the uploaded file
     * @throws Exception If upload fails
     */
    public String uploadFile(java.io.File file) throws Exception {
        logger.info("Starting upload of file: {}", file.getName());
        
        // Validate input file
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }
        
        if (!file.canRead()) {
            throw new IllegalArgumentException("Cannot read file: " + file.getAbsolutePath());
        }
        
        try {
            // Ensure we have a valid Drive service
            Drive drive = driveService.getDriveService();
            if (drive == null) {
                throw new IllegalStateException("Google Drive service not initialized");
            }
            
            // Get or create upload folder
            String folderId = getOrCreateUploadFolder();
            
            // Create file metadata
            File fileMetadata = createFileMetadata(file, folderId);
            
            // Create file content
            FileContent mediaContent = new FileContent(getMimeType(file), file);
            
            // Upload file with progress tracking
            Drive.Files.Create uploadRequest = drive.files().create(fileMetadata, mediaContent);
            
            // Execute upload
            File uploadedFile = uploadRequest.execute();
            
            logger.info("Successfully uploaded file: {} (ID: {})", 
                       file.getName(), uploadedFile.getId());
            
            return uploadedFile.getId();
            
        } catch (IOException e) {
            logger.error("Failed to upload file {}: {}", file.getName(), e.getMessage());
            throw new Exception("Upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during upload of {}: {}", file.getName(), e.getMessage());
            throw new Exception("Upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload a file to Google Drive with a custom name.
     * 
     * @param file The file to upload
     * @param customName The custom name for the uploaded file
     * @return The Google Drive file ID of the uploaded file
     * @throws Exception If upload fails
     */
    public String uploadFile(java.io.File file, String customName) throws Exception {
        logger.info("Starting upload of file: {} with custom name: {}", file.getName(), customName);
        
        // Validate input file
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }
        
        if (!file.canRead()) {
            throw new IllegalArgumentException("Cannot read file: " + file.getAbsolutePath());
        }
        
        if (customName == null || customName.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom name cannot be null or empty");
        }
        
        try {
            // Ensure we have a valid Drive service
            Drive drive = driveService.getDriveService();
            if (drive == null) {
                throw new IllegalStateException("Google Drive service not initialized");
            }
            
            // Get or create upload folder
            String folderId = getOrCreateUploadFolder();
            
            // Create file metadata with custom name
            File fileMetadata = createFileMetadataWithName(customName, folderId);
            
            // Create file content
            FileContent mediaContent = new FileContent(getMimeType(file), file);
            
            // Upload file
            Drive.Files.Create uploadRequest = drive.files().create(fileMetadata, mediaContent);
            File uploadedFile = uploadRequest.execute();
            
            logger.info("Successfully uploaded file: {} as {} (ID: {})", 
                       file.getName(), customName, uploadedFile.getId());
            
            return uploadedFile.getId();
            
        } catch (IOException e) {
            logger.error("Failed to upload file {} with custom name {}: {}", 
                        file.getName(), customName, e.getMessage());
            throw new Exception("Upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during upload of {} with custom name {}: {}", 
                        file.getName(), customName, e.getMessage());
            throw new Exception("Upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get or create the upload folder in Google Drive.
     * 
     * @return The folder ID
     * @throws IOException If folder operations fail
     */
    private String getOrCreateUploadFolder() throws IOException {
        if (uploadFolderId != null) {
            return uploadFolderId;
        }
        
        Drive drive = driveService.getDriveService();
        
        // Search for existing folder
        String query = "name='" + DEFAULT_FOLDER_NAME + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
        List<File> files = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .execute()
            .getFiles();
        
        if (!files.isEmpty()) {
            uploadFolderId = files.get(0).getId();
            logger.info("Found existing upload folder: {} (ID: {})", DEFAULT_FOLDER_NAME, uploadFolderId);
            return uploadFolderId;
        }
        
        // Create new folder
        File folderMetadata = new File();
        folderMetadata.setName(DEFAULT_FOLDER_NAME);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        
        File folder = drive.files().create(folderMetadata)
            .setFields("id")
            .execute();
        
        uploadFolderId = folder.getId();
        logger.info("Created new upload folder: {} (ID: {})", DEFAULT_FOLDER_NAME, uploadFolderId);
        
        return uploadFolderId;
    }
    
    /**
     * Create file metadata for upload.
     * 
     * @param file The file to upload
     * @param folderId The parent folder ID
     * @return File metadata
     */
    private File createFileMetadata(java.io.File file, String folderId) {
        File fileMetadata = new File();
        fileMetadata.setName(file.getName());
        fileMetadata.setParents(Collections.singletonList(folderId));
        
        // Add description with upload timestamp
        String description = "Uploaded by SecureDriveUploader on " + 
                           java.time.Instant.now().toString();
        fileMetadata.setDescription(description);
        
        return fileMetadata;
    }
    
    /**
     * Create file metadata with custom name.
     * 
     * @param customName The custom name for the file
     * @param folderId The parent folder ID
     * @return File metadata
     */
    private File createFileMetadataWithName(String customName, String folderId) {
        File fileMetadata = new File();
        fileMetadata.setName(customName);
        fileMetadata.setParents(Collections.singletonList(folderId));
        
        // Add description with upload timestamp
        String description = "Uploaded by SecureDriveUploader on " + 
                           java.time.Instant.now().toString();
        fileMetadata.setDescription(description);
        
        return fileMetadata;
    }
    
    /**
     * Get MIME type for a file.
     * 
     * @param file The file
     * @return The MIME type
     */
    private String getMimeType(java.io.File file) {
        try {
            String fileName = file.getName();
            String extension = "";
            
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            }
            
            // Map common extensions to MIME types
            switch (extension) {
                case "txt":
                    return "text/plain";
                case "pdf":
                    return "application/pdf";
                case "doc":
                    return "application/msword";
                case "docx":
                    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "xls":
                    return "application/vnd.ms-excel";
                case "xlsx":
                    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "ppt":
                    return "application/vnd.ms-powerpoint";
                case "pptx":
                    return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "mp4":
                    return "video/mp4";
                case "mp3":
                    return "audio/mpeg";
                case "zip":
                    return "application/zip";
                case "encrypted":
                    return MIME_TYPE_APPLICATION_OCTET;
                default:
                    return MIME_TYPE_APPLICATION_OCTET;
            }
        } catch (Exception e) {
            logger.warn("Could not determine MIME type for file: {}, using default", file.getName());
            return MIME_TYPE_APPLICATION_OCTET;
        }
    }
    
    /**
     * Check if the Google Drive service is properly initialized.
     * 
     * @return True if initialized, false otherwise
     */
    public boolean isServiceInitialized() {
        try {
            return driveService.getDriveService() != null;
        } catch (Exception e) {
            logger.warn("Drive service not initialized: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the current upload folder ID.
     * 
     * @return The folder ID, or null if not set
     */
    public String getUploadFolderId() {
        return uploadFolderId;
    }
    
    /**
     * Set a custom upload folder ID.
     * 
     * @param folderId The folder ID to use
     */
    public void setUploadFolderId(String folderId) {
        this.uploadFolderId = folderId;
        logger.info("Set custom upload folder ID: {}", folderId);
    }
    
    /**
     * Reset the upload folder to use the default folder.
     */
    public void resetUploadFolder() {
        this.uploadFolderId = null;
        logger.info("Reset to default upload folder");
    }
}

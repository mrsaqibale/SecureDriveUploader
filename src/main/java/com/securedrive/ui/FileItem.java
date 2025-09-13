package com.securedrive.ui;

import java.io.File;

/**
 * Represents a file item in the main window table.
 * Contains information about the original file, encrypted file, and upload status.
 */
public class FileItem {
    
    private final File file;
    private File encryptedFile;
    private String status;
    private String driveFileId;
    
    /**
     * Constructor for FileItem.
     * 
     * @param file The original file
     */
    public FileItem(File file) {
        this.file = file;
        this.status = "Selected";
        this.encryptedFile = null;
        this.driveFileId = null;
    }
    
    /**
     * Get the original file.
     * 
     * @return The original file
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Get the file name.
     * 
     * @return The file name
     */
    public String getFileName() {
        return file.getName();
    }
    
    /**
     * Get the file size as a formatted string.
     * 
     * @return Formatted file size
     */
    public String getFileSize() {
        long size = file.length();
        return formatFileSize(size);
    }
    
    /**
     * Get the current status.
     * 
     * @return The status string
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Set the status.
     * 
     * @param status The new status
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Get the encrypted file.
     * 
     * @return The encrypted file, or null if not encrypted
     */
    public File getEncryptedFile() {
        return encryptedFile;
    }
    
    /**
     * Set the encrypted file.
     * 
     * @param encryptedFile The encrypted file
     */
    public void setEncryptedFile(File encryptedFile) {
        this.encryptedFile = encryptedFile;
    }
    
    /**
     * Check if the file is encrypted.
     * 
     * @return True if encrypted, false otherwise
     */
    public boolean isEncrypted() {
        return encryptedFile != null;
    }
    
    /**
     * Get the Google Drive file ID.
     * 
     * @return The Drive file ID, or null if not uploaded
     */
    public String getDriveFileId() {
        return driveFileId;
    }
    
    /**
     * Set the Google Drive file ID.
     * 
     * @param driveFileId The Drive file ID
     */
    public void setDriveFileId(String driveFileId) {
        this.driveFileId = driveFileId;
    }
    
    /**
     * Check if the file is uploaded to Google Drive.
     * 
     * @return True if uploaded, false otherwise
     */
    public boolean isUploaded() {
        return driveFileId != null;
    }
    
    /**
     * Format file size in human-readable format.
     * 
     * @param size The size in bytes
     * @return Formatted size string
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    @Override
    public String toString() {
        return "FileItem{" +
                "fileName='" + getFileName() + '\'' +
                ", size=" + getFileSize() +
                ", status='" + status + '\'' +
                ", encrypted=" + isEncrypted() +
                ", uploaded=" + isUploaded() +
                '}';
    }
}

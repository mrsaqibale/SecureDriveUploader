package com.securedrive.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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
    
    /**
     * Gets the display text for the file item.
     * 
     * @return Formatted display text
     */
    public String getDisplayText() {
        return getFileName() + " (" + getFileSize() + ")";
    }
    
    /**
     * Gets the icon for the file based on its type.
     * 
     * @return The icon node
     */
    public Node getIcon() {
        ImageView iconView = new ImageView();
        iconView.setFitHeight(20);
        iconView.setFitWidth(20);
        
        // Set icon based on file extension
        String fileName = getFileName().toLowerCase();
        String iconPath = getIconPath(fileName);
        
        try {
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            iconView.setImage(icon);
        } catch (Exception e) {
            // Use default file icon if specific icon not found
            try {
                Image defaultIcon = new Image(getClass().getResourceAsStream("/icons/file.png"));
                iconView.setImage(defaultIcon);
            } catch (Exception ex) {
                // If no icon is available, create a text-based icon
                Label textIcon = new Label("📄");
                textIcon.setFont(Font.font("System", FontWeight.BOLD, 16));
                return textIcon;
            }
        }
        
        return iconView;
    }
    
    /**
     * Gets the icon path based on file extension.
     * 
     * @param fileName The file name
     * @return The icon path
     */
    private String getIconPath(String fileName) {
        if (fileName.endsWith(".pdf")) return "/icons/pdf.png";
        if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) return "/icons/word.png";
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) return "/icons/excel.png";
        if (fileName.endsWith(".pptx") || fileName.endsWith(".ppt")) return "/icons/powerpoint.png";
        if (fileName.endsWith(".txt")) return "/icons/text.png";
        if (fileName.endsWith(".zip") || fileName.endsWith(".rar") || fileName.endsWith(".7z")) return "/icons/archive.png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || 
            fileName.endsWith(".gif") || fileName.endsWith(".bmp")) return "/icons/image.png";
        if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov")) return "/icons/video.png";
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".flac")) return "/icons/audio.png";
        return "/icons/file.png";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FileItem fileItem = (FileItem) obj;
        return file.equals(fileItem.file);
    }
    
    @Override
    public int hashCode() {
        return file.hashCode();
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

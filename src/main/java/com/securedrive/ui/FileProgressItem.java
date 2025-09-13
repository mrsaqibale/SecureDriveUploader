package com.securedrive.ui;

import javafx.scene.control.ProgressBar;

/**
 * Represents a file item with progress tracking for the upload screen.
 * Contains information about the file and its upload progress.
 */
public class FileProgressItem {
    
    private final FileItem fileItem;
    private String status;
    private double progress;
    private ProgressBar progressBar;
    
    /**
     * Constructor for FileProgressItem.
     * 
     * @param fileItem The file item to track
     */
    public FileProgressItem(FileItem fileItem) {
        this.fileItem = fileItem;
        this.status = "Waiting";
        this.progress = 0.0;
        this.progressBar = new ProgressBar(0.0);
        this.progressBar.setPrefWidth(200);
    }
    
    /**
     * Get the file item.
     * 
     * @return The file item
     */
    public FileItem getFileItem() {
        return fileItem;
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
     * Get the current progress (0.0 to 1.0).
     * 
     * @return The progress value
     */
    public double getProgress() {
        return progress;
    }
    
    /**
     * Set the progress.
     * 
     * @param progress The progress value (0.0 to 1.0)
     */
    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        this.progressBar.setProgress(this.progress);
    }
    
    /**
     * Get the progress bar.
     * 
     * @return The progress bar
     */
    public ProgressBar getProgressBar() {
        return progressBar;
    }
    
    /**
     * Get the display text for the file item.
     * 
     * @return Formatted display text
     */
    public String getDisplayText() {
        return fileItem.getFileName() + " - " + status + " (" + fileItem.getFileSize() + ")";
    }
    
    @Override
    public String toString() {
        return "FileProgressItem{" +
                "fileName='" + fileItem.getFileName() + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                '}';
    }
}

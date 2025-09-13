package com.securedrive.ui;

import com.securedrive.crypto.FileEncryptor;
import com.securedrive.drive.DriveUploader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller for the Upload Screen.
 * Handles file encryption and upload to Google Drive with progress tracking.
 */
public class UploadController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    
    @FXML
    private ProgressBar overallProgressBar;
    
    @FXML
    private Label overallProgressLabel;
    
    @FXML
    private Label overallStatusLabel;
    
    @FXML
    private ListView<FileProgressItem> filesProgressListView;
    
    @FXML
    private Label currentFileLabel;
    
    @FXML
    private ProgressBar currentFileProgressBar;
    
    @FXML
    private Label currentFileProgressLabel;
    
    @FXML
    private Label currentFileStatusLabel;
    
    @FXML
    private Label filesProcessedLabel;
    
    @FXML
    private Label dataUploadedLabel;
    
    @FXML
    private Label uploadSpeedLabel;
    
    @FXML
    private Label timeRemainingLabel;
    
    @FXML
    private TextArea logArea;
    
    @FXML
    private Button startButton;
    
    @FXML
    private Button pauseButton;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button backButton;
    
    private ScreenManager screenManager;
    private ObservableList<FileProgressItem> fileProgressItems;
    
    // Upload state
    private AtomicBoolean isUploading = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private Task<Void> uploadTask;
    
    // Statistics
    private AtomicInteger filesProcessed = new AtomicInteger(0);
    private AtomicLong totalDataUploaded = new AtomicLong(0);
    private LocalDateTime startTime;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Upload Screen");
        
        // Initialize file progress list
        fileProgressItems = FXCollections.observableArrayList();
        filesProgressListView.setItems(fileProgressItems);
        
        // Set up list view cell factory
        filesProgressListView.setCellFactory(listView -> new ListCell<FileProgressItem>() {
            @Override
            protected void updateItem(FileProgressItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayText());
                    setGraphic(item.getProgressBar());
                }
            }
        });
        
        // Initialize UI state
        updateUIState();
        
        logger.info("Upload Screen initialized successfully");
    }
    
    /**
     * Handles the Start Upload button click event.
     * Begins the encryption and upload process.
     */
    @FXML
    private void handleStartButton() {
        logger.info("Start Upload button clicked");
        
        if (isUploading.get()) {
            return;
        }
        
        // Get files to upload (this would come from the file selection screen)
        // For now, we'll simulate with empty list
        List<FileItem> filesToUpload = getFilesToUpload();
        
        if (filesToUpload.isEmpty()) {
            showErrorDialog("No Files", "No files selected for upload.");
            return;
        }
        
        // Initialize upload state
        isUploading.set(true);
        isPaused.set(false);
        isCancelled.set(false);
        filesProcessed.set(0);
        totalDataUploaded.set(0);
        startTime = LocalDateTime.now();
        
        // Create file progress items
        fileProgressItems.clear();
        for (FileItem fileItem : filesToUpload) {
            fileProgressItems.add(new FileProgressItem(fileItem));
        }
        
        // Start upload task
        startUploadTask(filesToUpload);
        
        updateUIState();
        appendLog("Starting upload process...");
    }
    
    /**
     * Handles the Pause button click event.
     * Pauses or resumes the upload process.
     */
    @FXML
    private void handlePauseButton() {
        if (isPaused.get()) {
            logger.info("Resuming upload");
            isPaused.set(false);
            pauseButton.setText("⏸️ Pause");
            appendLog("Upload resumed");
        } else {
            logger.info("Pausing upload");
            isPaused.set(true);
            pauseButton.setText("▶️ Resume");
            appendLog("Upload paused");
        }
    }
    
    /**
     * Handles the Cancel button click event.
     * Cancels the upload process.
     */
    @FXML
    private void handleCancelButton() {
        logger.info("Cancel button clicked");
        
        if (isUploading.get()) {
            isCancelled.set(true);
            appendLog("Upload cancelled by user");
        }
    }
    
    /**
     * Handles the Back button click event.
     * Returns to the encryption setup screen.
     */
    @FXML
    private void handleBackButton() {
        logger.info("Back button clicked - returning to encryption screen");
        
        if (!isUploading.get() && screenManager != null) {
            screenManager.transitionToEncryption();
        }
    }
    
    /**
     * Starts the upload task.
     * 
     * @param filesToUpload The files to upload
     */
    private void startUploadTask(List<FileItem> filesToUpload) {
        uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    FileEncryptor encryptor = new FileEncryptor();
                    DriveUploader uploader = new DriveUploader();
                    
                    int totalFiles = filesToUpload.size();
                    
                    for (int i = 0; i < totalFiles && !isCancelled.get(); i++) {
                        // Wait if paused
                        while (isPaused.get() && !isCancelled.get()) {
                            Thread.sleep(100);
                        }
                        
                        if (isCancelled.get()) {
                            break;
                        }
                        
                        FileItem fileItem = filesToUpload.get(i);
                        FileProgressItem progressItem = fileProgressItems.get(i);
                        
                        // Update current file
                        Platform.runLater(() -> {
                            currentFileLabel.setText("Current File: " + fileItem.getFileName());
                            currentFileStatusLabel.setText("Processing...");
                        });
                        
                        try {
                            // Step 1: Encrypt file
                            Platform.runLater(() -> {
                                progressItem.setStatus("Encrypting...");
                                currentFileStatusLabel.setText("Encrypting...");
                            });
                            
                            File encryptedFile = encryptor.encryptFile(fileItem.getFile(), getEncryptionKey(), getEncryptionAlgorithm());
                            fileItem.setEncryptedFile(encryptedFile);
                            
                            // Step 2: Upload to Drive
                            Platform.runLater(() -> {
                                progressItem.setStatus("Uploading...");
                                currentFileStatusLabel.setText("Uploading to Google Drive...");
                            });
                            
                            String driveFileId = uploader.uploadFile(encryptedFile, fileItem.getFileName() + ".encrypted");
                            fileItem.setDriveFileId(driveFileId);
                            
                            // Update progress
                            filesProcessed.incrementAndGet();
                            totalDataUploaded.addAndGet(fileItem.getFile().length());
                            
                            Platform.runLater(() -> {
                                progressItem.setStatus("Completed");
                                progressItem.setProgress(1.0);
                                updateOverallProgress();
                                updateStatistics();
                            });
                            
                            appendLog("✅ Uploaded: " + fileItem.getFileName());
                            
                        } catch (Exception e) {
                            logger.error("Failed to process file {}: {}", fileItem.getFileName(), e.getMessage(), e);
                            
                            Platform.runLater(() -> {
                                progressItem.setStatus("Failed: " + e.getMessage());
                                progressItem.setProgress(0.0);
                            });
                            
                            appendLog("❌ Failed: " + fileItem.getFileName() + " - " + e.getMessage());
                        }
                    }
                    
                    if (!isCancelled.get()) {
                        Platform.runLater(() -> {
                            overallStatusLabel.setText("Upload completed successfully!");
                            appendLog("🎉 All files uploaded successfully!");
                            
                            // Enable transition to success screen
                            backButton.setDisable(false);
                        });
                    } else {
                        Platform.runLater(() -> {
                            overallStatusLabel.setText("Upload cancelled");
                            appendLog("Upload process cancelled");
                        });
                    }
                    
                } catch (Exception e) {
                    logger.error("Upload task failed: {}", e.getMessage(), e);
                    Platform.runLater(() -> {
                        overallStatusLabel.setText("Upload failed: " + e.getMessage());
                        appendLog("❌ Upload failed: " + e.getMessage());
                    });
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    isUploading.set(false);
                    updateUIState();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    isUploading.set(false);
                    updateUIState();
                });
            }
        };
        
        Thread uploadThread = new Thread(uploadTask);
        uploadThread.setDaemon(true);
        uploadThread.start();
    }
    
    /**
     * Updates the overall progress bar and label.
     */
    private void updateOverallProgress() {
        int totalFiles = fileProgressItems.size();
        int completedFiles = filesProcessed.get();
        
        double progress = totalFiles > 0 ? (double) completedFiles / totalFiles : 0.0;
        
        Platform.runLater(() -> {
            overallProgressBar.setProgress(progress);
            overallProgressLabel.setText(String.format("%d of %d files completed", completedFiles, totalFiles));
        });
    }
    
    /**
     * Updates the statistics display.
     */
    private void updateStatistics() {
        Platform.runLater(() -> {
            filesProcessedLabel.setText(String.valueOf(filesProcessed.get()));
            dataUploadedLabel.setText(formatFileSize(totalDataUploaded.get()));
            
            // Calculate upload speed
            if (startTime != null) {
                Duration elapsed = Duration.between(startTime, LocalDateTime.now());
                if (elapsed.toSeconds() > 0) {
                    double speed = totalDataUploaded.get() / (1024.0 * 1024.0) / elapsed.toSeconds();
                    uploadSpeedLabel.setText(String.format("%.1f MB/s", speed));
                    
                    // Calculate time remaining
                    if (speed > 0) {
                        long remainingBytes = getTotalSize() - totalDataUploaded.get();
                        long remainingSeconds = (long) (remainingBytes / (1024.0 * 1024.0) / speed);
                        timeRemainingLabel.setText(formatDuration(remainingSeconds));
                    }
                }
            }
        });
    }
    
    /**
     * Updates the UI state based on current upload status.
     */
    private void updateUIState() {
        boolean uploading = isUploading.get();
        boolean paused = isPaused.get();
        
        startButton.setDisable(uploading);
        pauseButton.setDisable(!uploading);
        cancelButton.setDisable(!uploading);
        backButton.setDisable(uploading);
        
        if (uploading) {
            pauseButton.setText(paused ? "▶️ Resume" : "⏸️ Pause");
        }
    }
    
    /**
     * Appends a message to the log area.
     * 
     * @param message The message to append
     */
    private void appendLog(String message) {
        Platform.runLater(() -> {
            if (logArea != null) {
                String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
                logArea.appendText("[" + timestamp + "] " + message + "\n");
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }
    
    /**
     * Gets the files to upload (placeholder method).
     * In a real implementation, this would get files from the file selection screen.
     * 
     * @return List of files to upload
     */
    private List<FileItem> getFilesToUpload() {
        // This would be populated from the file selection screen
        // For now, return empty list
        return java.util.Collections.emptyList();
    }
    
    /**
     * Gets the encryption key (placeholder method).
     * 
     * @return The encryption key
     */
    private byte[] getEncryptionKey() {
        // This would come from the encryption setup screen
        return new byte[32]; // Placeholder
    }
    
    /**
     * Gets the encryption algorithm (placeholder method).
     * 
     * @return The encryption algorithm
     */
    private String getEncryptionAlgorithm() {
        // This would come from the encryption setup screen
        return "AES-256-CBC";
    }
    
    /**
     * Gets the total size of all files (placeholder method).
     * 
     * @return Total size in bytes
     */
    private long getTotalSize() {
        // This would be calculated from the selected files
        return 0;
    }
    
    /**
     * Formats file size in human-readable format.
     * 
     * @param bytes The size in bytes
     * @return Formatted size string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Formats duration in MM:SS format.
     * 
     * @param seconds The duration in seconds
     * @return Formatted duration string
     */
    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
    
    /**
     * Sets the screen manager instance.
     * 
     * @param screenManager The screen manager instance
     */
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
    
    /**
     * Shows an error dialog to the user.
     * 
     * @param title The dialog title
     * @param message The error message
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

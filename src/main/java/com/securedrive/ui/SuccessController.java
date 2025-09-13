package com.securedrive.ui;

import com.securedrive.crypto.KeyManager;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Success Screen.
 * Displays upload completion status and provides access to uploaded files.
 */
public class SuccessController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(SuccessController.class);
    
    @FXML
    private Label filesUploadedLabel;
    
    @FXML
    private Label totalSizeLabel;
    
    @FXML
    private Label uploadTimeLabel;
    
    @FXML
    private ListView<FileItem> uploadedFilesListView;
    
    @FXML
    private Button openDriveButton;
    
    @FXML
    private Hyperlink driveFolderLink;
    
    @FXML
    private Button uploadMoreButton;
    
    @FXML
    private Button exportKeyButton;
    
    @FXML
    private Button finishButton;
    
    private ScreenManager screenManager;
    private ObservableList<FileItem> uploadedFiles;
    private String driveFolderId;
    private String driveFolderUrl;
    private long totalUploadSize;
    private long uploadDuration;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Success Screen");
        
        // Initialize uploaded files list
        uploadedFiles = FXCollections.observableArrayList();
        uploadedFilesListView.setItems(uploadedFiles);
        
        // Set up list view cell factory
        uploadedFilesListView.setCellFactory(listView -> new ListCell<FileItem>() {
            @Override
            protected void updateItem(FileItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayText());
                    setGraphic(item.getIcon());
                }
            }
        });
        
        // Set up context menu for list items
        setupContextMenu();
        
        // Initialize with placeholder data
        initializeWithPlaceholderData();
        
        logger.info("Success Screen initialized successfully");
    }
    
    /**
     * Sets up the context menu for the uploaded files list view.
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem openInDriveItem = new MenuItem("Open in Google Drive");
        openInDriveItem.setOnAction(e -> {
            FileItem selectedItem = uploadedFilesListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getDriveFileId() != null) {
                openFileInDrive(selectedItem.getDriveFileId());
            }
        });
        
        MenuItem copyLinkItem = new MenuItem("Copy Drive Link");
        copyLinkItem.setOnAction(e -> {
            FileItem selectedItem = uploadedFilesListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getDriveFileId() != null) {
                copyDriveLinkToClipboard(selectedItem.getDriveFileId());
            }
        });
        
        contextMenu.getItems().addAll(openInDriveItem, new SeparatorMenuItem(), copyLinkItem);
        uploadedFilesListView.setContextMenu(contextMenu);
    }
    
    /**
     * Initializes the screen with placeholder data.
     * In a real implementation, this would be populated from the upload process.
     */
    private void initializeWithPlaceholderData() {
        // Set placeholder values
        filesUploadedLabel.setText("0");
        totalSizeLabel.setText("0 MB");
        uploadTimeLabel.setText("0:00");
        
        // Set placeholder drive folder URL
        driveFolderUrl = "https://drive.google.com/drive/folders/placeholder";
        driveFolderLink.setText("View in Google Drive");
    }
    
    /**
     * Handles the Open Drive Folder button click event.
     * Opens the Google Drive folder in the default browser.
     */
    @FXML
    private void handleOpenDriveButton() {
        logger.info("Open Drive Folder button clicked");
        
        if (driveFolderUrl != null && !driveFolderUrl.isEmpty()) {
            openUrlInBrowser(driveFolderUrl);
        } else {
            showErrorDialog("No Folder", "No Google Drive folder URL available.");
        }
    }
    
    /**
     * Handles the Drive Folder Link click event.
     * Opens the Google Drive folder in the default browser.
     */
    @FXML
    private void handleDriveFolderLink() {
        logger.info("Drive Folder Link clicked");
        handleOpenDriveButton();
    }
    
    /**
     * Handles the Upload More Files button click event.
     * Returns to the file selection screen.
     */
    @FXML
    private void handleUploadMoreButton() {
        logger.info("Upload More Files button clicked");
        
        if (screenManager != null) {
            screenManager.transitionToFileSelect();
        }
    }
    
    /**
     * Handles the Export Key button click event.
     * Opens a file chooser to save the encryption key.
     */
    @FXML
    private void handleExportKeyButton() {
        logger.info("Export Key button clicked");
        
        try {
            KeyManager keyManager = new KeyManager();
            byte[] currentKey = getCurrentEncryptionKey();
            
            if (currentKey == null) {
                showErrorDialog("No Key", "No encryption key available to export.");
                return;
            }
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Encryption Key");
            fileChooser.setInitialFileName("encryption_key.key");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Key Files", "*.key"),
                new FileChooser.ExtensionFilter("Data Files", "*.dat"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            // Set initial directory
            String userHome = System.getProperty("user.home");
            fileChooser.setInitialDirectory(new File(userHome));
            
            File selectedFile = fileChooser.showSaveDialog(exportKeyButton.getScene().getWindow());
            
            if (selectedFile != null) {
                keyManager.saveKeyToFile(currentKey, selectedFile);
                showInfoDialog("Key Exported", "Encryption key exported successfully to: " + selectedFile.getName());
            }
            
        } catch (Exception e) {
            logger.error("Failed to export key: {}", e.getMessage(), e);
            showErrorDialog("Export Error", "Failed to export encryption key: " + e.getMessage());
        }
    }
    
    /**
     * Handles the Finish button click event.
     * Returns to the welcome screen or exits the application.
     */
    @FXML
    private void handleFinishButton() {
        logger.info("Finish button clicked");
        
        if (screenManager != null) {
            screenManager.transitionToWelcome();
        }
    }
    
    /**
     * Opens a URL in the default browser.
     * 
     * @param url The URL to open
     */
    private void openUrlInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for systems that don't support Desktop.browse()
                String os = System.getProperty("os.name").toLowerCase();
                Runtime runtime = Runtime.getRuntime();
                
                if (os.contains("win")) {
                    runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    runtime.exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    runtime.exec("xdg-open " + url);
                } else {
                    showErrorDialog("Browser Error", "Cannot open browser automatically. Please copy this URL: " + url);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to open URL in browser: {}", e.getMessage(), e);
            showErrorDialog("Browser Error", "Failed to open browser. Please copy this URL: " + url);
        }
    }
    
    /**
     * Opens a specific file in Google Drive.
     * 
     * @param fileId The Google Drive file ID
     */
    private void openFileInDrive(String fileId) {
        String fileUrl = "https://drive.google.com/file/d/" + fileId + "/view";
        openUrlInBrowser(fileUrl);
    }
    
    /**
     * Copies a Google Drive file link to the clipboard.
     * 
     * @param fileId The Google Drive file ID
     */
    private void copyDriveLinkToClipboard(String fileId) {
        String fileUrl = "https://drive.google.com/file/d/" + fileId + "/view";
        
        try {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(fileUrl);
            clipboard.setContent(content);
            
            showInfoDialog("Link Copied", "Google Drive link copied to clipboard.");
        } catch (Exception e) {
            logger.error("Failed to copy link to clipboard: {}", e.getMessage(), e);
            showErrorDialog("Copy Error", "Failed to copy link to clipboard.");
        }
    }
    
    /**
     * Gets the current encryption key (placeholder method).
     * 
     * @return The encryption key, or null if not available
     */
    private byte[] getCurrentEncryptionKey() {
        // This would come from the encryption setup screen or be stored in the application context
        // For now, return null as placeholder
        return null;
    }
    
    /**
     * Sets the uploaded files data.
     * 
     * @param files The list of uploaded files
     * @param totalSize The total size of uploaded files
     * @param duration The upload duration in seconds
     * @param folderId The Google Drive folder ID
     * @param folderUrl The Google Drive folder URL
     */
    public void setUploadData(List<FileItem> files, long totalSize, long duration, String folderId, String folderUrl) {
        this.uploadedFiles.clear();
        this.uploadedFiles.addAll(files);
        this.totalUploadSize = totalSize;
        this.uploadDuration = duration;
        this.driveFolderId = folderId;
        this.driveFolderUrl = folderUrl;
        
        // Update UI
        filesUploadedLabel.setText(String.valueOf(files.size()));
        totalSizeLabel.setText(formatFileSize(totalSize));
        uploadTimeLabel.setText(formatDuration(duration));
        
        if (folderUrl != null && !folderUrl.isEmpty()) {
            driveFolderLink.setText("View in Google Drive");
        }
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
    
    /**
     * Shows an info dialog to the user.
     * 
     * @param title The dialog title
     * @param message The info message
     */
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

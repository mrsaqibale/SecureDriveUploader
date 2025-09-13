package com.securedrive.ui;

import com.securedrive.crypto.FileEncryptor;
import com.securedrive.crypto.KeyManager;
import com.securedrive.drive.DriveUploader;
import com.securedrive.util.FileChooserUtil;
import com.securedrive.util.ConfigManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main window controller for the SecureDriveUploader application.
 * Handles the user interface and coordinates between encryption and upload operations.
 */
public class MainWindow implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    @FXML private Button selectFilesButton;
    @FXML private Button encryptButton;
    @FXML private Button uploadButton;
    @FXML private Button clearButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TableView<FileItem> filesTable;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, String> fileSizeColumn;
    @FXML private TableColumn<FileItem, String> statusColumn;
    @FXML private CheckBox encryptCheckBox;
    @FXML private CheckBox uploadCheckBox;
    
    private Stage primaryStage;
    private FileEncryptor fileEncryptor;
    private KeyManager keyManager;
    private DriveUploader driveUploader;
    private ExecutorService executorService;
    private ConfigManager configManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainWindow controller");
        
        // Initialize services
        initializeServices();
        
        // Setup table columns
        setupTableColumns();
        
        // Setup event handlers
        setupEventHandlers();
        
        // Load last used configuration
        loadConfiguration();
        
        logger.info("MainWindow controller initialized successfully");
    }
    
    /**
     * Initialize all required services and components.
     */
    private void initializeServices() {
        try {
            keyManager = new KeyManager();
            fileEncryptor = new FileEncryptor(keyManager);
            driveUploader = new DriveUploader();
            executorService = Executors.newFixedThreadPool(4);
            configManager = ConfigManager.getInstance();
            
            logger.info("All services initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize services: {}", e.getMessage(), e);
            showError("Initialization Error", "Failed to initialize application services.");
        }
    }
    
    /**
     * Setup table columns for file display.
     */
    private void setupTableColumns() {
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Set column widths
        fileNameColumn.setPrefWidth(300);
        fileSizeColumn.setPrefWidth(100);
        statusColumn.setPrefWidth(150);
    }
    
    /**
     * Setup event handlers for UI components.
     */
    private void setupEventHandlers() {
        selectFilesButton.setOnAction(e -> selectFiles());
        encryptButton.setOnAction(e -> encryptFiles());
        uploadButton.setOnAction(e -> uploadFiles());
        clearButton.setOnAction(e -> clearFiles());
        
        // Enable/disable buttons based on checkboxes
        encryptCheckBox.setOnAction(e -> updateButtonStates());
        uploadCheckBox.setOnAction(e -> updateButtonStates());
    }
    
    /**
     * Load configuration from ConfigManager.
     */
    private void loadConfiguration() {
        try {
            // Load last used folder
            String lastFolder = configManager.getLastUsedFolder();
            if (lastFolder != null && !lastFolder.isEmpty()) {
                // Set default directory for file chooser
                FileChooserUtil.setDefaultDirectory(Paths.get(lastFolder));
            }
            
            // Load other preferences
            encryptCheckBox.setSelected(configManager.isEncryptionEnabled());
            uploadCheckBox.setSelected(configManager.isUploadEnabled());
            
            updateButtonStates();
            
        } catch (Exception e) {
            logger.warn("Failed to load configuration: {}", e.getMessage());
        }
    }
    
    /**
     * Update button states based on current selections and file list.
     */
    private void updateButtonStates() {
        boolean hasFiles = !filesTable.getItems().isEmpty();
        boolean encryptEnabled = encryptCheckBox.isSelected();
        boolean uploadEnabled = uploadCheckBox.isSelected();
        
        encryptButton.setDisable(!hasFiles || !encryptEnabled);
        uploadButton.setDisable(!hasFiles || !uploadEnabled);
        clearButton.setDisable(!hasFiles);
    }
    
    /**
     * Set the primary stage reference.
     * 
     * @param primaryStage The primary stage
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    /**
     * Handle file selection.
     */
    @FXML
    private void selectFiles() {
        try {
            List<File> selectedFiles = FileChooserUtil.showOpenMultipleDialog(primaryStage);
            
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                // Add files to table
                for (File file : selectedFiles) {
                    FileItem fileItem = new FileItem(file);
                    filesTable.getItems().add(fileItem);
                }
                
                // Save last used folder
                File parentDir = selectedFiles.get(0).getParentFile();
                if (parentDir != null) {
                    configManager.setLastUsedFolder(parentDir.getAbsolutePath());
                }
                
                updateButtonStates();
                updateStatus("Selected " + selectedFiles.size() + " file(s)");
                
                logger.info("Selected {} files for processing", selectedFiles.size());
            }
        } catch (Exception e) {
            logger.error("Error selecting files: {}", e.getMessage(), e);
            showError("File Selection Error", "Failed to select files: " + e.getMessage());
        }
    }
    
    /**
     * Handle file encryption.
     */
    @FXML
    private void encryptFiles() {
        if (!encryptCheckBox.isSelected()) {
            return;
        }
        
        Task<Void> encryptTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<FileItem> filesToEncrypt = filesTable.getItems().stream()
                    .filter(item -> !item.isEncrypted())
                    .toList();
                
                if (filesToEncrypt.isEmpty()) {
                    Platform.runLater(() -> updateStatus("No files to encrypt"));
                    return null;
                }
                
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    updateStatus("Encrypting files...");
                });
                
                for (int i = 0; i < filesToEncrypt.size(); i++) {
                    FileItem fileItem = filesToEncrypt.get(i);
                    
                    try {
                        // Update progress
                        final int currentIndex = i;
                        Platform.runLater(() -> {
                            progressBar.setProgress((double) currentIndex / filesToEncrypt.size());
                            updateStatus("Encrypting: " + fileItem.getFileName());
                        });
                        
                        // Encrypt file
                        Path encryptedPath = fileEncryptor.encryptFile(fileItem.getFile().toPath());
                        fileItem.setEncryptedFile(encryptedPath.toFile());
                        fileItem.setStatus("Encrypted");
                        
                        logger.info("Encrypted file: {}", fileItem.getFileName());
                        
                    } catch (Exception e) {
                        logger.error("Failed to encrypt file {}: {}", fileItem.getFileName(), e.getMessage());
                        fileItem.setStatus("Encryption Failed");
                    }
                }
                
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("Encryption completed");
                    updateButtonStates();
                });
                
                return null;
            }
        };
        
        executorService.submit(encryptTask);
    }
    
    /**
     * Handle file upload.
     */
    @FXML
    private void uploadFiles() {
        if (!uploadCheckBox.isSelected()) {
            return;
        }
        
        Task<Void> uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<FileItem> filesToUpload = filesTable.getItems().stream()
                    .filter(item -> item.isEncrypted() || !encryptCheckBox.isSelected())
                    .toList();
                
                if (filesToUpload.isEmpty()) {
                    Platform.runLater(() -> updateStatus("No files to upload"));
                    return null;
                }
                
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    updateStatus("Uploading files...");
                });
                
                for (int i = 0; i < filesToUpload.size(); i++) {
                    FileItem fileItem = filesToUpload.get(i);
                    
                    try {
                        // Update progress
                        final int currentIndex = i;
                        Platform.runLater(() -> {
                            progressBar.setProgress((double) currentIndex / filesToUpload.size());
                            updateStatus("Uploading: " + fileItem.getFileName());
                        });
                        
                        // Upload file
                        File fileToUpload = fileItem.isEncrypted() ? 
                            fileItem.getEncryptedFile() : fileItem.getFile();
                        
                        String driveFileId = driveUploader.uploadFile(fileToUpload);
                        fileItem.setDriveFileId(driveFileId);
                        fileItem.setStatus("Uploaded");
                        
                        logger.info("Uploaded file: {} (ID: {})", fileItem.getFileName(), driveFileId);
                        
                    } catch (Exception e) {
                        logger.error("Failed to upload file {}: {}", fileItem.getFileName(), e.getMessage());
                        fileItem.setStatus("Upload Failed");
                    }
                }
                
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("Upload completed");
                    updateButtonStates();
                });
                
                return null;
            }
        };
        
        executorService.submit(uploadTask);
    }
    
    /**
     * Clear all files from the table.
     */
    @FXML
    private void clearFiles() {
        filesTable.getItems().clear();
        updateButtonStates();
        updateStatus("Files cleared");
        logger.info("Cleared all files from table");
    }
    
    /**
     * Update the status label.
     * 
     * @param message The status message
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
        logger.debug("Status updated: {}", message);
    }
    
    /**
     * Show an error dialog.
     * 
     * @param title The dialog title
     * @param message The error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Cleanup resources when the window is closed.
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // Save configuration
        try {
            configManager.setEncryptionEnabled(encryptCheckBox.isSelected());
            configManager.setUploadEnabled(uploadCheckBox.isSelected());
            configManager.saveConfig();
        } catch (Exception e) {
            logger.error("Failed to save configuration: {}", e.getMessage());
        }
        
        logger.info("MainWindow cleanup completed");
    }
}

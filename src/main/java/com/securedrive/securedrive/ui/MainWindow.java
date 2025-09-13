package com.securedrive.securedrive.ui;

import com.securedrive.securedrive.crypto.FileEncryptor;
import com.securedrive.securedrive.crypto.KeyManager;
import com.securedrive.securedrive.drive.DriveUploader;
import com.securedrive.securedrive.util.ConfigManager;
import com.securedrive.securedrive.util.FileChooserUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main window controller for the SecureDriveUploader application.
 * Handles the user interface and coordinates between encryption and upload services.
 */
public class MainWindow implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    @FXML private VBox mainContainer;
    @FXML private Button selectFileButton;
    @FXML private Button encryptButton;
    @FXML private Button uploadButton;
    @FXML private Button clearButton;
    @FXML private TextField filePathField;
    @FXML private TextField encryptedFilePathField;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea statusArea;
    @FXML private Label statusLabel;
    @FXML private CheckBox deleteOriginalCheckBox;
    @FXML private CheckBox autoUploadCheckBox;
    
    private File selectedFile;
    private File encryptedFile;
    private KeyManager keyManager;
    private FileEncryptor fileEncryptor;
    private DriveUploader driveUploader;
    private ConfigManager configManager;
    private FileChooserUtil fileChooserUtil;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainWindow");
        
        // Initialize services
        initializeServices();
        
        // Set up UI components
        setupUI();
        
        // Load configuration
        loadConfiguration();
        
        logger.info("MainWindow initialized successfully");
    }
    
    /**
     * Initialize all required services.
     */
    private void initializeServices() {
        try {
            keyManager = new KeyManager();
            fileEncryptor = new FileEncryptor(keyManager);
            driveUploader = new DriveUploader();
            configManager = new ConfigManager();
            fileChooserUtil = new FileChooserUtil();
            
            logger.debug("All services initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize services", e);
            showError("Initialization Error", "Failed to initialize application services: " + e.getMessage());
        }
    }
    
    /**
     * Set up UI components and event handlers.
     */
    private void setupUI() {
        // Set up button event handlers
        selectFileButton.setOnAction(e -> selectFile());
        encryptButton.setOnAction(e -> encryptFile());
        uploadButton.setOnAction(e -> uploadFile());
        clearButton.setOnAction(e -> clearAll());
        
        // Set up text field properties
        filePathField.setEditable(false);
        encryptedFilePathField.setEditable(false);
        
        // Set up progress bar
        progressBar.setVisible(false);
        
        // Set up status area
        statusArea.setEditable(false);
        statusArea.setWrapText(true);
        
        // Set up checkboxes
        deleteOriginalCheckBox.setSelected(false);
        autoUploadCheckBox.setSelected(true);
        
        // Disable buttons initially
        encryptButton.setDisable(true);
        uploadButton.setDisable(true);
        
        logger.debug("UI components set up successfully");
    }
    
    /**
     * Load application configuration.
     */
    private void loadConfiguration() {
        try {
            // Load last used directory
            String lastDirectory = configManager.getLastUsedDirectory();
            if (lastDirectory != null) {
                fileChooserUtil.setInitialDirectory(Paths.get(lastDirectory));
            }
            
            // Load other preferences
            deleteOriginalCheckBox.setSelected(configManager.getDeleteOriginalAfterEncryption());
            autoUploadCheckBox.setSelected(configManager.getAutoUploadAfterEncryption());
            
            logger.debug("Configuration loaded successfully");
        } catch (Exception e) {
            logger.warn("Failed to load configuration", e);
        }
    }
    
    /**
     * Handle file selection.
     */
    @FXML
    private void selectFile() {
        try {
            selectedFile = fileChooserUtil.showOpenDialog();
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
                encryptButton.setDisable(false);
                
                // Save the directory for next time
                configManager.setLastUsedDirectory(selectedFile.getParent());
                
                updateStatus("File selected: " + selectedFile.getName());
                logger.info("File selected: {}", selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Error selecting file", e);
            showError("File Selection Error", "Failed to select file: " + e.getMessage());
        }
    }
    
    /**
     * Handle file encryption.
     */
    @FXML
    private void encryptFile() {
        if (selectedFile == null) {
            showError("No File Selected", "Please select a file to encrypt.");
            return;
        }
        
        Task<Void> encryptionTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    encryptButton.setDisable(true);
                    updateStatus("Encrypting file...");
                });
                
                try {
                    // Generate encryption key
                    keyManager.generateKey();
                    
                    // Encrypt the file
                    encryptedFile = fileEncryptor.encryptFile(selectedFile);
                    
                    Platform.runLater(() -> {
                        encryptedFilePathField.setText(encryptedFile.getAbsolutePath());
                        uploadButton.setDisable(false);
                        updateStatus("File encrypted successfully: " + encryptedFile.getName());
                    });
                    
                    logger.info("File encrypted successfully: {}", encryptedFile.getAbsolutePath());
                    
                    // Auto-upload if enabled
                    if (autoUploadCheckBox.isSelected()) {
                        Platform.runLater(() -> uploadFile());
                    }
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        updateStatus("Encryption failed: " + e.getMessage());
                        showError("Encryption Error", "Failed to encrypt file: " + e.getMessage());
                    });
                    logger.error("Encryption failed", e);
                    throw e;
                } finally {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        encryptButton.setDisable(false);
                    });
                }
                
                return null;
            }
        };
        
        new Thread(encryptionTask).start();
    }
    
    /**
     * Handle file upload to Google Drive.
     */
    @FXML
    private void uploadFile() {
        if (encryptedFile == null) {
            showError("No Encrypted File", "Please encrypt a file first.");
            return;
        }
        
        Task<Void> uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    uploadButton.setDisable(true);
                    updateStatus("Uploading to Google Drive...");
                });
                
                try {
                    // Upload the encrypted file
                    String fileId = driveUploader.uploadFile(encryptedFile);
                    
                    Platform.runLater(() -> {
                        updateStatus("File uploaded successfully to Google Drive. File ID: " + fileId);
                        
                        // Delete original file if requested
                        if (deleteOriginalCheckBox.isSelected() && selectedFile != null) {
                            if (selectedFile.delete()) {
                                updateStatus("Original file deleted: " + selectedFile.getName());
                            }
                        }
                    });
                    
                    logger.info("File uploaded successfully to Google Drive. File ID: {}", fileId);
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        updateStatus("Upload failed: " + e.getMessage());
                        showError("Upload Error", "Failed to upload file: " + e.getMessage());
                    });
                    logger.error("Upload failed", e);
                    throw e;
                } finally {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        uploadButton.setDisable(false);
                    });
                }
                
                return null;
            }
        };
        
        new Thread(uploadTask).start();
    }
    
    /**
     * Clear all fields and reset the interface.
     */
    @FXML
    private void clearAll() {
        selectedFile = null;
        encryptedFile = null;
        filePathField.clear();
        encryptedFilePathField.clear();
        statusArea.clear();
        statusLabel.setText("Ready");
        
        encryptButton.setDisable(true);
        uploadButton.setDisable(true);
        
        updateStatus("Interface cleared");
        logger.info("Interface cleared");
    }
    
    /**
     * Update the status message.
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusArea.appendText(message + "\n");
            statusArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    /**
     * Show an error dialog.
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

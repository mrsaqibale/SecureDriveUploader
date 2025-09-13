package com.securedrive.ui;

import com.securedrive.crypto.KeyManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

/**
 * Controller for the Encryption Setup Screen.
 * Handles encryption algorithm selection and key management.
 */
public class EncryptionController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionController.class);
    
    @FXML
    private ComboBox<String> algorithmComboBox;
    
    @FXML
    private Button generateKeyButton;
    
    @FXML
    private Button importKeyButton;
    
    @FXML
    private Button exportKeyButton;
    
    @FXML
    private TextField keyDisplayField;
    
    @FXML
    private CheckBox showKeyCheckBox;
    
    @FXML
    private CheckBox saveKeyToFileCheckBox;
    
    @FXML
    private CheckBox rememberKeyCheckBox;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Button nextButton;
    
    private ScreenManager screenManager;
    private KeyManager keyManager;
    private byte[] currentKey;
    private String selectedAlgorithm;
    
    // Available encryption algorithms
    private static final String[] ALGORITHMS = {
        "AES-128-CBC",
        "AES-192-CBC", 
        "AES-256-CBC",
        "AES-128-GCM",
        "AES-256-GCM"
    };
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Encryption Setup Screen");
        
        // Initialize key manager
        this.keyManager = new KeyManager();
        
        // Set up algorithm combo box
        setupAlgorithmComboBox();
        
        // Set up initial UI state
        updateUIState();
        
        logger.info("Encryption Setup Screen initialized successfully");
    }
    
    /**
     * Sets up the algorithm combo box with available encryption algorithms.
     */
    private void setupAlgorithmComboBox() {
        algorithmComboBox.getItems().addAll(ALGORITHMS);
        algorithmComboBox.setValue(ALGORITHMS[2]); // Default to AES-256-CBC
        selectedAlgorithm = ALGORITHMS[2];
        
        algorithmComboBox.setOnAction(e -> {
            selectedAlgorithm = algorithmComboBox.getValue();
            logger.info("Selected encryption algorithm: {}", selectedAlgorithm);
            
            // If we have a key, check if it's compatible with the new algorithm
            if (currentKey != null) {
                if (!isKeyCompatibleWithAlgorithm(currentKey, selectedAlgorithm)) {
                    showWarningDialog("Key Incompatible", 
                        "The current key is not compatible with the selected algorithm. Please generate a new key.");
                    currentKey = null;
                    updateUIState();
                }
            }
        });
    }
    
    /**
     * Handles the Generate New Key button click event.
     * Generates a new encryption key based on the selected algorithm.
     */
    @FXML
    private void handleGenerateKeyButton() {
        logger.info("Generate Key button clicked");
        
        try {
            // Generate new key based on selected algorithm
            currentKey = keyManager.generateKey(selectedAlgorithm);
            
            // Update UI
            updateUIState();
            
            showInfoDialog("Key Generated", "A new encryption key has been generated successfully.");
            
        } catch (Exception e) {
            logger.error("Failed to generate key: {}", e.getMessage(), e);
            showErrorDialog("Key Generation Error", "Failed to generate encryption key: " + e.getMessage());
        }
    }
    
    /**
     * Handles the Import Key button click event.
     * Opens a file chooser to import an existing encryption key.
     */
    @FXML
    private void handleImportKeyButton() {
        logger.info("Import Key button clicked");
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Encryption Key");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Key Files", "*.key", "*.dat"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Set initial directory
        String userHome = System.getProperty("user.home");
        fileChooser.setInitialDirectory(new File(userHome));
        
        File selectedFile = fileChooser.showOpenDialog(importKeyButton.getScene().getWindow());
        
        if (selectedFile != null) {
            try {
                currentKey = keyManager.loadKeyFromFile(selectedFile);
                
                // Check if key is compatible with selected algorithm
                if (!isKeyCompatibleWithAlgorithm(currentKey, selectedAlgorithm)) {
                    showWarningDialog("Key Incompatible", 
                        "The imported key is not compatible with the selected algorithm. Please select a different algorithm or import a different key.");
                    currentKey = null;
                } else {
                    updateUIState();
                    showInfoDialog("Key Imported", "Encryption key imported successfully.");
                }
                
            } catch (Exception e) {
                logger.error("Failed to import key: {}", e.getMessage(), e);
                showErrorDialog("Key Import Error", "Failed to import encryption key: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles the Export Key button click event.
     * Opens a file chooser to save the current encryption key.
     */
    @FXML
    private void handleExportKeyButton() {
        logger.info("Export Key button clicked");
        
        if (currentKey == null) {
            showErrorDialog("No Key", "No encryption key to export.");
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
            try {
                keyManager.saveKeyToFile(currentKey, selectedFile);
                showInfoDialog("Key Exported", "Encryption key exported successfully to: " + selectedFile.getName());
                
            } catch (Exception e) {
                logger.error("Failed to export key: {}", e.getMessage(), e);
                showErrorDialog("Key Export Error", "Failed to export encryption key: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles the Show Key checkbox change event.
     * Toggles the visibility of the encryption key.
     */
    @FXML
    private void handleShowKeyCheckBox() {
        if (currentKey != null) {
            if (showKeyCheckBox.isSelected()) {
                keyDisplayField.setText(Base64.getEncoder().encodeToString(currentKey));
            } else {
                keyDisplayField.setText("••••••••••••••••••••••••••••••••");
            }
        }
    }
    
    /**
     * Handles the Back button click event.
     * Returns to the file selection screen.
     */
    @FXML
    private void handleBackButton() {
        logger.info("Back button clicked - returning to file selection screen");
        
        if (screenManager != null) {
            screenManager.transitionToFileSelect();
        }
    }
    
    /**
     * Handles the Next button click event.
     * Proceeds to the upload screen.
     */
    @FXML
    private void handleNextButton() {
        logger.info("Next button clicked - proceeding to upload screen");
        
        if (currentKey != null && screenManager != null) {
            // Store encryption settings for use in upload process
            // This could be done through a shared data model or the screen manager
            screenManager.transitionToUpload();
        } else {
            showErrorDialog("No Encryption Key", "Please generate or import an encryption key to continue.");
        }
    }
    
    /**
     * Updates the UI state based on current key status.
     */
    private void updateUIState() {
        boolean hasKey = currentKey != null;
        
        // Update key display
        if (hasKey) {
            if (showKeyCheckBox.isSelected()) {
                keyDisplayField.setText(Base64.getEncoder().encodeToString(currentKey));
            } else {
                keyDisplayField.setText("••••••••••••••••••••••••••••••••");
            }
        } else {
            keyDisplayField.setText("No key selected");
        }
        
        // Update button states
        exportKeyButton.setDisable(!hasKey);
        nextButton.setDisable(!hasKey);
        
        // Update key display field state
        keyDisplayField.setDisable(!hasKey);
    }
    
    /**
     * Checks if a key is compatible with the selected algorithm.
     * 
     * @param key The encryption key
     * @param algorithm The encryption algorithm
     * @return true if compatible, false otherwise
     */
    private boolean isKeyCompatibleWithAlgorithm(byte[] key, String algorithm) {
        if (key == null) return false;
        
        int keyLength = key.length * 8; // Convert bytes to bits
        
        switch (algorithm) {
            case "AES-128-CBC":
            case "AES-128-GCM":
                return keyLength == 128;
            case "AES-192-CBC":
                return keyLength == 192;
            case "AES-256-CBC":
            case "AES-256-GCM":
                return keyLength == 256;
            default:
                return false;
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
     * Gets the current encryption key.
     * 
     * @return The encryption key, or null if not set
     */
    public byte[] getCurrentKey() {
        return currentKey;
    }
    
    /**
     * Gets the selected encryption algorithm.
     * 
     * @return The encryption algorithm
     */
    public String getSelectedAlgorithm() {
        return selectedAlgorithm;
    }
    
    /**
     * Gets whether to save key to file.
     * 
     * @return true if key should be saved to file
     */
    public boolean shouldSaveKeyToFile() {
        return saveKeyToFileCheckBox.isSelected();
    }
    
    /**
     * Gets whether to remember key for future sessions.
     * 
     * @return true if key should be remembered
     */
    public boolean shouldRememberKey() {
        return rememberKeyCheckBox.isSelected();
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
    
    /**
     * Shows a warning dialog to the user.
     * 
     * @param title The dialog title
     * @param message The warning message
     */
    private void showWarningDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

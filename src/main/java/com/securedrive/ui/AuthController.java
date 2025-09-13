package com.securedrive.ui;

import com.securedrive.drive.DriveService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Google Drive Authentication Screen.
 * Handles OAuth2 authentication with Google Drive API.
 */
public class AuthController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @FXML
    private Button signInButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Button nextButton;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private TextArea logArea;
    
    private ScreenManager screenManager;
    private DriveService driveService;
    private boolean isAuthenticated = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Authentication Screen");
        
        // Initialize drive service
        this.driveService = new DriveService();
        
        // Set up initial UI state
        updateUIState();
        
        // Add initial log message
        appendLog("Ready to authenticate with Google Drive");
        
        logger.info("Authentication Screen initialized successfully");
    }
    
    /**
     * Handles the Sign In button click event.
     * Initiates the Google Drive OAuth2 authentication process.
     */
    @FXML
    private void handleSignInButton() {
        logger.info("Sign In button clicked - starting authentication process");
        
        // Disable sign in button and show progress
        signInButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Authenticating...");
        appendLog("Starting Google Drive authentication...");
        
        // Create authentication task
        Task<Boolean> authTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // Update progress
                    updateMessage("Connecting to Google...");
                    appendLog("Connecting to Google OAuth2 service...");
                    
                    // Perform authentication
                    boolean success = driveService.authenticate();
                    
                    if (success) {
                        updateMessage("Authentication successful!");
                        appendLog("✅ Successfully authenticated with Google Drive");
                        appendLog("User: " + driveService.getUserEmail());
                        return true;
                    } else {
                        updateMessage("Authentication failed");
                        appendLog("❌ Authentication failed - please try again");
                        return false;
                    }
                    
                } catch (Exception e) {
                    logger.error("Authentication error: {}", e.getMessage(), e);
                    updateMessage("Authentication error: " + e.getMessage());
                    appendLog("❌ Error: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    boolean success = getValue();
                    isAuthenticated = success;
                    updateUIState();
                    
                    progressIndicator.setVisible(false);
                    signInButton.setDisable(false);
                    
                    if (success) {
                        statusLabel.setText("✅ Connected to Google Drive");
                        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    } else {
                        statusLabel.setText("❌ Authentication failed");
                        statusLabel.setStyle("-fx-text-fill: #F44336;");
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    isAuthenticated = false;
                    updateUIState();
                    
                    progressIndicator.setVisible(false);
                    signInButton.setDisable(false);
                    statusLabel.setText("❌ Authentication failed");
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                    
                    appendLog("❌ Authentication failed: " + getException().getMessage());
                });
            }
        };
        
        // Start the authentication task
        Thread authThread = new Thread(authTask);
        authThread.setDaemon(true);
        authThread.start();
    }
    
    /**
     * Handles the Back button click event.
     * Returns to the welcome screen.
     */
    @FXML
    private void handleBackButton() {
        logger.info("Back button clicked - returning to welcome screen");
        
        if (screenManager != null) {
            screenManager.transitionToWelcome();
        }
    }
    
    /**
     * Handles the Next button click event.
     * Proceeds to the file selection screen.
     */
    @FXML
    private void handleNextButton() {
        logger.info("Next button clicked - proceeding to file selection screen");
        
        if (isAuthenticated && screenManager != null) {
            screenManager.transitionToFileSelect();
        } else {
            showErrorDialog("Authentication Required", "Please sign in to Google Drive first.");
        }
    }
    
    /**
     * Updates the UI state based on authentication status.
     */
    private void updateUIState() {
        if (isAuthenticated) {
            nextButton.setDisable(false);
            signInButton.setText("Sign Out");
            signInButton.setOnAction(e -> handleSignOut());
        } else {
            nextButton.setDisable(true);
            signInButton.setText("Sign in with Google");
            signInButton.setOnAction(e -> handleSignInButton());
        }
    }
    
    /**
     * Handles sign out functionality.
     */
    private void handleSignOut() {
        logger.info("Signing out from Google Drive");
        
        try {
            driveService.signOut();
            isAuthenticated = false;
            updateUIState();
            
            statusLabel.setText("Signed out");
            statusLabel.setStyle("-fx-text-fill: #666666;");
            appendLog("Signed out from Google Drive");
            
        } catch (Exception e) {
            logger.error("Error signing out: {}", e.getMessage(), e);
            appendLog("❌ Error signing out: " + e.getMessage());
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
     * Sets the screen manager instance.
     * 
     * @param screenManager The screen manager instance
     */
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
    
    /**
     * Gets the authentication status.
     * 
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    /**
     * Gets the drive service instance.
     * 
     * @return The drive service instance
     */
    public DriveService getDriveService() {
        return driveService;
    }
    
    /**
     * Shows an error dialog to the user.
     * 
     * @param title The dialog title
     * @param message The error message
     */
    private void showErrorDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

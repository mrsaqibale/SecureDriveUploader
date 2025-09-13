package com.securedrive.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Welcome Screen.
 * This screen greets the user and provides an overview of the application features.
 */
public class WelcomeController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(WelcomeController.class);
    
    @FXML
    private Button nextButton;
    
    @FXML
    private Label welcomeTitle;
    
    @FXML
    private Label welcomeSubtitle;
    
    @FXML
    private ImageView appLogo;
    
    private ScreenManager screenManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Welcome Screen");
        
        // Set up button hover effects
        setupButtonEffects();
        
        // Initialize screen manager
        this.screenManager = new ScreenManager(null); // Will be set by ScreenManager
        
        logger.info("Welcome Screen initialized successfully");
    }
    
    /**
     * Sets up visual effects for buttons.
     */
    private void setupButtonEffects() {
        if (nextButton != null) {
            // Add hover effect
            nextButton.setOnMouseEntered(e -> {
                nextButton.setStyle("-fx-background-color: #66BB6A; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6,0,0,3);");
            });
            
            nextButton.setOnMouseExited(e -> {
                nextButton.setStyle("-fx-background-color: linear-gradient(to bottom, #4CAF50, #2E7D32); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 4,0,0,2);");
            });
        }
    }
    
    /**
     * Handles the Next button click event.
     * Transitions to the Google Drive authentication screen.
     */
    @FXML
    private void handleNextButton() {
        logger.info("Next button clicked - transitioning to authentication screen");
        
        try {
            // Get the screen manager from the application context
            // For now, we'll create a new one - in a real app, this would be injected
            ScreenManager manager = new ScreenManager(null);
            manager.transitionToAuth();
        } catch (Exception e) {
            logger.error("Failed to transition to authentication screen: {}", e.getMessage(), e);
            showErrorDialog("Navigation Error", "Failed to navigate to the next screen.");
        }
    }
    
    /**
     * Sets the screen manager instance.
     * This method is called by the ScreenManager when loading this screen.
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
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

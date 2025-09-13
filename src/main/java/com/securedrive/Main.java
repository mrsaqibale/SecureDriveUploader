package com.securedrive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.securedrive.ui.MainWindow;
import com.securedrive.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Main entry point for the SecureDriveUploader application.
 * This class initializes the JavaFX application and sets up the main window.
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String APP_TITLE = "Secure Drive Uploader";
    private static final String FXML_PATH = "/ui/main.fxml";
    private static final String ICON_PATH = "/icons/app-icon.png";
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting SecureDriveUploader application");
            
            // Load the main FXML file
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_PATH));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            
            // Set up the main window
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            
            // Set application icon if available
            try {
                Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(ICON_PATH)));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                logger.warn("Could not load application icon: {}", e.getMessage());
            }
            
            // Initialize configuration manager
            ConfigManager.getInstance().loadConfig();
            
            // Set up the main window controller
            MainWindow controller = fxmlLoader.getController();
            controller.setPrimaryStage(primaryStage);
            
            // Show the application
            primaryStage.show();
            
            logger.info("Application started successfully");
            
        } catch (IOException e) {
            logger.error("Failed to load FXML file: {}", e.getMessage(), e);
            showErrorDialog("Failed to load application", "Could not load the main window layout.");
        } catch (Exception e) {
            logger.error("Unexpected error during application startup: {}", e.getMessage(), e);
            showErrorDialog("Application Error", "An unexpected error occurred during startup.");
        }
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
    
    @Override
    public void stop() {
        logger.info("Shutting down SecureDriveUploader application");
        
        // Save configuration before exit
        try {
            ConfigManager.getInstance().saveConfig();
        } catch (Exception e) {
            logger.error("Failed to save configuration: {}", e.getMessage(), e);
        }
        
        logger.info("Application shutdown complete");
    }
    
    /**
     * Main method to launch the JavaFX application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        logger.info("Launching SecureDriveUploader application");
        launch(args);
    }
}

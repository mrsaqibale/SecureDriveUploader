package com.securedrive.securedrive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main entry point for the SecureDriveUploader application.
 * This application provides secure file encryption and upload to Google Drive.
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting SecureDriveUploader application");
            
            // Load the FXML file for the main window
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/ui/main.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            
            // Set up the primary stage
            primaryStage.setTitle("SecureDriveUploader");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            
            // Show the application window
            primaryStage.show();
            
            logger.info("Application started successfully");
            
        } catch (IOException e) {
            logger.error("Failed to load FXML file", e);
            System.err.println("Error loading application: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error starting application", e);
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Main method to launch the JavaFX application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        logger.info("Launching SecureDriveUploader");
        
        // Initialize any required services before starting the UI
        try {
            // Add any initialization code here if needed
            logger.debug("Application initialization completed");
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
            System.err.println("Initialization error: " + e.getMessage());
            System.exit(1);
        }
        
        // Launch the JavaFX application
        launch(args);
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("Shutting down SecureDriveUploader");
        
        // Add any cleanup code here if needed
        super.stop();
    }
}

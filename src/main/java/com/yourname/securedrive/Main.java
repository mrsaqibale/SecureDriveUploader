package com.yourname.securedrive;

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
            
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/ui/main.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            
            primaryStage.setTitle("SecureDriveUploader");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            primaryStage.show();
            
            logger.info("Application started successfully");
            
        } catch (IOException e) {
            logger.error("Failed to load FXML file", e);
            System.err.println("Error loading application: " + e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        logger.info("Application is shutting down");
    }
    
    public static void main(String[] args) {
        logger.info("Launching SecureDriveUploader");
        launch(args);
    }
}

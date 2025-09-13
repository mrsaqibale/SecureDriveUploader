package com.securedrive.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages screen transitions and scene loading for the SecureDriveUploader application.
 * This class handles the flow between different screens in the application.
 */
public class ScreenManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ScreenManager.class);
    
    private final Stage primaryStage;
    private final Map<String, Scene> sceneCache;
    private final Map<String, Object> controllerCache;
    
    // Screen identifiers
    public static final String WELCOME_SCREEN = "welcome";
    public static final String AUTH_SCREEN = "auth";
    public static final String FILE_SELECT_SCREEN = "fileSelect";
    public static final String ENCRYPTION_SCREEN = "encryption";
    public static final String UPLOAD_SCREEN = "upload";
    public static final String SUCCESS_SCREEN = "success";
    
    public ScreenManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.sceneCache = new HashMap<>();
        this.controllerCache = new HashMap<>();
    }
    
    /**
     * Loads the welcome screen as the initial screen.
     * 
     * @return The welcome screen scene
     * @throws IOException if the FXML file cannot be loaded
     */
    public Scene loadWelcomeScreen() throws IOException {
        return loadScreen(WELCOME_SCREEN, "/ui/welcome.fxml");
    }
    
    /**
     * Loads a specific screen by identifier.
     * 
     * @param screenId The screen identifier
     * @param fxmlPath The path to the FXML file
     * @return The loaded scene
     * @throws IOException if the FXML file cannot be loaded
     */
    public Scene loadScreen(String screenId, String fxmlPath) throws IOException {
        // Check if scene is already cached
        if (sceneCache.containsKey(screenId)) {
            logger.debug("Loading cached scene for screen: {}", screenId);
            return sceneCache.get(screenId);
        }
        
        logger.info("Loading new scene for screen: {} from {}", screenId, fxmlPath);
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1000, 700);
            
            // Cache the scene and controller
            sceneCache.put(screenId, scene);
            controllerCache.put(screenId, loader.getController());
            
            // Apply CSS styles if available
            String cssPath = "/ui/styles.css";
            try {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            } catch (Exception e) {
                logger.warn("Could not load CSS styles: {}", e.getMessage());
            }
            
            logger.info("Successfully loaded scene for screen: {}", screenId);
            return scene;
            
        } catch (IOException e) {
            logger.error("Failed to load FXML file for screen {}: {}", screenId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Transitions to a specific screen.
     * 
     * @param screenId The screen identifier to transition to
     * @param fxmlPath The path to the FXML file
     */
    public void transitionTo(String screenId, String fxmlPath) {
        try {
            Scene scene = loadScreen(screenId, fxmlPath);
            primaryStage.setScene(scene);
            logger.info("Transitioned to screen: {}", screenId);
        } catch (IOException e) {
            logger.error("Failed to transition to screen {}: {}", screenId, e.getMessage());
            showErrorDialog("Screen Transition Error", "Failed to load screen: " + screenId);
        }
    }
    
    /**
     * Transitions to the welcome screen.
     */
    public void transitionToWelcome() {
        transitionTo(WELCOME_SCREEN, "/ui/welcome.fxml");
    }
    
    /**
     * Transitions to the authentication screen.
     */
    public void transitionToAuth() {
        transitionTo(AUTH_SCREEN, "/ui/auth.fxml");
    }
    
    /**
     * Transitions to the file selection screen.
     */
    public void transitionToFileSelect() {
        transitionTo(FILE_SELECT_SCREEN, "/ui/fileSelect.fxml");
    }
    
    /**
     * Transitions to the encryption setup screen.
     */
    public void transitionToEncryption() {
        transitionTo(ENCRYPTION_SCREEN, "/ui/encryption.fxml");
    }
    
    /**
     * Transitions to the upload screen.
     */
    public void transitionToUpload() {
        transitionTo(UPLOAD_SCREEN, "/ui/upload.fxml");
    }
    
    /**
     * Transitions to the success screen.
     */
    public void transitionToSuccess() {
        transitionTo(SUCCESS_SCREEN, "/ui/success.fxml");
    }
    
    /**
     * Gets the controller for a specific screen.
     * 
     * @param screenId The screen identifier
     * @return The controller instance, or null if not found
     */
    public Object getController(String screenId) {
        return controllerCache.get(screenId);
    }
    
    /**
     * Clears the scene cache. Useful for memory management.
     */
    public void clearCache() {
        sceneCache.clear();
        controllerCache.clear();
        logger.info("Screen cache cleared");
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

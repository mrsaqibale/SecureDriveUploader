package com.securedrive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages application configuration and user preferences.
 * Handles loading and saving configuration to a properties file.
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_DIR = ".securedrive";
    private static final String CONFIG_FILE = "app.properties";
    private static final String DEFAULT_CONFIG_FILE = "/app.properties";
    
    // Configuration keys
    private static final String KEY_LAST_USED_FOLDER = "last.used.folder";
    private static final String KEY_ENCRYPTION_ENABLED = "encryption.enabled";
    private static final String KEY_UPLOAD_ENABLED = "upload.enabled";
    private static final String KEY_WINDOW_WIDTH = "window.width";
    private static final String KEY_WINDOW_HEIGHT = "window.height";
    private static final String KEY_WINDOW_X = "window.x";
    private static final String KEY_WINDOW_Y = "window.y";
    private static final String KEY_DRIVE_FOLDER_ID = "drive.folder.id";
    private static final String KEY_AUTO_ENCRYPT = "auto.encrypt";
    private static final String KEY_AUTO_UPLOAD = "auto.upload";
    private static final String KEY_SHOW_NOTIFICATIONS = "show.notifications";
    private static final String KEY_LOG_LEVEL = "log.level";
    
    // Default values
    private static final String DEFAULT_LAST_USED_FOLDER = System.getProperty("user.home");
    private static final boolean DEFAULT_ENCRYPTION_ENABLED = true;
    private static final boolean DEFAULT_UPLOAD_ENABLED = true;
    private static final int DEFAULT_WINDOW_WIDTH = 800;
    private static final int DEFAULT_WINDOW_HEIGHT = 600;
    private static final int DEFAULT_WINDOW_X = 100;
    private static final int DEFAULT_WINDOW_Y = 100;
    private static final String DEFAULT_DRIVE_FOLDER_ID = "";
    private static final boolean DEFAULT_AUTO_ENCRYPT = false;
    private static final boolean DEFAULT_AUTO_UPLOAD = false;
    private static final boolean DEFAULT_SHOW_NOTIFICATIONS = true;
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    
    private static ConfigManager instance;
    private final Properties properties;
    private final Path configFilePath;
    private boolean isLoaded = false;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ConfigManager() {
        this.properties = new Properties();
        this.configFilePath = getConfigFilePath();
        logger.info("ConfigManager initialized");
    }
    
    /**
     * Get the singleton instance of ConfigManager.
     * 
     * @return The ConfigManager instance
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * Load configuration from file.
     * 
     * @throws Exception If loading fails
     */
    public void loadConfig() throws Exception {
        try {
            logger.info("Loading configuration from: {}", configFilePath);
            
            // Create config directory if it doesn't exist
            Path configDir = configFilePath.getParent();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                logger.debug("Created config directory: {}", configDir);
            }
            
            // Load default properties first
            loadDefaultProperties();
            
            // Load user properties if file exists
            if (Files.exists(configFilePath)) {
                try (InputStream input = Files.newInputStream(configFilePath)) {
                    properties.load(input);
                    logger.debug("Loaded user configuration from file");
                }
            } else {
                logger.debug("Config file does not exist, using defaults");
            }
            
            this.isLoaded = true;
            logger.info("Configuration loaded successfully");
            
        } catch (Exception e) {
            logger.error("Failed to load configuration: {}", e.getMessage(), e);
            throw new Exception("Configuration load failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save configuration to file.
     * 
     * @throws Exception If saving fails
     */
    public void saveConfig() throws Exception {
        try {
            logger.info("Saving configuration to: {}", configFilePath);
            
            // Create config directory if it doesn't exist
            Path configDir = configFilePath.getParent();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                logger.debug("Created config directory: {}", configDir);
            }
            
            // Save properties to file
            try (OutputStream output = Files.newOutputStream(configFilePath)) {
                properties.store(output, "SecureDriveUploader Configuration");
                logger.debug("Configuration saved successfully");
            }
            
        } catch (Exception e) {
            logger.error("Failed to save configuration: {}", e.getMessage(), e);
            throw new Exception("Configuration save failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load default properties from the application resources.
     */
    private void loadDefaultProperties() {
        try (InputStream input = getClass().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (input != null) {
                Properties defaultProps = new Properties();
                defaultProps.load(input);
                properties.putAll(defaultProps);
                logger.debug("Loaded default configuration from resources");
            }
        } catch (Exception e) {
            logger.warn("Could not load default configuration from resources: {}", e.getMessage());
        }
        
        // Set hardcoded defaults
        setDefaultValue(KEY_LAST_USED_FOLDER, DEFAULT_LAST_USED_FOLDER);
        setDefaultValue(KEY_ENCRYPTION_ENABLED, String.valueOf(DEFAULT_ENCRYPTION_ENABLED));
        setDefaultValue(KEY_UPLOAD_ENABLED, String.valueOf(DEFAULT_UPLOAD_ENABLED));
        setDefaultValue(KEY_WINDOW_WIDTH, String.valueOf(DEFAULT_WINDOW_WIDTH));
        setDefaultValue(KEY_WINDOW_HEIGHT, String.valueOf(DEFAULT_WINDOW_HEIGHT));
        setDefaultValue(KEY_WINDOW_X, String.valueOf(DEFAULT_WINDOW_X));
        setDefaultValue(KEY_WINDOW_Y, String.valueOf(DEFAULT_WINDOW_Y));
        setDefaultValue(KEY_DRIVE_FOLDER_ID, DEFAULT_DRIVE_FOLDER_ID);
        setDefaultValue(KEY_AUTO_ENCRYPT, String.valueOf(DEFAULT_AUTO_ENCRYPT));
        setDefaultValue(KEY_AUTO_UPLOAD, String.valueOf(DEFAULT_AUTO_UPLOAD));
        setDefaultValue(KEY_SHOW_NOTIFICATIONS, String.valueOf(DEFAULT_SHOW_NOTIFICATIONS));
        setDefaultValue(KEY_LOG_LEVEL, DEFAULT_LOG_LEVEL);
    }
    
    /**
     * Set a default value if the key doesn't exist.
     * 
     * @param key The configuration key
     * @param defaultValue The default value
     */
    private void setDefaultValue(String key, String defaultValue) {
        if (!properties.containsKey(key)) {
            properties.setProperty(key, defaultValue);
        }
    }
    
    /**
     * Get the configuration file path.
     * 
     * @return The configuration file path
     */
    private Path getConfigFilePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, CONFIG_DIR, CONFIG_FILE);
    }
    
    // Getter methods for configuration values
    
    /**
     * Get the last used folder path.
     * 
     * @return The last used folder path
     */
    public String getLastUsedFolder() {
        return properties.getProperty(KEY_LAST_USED_FOLDER, DEFAULT_LAST_USED_FOLDER);
    }
    
    /**
     * Set the last used folder path.
     * 
     * @param folderPath The folder path
     */
    public void setLastUsedFolder(String folderPath) {
        properties.setProperty(KEY_LAST_USED_FOLDER, folderPath);
    }
    
    /**
     * Check if encryption is enabled by default.
     * 
     * @return True if encryption is enabled
     */
    public boolean isEncryptionEnabled() {
        return Boolean.parseBoolean(properties.getProperty(KEY_ENCRYPTION_ENABLED, 
                                                          String.valueOf(DEFAULT_ENCRYPTION_ENABLED)));
    }
    
    /**
     * Set encryption enabled preference.
     * 
     * @param enabled True to enable encryption by default
     */
    public void setEncryptionEnabled(boolean enabled) {
        properties.setProperty(KEY_ENCRYPTION_ENABLED, String.valueOf(enabled));
    }
    
    /**
     * Check if upload is enabled by default.
     * 
     * @return True if upload is enabled
     */
    public boolean isUploadEnabled() {
        return Boolean.parseBoolean(properties.getProperty(KEY_UPLOAD_ENABLED, 
                                                          String.valueOf(DEFAULT_UPLOAD_ENABLED)));
    }
    
    /**
     * Set upload enabled preference.
     * 
     * @param enabled True to enable upload by default
     */
    public void setUploadEnabled(boolean enabled) {
        properties.setProperty(KEY_UPLOAD_ENABLED, String.valueOf(enabled));
    }
    
    /**
     * Get the default window width.
     * 
     * @return The window width
     */
    public int getWindowWidth() {
        return Integer.parseInt(properties.getProperty(KEY_WINDOW_WIDTH, 
                                                     String.valueOf(DEFAULT_WINDOW_WIDTH)));
    }
    
    /**
     * Set the window width.
     * 
     * @param width The window width
     */
    public void setWindowWidth(int width) {
        properties.setProperty(KEY_WINDOW_WIDTH, String.valueOf(width));
    }
    
    /**
     * Get the default window height.
     * 
     * @return The window height
     */
    public int getWindowHeight() {
        return Integer.parseInt(properties.getProperty(KEY_WINDOW_HEIGHT, 
                                                     String.valueOf(DEFAULT_WINDOW_HEIGHT)));
    }
    
    /**
     * Set the window height.
     * 
     * @param height The window height
     */
    public void setWindowHeight(int height) {
        properties.setProperty(KEY_WINDOW_HEIGHT, String.valueOf(height));
    }
    
    /**
     * Get the default window X position.
     * 
     * @return The window X position
     */
    public int getWindowX() {
        return Integer.parseInt(properties.getProperty(KEY_WINDOW_X, 
                                                     String.valueOf(DEFAULT_WINDOW_X)));
    }
    
    /**
     * Set the window X position.
     * 
     * @param x The window X position
     */
    public void setWindowX(int x) {
        properties.setProperty(KEY_WINDOW_X, String.valueOf(x));
    }
    
    /**
     * Get the default window Y position.
     * 
     * @return The window Y position
     */
    public int getWindowY() {
        return Integer.parseInt(properties.getProperty(KEY_WINDOW_Y, 
                                                     String.valueOf(DEFAULT_WINDOW_Y)));
    }
    
    /**
     * Set the window Y position.
     * 
     * @param y The window Y position
     */
    public void setWindowY(int y) {
        properties.setProperty(KEY_WINDOW_Y, String.valueOf(y));
    }
    
    /**
     * Get the Google Drive folder ID.
     * 
     * @return The Drive folder ID
     */
    public String getDriveFolderId() {
        return properties.getProperty(KEY_DRIVE_FOLDER_ID, DEFAULT_DRIVE_FOLDER_ID);
    }
    
    /**
     * Set the Google Drive folder ID.
     * 
     * @param folderId The Drive folder ID
     */
    public void setDriveFolderId(String folderId) {
        properties.setProperty(KEY_DRIVE_FOLDER_ID, folderId);
    }
    
    /**
     * Check if auto-encrypt is enabled.
     * 
     * @return True if auto-encrypt is enabled
     */
    public boolean isAutoEncrypt() {
        return Boolean.parseBoolean(properties.getProperty(KEY_AUTO_ENCRYPT, 
                                                          String.valueOf(DEFAULT_AUTO_ENCRYPT)));
    }
    
    /**
     * Set auto-encrypt preference.
     * 
     * @param autoEncrypt True to enable auto-encrypt
     */
    public void setAutoEncrypt(boolean autoEncrypt) {
        properties.setProperty(KEY_AUTO_ENCRYPT, String.valueOf(autoEncrypt));
    }
    
    /**
     * Check if auto-upload is enabled.
     * 
     * @return True if auto-upload is enabled
     */
    public boolean isAutoUpload() {
        return Boolean.parseBoolean(properties.getProperty(KEY_AUTO_UPLOAD, 
                                                          String.valueOf(DEFAULT_AUTO_UPLOAD)));
    }
    
    /**
     * Set auto-upload preference.
     * 
     * @param autoUpload True to enable auto-upload
     */
    public void setAutoUpload(boolean autoUpload) {
        properties.setProperty(KEY_AUTO_UPLOAD, String.valueOf(autoUpload));
    }
    
    /**
     * Check if notifications are enabled.
     * 
     * @return True if notifications are enabled
     */
    public boolean isShowNotifications() {
        return Boolean.parseBoolean(properties.getProperty(KEY_SHOW_NOTIFICATIONS, 
                                                          String.valueOf(DEFAULT_SHOW_NOTIFICATIONS)));
    }
    
    /**
     * Set show notifications preference.
     * 
     * @param showNotifications True to show notifications
     */
    public void setShowNotifications(boolean showNotifications) {
        properties.setProperty(KEY_SHOW_NOTIFICATIONS, String.valueOf(showNotifications));
    }
    
    /**
     * Get the log level.
     * 
     * @return The log level
     */
    public String getLogLevel() {
        return properties.getProperty(KEY_LOG_LEVEL, DEFAULT_LOG_LEVEL);
    }
    
    /**
     * Set the log level.
     * 
     * @param logLevel The log level
     */
    public void setLogLevel(String logLevel) {
        properties.setProperty(KEY_LOG_LEVEL, logLevel);
    }
    
    /**
     * Check if configuration has been loaded.
     * 
     * @return True if configuration is loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Reset configuration to default values.
     */
    public void resetToDefaults() {
        logger.info("Resetting configuration to defaults");
        properties.clear();
        loadDefaultProperties();
    }
    
    /**
     * Get all configuration properties as a string (for debugging).
     * 
     * @return Configuration properties string
     */
    public String getConfigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration Info:\n");
        sb.append("Config file: ").append(configFilePath).append("\n");
        sb.append("Loaded: ").append(isLoaded).append("\n");
        sb.append("Properties:\n");
        
        for (String key : properties.stringPropertyNames()) {
            sb.append("  ").append(key).append(" = ").append(properties.getProperty(key)).append("\n");
        }
        
        return sb.toString();
    }
}

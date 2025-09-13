package com.securedrive.util;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility class for file selection dialogs.
 * Provides methods for opening and saving files with proper filtering and directory management.
 */
public class FileChooserUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileChooserUtil.class);
    private static final String DEFAULT_TITLE = "Select File";
    private static final String SAVE_TITLE = "Save File";
    private static final String OPEN_TITLE = "Open File";
    
    private static Path defaultDirectory = null;
    
    /**
     * Show a file open dialog for selecting a single file.
     * 
     * @param primaryStage The primary stage for the dialog
     * @return The selected file, or null if cancelled
     */
    public static File showOpenDialog(Stage primaryStage) {
        return showOpenDialog(primaryStage, OPEN_TITLE, null);
    }
    
    /**
     * Show a file open dialog for selecting a single file with custom title.
     * 
     * @param primaryStage The primary stage for the dialog
     * @param title The dialog title
     * @return The selected file, or null if cancelled
     */
    public static File showOpenDialog(Stage primaryStage, String title) {
        return showOpenDialog(primaryStage, title, null);
    }
    
    /**
     * Show a file open dialog for selecting a single file with custom title and extension filter.
     * 
     * @param primaryStage The primary stage for the dialog
     * @param title The dialog title
     * @param extensionFilter The file extension filter
     * @return The selected file, or null if cancelled
     */
    public static File showOpenDialog(Stage primaryStage, String title, FileChooser.ExtensionFilter extensionFilter) {
        try {
            FileChooser fileChooser = createFileChooser(title);
            
            if (extensionFilter != null) {
                fileChooser.getExtensionFilters().add(extensionFilter);
            }
            
            // Add common file filters
            addCommonExtensionFilters(fileChooser);
            
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            
            if (selectedFile != null) {
                // Update default directory for next time
                setDefaultDirectory(selectedFile.getParentFile().toPath());
                logger.info("Selected file: {}", selectedFile.getAbsolutePath());
            }
            
            return selectedFile;
            
        } catch (Exception e) {
            logger.error("Error showing open dialog: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Show a file open dialog for selecting multiple files.
     * 
     * @param primaryStage The primary stage for the dialog
     * @return List of selected files, or null if cancelled
     */
    public static List<File> showOpenMultipleDialog(Stage primaryStage) {
        return showOpenMultipleDialog(primaryStage, OPEN_TITLE, null);
    }
    
    /**
     * Show a file open dialog for selecting multiple files with custom title.
     * 
     * @param primaryStage The primary stage for the dialog
     * @param title The dialog title
     * @return List of selected files, or null if cancelled
     */
    public static List<File> showOpenMultipleDialog(Stage primaryStage, String title) {
        return showOpenMultipleDialog(primaryStage, title, null);
    }
    
    /**
     * Show a file open dialog for selecting multiple files with custom title and extension filter.
     * 
     * @param primaryStage The primary stage for the dialog
     * @param title The dialog title
     * @param extensionFilter The file extension filter
     * @return List of selected files, or null if cancelled
     */
    public static List<File> showOpenMultipleDialog(Stage primaryStage, String title, FileChooser.ExtensionFilter extensionFilter) {
        try {
            FileChooser fileChooser = createFileChooser(title);
            
            if (extensionFilter != null) {
                fileChooser.getExtensionFilters().add(extensionFilter);
            }
            
            // Add common file filters
            addCommonExtensionFilters(fileChooser);
            
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
            
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                // Update default directory for next time
                setDefaultDirectory(selectedFiles.get(0).getParentFile().toPath());
                logger.info("Selected {} files", selectedFiles.size());
            }
            
            return selectedFiles;
            
        } catch (Exception e) {
            logger.error("Error showing open multiple dialog: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Show a file save dialog.
     * 
     * @param primaryStage The primary stage for the dialog
     * @return The selected file, or null if cancelled
     */
    public static File showSaveDialog(Stage primaryStage) {
        return showSaveDialog(primaryStage, SAVE_TITLE, null);
    }
    
    /**
     * Show a file save dialog with custom title.
     * 
     * @param primaryStage The primary stage for the dialog
     * @param title The dialog title
     * @return The selected file, or null if cancelled
     */
    public static File showSaveDialog(Stage primaryStage, String title) {
        return showSaveDialog(primaryStage, title, null);
    }
    
    /**
     * Show a file save dialog with custom title and extension filter.
     * 
     * @param primaryStage The primary stage for the dialog
     * @param title The dialog title
     * @param extensionFilter The file extension filter
     * @return The selected file, or null if cancelled
     */
    public static File showSaveDialog(Stage primaryStage, String title, FileChooser.ExtensionFilter extensionFilter) {
        try {
            FileChooser fileChooser = createFileChooser(title);
            
            if (extensionFilter != null) {
                fileChooser.getExtensionFilters().add(extensionFilter);
            }
            
            // Add common file filters
            addCommonExtensionFilters(fileChooser);
            
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            
            if (selectedFile != null) {
                // Update default directory for next time
                setDefaultDirectory(selectedFile.getParentFile().toPath());
                logger.info("Selected save file: {}", selectedFile.getAbsolutePath());
            }
            
            return selectedFile;
            
        } catch (Exception e) {
            logger.error("Error showing save dialog: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create a FileChooser with the specified title.
     * 
     * @param title The dialog title
     * @return Configured FileChooser
     */
    private static FileChooser createFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        
        // Set initial directory
        if (defaultDirectory != null && defaultDirectory.toFile().exists()) {
            fileChooser.setInitialDirectory(defaultDirectory.toFile());
        } else {
            // Use user's home directory as default
            String userHome = System.getProperty("user.home");
            fileChooser.setInitialDirectory(new File(userHome));
        }
        
        return fileChooser;
    }
    
    /**
     * Add common extension filters to the FileChooser.
     * 
     * @param fileChooser The FileChooser to add filters to
     */
    private static void addCommonExtensionFilters(FileChooser fileChooser) {
        // All files filter
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Document filters
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf")
        );
        
        // Image filters
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.tiff")
        );
        
        // Video filters
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov", "*.wmv", "*.flv", "*.mkv")
        );
        
        // Audio filters
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.flac", "*.aac", "*.ogg")
        );
        
        // Archive filters
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z", "*.tar", "*.gz")
        );
        
        // Encrypted files filter
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Encrypted Files", "*.encrypted")
        );
        
        // Set the first filter as default
        if (!fileChooser.getExtensionFilters().isEmpty()) {
            fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        }
    }
    
    /**
     * Set the default directory for file dialogs.
     * 
     * @param directory The default directory path
     */
    public static void setDefaultDirectory(Path directory) {
        if (directory != null && directory.toFile().exists() && directory.toFile().isDirectory()) {
            defaultDirectory = directory;
            logger.debug("Set default directory: {}", directory);
        }
    }
    
    /**
     * Get the current default directory.
     * 
     * @return The current default directory, or null if not set
     */
    public static Path getDefaultDirectory() {
        return defaultDirectory;
    }
    
    /**
     * Reset the default directory to the user's home directory.
     */
    public static void resetDefaultDirectory() {
        String userHome = System.getProperty("user.home");
        defaultDirectory = Paths.get(userHome);
        logger.debug("Reset default directory to: {}", defaultDirectory);
    }
    
    /**
     * Create a custom extension filter.
     * 
     * @param description The filter description
     * @param extensions The file extensions (e.g., "*.txt", "*.doc")
     * @return The extension filter
     */
    public static FileChooser.ExtensionFilter createExtensionFilter(String description, String... extensions) {
        return new FileChooser.ExtensionFilter(description, extensions);
    }
    
    /**
     * Get the file extension from a file name.
     * 
     * @param fileName The file name
     * @return The file extension (without the dot), or empty string if no extension
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * Check if a file has a specific extension.
     * 
     * @param fileName The file name
     * @param extension The extension to check (without the dot)
     * @return True if the file has the specified extension
     */
    public static boolean hasExtension(String fileName, String extension) {
        String fileExtension = getFileExtension(fileName);
        return fileExtension.equals(extension.toLowerCase());
    }
}

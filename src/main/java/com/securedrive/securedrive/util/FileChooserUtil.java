package com.securedrive.securedrive.util;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for handling file chooser dialogs.
 * Provides convenient methods for opening and saving files with proper filtering.
 */
public class FileChooserUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileChooserUtil.class);
    
    private static final String DEFAULT_TITLE = "Select File";
    private static final String SAVE_TITLE = "Save File";
    private static final String OPEN_TITLE = "Open File";
    
    private Path initialDirectory;
    private String lastSelectedDirectory;
    
    /**
     * Constructor for FileChooserUtil.
     */
    public FileChooserUtil() {
        this.initialDirectory = Paths.get(System.getProperty("user.home"));
        logger.debug("FileChooserUtil initialized with initial directory: {}", initialDirectory);
    }
    
    /**
     * Shows a file open dialog.
     * 
     * @return the selected file, or null if cancelled
     */
    public File showOpenDialog() {
        return showOpenDialog(OPEN_TITLE, null);
    }
    
    /**
     * Shows a file open dialog with custom title.
     * 
     * @param title the dialog title
     * @return the selected file, or null if cancelled
     */
    public File showOpenDialog(String title) {
        return showOpenDialog(title, null);
    }
    
    /**
     * Shows a file open dialog with custom title and file filters.
     * 
     * @param title the dialog title
     * @param fileFilters the file filters to apply
     * @return the selected file, or null if cancelled
     */
    public File showOpenDialog(String title, List<FileChooser.ExtensionFilter> fileFilters) {
        logger.debug("Showing file open dialog with title: {}", title);
        
        FileChooser fileChooser = createFileChooser(title);
        
        if (fileFilters != null && !fileFilters.isEmpty()) {
            fileChooser.getExtensionFilters().addAll(fileFilters);
        } else {
            // Add default filters
            addDefaultFilters(fileChooser);
        }
        
        // Set initial directory
        if (initialDirectory != null && initialDirectory.toFile().exists()) {
            fileChooser.setInitialDirectory(initialDirectory.toFile());
        }
        
        File selectedFile = fileChooser.showOpenDialog(getCurrentStage());
        
        if (selectedFile != null) {
            lastSelectedDirectory = selectedFile.getParent();
            logger.info("File selected: {}", selectedFile.getAbsolutePath());
        }
        
        return selectedFile;
    }
    
    /**
     * Shows a file save dialog.
     * 
     * @return the selected file, or null if cancelled
     */
    public File showSaveDialog() {
        return showSaveDialog(SAVE_TITLE, null);
    }
    
    /**
     * Shows a file save dialog with custom title.
     * 
     * @param title the dialog title
     * @return the selected file, or null if cancelled
     */
    public File showSaveDialog(String title) {
        return showSaveDialog(title, null);
    }
    
    /**
     * Shows a file save dialog with custom title and file filters.
     * 
     * @param title the dialog title
     * @param fileFilters the file filters to apply
     * @return the selected file, or null if cancelled
     */
    public File showSaveDialog(String title, List<FileChooser.ExtensionFilter> fileFilters) {
        logger.debug("Showing file save dialog with title: {}", title);
        
        FileChooser fileChooser = createFileChooser(title);
        
        if (fileFilters != null && !fileFilters.isEmpty()) {
            fileChooser.getExtensionFilters().addAll(fileFilters);
        } else {
            // Add default filters
            addDefaultFilters(fileChooser);
        }
        
        // Set initial directory
        if (initialDirectory != null && initialDirectory.toFile().exists()) {
            fileChooser.setInitialDirectory(initialDirectory.toFile());
        }
        
        File selectedFile = fileChooser.showSaveDialog(getCurrentStage());
        
        if (selectedFile != null) {
            lastSelectedDirectory = selectedFile.getParent();
            logger.info("File selected for saving: {}", selectedFile.getAbsolutePath());
        }
        
        return selectedFile;
    }
    
    /**
     * Shows a file open dialog for encrypted files only.
     * 
     * @return the selected encrypted file, or null if cancelled
     */
    public File showEncryptedFileOpenDialog() {
        logger.debug("Showing encrypted file open dialog");
        
        FileChooser fileChooser = createFileChooser("Select Encrypted File");
        
        // Add filter for encrypted files
        FileChooser.ExtensionFilter encryptedFilter = new FileChooser.ExtensionFilter(
                "Encrypted Files (*.enc)", "*.enc");
        fileChooser.getExtensionFilters().add(encryptedFilter);
        
        // Add all files filter
        FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter(
                "All Files (*.*)", "*.*");
        fileChooser.getExtensionFilters().add(allFilesFilter);
        
        // Set initial directory
        if (initialDirectory != null && initialDirectory.toFile().exists()) {
            fileChooser.setInitialDirectory(initialDirectory.toFile());
        }
        
        File selectedFile = fileChooser.showOpenDialog(getCurrentStage());
        
        if (selectedFile != null) {
            lastSelectedDirectory = selectedFile.getParent();
            logger.info("Encrypted file selected: {}", selectedFile.getAbsolutePath());
        }
        
        return selectedFile;
    }
    
    /**
     * Shows a file save dialog for encrypted files.
     * 
     * @param suggestedName the suggested file name
     * @return the selected file, or null if cancelled
     */
    public File showEncryptedFileSaveDialog(String suggestedName) {
        logger.debug("Showing encrypted file save dialog with suggested name: {}", suggestedName);
        
        FileChooser fileChooser = createFileChooser("Save Encrypted File");
        
        // Add filter for encrypted files
        FileChooser.ExtensionFilter encryptedFilter = new FileChooser.ExtensionFilter(
                "Encrypted Files (*.enc)", "*.enc");
        fileChooser.getExtensionFilters().add(encryptedFilter);
        
        // Add all files filter
        FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter(
                "All Files (*.*)", "*.*");
        fileChooser.getExtensionFilters().add(allFilesFilter);
        
        // Set initial directory
        if (initialDirectory != null && initialDirectory.toFile().exists()) {
            fileChooser.setInitialDirectory(initialDirectory.toFile());
        }
        
        // Set initial file name
        if (suggestedName != null && !suggestedName.trim().isEmpty()) {
            fileChooser.setInitialFileName(suggestedName);
        }
        
        File selectedFile = fileChooser.showSaveDialog(getCurrentStage());
        
        if (selectedFile != null) {
            lastSelectedDirectory = selectedFile.getParent();
            logger.info("Encrypted file save location selected: {}", selectedFile.getAbsolutePath());
        }
        
        return selectedFile;
    }
    
    /**
     * Sets the initial directory for file chooser dialogs.
     * 
     * @param directory the initial directory path
     */
    public void setInitialDirectory(Path directory) {
        if (directory != null && directory.toFile().exists() && directory.toFile().isDirectory()) {
            this.initialDirectory = directory;
            logger.debug("Initial directory set to: {}", directory);
        } else {
            logger.warn("Invalid directory provided: {}", directory);
        }
    }
    
    /**
     * Sets the initial directory for file chooser dialogs.
     * 
     * @param directory the initial directory as a string
     */
    public void setInitialDirectory(String directory) {
        if (directory != null && !directory.trim().isEmpty()) {
            setInitialDirectory(Paths.get(directory));
        }
    }
    
    /**
     * Gets the last selected directory.
     * 
     * @return the last selected directory, or null if none
     */
    public String getLastSelectedDirectory() {
        return lastSelectedDirectory;
    }
    
    /**
     * Gets the initial directory.
     * 
     * @return the initial directory path
     */
    public Path getInitialDirectory() {
        return initialDirectory;
    }
    
    /**
     * Creates a file chooser with the specified title.
     * 
     * @param title the dialog title
     * @return the configured file chooser
     */
    private FileChooser createFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        return fileChooser;
    }
    
    /**
     * Adds default file filters to the file chooser.
     * 
     * @param fileChooser the file chooser to add filters to
     */
    private void addDefaultFilters(FileChooser fileChooser) {
        // All files filter
        FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter(
                "All Files (*.*)", "*.*");
        fileChooser.getExtensionFilters().add(allFilesFilter);
        
        // Common file types
        FileChooser.ExtensionFilter documentsFilter = new FileChooser.ExtensionFilter(
                "Documents (*.pdf, *.doc, *.docx, *.txt, *.rtf)", 
                "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf");
        fileChooser.getExtensionFilters().add(documentsFilter);
        
        FileChooser.ExtensionFilter imagesFilter = new FileChooser.ExtensionFilter(
                "Images (*.jpg, *.jpeg, *.png, *.gif, *.bmp)", 
                "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp");
        fileChooser.getExtensionFilters().add(imagesFilter);
        
        FileChooser.ExtensionFilter archivesFilter = new FileChooser.ExtensionFilter(
                "Archives (*.zip, *.rar, *.7z, *.tar, *.gz)", 
                "*.zip", "*.rar", "*.7z", "*.tar", "*.gz");
        fileChooser.getExtensionFilters().add(archivesFilter);
    }
    
    /**
     * Gets the current JavaFX stage.
     * This is a simple implementation that may need to be enhanced
     * based on your application's stage management.
     * 
     * @return the current stage, or null if not available
     */
    private Stage getCurrentStage() {
        // This is a simplified implementation
        // In a real application, you might want to pass the stage
        // or use a more sophisticated stage management system
        return null;
    }
    
    /**
     * Creates a file chooser with multiple file selection support.
     * 
     * @param title the dialog title
     * @return the configured file chooser
     */
    public FileChooser createMultiFileChooser(String title) {
        FileChooser fileChooser = createFileChooser(title);
        addDefaultFilters(fileChooser);
        
        if (initialDirectory != null && initialDirectory.toFile().exists()) {
            fileChooser.setInitialDirectory(initialDirectory.toFile());
        }
        
        return fileChooser;
    }
    
    /**
     * Shows a directory chooser dialog.
     * 
     * @param title the dialog title
     * @return the selected directory, or null if cancelled
     */
    public File showDirectoryChooser(String title) {
        logger.debug("Showing directory chooser with title: {}", title);
        
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle(title);
        
        if (initialDirectory != null && initialDirectory.toFile().exists()) {
            directoryChooser.setInitialDirectory(initialDirectory.toFile());
        }
        
        File selectedDirectory = directoryChooser.showDialog(getCurrentStage());
        
        if (selectedDirectory != null) {
            lastSelectedDirectory = selectedDirectory.getAbsolutePath();
            logger.info("Directory selected: {}", selectedDirectory.getAbsolutePath());
        }
        
        return selectedDirectory;
    }
}

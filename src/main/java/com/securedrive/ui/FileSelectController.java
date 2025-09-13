package com.securedrive.ui;

import com.securedrive.util.FileChooserUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the File Selection Screen.
 * Handles multi-file selection and file management for upload.
 */
public class FileSelectController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(FileSelectController.class);
    
    @FXML
    private Button selectFilesButton;
    
    @FXML
    private Button selectFolderButton;
    
    @FXML
    private Button clearAllButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Button nextButton;
    
    @FXML
    private ListView<FileItem> filesListView;
    
    @FXML
    private Label fileCountLabel;
    
    @FXML
    private Label totalSizeLabel;
    
    private ScreenManager screenManager;
    private ObservableList<FileItem> selectedFiles;
    private long totalSize = 0;
    
    // Supported file extensions
    private static final String[] SUPPORTED_EXTENSIONS = {
        ".txt", ".pdf", ".docx", ".xlsx", ".pptx", ".zip", ".rar", ".7z",
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff",
        ".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv",
        ".mp3", ".wav", ".flac", ".aac", ".ogg",
        ".csv", ".json", ".xml", ".html", ".css", ".js"
    };
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing File Selection Screen");
        
        // Initialize the files list
        selectedFiles = FXCollections.observableArrayList();
        filesListView.setItems(selectedFiles);
        
        // Set up list view cell factory for custom display
        filesListView.setCellFactory(listView -> new ListCell<FileItem>() {
            @Override
            protected void updateItem(FileItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayText());
                    setGraphic(item.getIcon());
                }
            }
        });
        
        // Set up context menu for list items
        setupContextMenu();
        
        // Update UI state
        updateUIState();
        
        logger.info("File Selection Screen initialized successfully");
    }
    
    /**
     * Sets up the context menu for the files list view.
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(e -> {
            FileItem selectedItem = filesListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                removeFile(selectedItem);
            }
        });
        
        MenuItem removeAllItem = new MenuItem("Remove All");
        removeAllItem.setOnAction(e -> clearAllFiles());
        
        contextMenu.getItems().addAll(removeItem, new SeparatorMenuItem(), removeAllItem);
        filesListView.setContextMenu(contextMenu);
    }
    
    /**
     * Handles the Select Files button click event.
     * Opens a file chooser for multi-file selection.
     */
    @FXML
    private void handleSelectFilesButton() {
        logger.info("Select Files button clicked");
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Upload");
        
        // Set initial directory
        String lastUsedFolder = System.getProperty("user.home");
        fileChooser.setInitialDirectory(new File(lastUsedFolder));
        
        // Add file extension filters
        FileChooser.ExtensionFilter allFiles = new FileChooser.ExtensionFilter("All Files", "*.*");
        FileChooser.ExtensionFilter documents = new FileChooser.ExtensionFilter("Documents", "*.txt", "*.pdf", "*.docx", "*.xlsx", "*.pptx");
        FileChooser.ExtensionFilter images = new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp");
        FileChooser.ExtensionFilter videos = new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov", "*.wmv");
        FileChooser.ExtensionFilter audio = new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.flac", "*.aac");
        FileChooser.ExtensionFilter archives = new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z");
        
        fileChooser.getExtensionFilters().addAll(documents, images, videos, audio, archives, allFiles);
        fileChooser.setSelectedExtensionFilter(documents);
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(selectFilesButton.getScene().getWindow());
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            addFiles(selectedFiles);
        }
    }
    
    /**
     * Handles the Select Folder button click event.
     * Opens a directory chooser and adds all supported files from the selected folder.
     */
    @FXML
    private void handleSelectFolderButton() {
        logger.info("Select Folder button clicked");
        
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Upload");
        
        // Set initial directory
        String lastUsedFolder = System.getProperty("user.home");
        directoryChooser.setInitialDirectory(new File(lastUsedFolder));
        
        File selectedDirectory = directoryChooser.showDialog(selectFolderButton.getScene().getWindow());
        
        if (selectedDirectory != null) {
            List<File> filesInFolder = getSupportedFilesFromFolder(selectedDirectory);
            if (!filesInFolder.isEmpty()) {
                addFiles(filesInFolder);
            } else {
                showInfoDialog("No Supported Files", "No supported files found in the selected folder.");
            }
        }
    }
    
    /**
     * Handles the Clear All button click event.
     * Removes all selected files from the list.
     */
    @FXML
    private void handleClearAllButton() {
        logger.info("Clear All button clicked");
        clearAllFiles();
    }
    
    /**
     * Handles the Back button click event.
     * Returns to the authentication screen.
     */
    @FXML
    private void handleBackButton() {
        logger.info("Back button clicked - returning to authentication screen");
        
        if (screenManager != null) {
            screenManager.transitionToAuth();
        }
    }
    
    /**
     * Handles the Next button click event.
     * Proceeds to the encryption setup screen.
     */
    @FXML
    private void handleNextButton() {
        logger.info("Next button clicked - proceeding to encryption screen");
        
        if (!selectedFiles.isEmpty() && screenManager != null) {
            // Store selected files in a way that can be accessed by other screens
            // This could be done through a shared data model or the screen manager
            screenManager.transitionToEncryption();
        } else {
            showErrorDialog("No Files Selected", "Please select at least one file to continue.");
        }
    }
    
    /**
     * Adds files to the selection list.
     * 
     * @param files The files to add
     */
    private void addFiles(List<File> files) {
        for (File file : files) {
            if (file.exists() && file.isFile() && isSupportedFile(file)) {
                FileItem fileItem = new FileItem(file);
                
                // Check if file is already in the list
                if (!selectedFiles.contains(fileItem)) {
                    selectedFiles.add(fileItem);
                    totalSize += file.length();
                    logger.debug("Added file: {} ({} bytes)", file.getName(), file.length());
                }
            }
        }
        
        updateUIState();
    }
    
    /**
     * Removes a file from the selection list.
     * 
     * @param fileItem The file item to remove
     */
    private void removeFile(FileItem fileItem) {
        if (selectedFiles.remove(fileItem)) {
            totalSize -= fileItem.getFile().length();
            updateUIState();
            logger.debug("Removed file: {}", fileItem.getFile().getName());
        }
    }
    
    /**
     * Clears all files from the selection list.
     */
    private void clearAllFiles() {
        selectedFiles.clear();
        totalSize = 0;
        updateUIState();
        logger.info("Cleared all selected files");
    }
    
    /**
     * Gets all supported files from a directory recursively.
     * 
     * @param directory The directory to search
     * @return List of supported files
     */
    private List<File> getSupportedFilesFromFolder(File directory) {
        List<File> supportedFiles = new ArrayList<>();
        
        try {
            Files.walk(Paths.get(directory.getAbsolutePath()))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(this::isSupportedFile)
                .forEach(supportedFiles::add);
        } catch (Exception e) {
            logger.error("Error scanning folder: {}", e.getMessage(), e);
        }
        
        return supportedFiles;
    }
    
    /**
     * Checks if a file is supported based on its extension.
     * 
     * @param file The file to check
     * @return true if supported, false otherwise
     */
    private boolean isSupportedFile(File file) {
        String fileName = file.getName().toLowerCase();
        
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Updates the UI state based on current file selection.
     */
    private void updateUIState() {
        int fileCount = selectedFiles.size();
        
        // Update file count label
        fileCountLabel.setText("(" + fileCount + " file" + (fileCount != 1 ? "s" : "") + ")");
        
        // Update total size label
        totalSizeLabel.setText("Total size: " + formatFileSize(totalSize));
        
        // Update button states
        clearAllButton.setDisable(fileCount == 0);
        nextButton.setDisable(fileCount == 0);
    }
    
    /**
     * Formats file size in human-readable format.
     * 
     * @param bytes The size in bytes
     * @return Formatted size string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
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
     * Gets the list of selected files.
     * 
     * @return Observable list of selected file items
     */
    public ObservableList<FileItem> getSelectedFiles() {
        return selectedFiles;
    }
    
    /**
     * Gets the total size of selected files.
     * 
     * @return Total size in bytes
     */
    public long getTotalSize() {
        return totalSize;
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
}

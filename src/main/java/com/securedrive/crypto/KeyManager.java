package com.securedrive.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Manages encryption keys for the SecureDriveUploader application.
 * Handles key generation, storage, and retrieval with secure practices.
 */
public class KeyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256; // 256 bits
    private static final String KEY_FILE_NAME = "encryption.key";
    private static final String CONFIG_DIR = ".securedrive";
    
    private byte[] encryptionKey;
    private final Path keyFilePath;
    
    /**
     * Constructor for KeyManager.
     * Initializes the key file path and loads or generates encryption key.
     */
    public KeyManager() {
        this.keyFilePath = getKeyFilePath();
        initializeKey();
    }
    
    /**
     * Initialize the encryption key by loading from file or generating a new one.
     */
    private void initializeKey() {
        try {
            if (loadKeyFromFile()) {
                logger.info("Encryption key loaded from file");
            } else {
                generateNewKey();
                saveKeyToFile();
                logger.info("New encryption key generated and saved");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize encryption key: {}", e.getMessage(), e);
            throw new RuntimeException("Key initialization failed", e);
        }
    }
    
    /**
     * Generate a new AES-256 encryption key.
     * 
     * @throws NoSuchAlgorithmException If AES algorithm is not available
     */
    private void generateNewKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(KEY_LENGTH, SecureRandom.getInstanceStrong());
        SecretKey secretKey = keyGenerator.generateKey();
        this.encryptionKey = secretKey.getEncoded();
        
        logger.info("Generated new AES-256 encryption key");
    }
    
    /**
     * Get the current encryption key.
     * 
     * @return The encryption key bytes
     */
    public byte[] getEncryptionKey() {
        if (encryptionKey == null) {
            throw new IllegalStateException("Encryption key not initialized");
        }
        return encryptionKey.clone(); // Return a copy for security
    }
    
    /**
     * Get the encryption key as a SecretKeySpec.
     * 
     * @return The SecretKeySpec for encryption operations
     */
    public SecretKeySpec getSecretKeySpec() {
        if (encryptionKey == null) {
            throw new IllegalStateException("Encryption key not initialized");
        }
        return new SecretKeySpec(encryptionKey, KEY_ALGORITHM);
    }
    
    /**
     * Load encryption key from file.
     * 
     * @return True if key was loaded successfully, false otherwise
     */
    private boolean loadKeyFromFile() {
        try {
            if (!Files.exists(keyFilePath)) {
                logger.debug("Key file does not exist: {}", keyFilePath);
                return false;
            }
            
            if (!Files.isRegularFile(keyFilePath)) {
                logger.warn("Key file path is not a regular file: {}", keyFilePath);
                return false;
            }
            
            // Check file permissions (should be readable only by owner)
            if (!isKeyFileSecure()) {
                logger.warn("Key file has insecure permissions: {}", keyFilePath);
                return false;
            }
            
            // Read key from file
            String encodedKey = Files.readString(keyFilePath).trim();
            this.encryptionKey = Base64.getDecoder().decode(encodedKey);
            
            // Validate key length
            if (encryptionKey.length != KEY_LENGTH / 8) {
                logger.error("Invalid key length: expected {}, got {}", 
                           KEY_LENGTH / 8, encryptionKey.length);
                this.encryptionKey = null;
                return false;
            }
            
            logger.debug("Successfully loaded encryption key from file");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load key from file: {}", e.getMessage());
            this.encryptionKey = null;
            return false;
        }
    }
    
    /**
     * Save encryption key to file.
     */
    private void saveKeyToFile() {
        try {
            // Create config directory if it doesn't exist
            Path configDir = keyFilePath.getParent();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                logger.debug("Created config directory: {}", configDir);
            }
            
            // Encode key as Base64
            String encodedKey = Base64.getEncoder().encodeToString(encryptionKey);
            
            // Write key to file
            Files.writeString(keyFilePath, encodedKey);
            
            // Set secure file permissions (readable only by owner)
            setSecureFilePermissions(keyFilePath);
            
            logger.debug("Successfully saved encryption key to file");
            
        } catch (Exception e) {
            logger.error("Failed to save key to file: {}", e.getMessage(), e);
            throw new RuntimeException("Key save failed", e);
        }
    }
    
    /**
     * Get the path to the key file.
     * 
     * @return The key file path
     */
    private Path getKeyFilePath() {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, CONFIG_DIR);
        return configDir.resolve(KEY_FILE_NAME);
    }
    
    /**
     * Check if the key file has secure permissions.
     * 
     * @return True if permissions are secure, false otherwise
     */
    private boolean isKeyFileSecure() {
        try {
            // On Unix-like systems, check if file is readable only by owner
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                // Windows permission checking is more complex, assume secure for now
                return true;
            }
            
            // Check file permissions using Java NIO
            java.nio.file.attribute.PosixFileAttributes attrs = 
                Files.readAttributes(keyFilePath, java.nio.file.attribute.PosixFileAttributes.class);
            
            java.nio.file.attribute.PosixFilePermissions perms = attrs.permissions();
            
            // File should be readable only by owner
            return perms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_READ) &&
                   !perms.contains(java.nio.file.attribute.PosixFilePermission.GROUP_READ) &&
                   !perms.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_READ);
            
        } catch (Exception e) {
            logger.warn("Could not check key file permissions: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Set secure file permissions for the key file.
     * 
     * @param filePath The file path to secure
     */
    private void setSecureFilePermissions(Path filePath) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                // Windows permission setting is more complex, skip for now
                return;
            }
            
            // Set permissions to read-only for owner
            java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = 
                java.util.EnumSet.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ);
            
            Files.setPosixFilePermissions(filePath, perms);
            logger.debug("Set secure permissions for key file");
            
        } catch (Exception e) {
            logger.warn("Could not set secure file permissions: {}", e.getMessage());
        }
    }
    
    /**
     * Regenerate the encryption key.
     * This will invalidate all previously encrypted files.
     */
    public void regenerateKey() {
        try {
            generateNewKey();
            saveKeyToFile();
            logger.warn("Encryption key regenerated - previously encrypted files cannot be decrypted");
        } catch (Exception e) {
            logger.error("Failed to regenerate encryption key: {}", e.getMessage(), e);
            throw new RuntimeException("Key regeneration failed", e);
        }
    }
    
    /**
     * Check if the key manager is properly initialized.
     * 
     * @return True if initialized, false otherwise
     */
    public boolean isInitialized() {
        return encryptionKey != null && encryptionKey.length == KEY_LENGTH / 8;
    }
    
    /**
     * Get information about the current key (for debugging purposes only).
     * 
     * @return Key information string
     */
    public String getKeyInfo() {
        if (encryptionKey == null) {
            return "Key not initialized";
        }
        
        return String.format("Key algorithm: %s, Length: %d bits, File: %s", 
                           KEY_ALGORITHM, KEY_LENGTH, keyFilePath);
    }
}

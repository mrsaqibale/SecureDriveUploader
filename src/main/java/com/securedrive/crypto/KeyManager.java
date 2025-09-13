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
     * Generate a new encryption key for the specified algorithm.
     * 
     * @param algorithm The encryption algorithm (e.g., "AES-256-CBC")
     * @return The generated key bytes
     * @throws Exception If key generation fails
     */
    public byte[] generateKey(String algorithm) throws Exception {
        try {
            int keyLength = getKeyLengthFromAlgorithm(algorithm);
            String baseAlgorithm = getBaseAlgorithmFromAlgorithm(algorithm);
            
            KeyGenerator keyGenerator = KeyGenerator.getInstance(baseAlgorithm);
            keyGenerator.init(keyLength, SecureRandom.getInstanceStrong());
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] key = secretKey.getEncoded();
            
            logger.info("Generated new {} encryption key", algorithm);
            return key;
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unsupported algorithm: {}", algorithm);
            throw new Exception("Unsupported encryption algorithm: " + algorithm, e);
        }
    }
    
    /**
     * Load encryption key from a specific file.
     * 
     * @param keyFile The file containing the key
     * @return The loaded key bytes
     * @throws Exception If key loading fails
     */
    public byte[] loadKeyFromFile(java.io.File keyFile) throws Exception {
        try {
            if (!keyFile.exists()) {
                throw new FileNotFoundException("Key file does not exist: " + keyFile.getAbsolutePath());
            }
            
            if (!keyFile.isFile()) {
                throw new IllegalArgumentException("Path is not a file: " + keyFile.getAbsolutePath());
            }
            
            if (!keyFile.canRead()) {
                throw new SecurityException("Cannot read key file: " + keyFile.getAbsolutePath());
            }
            
            // Read key from file
            String encodedKey = Files.readString(keyFile.toPath()).trim();
            byte[] key = Base64.getDecoder().decode(encodedKey);
            
            logger.info("Successfully loaded encryption key from file: {}", keyFile.getName());
            return key;
            
        } catch (Exception e) {
            logger.error("Failed to load key from file {}: {}", keyFile.getName(), e.getMessage());
            throw new Exception("Failed to load key from file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save encryption key to a specific file.
     * 
     * @param key The key bytes to save
     * @param keyFile The file to save the key to
     * @throws Exception If key saving fails
     */
    public void saveKeyToFile(byte[] key, java.io.File keyFile) throws Exception {
        try {
            // Create parent directory if it doesn't exist
            java.io.File parentDir = keyFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Encode key as Base64
            String encodedKey = Base64.getEncoder().encodeToString(key);
            
            // Write key to file
            Files.writeString(keyFile.toPath(), encodedKey);
            
            // Set secure file permissions
            setSecureFilePermissions(keyFile.toPath());
            
            logger.info("Successfully saved encryption key to file: {}", keyFile.getName());
            
        } catch (Exception e) {
            logger.error("Failed to save key to file {}: {}", keyFile.getName(), e.getMessage());
            throw new Exception("Failed to save key to file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the key length from algorithm string.
     * 
     * @param algorithm The algorithm string (e.g., "AES-256-CBC")
     * @return The key length in bits
     */
    private int getKeyLengthFromAlgorithm(String algorithm) {
        if (algorithm.contains("128")) return 128;
        if (algorithm.contains("192")) return 192;
        if (algorithm.contains("256")) return 256;
        return 256; // Default to 256 bits
    }
    
    /**
     * Get the base algorithm from algorithm string.
     * 
     * @param algorithm The algorithm string (e.g., "AES-256-CBC")
     * @return The base algorithm (e.g., "AES")
     */
    private String getBaseAlgorithmFromAlgorithm(String algorithm) {
        if (algorithm.startsWith("AES")) return "AES";
        return "AES"; // Default to AES
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
            
            java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = attrs.permissions();
            
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

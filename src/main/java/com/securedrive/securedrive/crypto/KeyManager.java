package com.securedrive.securedrive.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.security.Security;
import java.util.Base64;

/**
 * Manages encryption keys for the SecureDriveUploader application.
 * Handles key generation, storage, and retrieval with secure practices.
 */
public class KeyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);
    
    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256; // 256 bits
    private static final String KEY_FILE_NAME = "encryption.key";
    private static final String KEY_DIRECTORY = ".securedrive";
    
    private byte[] currentKey;
    private final Path keyDirectory;
    private final Path keyFilePath;
    
    static {
        // Add Bouncy Castle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            logger.debug("Bouncy Castle provider added to KeyManager");
        }
    }
    
    /**
     * Constructor for KeyManager.
     * Initializes the key directory and file paths.
     */
    public KeyManager() {
        this.keyDirectory = Paths.get(System.getProperty("user.home"), KEY_DIRECTORY);
        this.keyFilePath = keyDirectory.resolve(KEY_FILE_NAME);
        
        logger.debug("KeyManager initialized with key directory: {}", keyDirectory);
        
        // Ensure key directory exists
        createKeyDirectoryIfNeeded();
    }
    
    /**
     * Generates a new AES encryption key.
     * 
     * @throws NoSuchAlgorithmException if AES algorithm is not available
     */
    public void generateKey() throws NoSuchAlgorithmException {
        logger.info("Generating new AES encryption key");
        
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        keyGenerator.init(KEY_LENGTH, SecureRandom.getInstanceStrong());
        
        SecretKey secretKey = keyGenerator.generateKey();
        currentKey = secretKey.getEncoded();
        
        logger.info("New encryption key generated successfully");
    }
    
    /**
     * Gets the current encryption key.
     * If no key exists, generates a new one.
     * 
     * @return the encryption key as byte array
     * @throws NoSuchAlgorithmException if key generation fails
     */
    public byte[] getKey() throws NoSuchAlgorithmException {
        if (currentKey == null) {
            // Try to load existing key first
            if (loadKeyFromFile()) {
                logger.debug("Loaded existing key from file");
            } else {
                // Generate new key if none exists
                generateKey();
                saveKeyToFile();
            }
        }
        
        return currentKey.clone(); // Return a copy for security
    }
    
    /**
     * Saves the current key to a secure file.
     * 
     * @return true if key was saved successfully
     */
    public boolean saveKeyToFile() {
        if (currentKey == null) {
            logger.warn("No key to save");
            return false;
        }
        
        try {
            // Ensure directory exists
            createKeyDirectoryIfNeeded();
            
            // Save key to file
            try (FileOutputStream fos = new FileOutputStream(keyFilePath.toFile());
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                
                // Encode key in Base64 for storage
                String encodedKey = Base64.getEncoder().encodeToString(currentKey);
                oos.writeObject(encodedKey);
                oos.flush();
                
                // Set restrictive permissions on the key file
                setSecureFilePermissions(keyFilePath);
                
                logger.info("Encryption key saved to file: {}", keyFilePath);
                return true;
            }
            
        } catch (IOException e) {
            logger.error("Failed to save encryption key to file", e);
            return false;
        }
    }
    
    /**
     * Loads the encryption key from file.
     * 
     * @return true if key was loaded successfully
     */
    public boolean loadKeyFromFile() {
        if (!Files.exists(keyFilePath)) {
            logger.debug("No key file found at: {}", keyFilePath);
            return false;
        }
        
        try (FileInputStream fis = new FileInputStream(keyFilePath.toFile());
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            String encodedKey = (String) ois.readObject();
            currentKey = Base64.getDecoder().decode(encodedKey);
            
            logger.info("Encryption key loaded from file: {}", keyFilePath);
            return true;
            
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Failed to load encryption key from file", e);
            return false;
        }
    }
    
    /**
     * Deletes the stored key file.
     * 
     * @return true if key file was deleted successfully
     */
    public boolean deleteKeyFile() {
        try {
            if (Files.exists(keyFilePath)) {
                Files.delete(keyFilePath);
                logger.info("Encryption key file deleted: {}", keyFilePath);
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to delete encryption key file", e);
        }
        
        return false;
    }
    
    /**
     * Clears the current key from memory.
     * This is a security measure to prevent key exposure.
     */
    public void clearKey() {
        if (currentKey != null) {
            // Overwrite the key array with random data
            SecureRandom.getInstanceStrong().nextBytes(currentKey);
            currentKey = null;
            logger.debug("Encryption key cleared from memory");
        }
    }
    
    /**
     * Checks if a key file exists.
     * 
     * @return true if key file exists
     */
    public boolean hasStoredKey() {
        return Files.exists(keyFilePath);
    }
    
    /**
     * Gets the key file path.
     * 
     * @return the path to the key file
     */
    public Path getKeyFilePath() {
        return keyFilePath;
    }
    
    /**
     * Gets the key directory path.
     * 
     * @return the path to the key directory
     */
    public Path getKeyDirectory() {
        return keyDirectory;
    }
    
    /**
     * Creates the key directory if it doesn't exist.
     */
    private void createKeyDirectoryIfNeeded() {
        try {
            if (!Files.exists(keyDirectory)) {
                Files.createDirectories(keyDirectory);
                logger.debug("Created key directory: {}", keyDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create key directory", e);
        }
    }
    
    /**
     * Sets secure file permissions on the key file.
     * 
     * @param filePath the path to the file
     */
    private void setSecureFilePermissions(Path filePath) {
        try {
            // Set restrictive permissions (owner read/write only)
            Files.setPosixFilePermissions(filePath, 
                java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
                )
            );
            logger.debug("Set secure permissions on key file");
        } catch (IOException e) {
            logger.warn("Failed to set secure permissions on key file", e);
        }
    }
    
    /**
     * Validates that the current key is valid for AES encryption.
     * 
     * @return true if the key is valid
     */
    public boolean isValidKey() {
        if (currentKey == null) {
            return false;
        }
        
        try {
            // Try to create a SecretKeySpec with the current key
            SecretKeySpec keySpec = new SecretKeySpec(currentKey, ALGORITHM);
            return keySpec.getEncoded().length == KEY_LENGTH / 8; // 32 bytes for 256-bit key
        } catch (Exception e) {
            logger.warn("Key validation failed", e);
            return false;
        }
    }
    
    /**
     * Gets information about the current key.
     * 
     * @return a string describing the key status
     */
    public String getKeyInfo() {
        if (currentKey == null) {
            return "No key loaded";
        }
        
        return String.format("AES-%d key loaded (%d bytes)", KEY_LENGTH, currentKey.length);
    }
}

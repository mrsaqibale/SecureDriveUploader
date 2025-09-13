package com.yourname.securedrive.crypto;

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
 * Handles key generation, storage, and retrieval using AES-256 encryption.
 */
public class KeyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);
    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256;
    private static final String KEY_FILE = "secure_drive_key.dat";
    
    static {
        // Add Bouncy Castle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Generates a new AES-256 secret key.
     * 
     * @return A new SecretKey for AES encryption
     * @throws NoSuchAlgorithmException if AES algorithm is not available
     */
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        logger.info("Generating new AES-256 encryption key");
        
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        SecretKey key = keyGenerator.generateKey();
        
        logger.info("Successfully generated new encryption key");
        return key;
    }
    
    /**
     * Saves a secret key to a file in the user's home directory.
     * 
     * @param key The secret key to save
     * @throws IOException if file operations fail
     */
    public static void saveKey(SecretKey key) throws IOException {
        logger.info("Saving encryption key to file");
        
        Path keyPath = Paths.get(System.getProperty("user.home"), KEY_FILE);
        
        try (FileOutputStream fos = new FileOutputStream(keyPath.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            
            oos.writeObject(key);
            logger.info("Key saved successfully to: {}", keyPath);
        }
    }
    
    /**
     * Loads a secret key from a file in the user's home directory.
     * 
     * @return The loaded SecretKey, or null if no key file exists
     * @throws IOException if file operations fail
     * @throws ClassNotFoundException if the key object cannot be deserialized
     */
    public static SecretKey loadKey() throws IOException, ClassNotFoundException {
        Path keyPath = Paths.get(System.getProperty("user.home"), KEY_FILE);
        
        if (!Files.exists(keyPath)) {
            logger.info("No existing key file found");
            return null;
        }
        
        logger.info("Loading encryption key from: {}", keyPath);
        
        try (FileInputStream fis = new FileInputStream(keyPath.toFile());
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            SecretKey key = (SecretKey) ois.readObject();
            logger.info("Key loaded successfully");
            return key;
        }
    }
    
    /**
     * Gets or creates a secret key. If a key file exists, it loads the key.
     * Otherwise, it generates a new key and saves it.
     * 
     * @return A SecretKey for encryption operations
     * @throws Exception if key operations fail
     */
    public static SecretKey getOrCreateKey() throws Exception {
        SecretKey key = loadKey();
        
        if (key == null) {
            logger.info("No existing key found, generating new key");
            key = generateKey();
            saveKey(key);
        } else {
            logger.info("Using existing encryption key");
        }
        
        return key;
    }
    
    /**
     * Converts a SecretKey to a Base64 encoded string.
     * 
     * @param key The secret key to encode
     * @return Base64 encoded string representation of the key
     */
    public static String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Creates a SecretKey from a Base64 encoded string.
     * 
     * @param keyString Base64 encoded key string
     * @return SecretKey object
     */
    public static SecretKey stringToKey(String keyString) {
        byte[] keyBytes = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * Generates a random initialization vector (IV) for AES encryption.
     * 
     * @return A 16-byte random IV
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}

package com.securedrive.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Handles file encryption using AES/CBC mode with PKCS7 padding.
 * Uses Bouncy Castle provider for enhanced security features.
 */
public class FileEncryptor {
    
    private static final Logger logger = LoggerFactory.getLogger(FileEncryptor.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS7Padding";
    private static final String PROVIDER = "BC";
    private static final int IV_LENGTH = 16; // 128 bits
    private static final int BUFFER_SIZE = 8192;
    
    private final KeyManager keyManager;
    
    static {
        // Add Bouncy Castle provider
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Constructor for FileEncryptor.
     * 
     * @param keyManager The key manager instance
     */
    public FileEncryptor(KeyManager keyManager) {
        this.keyManager = keyManager;
        logger.info("FileEncryptor initialized with Bouncy Castle provider");
    }
    
    /**
     * Encrypt a file using AES/CBC encryption.
     * 
     * @param inputPath The path to the file to encrypt
     * @return The path to the encrypted file
     * @throws Exception If encryption fails
     */
    public Path encryptFile(Path inputPath) throws Exception {
        logger.info("Starting encryption of file: {}", inputPath.getFileName());
        
        // Validate input file
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("Input file does not exist: " + inputPath);
        }
        
        if (!Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException("Input path is not a regular file: " + inputPath);
        }
        
        // Generate output path
        Path outputPath = generateEncryptedFilePath(inputPath);
        
        // Get encryption key
        byte[] key = keyManager.getEncryptionKey();
        if (key == null || key.length != 32) { // AES-256 requires 32 bytes
            throw new IllegalStateException("Invalid encryption key");
        }
        
        // Generate random IV
        byte[] iv = generateRandomIV();
        
        try {
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            // Encrypt file
            try (FileInputStream fis = new FileInputStream(inputPath.toFile());
                 FileOutputStream fos = new FileOutputStream(outputPath.toFile());
                 CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                
                // Write IV to the beginning of the encrypted file
                fos.write(iv);
                
                // Encrypt and write file content
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("Successfully encrypted file: {} -> {}", 
                       inputPath.getFileName(), outputPath.getFileName());
            
            return outputPath;
            
        } catch (Exception e) {
            // Clean up partial file if encryption failed
            try {
                Files.deleteIfExists(outputPath);
            } catch (IOException cleanupException) {
                logger.warn("Failed to clean up partial encrypted file: {}", cleanupException.getMessage());
            }
            
            logger.error("Failed to encrypt file {}: {}", inputPath.getFileName(), e.getMessage());
            throw new Exception("Encryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decrypt a file using AES/CBC decryption.
     * 
     * @param inputPath The path to the encrypted file
     * @param outputPath The path where the decrypted file should be saved
     * @throws Exception If decryption fails
     */
    public void decryptFile(Path inputPath, Path outputPath) throws Exception {
        logger.info("Starting decryption of file: {}", inputPath.getFileName());
        
        // Validate input file
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("Encrypted file does not exist: " + inputPath);
        }
        
        if (!Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException("Input path is not a regular file: " + inputPath);
        }
        
        // Get decryption key
        byte[] key = keyManager.getEncryptionKey();
        if (key == null || key.length != 32) { // AES-256 requires 32 bytes
            throw new IllegalStateException("Invalid decryption key");
        }
        
        try {
            // Read IV from the beginning of the encrypted file
            byte[] iv = new byte[IV_LENGTH];
            try (FileInputStream fis = new FileInputStream(inputPath.toFile())) {
                int ivBytesRead = fis.read(iv);
                if (ivBytesRead != IV_LENGTH) {
                    throw new IOException("Invalid encrypted file format: IV not found");
                }
            }
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            // Decrypt file
            try (FileInputStream fis = new FileInputStream(inputPath.toFile());
                 FileOutputStream fos = new FileOutputStream(outputPath.toFile());
                 CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                
                // Skip IV bytes
                fis.skip(IV_LENGTH);
                
                // Decrypt and write file content
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("Successfully decrypted file: {} -> {}", 
                       inputPath.getFileName(), outputPath.getFileName());
            
        } catch (Exception e) {
            // Clean up partial file if decryption failed
            try {
                Files.deleteIfExists(outputPath);
            } catch (IOException cleanupException) {
                logger.warn("Failed to clean up partial decrypted file: {}", cleanupException.getMessage());
            }
            
            logger.error("Failed to decrypt file {}: {}", inputPath.getFileName(), e.getMessage());
            throw new Exception("Decryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a random initialization vector (IV).
     * 
     * @return Random IV bytes
     */
    private byte[] generateRandomIV() {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        return iv;
    }
    
    /**
     * Generate the output path for an encrypted file.
     * 
     * @param inputPath The input file path
     * @return The output path for the encrypted file
     */
    private Path generateEncryptedFilePath(Path inputPath) {
        String fileName = inputPath.getFileName().toString();
        String encryptedFileName = fileName + ".encrypted";
        return inputPath.getParent().resolve(encryptedFileName);
    }
    
    /**
     * Verify that a file is properly encrypted by checking its format.
     * 
     * @param filePath The path to the file to verify
     * @return True if the file appears to be encrypted, false otherwise
     */
    public boolean isEncryptedFile(Path filePath) {
        try {
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return false;
            }
            
            // Check if file is large enough to contain IV
            if (Files.size(filePath) < IV_LENGTH) {
                return false;
            }
            
            // Check file extension
            String fileName = filePath.getFileName().toString();
            return fileName.endsWith(".encrypted");
            
        } catch (IOException e) {
            logger.warn("Error checking if file is encrypted: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the size of the encrypted file (including IV overhead).
     * 
     * @param originalSize The size of the original file
     * @return The estimated size of the encrypted file
     */
    public long getEncryptedFileSize(long originalSize) {
        // Add IV length and padding overhead
        return originalSize + IV_LENGTH + (16 - (originalSize % 16));
    }
}

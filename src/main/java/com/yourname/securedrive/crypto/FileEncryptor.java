package com.yourname.securedrive.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;

/**
 * Handles file encryption and decryption using AES/CBC mode.
 * Provides secure file operations for the SecureDriveUploader application.
 */
public class FileEncryptor {
    
    private static final Logger logger = LoggerFactory.getLogger(FileEncryptor.class);
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";
    
    static {
        // Add Bouncy Castle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Encrypts a file using AES/CBC encryption.
     * 
     * @param inputFile The file to encrypt
     * @param outputFile The encrypted output file
     * @param key The encryption key
     * @throws Exception if encryption fails
     */
    public static void encryptFile(Path inputFile, Path outputFile, SecretKey key) throws Exception {
        logger.info("Encrypting file: {} to {}", inputFile, outputFile);
        
        // Generate random IV
        byte[] iv = KeyManager.generateIV();
        
        // Initialize cipher for encryption
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        
        try (FileInputStream fis = new FileInputStream(inputFile.toFile());
             FileOutputStream fos = new FileOutputStream(outputFile.toFile());
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
            
            // Write IV to the beginning of the encrypted file
            fos.write(iv);
            
            // Encrypt and write the file content
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            
            logger.info("File encrypted successfully");
        }
    }
    
    /**
     * Decrypts a file that was encrypted using AES/CBC encryption.
     * 
     * @param inputFile The encrypted file to decrypt
     * @param outputFile The decrypted output file
     * @param key The decryption key
     * @throws Exception if decryption fails
     */
    public static void decryptFile(Path inputFile, Path outputFile, SecretKey key) throws Exception {
        logger.info("Decrypting file: {} to {}", inputFile, outputFile);
        
        try (FileInputStream fis = new FileInputStream(inputFile.toFile());
             FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            
            // Read IV from the beginning of the encrypted file
            byte[] iv = new byte[16];
            int ivBytesRead = fis.read(iv);
            if (ivBytesRead != 16) {
                throw new IOException("Invalid encrypted file format: IV not found");
            }
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            
            try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                // Decrypt and write the file content
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("File decrypted successfully");
        }
    }
    
    /**
     * Encrypts a file in place (overwrites the original file).
     * 
     * @param filePath The file to encrypt
     * @param key The encryption key
     * @throws Exception if encryption fails
     */
    public static void encryptFileInPlace(Path filePath, SecretKey key) throws Exception {
        logger.info("Encrypting file in place: {}", filePath);
        
        // Create temporary file for encryption
        Path tempFile = Paths.get(filePath.toString() + ".tmp");
        
        try {
            encryptFile(filePath, tempFile, key);
            
            // Replace original file with encrypted version
            Files.delete(filePath);
            Files.move(tempFile, filePath);
            
            logger.info("File encrypted in place successfully");
        } catch (Exception e) {
            // Clean up temporary file if encryption fails
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
            throw e;
        }
    }
    
    /**
     * Decrypts a file in place (overwrites the original file).
     * 
     * @param filePath The encrypted file to decrypt
     * @param key The decryption key
     * @throws Exception if decryption fails
     */
    public static void decryptFileInPlace(Path filePath, SecretKey key) throws Exception {
        logger.info("Decrypting file in place: {}", filePath);
        
        // Create temporary file for decryption
        Path tempFile = Paths.get(filePath.toString() + ".tmp");
        
        try {
            decryptFile(filePath, tempFile, key);
            
            // Replace original file with decrypted version
            Files.delete(filePath);
            Files.move(tempFile, filePath);
            
            logger.info("File decrypted in place successfully");
        } catch (Exception e) {
            // Clean up temporary file if decryption fails
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
            throw e;
        }
    }
    
    /**
     * Checks if a file appears to be encrypted by this application.
     * This is done by checking if the file starts with a 16-byte IV.
     * 
     * @param filePath The file to check
     * @return true if the file appears to be encrypted
     */
    public static boolean isEncryptedFile(Path filePath) {
        try {
            if (!Files.exists(filePath) || Files.size(filePath) < 16) {
                return false;
            }
            
            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                byte[] iv = new byte[16];
                int bytesRead = fis.read(iv);
                return bytesRead == 16;
            }
        } catch (IOException e) {
            logger.warn("Error checking if file is encrypted: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the size of the original file before encryption.
     * This is an approximation based on the encrypted file size minus IV and padding.
     * 
     * @param encryptedFilePath The encrypted file path
     * @return Approximate original file size
     */
    public static long getOriginalFileSize(Path encryptedFilePath) {
        try {
            long encryptedSize = Files.size(encryptedFilePath);
            // Subtract IV size (16 bytes) and estimate padding overhead
            return Math.max(0, encryptedSize - 16 - 16); // Conservative estimate
        } catch (IOException e) {
            logger.warn("Error getting original file size: {}", e.getMessage());
            return 0;
        }
    }
}

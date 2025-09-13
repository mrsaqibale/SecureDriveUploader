package com.securedrive.securedrive.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Handles AES/CBC encryption and decryption of files.
 * Uses Bouncy Castle provider for enhanced cryptographic operations.
 */
public class FileEncryptor {
    
    private static final Logger logger = LoggerFactory.getLogger(FileEncryptor.class);
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16; // 128 bits
    private static final int BUFFER_SIZE = 8192;
    
    private final KeyManager keyManager;
    
    static {
        // Add Bouncy Castle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            logger.debug("Bouncy Castle provider added");
        }
    }
    
    /**
     * Constructor for FileEncryptor.
     * 
     * @param keyManager the key manager to use for encryption keys
     */
    public FileEncryptor(KeyManager keyManager) {
        this.keyManager = keyManager;
        logger.debug("FileEncryptor initialized");
    }
    
    /**
     * Encrypts a file using AES/CBC encryption.
     * 
     * @param inputFile the file to encrypt
     * @return the encrypted file
     * @throws IOException if file operations fail
     * @throws Exception if encryption fails
     */
    public File encryptFile(File inputFile) throws IOException, Exception {
        if (inputFile == null || !inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist or is null");
        }
        
        logger.info("Starting encryption of file: {}", inputFile.getName());
        
        // Generate a random IV
        byte[] iv = generateIV();
        
        // Get the encryption key
        byte[] key = keyManager.getKey();
        if (key == null) {
            throw new IllegalStateException("No encryption key available");
        }
        
        // Create output file
        File outputFile = createEncryptedFileName(inputFile);
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             CipherOutputStream cos = new CipherOutputStream(fos, createEncryptCipher(key, iv))) {
            
            // Write IV to the beginning of the encrypted file
            fos.write(iv);
            
            // Encrypt and write the file content
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;
            long fileSize = inputFile.length();
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // Log progress for large files
                if (fileSize > 1024 * 1024) { // 1MB
                    int progress = (int) ((totalBytes * 100) / fileSize);
                    if (progress % 10 == 0) {
                        logger.debug("Encryption progress: {}%", progress);
                    }
                }
            }
            
            cos.flush();
        }
        
        logger.info("File encrypted successfully: {} -> {}", inputFile.getName(), outputFile.getName());
        return outputFile;
    }
    
    /**
     * Decrypts a file using AES/CBC decryption.
     * 
     * @param encryptedFile the encrypted file to decrypt
     * @param outputFile the file to write the decrypted content to
     * @throws IOException if file operations fail
     * @throws Exception if decryption fails
     */
    public void decryptFile(File encryptedFile, File outputFile) throws IOException, Exception {
        if (encryptedFile == null || !encryptedFile.exists()) {
            throw new IllegalArgumentException("Encrypted file does not exist or is null");
        }
        
        logger.info("Starting decryption of file: {}", encryptedFile.getName());
        
        // Get the decryption key
        byte[] key = keyManager.getKey();
        if (key == null) {
            throw new IllegalStateException("No decryption key available");
        }
        
        try (FileInputStream fis = new FileInputStream(encryptedFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // Read IV from the beginning of the encrypted file
            byte[] iv = new byte[IV_LENGTH];
            int ivBytesRead = fis.read(iv);
            if (ivBytesRead != IV_LENGTH) {
                throw new IOException("Invalid encrypted file format - IV not found");
            }
            
            // Create cipher for decryption
            Cipher cipher = createDecryptCipher(key, iv);
            
            try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytes = 0;
                long fileSize = encryptedFile.length() - IV_LENGTH;
                
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    
                    // Log progress for large files
                    if (fileSize > 1024 * 1024) { // 1MB
                        int progress = (int) ((totalBytes * 100) / fileSize);
                        if (progress % 10 == 0) {
                            logger.debug("Decryption progress: {}%", progress);
                        }
                    }
                }
                
                fos.flush();
            }
        }
        
        logger.info("File decrypted successfully: {} -> {}", encryptedFile.getName(), outputFile.getName());
    }
    
    /**
     * Creates an encrypted file name by adding .enc extension.
     * 
     * @param originalFile the original file
     * @return the encrypted file
     */
    private File createEncryptedFileName(File originalFile) {
        String originalName = originalFile.getName();
        String encryptedName;
        
        if (originalName.endsWith(".enc")) {
            // If already has .enc extension, add timestamp
            String baseName = originalName.substring(0, originalName.length() - 4);
            encryptedName = baseName + "_" + System.currentTimeMillis() + ".enc";
        } else {
            encryptedName = originalName + ".enc";
        }
        
        return new File(originalFile.getParent(), encryptedName);
    }
    
    /**
     * Generates a random initialization vector (IV).
     * 
     * @return the generated IV
     */
    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        return iv;
    }
    
    /**
     * Creates a cipher for encryption.
     * 
     * @param key the encryption key
     * @param iv the initialization vector
     * @return the encryption cipher
     * @throws Exception if cipher creation fails
     */
    private Cipher createEncryptCipher(byte[] key, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        
        return cipher;
    }
    
    /**
     * Creates a cipher for decryption.
     * 
     * @param key the decryption key
     * @param iv the initialization vector
     * @return the decryption cipher
     * @throws Exception if cipher creation fails
     */
    private Cipher createDecryptCipher(byte[] key, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        
        return cipher;
    }
    
    /**
     * Validates that a file appears to be encrypted by this encryptor.
     * 
     * @param file the file to validate
     * @return true if the file appears to be encrypted
     */
    public boolean isEncryptedFile(File file) {
        if (file == null || !file.exists() || file.length() < IV_LENGTH) {
            return false;
        }
        
        // Check if file has .enc extension and minimum size
        return file.getName().endsWith(".enc") && file.length() > IV_LENGTH;
    }
    
    /**
     * Gets the size of the IV used in encryption.
     * 
     * @return the IV length in bytes
     */
    public int getIVLength() {
        return IV_LENGTH;
    }
}

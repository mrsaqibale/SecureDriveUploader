# SecureDriveUploader

A secure file uploader application that encrypts files using AES-256 encryption before uploading them to Google Drive. Built with JavaFX for a modern user interface and Maven for dependency management.

## Features

- **File Encryption**: AES-256/CBC encryption with PKCS7 padding using Bouncy Castle
- **Google Drive Integration**: Secure upload to Google Drive with OAuth2 authentication
- **Modern GUI**: JavaFX-based user interface with file selection and progress tracking
- **Configuration Management**: Persistent user preferences and settings
- **Secure Key Management**: Automatic key generation and secure storage
- **Multi-file Support**: Batch processing of multiple files
- **Progress Tracking**: Real-time progress updates for encryption and upload operations

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- Google Cloud Platform account with Drive API enabled
- Google OAuth2 credentials

## Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/SecureDriveUploader.git
cd SecureDriveUploader
```

2. Build the project:
```bash
mvn clean compile
```

3. Set up Google Drive API credentials:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select an existing one
   - Enable the Google Drive API
   - Create OAuth2 credentials (Desktop application)
   - Download the credentials JSON file
   - Place it in `src/main/resources/credentials.json`

## Usage

### Running the Application

```bash
mvn javafx:run
```

Or build and run the JAR:
```bash
mvn clean package
java -jar target/SecureDriveUploader-1.0.0.jar
```

### First Time Setup

1. Launch the application
2. Select files using the "Select Files" button
3. Choose whether to encrypt files and/or upload to Google Drive
4. Click "Encrypt Files" to encrypt selected files (if enabled)
5. Click "Upload Files" to upload files to Google Drive (if enabled)

### Configuration

The application stores configuration in `~/.securedrive/app.properties`. You can modify settings such as:
- Default encryption/upload preferences
- Window size and position
- Google Drive folder settings
- Logging levels

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── securedrive/
│   │           ├── Main.java                 # Application entry point
│   │           ├── ui/
│   │           │   ├── MainWindow.java       # Main GUI controller
│   │           │   └── FileItem.java         # File model class
│   │           ├── crypto/
│   │           │   ├── FileEncryptor.java    # AES encryption implementation
│   │           │   └── KeyManager.java       # Key generation and management
│   │           ├── drive/
│   │           │   ├── DriveUploader.java    # Google Drive upload logic
│   │           │   └── DriveService.java     # Google Drive API service
│   │           └── util/
│   │               ├── FileChooserUtil.java  # File selection dialogs
│   │               └── ConfigManager.java    # Configuration management
│   └── resources/
│       ├── ui/
│       │   └── main.fxml                     # GUI layout
│       └── app.properties                    # Default configuration
└── test/
    └── java/
        └── com/
            └── securedrive/                  # Test classes
```

## Security Features

- **AES-256 Encryption**: Industry-standard encryption algorithm
- **Secure Key Storage**: Keys stored with restricted file permissions
- **OAuth2 Authentication**: Secure Google Drive API authentication
- **No Plain Text Storage**: Encrypted files are never stored in plain text
- **Secure Random IV**: Each file encrypted with a unique initialization vector

## Dependencies

- **JavaFX**: Modern GUI framework
- **Google Drive API**: Google Drive integration
- **Bouncy Castle**: Cryptographic library for AES encryption
- **SLF4J + Logback**: Logging framework
- **JUnit 5**: Testing framework

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/SecureDriveUploader.git
cd SecureDriveUploader

# Compile the project
mvn clean compile

# Run tests
mvn test

# Package the application
mvn clean package

# Run the application
mvn javafx:run
```

## Configuration

### Google Drive Setup

1. Create a Google Cloud Platform project
2. Enable the Google Drive API
3. Create OAuth2 credentials for a desktop application
4. Download the credentials JSON file
5. Place it in `src/main/resources/credentials.json`

### Application Settings

The application configuration is stored in `~/.securedrive/app.properties`:

```properties
# Encryption settings
encryption.enabled=true
encryption.algorithm=AES
encryption.mode=CBC
key.length=256

# Upload settings
upload.enabled=true
drive.folder.name=SecureDriveUploader

# UI settings
window.width=800
window.height=600
show.notifications=true
```

## Troubleshooting

### Common Issues

1. **Google Drive Authentication Failed**
   - Ensure credentials.json is in the correct location
   - Check that the Google Drive API is enabled
   - Verify OAuth2 credentials are for a desktop application

2. **Encryption Errors**
   - Check file permissions
   - Ensure sufficient disk space
   - Verify Bouncy Castle provider is loaded

3. **Upload Failures**
   - Check internet connection
   - Verify Google Drive storage quota
   - Check file size limits

### Logs

Application logs are stored in the user's home directory under `.securedrive/logs/`. Check these files for detailed error information.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Google Drive API](https://developers.google.com/drive) for file storage
- [Bouncy Castle](https://www.bouncycastle.org/) for cryptographic functions
- [JavaFX](https://openjfx.io/) for the user interface
- [Maven](https://maven.apache.org/) for project management

## Support

For support, please open an issue on GitHub or contact the maintainers.

## Changelog

### Version 1.0.0
- Initial release
- AES-256 file encryption
- Google Drive integration
- JavaFX GUI
- Configuration management
- Multi-file support
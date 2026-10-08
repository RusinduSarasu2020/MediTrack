package com.meditrack.service;

import com.meditrack.exception.BusinessException;
import com.meditrack.exception.ResourceNotFoundException;
import com.meditrack.model.StoredFile;
import com.meditrack.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;
    private final StoredFileRepository repository;
    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "application/pdf");

    public FileStorageService(@Value("${app.upload-dir:C:/MediTrackData/uploads}") String uploadDir, StoredFileRepository repository) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.repository = repository;
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.uploadRoot, e);
        }
    }

    @Transactional
    public StoredFile store(MultipartFile file, Long ownerId, String purpose) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Cannot upload empty file.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("File size exceeds 5MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Only JPEG, PNG, and PDF files are allowed.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("The uploaded file has no name.");
        }
        originalName = originalName.trim();
        if (originalName.length() > 200 || originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")
                || originalName.chars().anyMatch(c -> c < 0x20 || c == 0x7f || "<>:\"|?*".indexOf(c) >= 0)) {
            throw new BusinessException("Invalid filename characters.");
        }

        // The extension and the actual file signature must both agree with the declared type,
        // so a renamed executable or HTML file cannot be stored as a "PDF" or "image".
        String lowerName = originalName.toLowerCase();
        String declaredType = contentType.toLowerCase();
        boolean extensionMatches = switch (declaredType) {
            case "image/jpeg" -> lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
            case "image/png" -> lowerName.endsWith(".png");
            case "application/pdf" -> lowerName.endsWith(".pdf");
            default -> false;
        };
        if (!extensionMatches) {
            throw new BusinessException("File extension must be .jpg, .jpeg, .png or .pdf and match the file type.");
        }
        if (!hasExpectedSignature(file, declaredType)) {
            throw new BusinessException("The file content does not look like a valid " + (declaredType.equals("application/pdf") ? "PDF" : "image") + ".");
        }

        String extension = switch (declaredType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".pdf";
        };

        String generatedName = UUID.randomUUID().toString() + extension;
        Path targetPath = this.uploadRoot.resolve(generatedName).normalize();

        if (!targetPath.startsWith(this.uploadRoot)) {
            throw new BusinessException("Security check failed: Path traversal detected.");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Failed to save uploaded file.", e);
        }

        deleteFileIfTransactionRollsBack(targetPath);

        StoredFile storedFile = new StoredFile(generatedName, originalName, contentType, file.getSize(), ownerId, purpose);
        try {
            return repository.save(storedFile);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {}
            throw new BusinessException("Failed to persist file metadata.", e);
        }
    }

    private static boolean hasExpectedSignature(MultipartFile file, String contentType) {
        byte[] head = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "image/png" -> read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                    && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A;
            case "application/pdf" -> read >= 5 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F' && head[4] == '-';
            default -> false;
        };
    }

    /** Removes the stored file from disk if the surrounding database transaction is rolled back. */
    private static void deleteFileIfTransactionRollsBack(Path targetPath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        Files.deleteIfExists(targetPath);
                    } catch (IOException ignored) {}
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public StoredFile getMetadata(Long fileId) {
        return repository.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    public Resource loadAsResource(Long fileId) {
        StoredFile metadata = getMetadata(fileId);
        try {
            Path filePath = this.uploadRoot.resolve(metadata.getGeneratedName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not accessible on disk: " + metadata.getGeneratedName());
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Could not read file: " + fileId);
        }
    }
}

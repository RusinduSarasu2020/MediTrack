package com.meditrack.controller;

import com.meditrack.enums.Role;
import com.meditrack.exception.BusinessException;
import com.meditrack.model.StoredFile;
import com.meditrack.model.User;
import com.meditrack.service.FileStorageService;
import com.meditrack.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FileController {

    private final FileStorageService fileStorageService;
    private final UserService userService;

    public FileController(FileStorageService fileStorageService, UserService userService) {
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long id, Authentication authentication) {
        StoredFile metadata = fileStorageService.getMetadata(id);
        User currentUser = userService.findByUsername(authentication.getName());

        // Authorization check: file owner or staff member (ADMIN, PHARMACIST, SUPPLIER_COORDINATOR)
        boolean isOwner = metadata.getOwnerId() != null && metadata.getOwnerId().equals(currentUser.getId());
        boolean isStaff = currentUser.getRole() == Role.ADMIN ||
                          currentUser.getRole() == Role.PHARMACIST ||
                          currentUser.getRole() == Role.SUPPLIER_COORDINATOR;

        if (!isOwner && !isStaff) {
            throw new BusinessException("Access denied to private file.");
        }

        Resource resource = fileStorageService.loadAsResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(metadata.getMediaType()))
                .body(resource);
    }
}

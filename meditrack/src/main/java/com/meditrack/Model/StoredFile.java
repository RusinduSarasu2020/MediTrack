package com.meditrack.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stored_files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String generatedName;

    @Column(nullable = false, length = 200)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String mediaType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 50)
    private String purpose;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StoredFile() {}

    public StoredFile(String generatedName, String originalFilename, String mediaType, long size, Long ownerId, String purpose) {
        this.generatedName = generatedName;
        this.originalFilename = originalFilename;
        this.mediaType = mediaType;
        this.size = size;
        this.ownerId = ownerId;
        this.purpose = purpose;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGeneratedName() { return generatedName; }
    public void setGeneratedName(String generatedName) { this.generatedName = generatedName; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

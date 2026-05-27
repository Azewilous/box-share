package org.azelabs.boxshare.dtos;

import org.azelabs.boxshare.application.enums.FileVisibility;
import org.azelabs.boxshare.application.enums.UploadStatus;

import java.util.UUID;

public record FileRecord(Long id, String name, Long size, String mimeType, String uploadedBy, UploadStatus status, FileVisibility visibility, UUID shareToken, String presignedUrl) {}
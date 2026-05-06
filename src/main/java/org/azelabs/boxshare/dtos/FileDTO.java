package org.azelabs.boxshare.dtos;

import org.azelabs.boxshare.application.enums.UploadStatus;

public record FileDTO(Long id, String name, Long size, String mimeType, String uploadedBy, UploadStatus status, String presignedUrl) {}

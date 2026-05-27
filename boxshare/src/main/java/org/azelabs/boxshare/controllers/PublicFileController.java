package org.azelabs.boxshare.controllers;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/file")
@RequiredArgsConstructor
public class PublicFileController {

    private final IFileService fileService;

    @GetMapping("/{shareToken}")
    public ResponseEntity<FileRecord> getSharedFile(@PathVariable UUID shareToken) {
        return fileService.getByShareToken(shareToken)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

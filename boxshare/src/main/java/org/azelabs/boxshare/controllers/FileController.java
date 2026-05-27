package org.azelabs.boxshare.controllers;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.CommandInvoker;
import org.azelabs.boxshare.application.enums.CommandType;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.VisibilityRequest;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final CommandInvoker invoker;
    private final IFileService fileService;

    @GetMapping("/{filename}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FileRecord> generateGetUrl(@PathVariable String filename) {
        FileRecord response = this.invoker.execute(CommandType.GET_FILE.getName(), filename);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FileRecord> generatePostUrl(@RequestBody FileRecord body) {
        FileRecord response = this.invoker.execute(CommandType.CREATE_FILE.getName(), body);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        Boolean deleted = this.invoker.execute(CommandType.DELETE_FILE.getName(), id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FileRecord> updateVisibility(@PathVariable Long id, @Validated @RequestBody VisibilityRequest request) {
        return ResponseEntity.ok(fileService.updateVisibility(id, request.visibility()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

}

package org.azelabs.boxshare.controllers;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.CommandInvoker;
import java.util.List;
import org.azelabs.boxshare.application.enums.CommandType;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.ShareFileRequest;
import org.azelabs.boxshare.dtos.VisibilityRequest;
import org.azelabs.boxshare.models.HybridUser;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final CommandInvoker invoker;
    private final IFileService fileService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<FileRecord>> listFiles(@AuthenticationPrincipal HybridUser principal) {
        List<FileRecord> files = invoker.execute(CommandType.LIST_MY_FILES.getName(), principal.getUsername());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{filename}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FileRecord> generateGetUrl(@PathVariable String filename) {
        FileRecord response = this.invoker.execute(CommandType.GET_FILE.getName(), filename);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FileRecord> generatePostUrl(@RequestBody FileRecord body, @AuthenticationPrincipal HybridUser principal) {
        FileRecord withOwner = new FileRecord(body.id(), body.name(), body.size(), body.mimeType(),
                principal.getUser(), body.status(), body.visibility(), body.shareToken(), body.presignedUrl(), false);
        FileRecord response = this.invoker.execute(CommandType.CREATE_FILE.getName(), withOwner);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @fileService.isOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        Boolean deleted = this.invoker.execute(CommandType.DELETE_FILE.getName(), id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<FileRecord> updateVisibility(@PathVariable Long id, @Validated @RequestBody VisibilityRequest request) {
        FileRecord record = this.invoker.execute(CommandType.TOGGLE_FILE_VISIBILITY.getName(), new VisibilityRequest(id, request.visibility()));
        return ResponseEntity.ok(record);
    }

    @PostMapping("/{fileId}/share")
    @PreAuthorize("hasRole('ADMIN') or @fileService.isOwner(#fileId, authentication.name)")
    public ResponseEntity<Void> shareFile(@PathVariable Long fileId, @RequestParam String email) {
        this.invoker.execute(CommandType.SHARE_FILE.getName(), new ShareFileRequest(fileId, email));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

}

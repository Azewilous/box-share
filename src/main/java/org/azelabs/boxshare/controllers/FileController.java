package org.azelabs.boxshare.controllers;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.CommandInvoker;
import org.azelabs.boxshare.application.enums.CommandType;
import org.azelabs.boxshare.dtos.FileDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final CommandInvoker invoker;

    @GetMapping("/{filename}")
    public ResponseEntity<FileDTO> generateGetUrl(@PathVariable String filename) {
        FileDTO response = this.invoker.execute(CommandType.GET_FILE.getName(), filename);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<FileDTO> generatePostUrl(@RequestBody FileDTO body) {
        FileDTO response = this.invoker.execute(CommandType.CREATE_FILE.getName(), body);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        Boolean deleted = this.invoker.execute(CommandType.DELETE_FILE.getName(), id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

}

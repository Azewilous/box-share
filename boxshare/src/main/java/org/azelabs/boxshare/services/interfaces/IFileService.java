package org.azelabs.boxshare.services.interfaces;

import org.azelabs.boxshare.application.enums.FileVisibility;
import org.azelabs.boxshare.dtos.FileRecord;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFileService {

    List<FileRecord> all();
    Optional<FileRecord> getById(Long id);
    Optional<FileRecord> getByName(String name);
    FileRecord save(FileRecord file);
    void update(FileRecord file);
    boolean delete(Long id);
    FileRecord generatePreSignedUrl(String fileName, SdkHttpMethod httpMethod);
    FileRecord updateVisibility(Long id, FileVisibility visibility);
    Optional<FileRecord> getByShareToken(UUID shareToken);

}

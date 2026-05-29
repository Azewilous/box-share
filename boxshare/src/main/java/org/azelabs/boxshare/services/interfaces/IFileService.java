package org.azelabs.boxshare.services.interfaces;

import org.azelabs.boxshare.application.enums.FileVisibility;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.ShareFileRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFileService {

    List<FileRecord> all();
    List<FileRecord> allByOwner(String identity);
    List<FileRecord> allForUser(String identity);
    Optional<FileRecord> getById(Long id);
    Optional<FileRecord> getByName(String name);
    FileRecord save(FileRecord file);
    void update(FileRecord file);
    boolean delete(Long id);
    FileRecord generatePreSignedUrl(String fileName, SdkHttpMethod httpMethod);
    FileRecord updateVisibility(Long id, FileVisibility visibility);
    Optional<FileRecord> getByShareToken(UUID shareToken);
    boolean isOwner(Long fileId, String identity);
    void shareFile(ShareFileRequest request);

}

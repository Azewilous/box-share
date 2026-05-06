package org.azelabs.boxshare.services.interfaces;

import org.azelabs.boxshare.dtos.FileDTO;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.util.List;
import java.util.Optional;

public interface IFileService {

    List<FileDTO> all();
    Optional<FileDTO> getById(Long id);
    Optional<FileDTO> getByName(String name);
    FileDTO save(FileDTO file);
    void update(FileDTO file);
    boolean delete(Long id);
    FileDTO generatePreSignedUrl(String fileName, SdkHttpMethod httpMethod);

}

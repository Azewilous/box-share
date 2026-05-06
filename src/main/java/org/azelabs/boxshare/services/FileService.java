package org.azelabs.boxshare.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.enums.UploadStatus;
import org.azelabs.boxshare.dtos.FileDTO;
import org.azelabs.boxshare.models.FileModel;
import org.azelabs.boxshare.repositories.IFileRepository;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final IFileRepository repository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Presigner s3Presigner;

    @Override
    public List<FileDTO> all() {
        return repository.findAll().stream().map(f -> toDTO(f, null)).collect(Collectors.toList());
    }

    @Override
    public Optional<FileDTO> getById(Long id) {
        return repository.findById(id).map(f -> toDTO(f, null));
    }

    @Override
    public Optional<FileDTO> getByName(String name) {
        return repository.findByName(name).map(f -> toDTO(f, null));
    }

    @Override
    public FileDTO save(FileDTO file) {
        FileModel model = toEntity(file);
        FileModel saved = repository.saveAndFlush(model);
        return toDTO(saved, null);
    }

    @Override
    public void update(FileDTO file) {
        FileModel model = repository.findById(file.id()).orElseThrow();
        model.setName(file.name());
        model.setSize(file.size());
        model.setMimeType(file.mimeType());
        model.setUploadStatus(file.status());
        model.setUploadedBy(file.uploadedBy());
        model.setUpdated_at(ZonedDateTime.now());
        //toDTO(model, null);
        repository.save(model);
    }

    @Override
    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Override
    public FileDTO generatePreSignedUrl(String fileName, SdkHttpMethod httpMethod) {
        UploadStatus status = null;
        String generatedUrl = switch (httpMethod) {
            case SdkHttpMethod.GET -> generateGetPresignedUrl(fileName);
            case SdkHttpMethod.PUT -> {
                status = UploadStatus.NOT_STARTED;
                yield generatePutPresignedUrl(fileName);
            }
            default -> throw new UnsupportedOperationException("Unsupported HTTP method: " + httpMethod);
        };

        FileModel file = repository.findByName(fileName).orElseThrow(
                () -> new EntityNotFoundException("Could not find file with name " + fileName));

        if (httpMethod == SdkHttpMethod.GET) {
            return toDTO(file, generatedUrl);
        }

        file.setUploadStatus(status);
        FileModel saved = repository.save(file);
        return toDTO(saved, generatedUrl);
    }

    private String generateGetPresignedUrl(String fileName) {
        log.info("Generating get presigned url for file: [{}]", fileName);
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))  // The URL will expire in 10 minutes.
                .getObjectRequest(objectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        log.info("Presigned URL: [{}]", presignedRequest.url().toString());

        return presignedRequest.url().toExternalForm();
    }

    private String generatePutPresignedUrl(String fileName) {
            log.info("Generating put presigned url for file: [{}]", fileName);
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))  // The URL expires in 60 minutes.
                    .putObjectRequest(objectRequest)
                    .build();


            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String myURL = presignedRequest.url().toString();
            log.info("Presigned URL to upload a file to: [{}]", myURL);

            return presignedRequest.url().toExternalForm();
    }

    private FileDTO toDTO(FileModel file, String presignedUrl) {
        return new FileDTO(file.getId(), file.getName(), file.getSize(), file.getMimeType(), file.getUploadedBy(), file.getUploadStatus(), presignedUrl);
    }

    private FileModel toEntity(FileDTO file) {
        FileModel model = new FileModel();
        model.setName(file.name());
        model.setSize(file.size());
        model.setMimeType(file.mimeType());
        model.setUploadedBy(file.uploadedBy());
        model.setUploadStatus(file.status());
        return model;
    }
}

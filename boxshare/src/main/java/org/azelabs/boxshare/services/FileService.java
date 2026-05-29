package org.azelabs.boxshare.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.enums.FileVisibility;
import org.azelabs.boxshare.application.enums.UploadStatus;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.ShareFileRequest;
import org.azelabs.boxshare.models.FileModel;
import org.azelabs.boxshare.models.SharedFileModel;
import org.azelabs.boxshare.models.UserModel;
import org.azelabs.boxshare.repositories.IFileRepository;
import org.azelabs.boxshare.repositories.ISharedFileRepository;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final IFileRepository repository;
    private final ISharedFileRepository sharedFileRepository;
    private final IUserRepository userRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Presigner s3Presigner;

    @Override
    public List<FileRecord> all() {
        return repository.findAll().stream().map(f -> toDTO(f, null)).collect(Collectors.toList());
    }

    @Override
    public List<FileRecord> allByOwner(String identity) {
        return repository.findAllByOwner_Identity(UUID.fromString(identity)).stream()
                .map(f -> toDTO(f, null, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<FileRecord> allForUser(String identity) {
        UUID uuid = UUID.fromString(identity);
        List<FileRecord> owned = repository.findAllByOwner_Identity(uuid).stream()
                .map(f -> toDTO(f, null, false))
                .collect(Collectors.toList());
        List<FileRecord> shared = sharedFileRepository.findByUser_Identity(uuid).stream()
                .map(sf -> toDTO(sf.getFile(), null, true))
                .collect(Collectors.toList());
        owned.addAll(shared);
        return owned;
    }

    @Override
    public Optional<FileRecord> getById(Long id) {
        return repository.findById(id).map(f -> toDTO(f, null));
    }

    @Override
    public Optional<FileRecord> getByName(String name) {
        return repository.findByName(name).map(f -> toDTO(f, null));
    }

    @Override
    public FileRecord save(FileRecord file) {
        FileModel model = toEntity(file);
        FileModel saved = repository.saveAndFlush(model);
        return toDTO(saved, null);
    }

    @Override
    public void update(FileRecord file) {
        FileModel model = repository.findById(file.id()).orElseThrow();
        model.setName(file.name());
        model.setSize(file.size());
        model.setMimeType(file.mimeType());
        model.setUploadStatus(file.status());
        model.setUpdated_at(ZonedDateTime.now());
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
    public FileRecord generatePreSignedUrl(String fileName, SdkHttpMethod httpMethod) {
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

    @Override
    @Transactional
    public void shareFile(ShareFileRequest request) {
        FileModel file = repository.findById(request.fileId())
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + request.fileId()));
        UserModel user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.email()));

        if (sharedFileRepository.existsByFileAndUser(file, user)) {
            return;
        }

        SharedFileModel share = new SharedFileModel();
        share.setFile(file);
        share.setUser(user);
        sharedFileRepository.save(share);
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

    @Override
    public boolean isOwner(Long fileId, String identity) {
        return repository.findById(fileId)
                .map(f -> UUID.fromString(identity).equals(f.getOwner().getIdentity()))
                .orElse(false);
    }

    @Override
    public FileRecord updateVisibility(Long id, FileVisibility visibility) {
        FileModel file = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Could not find file with id " + id));
        file.setVisibility(visibility);
        file.setShareToken(visibility == FileVisibility.PUBLIC ? UUID.randomUUID() : null);
        file.setUpdated_at(ZonedDateTime.now());
        return toDTO(repository.save(file), null);
    }

    @Override
    public Optional<FileRecord> getByShareToken(UUID shareToken) {
        return repository.findByShareToken(shareToken)
                .filter(f -> f.getVisibility() == FileVisibility.PUBLIC)
                .map(f -> toDTO(f, generateGetPresignedUrl(f.getName())));
    }

    private FileRecord toDTO(FileModel file, String presignedUrl) {
        return toDTO(file, presignedUrl, false);
    }

    private FileRecord toDTO(FileModel file, String presignedUrl, boolean shared) {
        return new FileRecord(file.getId(), file.getName(), file.getSize(), file.getMimeType(),
                file.getOwner(), file.getUploadStatus(), file.getVisibility(), file.getShareToken(), presignedUrl, shared);
    }

    private FileModel toEntity(FileRecord file) {
        FileModel model = new FileModel();
        model.setName(file.name());
        model.setSize(file.size());
        model.setMimeType(file.mimeType());
        model.setOwner(file.owner());
        model.setUploadStatus(file.status());
        model.setVisibility(FileVisibility.PRIVATE);
        return model;
    }
}

package org.azelabs.boxshare;

import jakarta.persistence.EntityNotFoundException;
import org.azelabs.boxshare.dtos.ShareFileRequest;
import org.azelabs.boxshare.models.FileModel;
import org.azelabs.boxshare.models.SharedFileModel;
import org.azelabs.boxshare.models.UserModel;
import org.azelabs.boxshare.repositories.IFileRepository;
import org.azelabs.boxshare.repositories.ISharedFileRepository;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.azelabs.boxshare.services.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceShareTest {

    @Mock IFileRepository fileRepository;
    @Mock ISharedFileRepository sharedFileRepository;
    @Mock IUserRepository userRepository;
    @Mock S3Presigner s3Presigner;

    @InjectMocks FileService fileService;

    private FileModel file;
    private UserModel recipient;

    @BeforeEach
    void setUp() throws Exception {
        file = new FileModel();
        setId(file, 1L);

        recipient = new UserModel();
        setId(recipient, 2L);
        recipient.setEmail("recipient@example.com");
    }

    private void setId(Object model, long id) throws Exception {
        Field field = model.getClass().getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(model, id);
    }

    @Test
    void shareFile_createsSharedFileRecord() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(sharedFileRepository.existsByFileAndUser(any(FileModel.class), any(UserModel.class))).thenReturn(false);

        fileService.shareFile(new ShareFileRequest(1L, "recipient@example.com"));

        ArgumentCaptor<SharedFileModel> captor = ArgumentCaptor.forClass(SharedFileModel.class);
        verify(sharedFileRepository).save(captor.capture());
        assertThat(captor.getValue().getFile()).isEqualTo(file);
        assertThat(captor.getValue().getUser()).isEqualTo(recipient);
    }

    @Test
    void shareFile_alreadyShared_doesNotDuplicate() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(sharedFileRepository.existsByFileAndUser(any(FileModel.class), any(UserModel.class))).thenReturn(true);

        fileService.shareFile(new ShareFileRequest(1L, "recipient@example.com"));

        verify(sharedFileRepository, never()).save(any());
    }

    @Test
    void shareFile_fileNotFound_throwsEntityNotFoundException() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.shareFile(new ShareFileRequest(99L, "recipient@example.com")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shareFile_userNotFound_throwsEntityNotFoundException() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.shareFile(new ShareFileRequest(1L, "unknown@example.com")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }
}

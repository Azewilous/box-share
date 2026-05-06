package org.azelabs.boxshare;

import org.azelabs.boxshare.application.enums.UploadStatus;
import org.azelabs.boxshare.dtos.FileDTO;
import org.azelabs.boxshare.dtos.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserFileE2ETest {

    @Autowired
    TestRestTemplate restTemplate;

    @MockitoBean
    S3Presigner s3Presigner;

    @MockitoBean
    S3Client s3Client;

    @MockitoBean
    SqsClient sqsClient;

    @MockitoBean
    SqsAsyncClient sqsAsyncClient;

    @BeforeEach
    void setUp() throws MalformedURLException {
        PresignedPutObjectRequest mockPutRequest = mock(PresignedPutObjectRequest.class);
        when(mockPutRequest.url()).thenReturn(new URL("https://fake-s3.amazonaws.com/test-bucket/test-file"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPutRequest);

        PresignedGetObjectRequest mockGetRequest = mock(PresignedGetObjectRequest.class);
        when(mockGetRequest.url()).thenReturn(new URL("https://fake-s3.amazonaws.com/test-bucket/test-file"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockGetRequest);

        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
                CompletableFuture.completedFuture(
                        GetQueueUrlResponse.builder().queueUrl("https://sqs.us-east-1.amazonaws.com/000000000000/test-queue").build()
                ));
    }

    @Test
    void createUser_createFile_updateUser_createSecondFile_deleteFirst_getFirstIsNotFound_getSecondSucceeds() {
        // 1. Create user
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String email = "john.doe+" + runId + "@example.com";
        UserDTO createRequest = new UserDTO(null, "John", "Doe", email);
        ResponseEntity<UserDTO> createResponse = restTemplate.postForEntity("/api/user", createRequest, UserDTO.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UserDTO user = createResponse.getBody();
        assertThat(user).isNotNull();
        assertThat(user.id()).isNotNull();
        assertThat(user.firstName()).isEqualTo("John");
        assertThat(user.lastName()).isEqualTo("Doe");

        // 2. Create first file for the user
        FileDTO file1Request = new FileDTO(null, "document-" + runId + ".pdf", null, null, user.email(), null, null);
        ResponseEntity<FileDTO> file1Response = restTemplate.postForEntity("/api/file", file1Request, FileDTO.class);

        assertThat(file1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FileDTO file1 = file1Response.getBody();
        assertThat(file1).isNotNull();
        assertThat(file1.id()).isNotNull();
        assertThat(file1.name()).isEqualTo("document-" + runId + ".pdf");
        assertThat(file1.uploadedBy()).isEqualTo(user.email());
        assertThat(file1.status()).isEqualTo(UploadStatus.NOT_STARTED);
        assertThat(file1.presignedUrl()).isNotBlank();

        // 3. Update the user
        UserDTO updateRequest = new UserDTO(user.id(), "Jane", "Smith", user.email());
        ResponseEntity<UserDTO> updateResponse = restTemplate.exchange(
                "/api/user", HttpMethod.PUT, new HttpEntity<>(updateRequest), UserDTO.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserDTO updatedUser = updateResponse.getBody();
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.firstName()).isEqualTo("Jane");
        assertThat(updatedUser.lastName()).isEqualTo("Smith");

        // 4. Create a second file for the updated user
        FileDTO file2Request = new FileDTO(null, "image-" + runId + ".png", null, null, updatedUser.email(), null, null);
        ResponseEntity<FileDTO> file2Response = restTemplate.postForEntity("/api/file", file2Request, FileDTO.class);

        assertThat(file2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FileDTO file2 = file2Response.getBody();
        assertThat(file2).isNotNull();
        assertThat(file2.id()).isNotNull();
        assertThat(file2.name()).isEqualTo("image-" + runId + ".png");
        assertThat(file2.status()).isEqualTo(UploadStatus.NOT_STARTED);
        assertThat(file2.presignedUrl()).isNotBlank();

        // 5. Delete the first file
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/file/" + file1.id(), HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 6. GET the deleted file — expect 404
        ResponseEntity<FileDTO> getDeletedResponse = restTemplate.getForEntity(
                "/api/file/" + file1.name(), FileDTO.class);

        assertThat(getDeletedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 7. GET the second file — expect success with a presigned URL
        ResponseEntity<FileDTO> getFile2Response = restTemplate.getForEntity(
                "/api/file/" + file2.name(), FileDTO.class);

        assertThat(getFile2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getFile2Response.getBody()).isNotNull();
        assertThat(getFile2Response.getBody().presignedUrl()).isNotBlank();
    }
}

package org.azelabs.boxshare;

import org.azelabs.boxshare.application.enums.UploadStatus;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.azelabs.boxshare.dtos.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
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

    @LocalServerPort private int port;

    @MockitoBean S3Presigner s3Presigner;
    @MockitoBean S3Client s3Client;
    @MockitoBean SqsClient sqsClient;
    @MockitoBean SqsAsyncClient sqsAsyncClient;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws MalformedURLException {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(HttpStatusCode statusCode) { return false; }
        });

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

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private <T> HttpEntity<T> withAuth(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> withAuth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private String registerAndGetToken(String runId) {
        RegisterRequest reg = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe+" + runId + "@example.com")
                .username("johndoe-" + runId)
                .password("Password123!")
                .build();
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/register"), reg, AuthResponse.class);
        return response.getBody().getToken();
    }

    @Test
    void registerUser_createFiles_updateUser_getBothFilesSucceed() {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndGetToken(runId);
        String email = "john.doe+" + runId + "@example.com";

        // 1. Resolve user record (registration already created the user; POST /api/user upserts by email)
        UserRecord createRequest = new UserRecord(null, "John", "Doe", email);
        ResponseEntity<UserRecord> createResponse = restTemplate.exchange(
                url("/api/user"), HttpMethod.POST, withAuth(token, createRequest), UserRecord.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UserRecord user = createResponse.getBody();
        assertThat(user).isNotNull();
        assertThat(user.id()).isNotNull();
        assertThat(user.firstName()).isEqualTo("John");
        assertThat(user.lastName()).isEqualTo("Doe");

        // 2. Create first file for the user
        FileRecord file1Request = new FileRecord(null, "document-" + runId + ".pdf", null, null, user.email(), null, null, null, null);
        ResponseEntity<FileRecord> file1Response = restTemplate.exchange(
                url("/api/file"), HttpMethod.POST, withAuth(token, file1Request), FileRecord.class);

        assertThat(file1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FileRecord file1 = file1Response.getBody();
        assertThat(file1).isNotNull();
        assertThat(file1.id()).isNotNull();
        assertThat(file1.name()).isEqualTo("document-" + runId + ".pdf");
        assertThat(file1.uploadedBy()).isEqualTo(user.email());
        assertThat(file1.status()).isEqualTo(UploadStatus.NOT_STARTED);
        assertThat(file1.presignedUrl()).isNotBlank();

        // 3. Update the user
        UserRecord updateRequest = new UserRecord(user.id(), "Jane", "Smith", user.email());
        ResponseEntity<UserRecord> updateResponse = restTemplate.exchange(
                url("/api/user"), HttpMethod.PUT, withAuth(token, updateRequest), UserRecord.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserRecord updatedUser = updateResponse.getBody();
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.firstName()).isEqualTo("Jane");
        assertThat(updatedUser.lastName()).isEqualTo("Smith");

        // 4. Create a second file for the updated user
        FileRecord file2Request = new FileRecord(null, "image-" + runId + ".png", null, null, updatedUser.email(), null, null, null, null);
        ResponseEntity<FileRecord> file2Response = restTemplate.exchange(
                url("/api/file"), HttpMethod.POST, withAuth(token, file2Request), FileRecord.class);

        assertThat(file2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FileRecord file2 = file2Response.getBody();
        assertThat(file2).isNotNull();
        assertThat(file2.id()).isNotNull();
        assertThat(file2.name()).isEqualTo("image-" + runId + ".png");
        assertThat(file2.status()).isEqualTo(UploadStatus.NOT_STARTED);
        assertThat(file2.presignedUrl()).isNotBlank();

        // 5. GET both files — expect success with presigned URLs
        ResponseEntity<FileRecord> getFile1Response = restTemplate.exchange(
                url("/api/file/" + file1.name()), HttpMethod.GET, withAuth(token), FileRecord.class);

        assertThat(getFile1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getFile1Response.getBody()).isNotNull();
        assertThat(getFile1Response.getBody().presignedUrl()).isNotBlank();

        ResponseEntity<FileRecord> getFile2Response = restTemplate.exchange(
                url("/api/file/" + file2.name()), HttpMethod.GET, withAuth(token), FileRecord.class);

        assertThat(getFile2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getFile2Response.getBody()).isNotNull();
        assertThat(getFile2Response.getBody().presignedUrl()).isNotBlank();
    }
}
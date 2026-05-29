package org.azelabs.boxshare;

import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
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
class FileSharingIntegrationTest {

    @LocalServerPort private int port;

    @MockitoBean S3Presigner s3Presigner;
    @MockitoBean S3Client s3Client;
    @MockitoBean SqsClient sqsClient;
    @MockitoBean SqsAsyncClient sqsAsyncClient;
    @MockitoBean JavaMailSender javaMailSender;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws MalformedURLException {
        restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(HttpStatusCode statusCode) { return false; }
        });

        PresignedPutObjectRequest mockPut = mock(PresignedPutObjectRequest.class);
        when(mockPut.url()).thenReturn(new URL("https://fake-s3.amazonaws.com/test-bucket/file"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPut);

        PresignedGetObjectRequest mockGet = mock(PresignedGetObjectRequest.class);
        when(mockGet.url()).thenReturn(new URL("https://fake-s3.amazonaws.com/test-bucket/file"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockGet);

        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
                CompletableFuture.completedFuture(
                        GetQueueUrlResponse.builder()
                                .queueUrl("https://sqs.us-east-1.amazonaws.com/000000000000/test-queue")
                                .build()));
    }

    // --- helpers ---

    private String url(String path) { return "http://localhost:" + port + path; }

    private String registerAndGetToken(String id) {
        RegisterRequest reg = RegisterRequest.builder()
                .firstName("Test").lastName("User")
                .email("user" + id + "@example.com")
                .username("user-" + id)
                .password("Password123!")
                .build();
        return restTemplate.postForEntity(url("/api/auth/register"), reg, AuthResponse.class)
                .getBody().getToken();
    }

    private String emailFor(String id) {
        return "user" + id + "@example.com";
    }

    private HttpEntity<Void> withAuth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private <T> HttpEntity<T> withAuth(String token, T body) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    private FileRecord createFile(String token, String filename) {
        FileRecord body = new FileRecord(null, filename, null, null, null, null, null, null, null, false);
        return restTemplate.exchange(url("/api/file"), HttpMethod.POST, withAuth(token, body), FileRecord.class).getBody();
    }

    private String shareUrl(Long fileId, String email) {
        return url("/api/file/" + fileId + "/share?email=" + email);
    }

    // --- share file ---

    @Test
    void shareFile_asOwner_returns204() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String ownerToken = registerAndGetToken(id);
        String recipientId = UUID.randomUUID().toString().substring(0, 8);
        registerAndGetToken(recipientId);
        String recipientEmail = emailFor(recipientId);

        FileRecord file = createFile(ownerToken, "share-" + id + ".txt");

        ResponseEntity<Void> response = restTemplate.exchange(
                shareUrl(file.id(), recipientEmail), HttpMethod.POST, withAuth(ownerToken), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shareFile_idempotent_returns204OnRepeat() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String ownerToken = registerAndGetToken(id);
        String recipientId = UUID.randomUUID().toString().substring(0, 8);
        registerAndGetToken(recipientId);
        String recipientEmail = emailFor(recipientId);

        FileRecord file = createFile(ownerToken, "share-idem-" + id + ".txt");

        restTemplate.exchange(shareUrl(file.id(), recipientEmail), HttpMethod.POST, withAuth(ownerToken), Void.class);
        ResponseEntity<Void> second = restTemplate.exchange(
                shareUrl(file.id(), recipientEmail), HttpMethod.POST, withAuth(ownerToken), Void.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shareFile_asNonOwner_returns403() {
        String ownerId = UUID.randomUUID().toString().substring(0, 8);
        String otherId = UUID.randomUUID().toString().substring(0, 8);
        String recipientId = UUID.randomUUID().toString().substring(0, 8);

        String ownerToken = registerAndGetToken(ownerId);
        String otherToken = registerAndGetToken(otherId);
        registerAndGetToken(recipientId);
        String recipientEmail = emailFor(recipientId);

        FileRecord file = createFile(ownerToken, "share-forbidden-" + ownerId + ".txt");

        ResponseEntity<Void> response = restTemplate.exchange(
                shareUrl(file.id(), recipientEmail), HttpMethod.POST, withAuth(otherToken), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shareFile_unauthenticated_returns401() {
        ResponseEntity<Void> response = restTemplate.exchange(
                shareUrl(999L, "someone@example.com"), HttpMethod.POST, new HttpEntity<Void>(new HttpHeaders()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shareFile_unknownEmail_returns404() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String ownerToken = registerAndGetToken(id);
        FileRecord file = createFile(ownerToken, "share-missing-" + id + ".txt");

        ResponseEntity<String> response = restTemplate.exchange(
                shareUrl(file.id(), "nobody@example.com"), HttpMethod.POST, withAuth(ownerToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shareFile_unknownFile_returns403() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String ownerToken = registerAndGetToken(id);

        // isOwner returns false for a nonexistent file → 403 before method executes
        ResponseEntity<String> response = restTemplate.exchange(
                shareUrl(999999L, emailFor(id)), HttpMethod.POST, withAuth(ownerToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- list files with shared flag ---

    @Test
    void listFiles_sharedFileAppearsInRecipientListWithSharedTrue() {
        String ownerId    = UUID.randomUUID().toString().substring(0, 8);
        String recipientId = UUID.randomUUID().toString().substring(0, 8);
        String ownerToken     = registerAndGetToken(ownerId);
        String recipientToken = registerAndGetToken(recipientId);

        FileRecord file = createFile(ownerToken, "shared-list-" + ownerId + ".txt");

        restTemplate.exchange(shareUrl(file.id(), emailFor(recipientId)), HttpMethod.POST, withAuth(ownerToken), Void.class);

        ResponseEntity<FileRecord[]> listResponse = restTemplate.exchange(
                url("/api/file"), HttpMethod.GET, withAuth(recipientToken), FileRecord[].class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        FileRecord[] files = listResponse.getBody();
        assertThat(files).isNotNull();

        FileRecord sharedEntry = java.util.Arrays.stream(files)
                .filter(f -> f.id().equals(file.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Shared file not found in recipient's list"));

        assertThat(sharedEntry.shared()).isTrue();
    }

    @Test
    void listFiles_ownedFileHasSharedFalse() {
        String id    = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndGetToken(id);
        FileRecord file = createFile(token, "owned-" + id + ".txt");

        ResponseEntity<FileRecord[]> listResponse = restTemplate.exchange(
                url("/api/file"), HttpMethod.GET, withAuth(token), FileRecord[].class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        FileRecord[] files = listResponse.getBody();
        assertThat(files).isNotNull();

        FileRecord ownedEntry = java.util.Arrays.stream(files)
                .filter(f -> f.id().equals(file.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Owned file not found in list"));

        assertThat(ownedEntry.shared()).isFalse();
    }
}

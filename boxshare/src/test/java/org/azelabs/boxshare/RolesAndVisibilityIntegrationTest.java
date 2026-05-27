package org.azelabs.boxshare;

import org.azelabs.boxshare.application.enums.FileVisibility;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.azelabs.boxshare.dtos.VisibilityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
class RolesAndVisibilityIntegrationTest {

    @LocalServerPort private int port;

    @MockitoBean S3Presigner s3Presigner;
    @MockitoBean S3Client s3Client;
    @MockitoBean SqsClient sqsClient;
    @MockitoBean SqsAsyncClient sqsAsyncClient;

    private RestTemplate restTemplate;   // standard — works for GET, POST, DELETE
    private RestTemplate patchTemplate;  // Apache — required for PATCH

    @BeforeEach
    void setUp() throws MalformedURLException {
        var noThrow = new DefaultResponseErrorHandler() {
            @Override public boolean hasError(HttpStatusCode statusCode) { return false; }
        };
        restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        restTemplate.setErrorHandler(noThrow);

        patchTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().disableRedirectHandling().build()));
        patchTemplate.setErrorHandler(noThrow);

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

    private String registerAndGetToken() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest reg = RegisterRequest.builder()
                .firstName("Test").lastName("User")
                .email("test+" + id + "@example.com")
                .username("user-" + id)
                .password("Password123!")
                .build();
        return restTemplate.postForEntity(url("/api/auth/register"), reg, AuthResponse.class)
                .getBody().getToken();
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
        FileRecord body = new FileRecord(null, filename, null, null, "user@example.com", null, null, null, null);
        return restTemplate.exchange(url("/api/file"), HttpMethod.POST, withAuth(token, body), FileRecord.class).getBody();
    }

    // --- role: registered users get USER role ---

    @Test
    void register_grantedUserRole_canAccessFileEndpoints() {
        String token = registerAndGetToken();
        String filename = "role-check-" + UUID.randomUUID() + ".txt";

        ResponseEntity<FileRecord> response = restTemplate.exchange(
                url("/api/file"), HttpMethod.POST,
                withAuth(token, new FileRecord(null, filename, null, null, null, null, null, null, null)),
                FileRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteFile_asUser_returns403() {
        String token = registerAndGetToken();
        FileRecord file = createFile(token, "delete-as-user-" + UUID.randomUUID() + ".txt");

        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/file/" + file.id()), HttpMethod.DELETE, withAuth(token), Void.class);

        // DELETE is ADMIN-only; USER gets 403
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- file visibility defaults ---

    @Test
    void createFile_defaultsToPrivate() {
        String token = registerAndGetToken();
        FileRecord file = createFile(token, "private-default-" + UUID.randomUUID() + ".txt");

        assertThat(file.visibility()).isEqualTo(FileVisibility.PRIVATE);
        assertThat(file.shareToken()).isNull();
    }

    // --- visibility toggle ---

    @Test
    void setVisibilityPublic_generatesShareToken() {
        String token = registerAndGetToken();
        FileRecord file = createFile(token, "toggle-public-" + UUID.randomUUID() + ".txt");

        ResponseEntity<FileRecord> response = patchTemplate.exchange(
                url("/api/file/" + file.id() + "/visibility"), HttpMethod.PATCH,
                withAuth(token, new VisibilityRequest(FileVisibility.PUBLIC)), FileRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().visibility()).isEqualTo(FileVisibility.PUBLIC);
        assertThat(response.getBody().shareToken()).isNotNull();
    }

    @Test
    void setVisibilityPrivate_clearsShareToken() {
        String token = registerAndGetToken();
        FileRecord file = createFile(token, "toggle-private-" + UUID.randomUUID() + ".txt");

        // make public first
        FileRecord publicFile = patchTemplate.exchange(
                url("/api/file/" + file.id() + "/visibility"), HttpMethod.PATCH,
                withAuth(token, new VisibilityRequest(FileVisibility.PUBLIC)), FileRecord.class).getBody();
        assertThat(publicFile.shareToken()).isNotNull();

        // revert to private
        ResponseEntity<FileRecord> response = patchTemplate.exchange(
                url("/api/file/" + file.id() + "/visibility"), HttpMethod.PATCH,
                withAuth(token, new VisibilityRequest(FileVisibility.PRIVATE)), FileRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().visibility()).isEqualTo(FileVisibility.PRIVATE);
        assertThat(response.getBody().shareToken()).isNull();
    }

    @Test
    void updateVisibility_withoutToken_returns401() {
        ResponseEntity<String> response = patchTemplate.exchange(
                url("/api/file/1/visibility"), HttpMethod.PATCH,
                new HttpEntity<>(new VisibilityRequest(FileVisibility.PUBLIC)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- public share link ---

    @Test
    void getPublicFile_withValidShareToken_returns200WithPresignedUrl() {
        String token = registerAndGetToken();
        FileRecord file = createFile(token, "shared-" + UUID.randomUUID() + ".txt");

        FileRecord publicFile = patchTemplate.exchange(
                url("/api/file/" + file.id() + "/visibility"), HttpMethod.PATCH,
                withAuth(token, new VisibilityRequest(FileVisibility.PUBLIC)), FileRecord.class).getBody();

        // access via share link — no auth header
        ResponseEntity<FileRecord> response = restTemplate.getForEntity(
                url("/api/public/file/" + publicFile.shareToken()), FileRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().presignedUrl()).isNotBlank();
    }

    @Test
    void getPublicFile_afterRevertingToPrivate_returns404() {
        String token = registerAndGetToken();
        FileRecord file = createFile(token, "reverted-" + UUID.randomUUID() + ".txt");

        // make public, capture token
        FileRecord publicFile = patchTemplate.exchange(
                url("/api/file/" + file.id() + "/visibility"), HttpMethod.PATCH,
                withAuth(token, new VisibilityRequest(FileVisibility.PUBLIC)), FileRecord.class).getBody();
        UUID shareToken = publicFile.shareToken();

        // revert to private
        patchTemplate.exchange(
                url("/api/file/" + file.id() + "/visibility"), HttpMethod.PATCH,
                withAuth(token, new VisibilityRequest(FileVisibility.PRIVATE)), FileRecord.class);

        // old share token should now 404
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/public/file/" + shareToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPublicFile_withUnknownToken_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/public/file/" + UUID.randomUUID()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

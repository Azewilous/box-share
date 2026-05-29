package org.azelabs.boxshare;

import org.azelabs.boxshare.dtos.AuthRequest;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
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
class AuthIntegrationTest {

    @LocalServerPort private int port;

    @MockitoBean S3Presigner s3Presigner;
    @MockitoBean S3Client s3Client;
    @MockitoBean SqsClient sqsClient;
    @MockitoBean SqsAsyncClient sqsAsyncClient;
    @MockitoBean JavaMailSender javaMailSender;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws MalformedURLException {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(HttpStatusCode statusCode) { return false; }
        });

        PresignedPutObjectRequest mockPut = mock(PresignedPutObjectRequest.class);
        when(mockPut.url()).thenReturn(new URL("https://fake-s3.amazonaws.com/test-bucket/test-file"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPut);

        PresignedGetObjectRequest mockGet = mock(PresignedGetObjectRequest.class);
        when(mockGet.url()).thenReturn(new URL("https://fake-s3.amazonaws.com/test-bucket/test-file"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockGet);

        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
                CompletableFuture.completedFuture(
                        GetQueueUrlResponse.builder()
                                .queueUrl("https://sqs.us-east-1.amazonaws.com/000000000000/test-queue")
                                .build()));
    }

    // --- helpers ---

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private RegisterRequest uniqueRegisterRequest() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test+" + id + "@example.com")
                .username("testuser-" + id)
                .password("Password123!")
                .build();
    }

    private String registerAndGetToken() {
        RegisterRequest reg = uniqueRegisterRequest();
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/register"), reg, AuthResponse.class);
        return response.getBody().getToken();
    }

    private HttpEntity<Void> withAuth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> withAuth(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    // --- auth endpoint tests ---

    @Test
    void register_withValidRequest_returns200WithToken() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/register"), uniqueRegisterRequest(), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    void login_withValidCredentials_returns200WithToken() {
        RegisterRequest reg = uniqueRegisterRequest();
        restTemplate.postForEntity(url("/api/auth/register"), reg, AuthResponse.class);

        AuthRequest login = AuthRequest.builder()
                .email(reg.getEmail())
                .password(reg.getPassword())
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/login"), login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    void login_withWrongPassword_returns401() {
        RegisterRequest reg = uniqueRegisterRequest();
        restTemplate.postForEntity(url("/api/auth/register"), reg, AuthResponse.class);

        AuthRequest login = AuthRequest.builder()
                .email(reg.getEmail())
                .password("wrongpassword")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/login"), login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_withUnknownEmail_returns401() {
        AuthRequest login = AuthRequest.builder()
                .email("nobody@example.com")
                .password("password")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/login"), login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- file endpoint: unauthenticated ---

    @Test
    void getFile_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/file/some-file.txt"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createFile_withoutToken_returns401() {
        FileRecord body = new FileRecord(null, "test.txt", null, null, null, null, null, null, null, false);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/file"), body, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- file endpoint: authenticated ---

    @Test
    void createFile_withValidToken_returns200WithPresignedUrl() {
        String token = registerAndGetToken();
        FileRecord body = new FileRecord(null, "upload-" + UUID.randomUUID() + ".txt", null, null, null, null, null, null, null, false);

        ResponseEntity<FileRecord> response = restTemplate.exchange(
                url("/api/file"), HttpMethod.POST, withAuth(token, body), FileRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().presignedUrl()).isNotBlank();
    }

    @Test
    void getFile_withValidToken_forExistingFile_returns200() {
        String token = registerAndGetToken();
        String filename = "download-" + UUID.randomUUID() + ".txt";

        FileRecord createBody = new FileRecord(null, filename, null, null, null, null, null, null, null, false);
        restTemplate.exchange(url("/api/file"), HttpMethod.POST, withAuth(token, createBody), FileRecord.class);

        ResponseEntity<FileRecord> response = restTemplate.exchange(
                url("/api/file/" + filename), HttpMethod.GET, withAuth(token), FileRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo(filename);
    }

    @Test
    void getFile_withValidToken_forMissingFile_returns404() {
        String token = registerAndGetToken();

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/file/does-not-exist.txt"), HttpMethod.GET, withAuth(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- GET /api/auth/me ---

    @Test
    void me_withValidToken_returns200WithUserInfo() {
        RegisterRequest reg = uniqueRegisterRequest();
        String token = restTemplate.postForEntity(url("/api/auth/register"), reg, AuthResponse.class)
                .getBody().getToken();

        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                url("/api/auth/me"), HttpMethod.GET, withAuth(token), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo(reg.getEmail());
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    void me_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/auth/me"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- POST /api/auth/logout ---

    @Test
    void logout_returns200WithClearedCookie() {
        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/auth/logout"), HttpMethod.POST, new HttpEntity<Void>(new HttpHeaders()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("access_token=");
        assertThat(cookie).contains("Max-Age=0");
    }

    // --- cookie assertions ---

    @Test
    void register_setsAccessTokenCookie() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/register"), uniqueRegisterRequest(), AuthResponse.class);

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("access_token=");
        assertThat(cookie).contains("HttpOnly");
    }

    @Test
    void login_setsAccessTokenCookie() {
        RegisterRequest reg = uniqueRegisterRequest();
        restTemplate.postForEntity(url("/api/auth/register"), reg, AuthResponse.class);

        AuthRequest login = AuthRequest.builder().email(reg.getEmail()).password(reg.getPassword()).build();
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url("/api/auth/login"), login, AuthResponse.class);

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("access_token=");
        assertThat(cookie).contains("HttpOnly");
    }

    // --- GET /api/file (list) unauthenticated ---

    @Test
    void listFiles_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/file"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
package org.azelabs.boxshare;

import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.azelabs.boxshare.dtos.UserRecord;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @LocalServerPort private int port;

    @MockitoBean S3Presigner s3Presigner;
    @MockitoBean S3Client s3Client;
    @MockitoBean SqsClient sqsClient;
    @MockitoBean SqsAsyncClient sqsAsyncClient;
    @MockitoBean JavaMailSender javaMailSender;

    @Autowired IUserRepository userRepository;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(HttpStatusCode statusCode) { return false; }
        });
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
                CompletableFuture.completedFuture(
                        GetQueueUrlResponse.builder()
                                .queueUrl("https://sqs.us-east-1.amazonaws.com/000000000000/test-queue")
                                .build()));
    }

    // --- helpers ---

    private String url(String path) { return "http://localhost:" + port + path; }

    private RegisterRequest uniqueRegisterRequest() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return RegisterRequest.builder()
                .firstName("Test").lastName("User")
                .email("user" + id + "@example.com")
                .username("user-" + id)
                .password("Password123!")
                .build();
    }

    private String registerAndGetToken(RegisterRequest reg) {
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

    // --- POST /api/user ---

    @Test
    void createUser_withExistingEmail_returns202() {
        RegisterRequest reg = uniqueRegisterRequest();
        String token = registerAndGetToken(reg);

        UserRecord body = new UserRecord(null, reg.getFirstName(), reg.getLastName(), reg.getEmail());
        ResponseEntity<UserRecord> response = restTemplate.exchange(
                url("/api/user"), HttpMethod.POST, withAuth(token, body), UserRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().email()).isEqualTo(reg.getEmail());
    }

    @Test
    void createUser_unauthenticated_returns401() {
        UserRecord body = new UserRecord(null, "Test", "User", "nobody@example.com");
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/user"), body, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- GET /api/user/{id} ---

    @Test
    void getUserById_existingUser_returns200() {
        RegisterRequest reg = uniqueRegisterRequest();
        String token = registerAndGetToken(reg);
        long userId = userRepository.findByEmail(reg.getEmail()).orElseThrow().getId();

        ResponseEntity<UserRecord> response = restTemplate.exchange(
                url("/api/user/" + userId), HttpMethod.GET, withAuth(token), UserRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo(reg.getEmail());
        assertThat(response.getBody().firstName()).isEqualTo(reg.getFirstName());
    }

    @Test
    void getUserById_unknownId_returns404() {
        String token = registerAndGetToken(uniqueRegisterRequest());

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/user/999999"), HttpMethod.GET, withAuth(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getUserById_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/user/1"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- PUT /api/user ---

    @Test
    void updateUser_withValidData_returns200() {
        RegisterRequest reg = uniqueRegisterRequest();
        String token = registerAndGetToken(reg);
        long userId = userRepository.findByEmail(reg.getEmail()).orElseThrow().getId();

        UserRecord update = new UserRecord(userId, "Updated", "Name", reg.getEmail());
        ResponseEntity<UserRecord> response = restTemplate.exchange(
                url("/api/user"), HttpMethod.PUT, withAuth(token, update), UserRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().firstName()).isEqualTo("Updated");
        assertThat(response.getBody().lastName()).isEqualTo("Name");
    }

    @Test
    void updateUser_unauthenticated_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        UserRecord body = new UserRecord(1L, "Test", "User", "test@example.com");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/user"), HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- DELETE /api/user/{id} ---

    @Test
    void deleteUser_existingUser_returnsTrue() {
        RegisterRequest reg = uniqueRegisterRequest();
        String token = registerAndGetToken(reg);
        long userId = userRepository.findByEmail(reg.getEmail()).orElseThrow().getId();

        ResponseEntity<Boolean> response = restTemplate.exchange(
                url("/api/user/" + userId), HttpMethod.DELETE, withAuth(token), Boolean.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    void deleteUser_unknownId_returnsFalse() {
        String token = registerAndGetToken(uniqueRegisterRequest());

        ResponseEntity<Boolean> response = restTemplate.exchange(
                url("/api/user/999999"), HttpMethod.DELETE, withAuth(token), Boolean.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    void deleteUser_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/user/1"), HttpMethod.DELETE,
                new HttpEntity<Void>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- POST /api/user/verify/{token} ---

    @Test
    void verifyEmail_withValidToken_returns200() {
        RegisterRequest reg = uniqueRegisterRequest();
        registerAndGetToken(reg);
        UUID verificationToken = userRepository.findByEmail(reg.getEmail())
                .orElseThrow().getVerificationToken();

        ResponseEntity<UserRecord> response = restTemplate.exchange(
                url("/api/user/verify/" + verificationToken), HttpMethod.POST,
                new HttpEntity<Void>(new HttpHeaders()), UserRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo(reg.getEmail());
    }

    @Test
    void verifyEmail_withInvalidToken_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/user/verify/" + UUID.randomUUID()), HttpMethod.POST,
                new HttpEntity<Void>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- POST /api/user/verify/resend ---

    @Test
    void resendVerification_authenticated_returns200() {
        RegisterRequest reg = uniqueRegisterRequest();
        String token = registerAndGetToken(reg);

        ResponseEntity<Void> response = restTemplate.exchange(
                url("/api/user/verify/resend"), HttpMethod.POST, withAuth(token), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

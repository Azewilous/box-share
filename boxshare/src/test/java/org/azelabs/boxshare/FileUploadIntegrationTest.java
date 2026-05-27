package org.azelabs.boxshare;

import org.azelabs.boxshare.application.enums.UploadStatus;
import org.azelabs.boxshare.dtos.AuthResponse;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class FileUploadIntegrationTest {

    @LocalServerPort private int port;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(HttpStatusCode statusCode) { return false; }
        });
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
                .firstName("Upload")
                .lastName("Test")
                .email("upload+" + runId + "@example.com")
                .username("upload-" + runId)
                .password("Password123!")
                .build();
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                url("/api/auth/register"), reg, AuthResponse.class);
        return response.getBody().getToken();
    }

    @Test
    void upload_realFile_triggersSqsConsumer_completesFileRecord() throws InterruptedException {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndGetToken(runId);

        // Create file record — returns presigned PUT URL from LocalStack
        String filename = "test-" + runId + ".txt";
        FileRecord createBody = new FileRecord(null, filename, null, null, "upload+" + runId + "@example.com", null, null, null, null);
        ResponseEntity<FileRecord> createResponse = restTemplate.exchange(
                url("/api/file"), HttpMethod.POST, withAuth(token, createBody), FileRecord.class);

        FileRecord fileRecord = createResponse.getBody();
        assertThat(fileRecord).isNotNull();
        assertThat(fileRecord.presignedUrl()).isNotBlank();

        // Generate random text content between 1KB and 4MB
        int sizeBytes = 1024 + new Random().nextInt(4 * 1024 * 1024 - 1024);
        byte[] content = generateTextContent(sizeBytes);

        // PUT content directly to LocalStack S3 via presigned URL
        // Use URI.create to avoid RestTemplate template expansion mangling the signed query params
        // No Content-Type header — presigned URL was not signed with one, so including it causes 400
        RestTemplate s3Http = new RestTemplate();
        ResponseEntity<Void> putResponse = s3Http.exchange(
                URI.create(fileRecord.presignedUrl()), HttpMethod.PUT, new HttpEntity<>(content), Void.class);
        assertThat(putResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // Poll until SQS consumer processes the S3 event and marks the file COMPLETED (up to 30s)
        FileRecord completed = null;
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);
            ResponseEntity<FileRecord> response = restTemplate.exchange(
                    url("/api/file/" + filename), HttpMethod.GET, withAuth(token), FileRecord.class);
            FileRecord body = response.getBody();
            if (body != null && body.status() == UploadStatus.COMPLETED) {
                completed = body;
                break;
            }
        }

        assertThat(completed).as("file should reach COMPLETED status within 30s").isNotNull();
        assertThat(completed.size()).isEqualTo((long) content.length);
        assertThat(completed.mimeType()).isNotBlank();
    }

    private byte[] generateTextContent(int sizeBytes) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeBytes);
        for (int i = 0; i < sizeBytes; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
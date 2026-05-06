package org.azelabs.boxshare;

import org.azelabs.boxshare.application.enums.UploadStatus;
import org.azelabs.boxshare.dtos.FileDTO;
import org.azelabs.boxshare.dtos.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class FileUploadIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void upload_realFile_triggersSqsConsumer_completesFileRecord() throws InterruptedException {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Create user
        UserDTO user = restTemplate.postForObject("/api/user",
                new UserDTO(null, "Upload", "Test", "upload+" + runId + "@example.com"),
                UserDTO.class);
        assertThat(user).isNotNull();

        // Create file record — returns presigned PUT URL from LocalStack
        String filename = "test-" + runId + ".txt";
        FileDTO fileRecord = restTemplate.postForObject("/api/file",
                new FileDTO(null, filename, null, null, user.email(), null, null),
                FileDTO.class);
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
        FileDTO completed = null;
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);
            ResponseEntity<FileDTO> response = restTemplate.getForEntity("/api/file/" + filename, FileDTO.class);
            FileDTO body = response.getBody();
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
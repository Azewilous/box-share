package org.azelabs.boxshare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@SpringBootTest
@ActiveProfiles("test")
class BoxshareApplicationTests {

	@MockitoBean S3Presigner s3Presigner;
	@MockitoBean S3Client s3Client;
	@MockitoBean SqsClient sqsClient;
	@MockitoBean SqsAsyncClient sqsAsyncClient;
	@MockitoBean JavaMailSender javaMailSender;

	@Test
	void contextLoads() {
	}

}

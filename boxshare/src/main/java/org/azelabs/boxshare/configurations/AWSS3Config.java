package org.azelabs.boxshare.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AWSS3Config {

    private static final Logger log = LoggerFactory.getLogger(AWSS3Config.class);

    @Value("${aws.access.key}")
    private String accessKey;

    @Value("${aws.secret.key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.endpoint:#{null}}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        Region awsRegion = Region.of(region);
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3ClientBuilder builder = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        Region awsRegion = Region.of(region);
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        log.info("Creating presigner on region {} with access key {} and secret {}", region, accessKey, secretKey);
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient() {
        Region awsRegion = Region.of(region);
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        SqsClientBuilder builder = SqsClient.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

}
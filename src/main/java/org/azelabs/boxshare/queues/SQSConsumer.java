package org.azelabs.boxshare.queues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.enums.UploadStatus;
import org.azelabs.boxshare.dtos.FileDTO;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SQSConsumer {

    private static final Logger log = LoggerFactory.getLogger(SQSConsumer.class);

    private final IFileService service;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @SqsListener("${aws.queue.name}")
    public void receiveMessage(Message message) {
        String body = message.body();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            Records recordList = objectMapper.readValue(body, Records.class);
            List<MessageResponse> responses = recordList.getRecords();
            for (MessageResponse record : responses) {

                MessageS3Response messageS3Response = record.getResponse();
                BucketObject bucketObject = messageS3Response.getObject();

                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(bucketObject.getKey())
                        .build();

                HeadObjectResponse headBucketResponse = s3Client.headObject(headObjectRequest);

                FileDTO file = service.getByName(bucketObject.getKey()).orElseThrow();
                FileDTO updatedFile = new FileDTO(file.id(), file.name(), bucketObject.getSize(), headBucketResponse.contentType(),
                        file.uploadedBy(), UploadStatus.COMPLETED, null);

                service.update(updatedFile);
            }
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
        }
    }



}

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class Records {
    @JsonProperty("Records")
    private List<MessageResponse> records = new ArrayList<>();
}

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class MessageResponse {

    @JsonProperty("eventTime")
    private LocalDateTime eventTime;
    @JsonProperty("s3")
    private MessageS3Response response;

}

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class MessageS3Response {
    @JsonProperty("bucket")
    private S3Bucket bucket;
    @JsonProperty("object")
    private BucketObject object;
}

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class S3Bucket {
    @JsonProperty("name")
    private String name;
}

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class BucketObject {
    @JsonProperty("key")
    private String key;
    @JsonProperty("size")
    private Long size;
}

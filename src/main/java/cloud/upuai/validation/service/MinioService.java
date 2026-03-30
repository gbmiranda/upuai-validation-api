package cloud.upuai.validation.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public MinioService(S3Client s3, S3Presigner presigner) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = System.getenv().getOrDefault("S3_BUCKET_NAME", "documents");
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created S3 bucket '{}'", bucket);
        } catch (Exception e) {
            log.warn("Could not verify bucket '{}': {}", bucket, e.getMessage());
        }
    }

    public String upload(String s3Key, MultipartFile file) throws IOException {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        log.info("Uploaded file to s3://{}/{}", bucket, s3Key);
        return s3Key;
    }

    public URL generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(r -> r.bucket(bucket).key(s3Key))
                .build();
        return presigner.presignGetObject(presignRequest).url();
    }

    public void delete(String s3Key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build());
        log.info("Deleted s3://{}/{}", bucket, s3Key);
    }
}

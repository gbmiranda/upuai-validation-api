package cloud.upuai.validation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public S3Client s3Client() {
        String endpoint = System.getenv("S3_ENDPOINT");
        String accessKey = System.getenv("S3_ACCESS_KEY_ID");
        String secretKey = System.getenv("S3_SECRET_ACCESS_KEY");

        if (endpoint == null || accessKey == null || secretKey == null) {
            throw new IllegalStateException(
                "S3_ENDPOINT, S3_ACCESS_KEY_ID and S3_SECRET_ACCESS_KEY are required");
        }

        log.info("Connecting to MinIO/S3 at {}", endpoint);
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        String publicEndpoint = System.getenv("S3_PUBLIC_ENDPOINT");
        String endpoint = publicEndpoint != null ? publicEndpoint : System.getenv("S3_ENDPOINT");
        String accessKey = System.getenv("S3_ACCESS_KEY_ID");
        String secretKey = System.getenv("S3_SECRET_ACCESS_KEY");

        log.info("S3 Presigner using endpoint {}", endpoint);
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}

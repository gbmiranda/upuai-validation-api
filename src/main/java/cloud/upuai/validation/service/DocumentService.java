package cloud.upuai.validation.service;

import cloud.upuai.validation.config.RedisConfig;
import cloud.upuai.validation.model.Document;
import cloud.upuai.validation.model.DocumentStatus;
import cloud.upuai.validation.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;
    private final MinioService minioService;
    private final StringRedisTemplate redisTemplate;

    public DocumentService(DocumentRepository repository, MinioService minioService,
                           StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.minioService = minioService;
        this.redisTemplate = redisTemplate;
    }

    public Document upload(MultipartFile file, String name, String category) throws IOException {
        UUID id = UUID.randomUUID();
        String s3Key = id + "/" + file.getOriginalFilename();

        // 1. Upload to S3
        minioService.upload(s3Key, file);

        // 2. Persist to DB with PROCESSING status
        Document doc = new Document();
        doc.setName(name);
        doc.setCategory(category);
        doc.setFilename(file.getOriginalFilename());
        doc.setS3Key(s3Key);
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setStatus(DocumentStatus.PROCESSING);

        Document saved = repository.save(doc);

        // 3. Publish to Redis Stream for async processing
        try {
            redisTemplate.opsForStream().add(
                    RedisConfig.STREAM_KEY,
                    Map.of("documentId", saved.getId().toString())
            );
            log.info("Published document {} to stream for processing", saved.getId());
        } catch (Exception e) {
            log.warn("Could not publish to Redis Stream (document will stay PROCESSING): {}", e.getMessage());
        }

        return saved;
    }

    public List<Document> list(String category, String status, int page, int size) {
        return repository.findAll(category, status, page, size);
    }

    public long count(String category, String status) {
        return repository.count(category, status);
    }

    public Optional<Document> findById(UUID id) {
        return repository.findById(id);
    }

    public Optional<Document> update(UUID id, String name, String category) {
        return repository.update(id, name, category);
    }

    public boolean delete(UUID id) {
        Optional<Document> doc = repository.findById(id);
        if (doc.isEmpty()) return false;

        // Delete from S3 first, then DB
        try {
            minioService.delete(doc.get().getS3Key());
        } catch (Exception e) {
            log.warn("Could not delete S3 object {}: {}", doc.get().getS3Key(), e.getMessage());
        }

        return repository.deleteById(id);
    }

    public Optional<String> getDownloadUrl(UUID id) {
        return repository.findById(id)
                .map(doc -> minioService.generatePresignedUrl(doc.getS3Key()).toString());
    }

    public List<String> listCategories() {
        return repository.findDistinctCategories();
    }
}

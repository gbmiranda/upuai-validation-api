package cloud.upuai.validation.service;

import cloud.upuai.validation.model.DocumentStatus;
import cloud.upuai.validation.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class DocumentProcessingConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final DocumentRepository repository;

    public DocumentProcessingConsumer(DocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String documentId = message.getValue().get("documentId");
        if (documentId == null) {
            log.warn("Received stream message without documentId: {}", message.getId());
            return;
        }

        log.info("Processing document {}", documentId);
        try {
            UUID id = UUID.fromString(documentId);
            repository.findById(id).ifPresentOrElse(doc -> {
                // Build metadata JSON with file info
                String sizeFormatted = formatSize(doc.getFileSize());
                String extension = extractExtension(doc.getFilename());
                String metadata = String.format(
                    "{\"extension\":\"%s\",\"sizeFormatted\":\"%s\",\"processedAt\":\"%s\"}",
                    extension, sizeFormatted, Instant.now().toString()
                );
                repository.updateStatus(id, DocumentStatus.READY, metadata);
                log.info("Document {} processed successfully ({})", documentId, sizeFormatted);
            }, () -> log.warn("Document {} not found for processing", documentId));

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);
            try {
                String metadata = String.format("{\"error\":\"%s\"}", e.getMessage().replace("\"", "'"));
                repository.updateStatus(UUID.fromString(documentId), DocumentStatus.ERROR, metadata);
            } catch (Exception ignored) {}
        }
    }

    private String formatSize(Long bytes) {
        if (bytes == null) return "unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}

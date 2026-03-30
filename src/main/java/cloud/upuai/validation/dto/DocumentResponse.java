package cloud.upuai.validation.dto;

import cloud.upuai.validation.model.Document;
import cloud.upuai.validation.model.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public class DocumentResponse {
    private UUID id;
    private String name;
    private String category;
    private String filename;
    private String contentType;
    private Long fileSize;
    private DocumentStatus status;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public static DocumentResponse from(Document doc) {
        DocumentResponse r = new DocumentResponse();
        r.id = doc.getId();
        r.name = doc.getName();
        r.category = doc.getCategory();
        r.filename = doc.getFilename();
        r.contentType = doc.getContentType();
        r.fileSize = doc.getFileSize();
        r.status = doc.getStatus();
        r.metadata = doc.getMetadata();
        r.createdAt = doc.getCreatedAt();
        r.updatedAt = doc.getUpdatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public DocumentStatus getStatus() { return status; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

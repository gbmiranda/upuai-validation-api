package cloud.upuai.validation.repository;

import cloud.upuai.validation.model.Document;
import cloud.upuai.validation.model.DocumentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbc;

    public DocumentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Document> ROW_MAPPER = (rs, rowNum) -> {
        Document d = new Document();
        d.setId(UUID.fromString(rs.getString("id")));
        d.setName(rs.getString("name"));
        d.setCategory(rs.getString("category"));
        d.setFilename(rs.getString("filename"));
        d.setS3Key(rs.getString("s3_key"));
        d.setContentType(rs.getString("content_type"));
        d.setFileSize(rs.getObject("file_size", Long.class));
        d.setStatus(DocumentStatus.valueOf(rs.getString("status")));
        d.setMetadata(rs.getString("metadata"));
        d.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        d.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return d;
    };

    public Document save(Document doc) {
        String sql = """
            INSERT INTO documents (name, category, filename, s3_key, content_type, file_size, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        return jdbc.queryForObject(sql, ROW_MAPPER,
                doc.getName(), doc.getCategory(), doc.getFilename(),
                doc.getS3Key(), doc.getContentType(), doc.getFileSize(),
                doc.getStatus().name());
    }

    public Optional<Document> findById(UUID id) {
        List<Document> results = jdbc.query(
                "SELECT * FROM documents WHERE id = ?", ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Document> findAll(String category, String status, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM documents WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((long) page * size);

        return jdbc.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public long count(String category, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM documents WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        Long count = jdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    public Optional<Document> update(UUID id, String name, String category) {
        List<Document> results = jdbc.query(
                "UPDATE documents SET name=?, category=?, updated_at=NOW() WHERE id=? RETURNING *",
                ROW_MAPPER, name, category, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void updateStatus(UUID id, DocumentStatus status, String metadata) {
        jdbc.update(
                "UPDATE documents SET status=?, metadata=?::jsonb, updated_at=NOW() WHERE id=?",
                status.name(), metadata, id);
    }

    public boolean deleteById(UUID id) {
        return jdbc.update("DELETE FROM documents WHERE id=?", id) > 0;
    }

    public List<String> findDistinctCategories() {
        return jdbc.queryForList(
                "SELECT DISTINCT category FROM documents WHERE category IS NOT NULL ORDER BY category",
                String.class);
    }
}

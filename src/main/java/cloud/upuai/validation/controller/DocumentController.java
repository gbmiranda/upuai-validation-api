package cloud.upuai.validation.controller;

import cloud.upuai.validation.dto.DocumentResponse;
import cloud.upuai.validation.dto.DocumentUpdateRequest;
import cloud.upuai.validation.dto.PagedResponse;
import cloud.upuai.validation.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "category", required = false) String category) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var doc = service.upload(file, name, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(doc));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DocumentResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        var items = service.list(category, status, page, size)
                .stream().map(DocumentResponse::from).toList();
        long total = service.count(category, status);
        return ResponseEntity.ok(new PagedResponse<>(items, page, size, total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(@PathVariable UUID id) {
        return service.findById(id)
                .map(doc -> ResponseEntity.ok(DocumentResponse.from(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentUpdateRequest req) {
        return service.update(id, req.getName(), req.getCategory())
                .map(doc -> ResponseEntity.ok(DocumentResponse.from(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> download(@PathVariable UUID id) {
        return service.getDownloadUrl(id)
                .map(url -> ResponseEntity.ok(Map.of("url", url)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(service.listCategories());
    }
}

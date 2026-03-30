package cloud.upuai.validation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "upuai-validation-api",
                "version", "1.0.0",
                "docs", "/health"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "upuai-validation-api",
                "runtime", "java-" + System.getProperty("java.version"),
                "timestamp", Instant.now().toString()
        ));
    }
}

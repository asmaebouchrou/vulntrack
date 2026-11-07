package com.vulntrack.backend.dependency;

import java.util.List;
import com.vulntrack.backend.scan.ScanService;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/dependencies")
public class DependencyController {

    private final DependencyService dependencyService;
    private final OsvService osvService;
    private final ScanService scanService;

    public DependencyController(DependencyService dependencyService, OsvService osvService, ScanService scanService) {
        this.dependencyService = dependencyService;
        this.osvService = osvService;
        this.scanService = scanService;

    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
        }

        if (!"pom.xml".equals(file.getOriginalFilename())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo debe llamarse pom.xml"));
        }

        try {
            List<DependencyInfo> dependencies = dependencyService.extractDependencies(file);

            var results = osvService.scan(dependencies);

            return ResponseEntity.ok(
                    scanService.save(file.getOriginalFilename(), dependencies, results));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
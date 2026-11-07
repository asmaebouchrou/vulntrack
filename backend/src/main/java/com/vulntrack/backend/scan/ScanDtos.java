package com.vulntrack.backend.scan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ScanDtos {
    private ScanDtos() {}

    public enum AnalysisStatus { ANALYZED, SKIPPED_UNRESOLVED_VERSION }

    public record DependencyResult(String groupId, String artifactId, String version,
            List<Map<String, Object>> vulnerabilities, AnalysisStatus status) {}

    public record UploadResponse(Long scanId, List<DependencyResult> results) {}

    public record ScanDetail(Long id, String filename, LocalDateTime createdAt,
            List<DependencyResult> dependencies) {}

    public record ScanSummary(Long id, String filename, LocalDateTime createdAt,
            long dependencyCount, long vulnerableDependencyCount) {}
}

package com.vulntrack.backend.scan;

import com.vulntrack.backend.dependency.DependencyInfo;
import com.vulntrack.backend.scan.ScanDtos.*;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScanService {
    private final ScanRepository repository;

    public ScanService(ScanRepository repository) { this.repository = repository; }

    @Transactional
    public UploadResponse save(String filename, List<DependencyInfo> dependencies,
            List<DependencyResult> results) {
        if (dependencies.size() != results.size()) {
            throw new IllegalStateException("Resultados de análisis incompletos");
        }
        Scan scan = new Scan(filename);
        for (int i = 0; i < dependencies.size(); i++) {
            var dependency = dependencies.get(i);
            var result = results.get(i);
            var expectedStatus = dependency.hasResolvedVersion() ? AnalysisStatus.ANALYZED
                    : AnalysisStatus.SKIPPED_UNRESOLVED_VERSION;
            if (!Objects.equals(dependency.groupId(), result.groupId())
                    || !Objects.equals(dependency.artifactId(), result.artifactId())
                    || !Objects.equals(dependency.version(), result.version())
                    || result.status() != expectedStatus || result.vulnerabilities() == null
                    || (expectedStatus != AnalysisStatus.ANALYZED && !result.vulnerabilities().isEmpty())) {
                throw new IllegalStateException("Resultados de análisis inconsistentes");
            }
            scan.addDependency(new ScanDependency(result.groupId(), result.artifactId(), result.version(),
                    result.vulnerabilities(), result.status()));
        }
        repository.saveAndFlush(scan);
        return new UploadResponse(scan.getId(), scan.getDependencies().stream().map(this::toResult).toList());
    }

    @Transactional(readOnly = true)
    public List<ScanSummary> history() {
        return repository.findHistory().stream().map(s -> new ScanSummary(s.getId(), s.getFilename(),
                s.getCreatedAt(), s.getDependencyCount(), s.getVulnerableDependencyCount())).toList();
    }

    @Transactional(readOnly = true)
    public ScanDetail detail(Long id) {
        var scan = repository.findDetailById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Análisis no encontrado"));
        return new ScanDetail(scan.getId(), scan.getFilename(), scan.getCreatedAt(),
                scan.getDependencies().stream().map(this::toResult).toList());
    }

    private DependencyResult toResult(ScanDependency dependency) {
        return new DependencyResult(dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getVersion(), dependency.getVulnerabilities(), dependency.getStatus());
    }
}

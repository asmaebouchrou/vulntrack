package com.vulntrack.backend.scan;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;
import com.vulntrack.backend.scan.ScanDtos.AnalysisStatus;

@Entity
@Table(name = "scan_dependencies")
public class ScanDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "artifact_id", nullable = false)
    private String artifactId;

    @Column(name = "version")
    private String version;

    @Column(name = "vulnerabilities", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(
        org.hibernate.type.SqlTypes.JSON
    )
    private List<Map<String, Object>> vulnerabilities = List.of();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AnalysisStatus status = AnalysisStatus.ANALYZED;

    protected ScanDependency() {
    }

    public ScanDependency(
            String groupId,
            String artifactId,
            String version,
            List<Map<String, Object>> vulnerabilities, AnalysisStatus status) {

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.vulnerabilities = List.copyOf(vulnerabilities);
        this.status = status;
    }

    void setScan(Scan scan) {
        this.scan = scan;
    }

    public Long getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public List<Map<String, Object>> getVulnerabilities() {
        return vulnerabilities;
    }
    public AnalysisStatus getStatus() { return status; }
}

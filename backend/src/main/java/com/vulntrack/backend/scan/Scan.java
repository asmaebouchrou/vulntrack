package com.vulntrack.backend.scan;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scans")
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(
        mappedBy = "scan",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<ScanDependency> dependencies = new ArrayList<>();

    protected Scan() {
    }

    public Scan(String filename) {
        this.filename = filename;
        this.createdAt = LocalDateTime.now();
    }

    public void addDependency(ScanDependency dependency) {
        dependencies.add(dependency);
        dependency.setScan(this);
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ScanDependency> getDependencies() {
        return java.util.Collections.unmodifiableList(dependencies);
    }
}

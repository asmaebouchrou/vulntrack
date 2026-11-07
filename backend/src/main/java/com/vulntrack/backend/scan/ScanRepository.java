package com.vulntrack.backend.scan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScanRepository extends JpaRepository<Scan, Long> {
    interface Summary {
        Long getId();
        String getFilename();
        LocalDateTime getCreatedAt();
        long getDependencyCount();
        long getVulnerableDependencyCount();
    }

    @Query(value = """
        SELECT s.id AS id, s.filename AS filename, s.created_at AS "createdAt",
               count(d.id) AS "dependencyCount",
               count(d.id) FILTER (WHERE jsonb_array_length(d.vulnerabilities) > 0)
                   AS "vulnerableDependencyCount"
        FROM scans s LEFT JOIN scan_dependencies d ON d.scan_id = s.id
        GROUP BY s.id, s.filename, s.created_at
        ORDER BY s.created_at DESC, s.id DESC
        """, nativeQuery = true)
    List<Summary> findHistory();

    @Query("select s from Scan s left join fetch s.dependencies where s.id = :id")
    Optional<Scan> findDetailById(@Param("id") Long id);
}

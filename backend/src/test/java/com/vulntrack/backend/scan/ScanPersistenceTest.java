package com.vulntrack.backend.scan;

import com.vulntrack.backend.PostgresTest;
import com.vulntrack.backend.dependency.DependencyInfo;
import com.vulntrack.backend.scan.ScanDtos.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ScanPersistenceTest extends PostgresTest {
    @Autowired ScanService service;
    @Autowired ScanRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @BeforeEach void clear() { repository.deleteAll(); }

    @Test void persistsAndRetrievesJsonArrayAndAllDependencies() {
        var dependencies = List.of(new DependencyInfo("g", "vulnerable", "1"),
                new DependencyInfo("g", "clean", "2"), new DependencyInfo("g", "unresolved", "${revision}"),
                new DependencyInfo("g", "missing", null));
        var vulnerabilities = List.<Map<String, Object>>of(Map.of("id", "GHSA-EXAMPLE", "modified", "2026-01-01"));
        var results = List.of(new DependencyResult("g", "vulnerable", "1", vulnerabilities, AnalysisStatus.ANALYZED),
                new DependencyResult("g", "clean", "2", List.of(), AnalysisStatus.ANALYZED),
                new DependencyResult("g", "unresolved", "${revision}", List.of(), AnalysisStatus.SKIPPED_UNRESOLVED_VERSION),
                new DependencyResult("g", "missing", null, List.of(), AnalysisStatus.SKIPPED_UNRESOLVED_VERSION));
        var saved = service.save("pom.xml", dependencies, results);
        assertThat(saved.scanId()).isPositive();
        var detail = service.detail(saved.scanId());
        assertThat(detail.dependencies()).containsExactlyInAnyOrderElementsOf(results);
        assertThat(detail.filename()).isEqualTo("pom.xml");
        assertThat(jdbc.queryForObject("select jsonb_typeof(vulnerabilities) from scan_dependencies where artifact_id='vulnerable'", String.class))
                .isEqualTo("array");
        assertThat(jdbc.queryForObject("select vulnerabilities->0->>'id' from scan_dependencies where artifact_id='vulnerable'", String.class))
                .isEqualTo("GHSA-EXAMPLE");
        var summary = service.history().getFirst();
        assertThat(summary.dependencyCount()).isEqualTo(4);
        assertThat(summary.vulnerableDependencyCount()).isEqualTo(1);
    }

    @Test void historyOrdersByDateThenIdAndIncludesEmptyScans() {
        var first = service.save("first.xml", List.of(), List.of());
        var second = service.save("second.xml", List.of(), List.of());
        var third = service.save("third.xml", List.of(), List.of());
        jdbc.update("update scans set created_at=? where id in (?, ?)", LocalDateTime.of(2026, 1, 1, 0, 0), first.scanId(), third.scanId());
        assertThat(service.history()).extracting(ScanSummary::id).containsExactly(second.scanId(), third.scanId(), first.scanId());
        assertThat(service.history()).allSatisfy(s -> assertThat(s.dependencyCount()).isZero());
    }

    @Test void missingScanIsNotFound() {
        assertThatThrownBy(() -> service.detail(Long.MAX_VALUE)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    @Test void persistenceFailureRollsBackParentAndChildren() {
        var dependencies = List.of(new DependencyInfo("g", "valid", "1"), new DependencyInfo("g", "x".repeat(256), "1"));
        var results = dependencies.stream().map(d -> new DependencyResult(d.groupId(), d.artifactId(), d.version(), List.of(), AnalysisStatus.ANALYZED)).toList();
        assertThatThrownBy(() -> service.save("pom.xml", dependencies, results)).isInstanceOf(RuntimeException.class);
        assertThat(repository.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from scan_dependencies", Long.class)).isZero();
    }

    @Test void incompleteResultsAreNeverSaved() {
        assertThatThrownBy(() -> service.save("pom.xml", List.of(new DependencyInfo("g", "a", "1")), List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.count()).isZero();
    }

@Test void historyAndDetailEachUseOneQuery() {
    var dependency = new DependencyInfo("g", "a", "1");
    var result = new DependencyResult("g", "a", "1", List.of(), AnalysisStatus.ANALYZED);
    var saved = service.save("pom.xml", List.of(dependency), List.of(result));
    service.save("pom.xml", List.of(dependency), List.of(result));
    var statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    try {
        statistics.clear();
        assertThat(service.history()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getEntityLoadCount()).isZero();
        statistics.clear();
        assertThat(service.detail(saved.scanId()).dependencies()).hasSize(1);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    } finally {
        statistics.setStatisticsEnabled(false);
    }
}

@Test void upgradesExistingV1DataWithoutChangingV1() {
    var schema = "migration_upgrade_test";
    org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
            .target("1").load().migrate();
    var id = jdbc.queryForObject("insert into " + schema + ".scans(filename) values ('old.xml') returning id", Long.class);
    jdbc.update("insert into " + schema + ".scan_dependencies(scan_id, group_id, artifact_id, version) values (?, 'g', 'a', ?)", id, "${revision}");
    org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
            .load().migrate();
    assertThat(jdbc.queryForObject("select status from " + schema + ".scan_dependencies", String.class))
            .isEqualTo("SKIPPED_UNRESOLVED_VERSION");
    assertThat(jdbc.queryForObject("select filename from " + schema + ".scans", String.class)).isEqualTo("old.xml");
}

    @Test void flywayHasAppliedBothMigrations() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success and version in ('1','2')", Long.class))
                .isEqualTo(2);
    }
}

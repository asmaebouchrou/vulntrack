package com.vulntrack.backend.dependecy;

import com.vulntrack.backend.PostgresTest;
import com.vulntrack.backend.dependency.*;
import com.vulntrack.backend.scan.ScanDtos.*;
import com.vulntrack.backend.scan.ScanRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DependencyIntegrationTest extends PostgresTest {
    @Autowired MockMvc mvc;
    @Autowired ScanRepository repository;
    @MockitoBean OsvService osv;
    private final String pom = """
        <project xmlns="http://maven.apache.org/POM/4.0.0"><dependencies>
          <dependency><groupId>g</groupId><artifactId>a</artifactId><version>1</version></dependency>
          <dependency><groupId>g</groupId><artifactId>b</artifactId></dependency>
        </dependencies></project>
        """;

    @BeforeEach void clear() { repository.deleteAll(); }

    private MockMultipartFile file(String filename, String content) {
        return new MockMultipartFile("file", filename, "application/xml", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test void uploadPersistsAndHistoryAndDetailReturnDtos() throws Exception {
        when(osv.scan(anyList())).thenReturn(List.of(
                new DependencyResult("g", "a", "1", List.of(Map.of("id", "GHSA-EXAMPLE")), AnalysisStatus.ANALYZED),
                new DependencyResult("g", "b", null, List.of(), AnalysisStatus.SKIPPED_UNRESOLVED_VERSION)));
        mvc.perform(multipart("/api/v1/dependencies/upload").file(file("pom.xml", pom)).with(user("test")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scanId").isNumber())
                .andExpect(jsonPath("$.results[0].vulnerabilities[0].id").value("GHSA-EXAMPLE"))
                .andExpect(jsonPath("$.results[1].status").value("SKIPPED_UNRESOLVED_VERSION"));
        var id = repository.findAll().getFirst().getId();
        mvc.perform(get("/api/v1/scans").with(user("another-user"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].dependencyCount").value(2))
                .andExpect(jsonPath("$[0].vulnerableDependencyCount").value(1));
        mvc.perform(get("/api/v1/scans/{id}", id).with(user("test"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("pom.xml")).andExpect(jsonPath("$.dependencies.length()").value(2));
        mvc.perform(get("/api/v1/scans/9223372036854775807").with(user("test"))).andExpect(status().isNotFound());
    }

    @Test void osvFailureDoesNotPersistAnything() throws Exception {
        when(osv.scan(anyList())).thenThrow(new OsvException(new IllegalStateException("sensitive upstream detail")));
        mvc.perform(multipart("/api/v1/dependencies/upload").file(file("pom.xml", pom)).with(user("test")).with(csrf()))
                .andExpect(status().isBadGateway()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sensitive"))));
        assertThat(repository.count()).isZero();
    }

@Test void persistenceErrorReturnsSafe503AndRollsBack() throws Exception {
    when(osv.scan(anyList())).thenAnswer(invocation -> {
        List<DependencyInfo> dependencies = invocation.getArgument(0);
        return dependencies.stream().map(d -> new DependencyResult(d.groupId(), d.artifactId(), d.version(),
                List.<Map<String, Object>>of(), d.hasResolvedVersion() ? AnalysisStatus.ANALYZED
                : AnalysisStatus.SKIPPED_UNRESOLVED_VERSION)).toList();
    });
    var invalid = pom.replace("<artifactId>a</artifactId>", "<artifactId>" + "x".repeat(256) + "</artifactId>");
    mvc.perform(multipart("/api/v1/dependencies/upload").file(file("pom.xml", invalid)).with(user("test")).with(csrf()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("No se ha podido acceder al historial de análisis"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQLException"))));
    assertThat(repository.count()).isZero();
}

    @Test void rejectsInvalidEmptyAndWrongFilename() throws Exception {
        for (var file : List.of(file("pom.xml", ""), file("other.xml", pom), file("pom.xml", "<broken"),
                file("pom.xml", "<!DOCTYPE project [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><project>&x;</project>"))) {
            mvc.perform(multipart("/api/v1/dependencies/upload").file(file).with(user("test")).with(csrf()))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(osv);
        assertThat(repository.count()).isZero();
    }

    @Test void securityProtectsHistoryAndUploadAndRetainsCsrf() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/scans")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/scans/1")).andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/api/v1/dependencies/upload").file(file("pom.xml", pom)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/api/v1/dependencies/upload").file(file("pom.xml", pom)).with(user("test")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(osv);
    }
}

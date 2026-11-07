package com.vulntrack.backend.dependency;

import com.vulntrack.backend.scan.ScanDtos.AnalysisStatus;
import com.vulntrack.backend.scan.ScanDtos.DependencyResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OsvService {
    private final RestClient client;

    public OsvService() { this(defaultClient()); }

    private static RestClient defaultClient() {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5)).build();
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(java.time.Duration.ofSeconds(20));
        return RestClient.builder().baseUrl("https://api.osv.dev").requestFactory(factory).build();
    }

    OsvService(RestClient client) { this.client = client; }

    public List<DependencyResult> scan(List<DependencyInfo> dependencies) {
        try {
            return query(dependencies);
        } catch (RuntimeException exception) {
            throw new OsvException(exception);
        }
    }

    private List<DependencyResult> query(List<DependencyInfo> dependencies) {
        var queries = dependencies.stream().filter(d -> d.hasResolvedVersion())
                .map(d -> Map.of("package", Map.of("ecosystem", "Maven", "name", d.groupId() + ":" + d.artifactId()),
                        "version", d.version())).toList();
        List<?> results = List.of();
        if (!queries.isEmpty()) {
            Map<?, ?> response = client.post().uri("/v1/querybatch").body(Map.of("queries", queries))
                    .retrieve().body(Map.class);
            if (response == null || !(response.get("results") instanceof List<?> list)
                    || list.size() != queries.size()) {
                throw new IllegalStateException("Respuesta inesperada de OSV");
            }
            results = list;
        }

        List<DependencyResult> output = new ArrayList<>();
        int resultIndex = 0;
        for (DependencyInfo dependency : dependencies) {
            if (!dependency.hasResolvedVersion()) {
                output.add(new DependencyResult(dependency.groupId(), dependency.artifactId(), dependency.version(),
                        List.of(), AnalysisStatus.SKIPPED_UNRESOLVED_VERSION));
                continue;
            }
            if (!(results.get(resultIndex++) instanceof Map<?, ?> result)
                    || result.containsKey("next_page_token")) {
                throw new IllegalStateException("Resultado OSV incompleto o inválido");
            }
            Object rawVulns = result.containsKey("vulns") ? result.get("vulns") : List.of();
            if (!(rawVulns instanceof List<?> vulns)) {
                throw new IllegalStateException("Listado de vulnerabilidades inválido");
            }
            List<Map<String, Object>> vulnerabilities = new ArrayList<>();
            for (Object value : vulns) {
                if (!(value instanceof Map<?, ?> vulnerability)
                        || !(vulnerability.get("id") instanceof String id) || id.isBlank()
                        || vulnerability.keySet().stream().anyMatch(key -> !(key instanceof String))) {
                    throw new IllegalStateException("Vulnerabilidad inválida");
                }
                vulnerabilities.add(stringKeyedMap(vulnerability));
            }
            output.add(new DependencyResult(dependency.groupId(), dependency.artifactId(), dependency.version(),
                    vulnerabilities, AnalysisStatus.ANALYZED));
        }
        return output;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> stringKeyedMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}

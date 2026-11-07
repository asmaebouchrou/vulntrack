package com.vulntrack.backend.dependency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OsvService {

    private final RestClient client = RestClient.create(
            "https://api.osv.dev");

    public List<Map<String, Object>> scan(
            List<DependencyInfo> dependencies) {

        List<Map<String, Object>> queries = new ArrayList<>();
        List<DependencyInfo> validDependencies = new ArrayList<>();

        for (DependencyInfo dependency : dependencies) {

            if (dependency.version() == null
                    || dependency.version().contains("${")) {
                continue;
            }

            queries.add(Map.of(
                    "package", Map.of(
                            "ecosystem", "Maven",
                            "name", dependency.groupId()
                                    + ":" + dependency.artifactId()),
                    "version", dependency.version()));

            validDependencies.add(dependency);
        }

        if (queries.isEmpty()) {
            return List.of();
        }

        Map<?, ?> response = client.post()
                .uri("/v1/querybatch")
                .body(Map.of("queries", queries))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "OSV no ha devuelto una respuesta");
        }

        Object rawResults = response.get("results");

        if (!(rawResults instanceof List<?> results)
                || results.size() != validDependencies.size()) {
            throw new IllegalStateException(
                    "Respuesta inesperada de OSV");
        }

        List<Map<String, Object>> output = new ArrayList<>();

        for (int i = 0; i < validDependencies.size(); i++) {

            DependencyInfo dependency = validDependencies.get(i);
            Object rawResult = results.get(i);

            if (!(rawResult instanceof Map<?, ?> result)) {
                throw new IllegalStateException(
                        "Resultado OSV inválido");
            }

            if (result.containsKey("next_page_token")) {
                throw new IllegalStateException(
                        "OSV ha devuelto resultados paginados");
            }

            Object rawVulns = result.containsKey("vulns")
                    ? result.get("vulns")
                    : List.of();

            if (!(rawVulns instanceof List<?> vulns)) {
                throw new IllegalStateException(
                        "Listado de vulnerabilidades inválido");
            }

            output.add(Map.of(
                    "groupId", dependency.groupId(),
                    "artifactId", dependency.artifactId(),
                    "version", dependency.version(),
                    "vulnerabilities", vulns));
        }

        return output;
    }
}

package com.vulntrack.backend.dependency;

import com.vulntrack.backend.scan.ScanDtos.AnalysisStatus;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OsvServiceTest {

    private MockRestServiceServer server;
    private OsvService service;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder()
                .baseUrl("https://api.osv.dev");

        server = MockRestServiceServer.bindTo(builder).build();
        service = new OsvService(builder.build());
    }

    @Test
    void queriesOnlyResolvedDependenciesAndPreservesOriginalOrder() {

        // Primera petición: consultar vulnerabilidades
        server.expect(requestTo("https://api.osv.dev/v1/querybatch"))
                .andExpect(content().json("""
                    {
                        "queries": [
                            {
                                "package": {
                                    "ecosystem": "Maven",
                                    "name": "g:a"
                                },
                                "version": "1"
                            },
                            {
                                "package": {
                                    "ecosystem": "Maven",
                                    "name": "g:c"
                                },
                                "version": "2"
                            }
                        ]
                    }
                    """))
                .andRespond(withSuccess(
                        """
                        {
                            "results": [
                                {
                                    "vulns": [
                                        {
                                            "id": "GHSA-EXAMPLE"
                                        }
                                    ]
                                },
                                {}
                            ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        // Segunda petición: obtener los detalles
        server.expect(requestTo(
                "https://api.osv.dev/v1/vulns/GHSA-EXAMPLE"
        ))
                .andRespond(withSuccess(
                        """
                        {
                            "id": "GHSA-EXAMPLE",
                            "summary": "Vulnerabilidad de prueba",
                            "severity": [],
                            "affected": [],
                            "references": []
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        // Ejecutar el servicio
        var output = service.scan(List.of(
                new DependencyInfo("g", "b", "${revision}"),
                new DependencyInfo("g", "a", "1"),
                new DependencyInfo("g", "missing", null),
                new DependencyInfo("g", "c", "2")
        ));

        // Comprobar que se conserva el orden
        assertThat(output)
                .extracting(r -> r.artifactId())
                .containsExactly("b", "a", "missing", "c");

        // Comprobar dependencia sin versión resuelta
        assertThat(output.get(0).status())
                .isEqualTo(AnalysisStatus.SKIPPED_UNRESOLVED_VERSION);

        // Comprobar vulnerabilidad encontrada
        assertThat(output.get(1).vulnerabilities().getFirst())
                .containsEntry("id", "GHSA-EXAMPLE");

        // Comprobar que recuperamos sus detalles
        assertThat(output.get(1).vulnerabilities().getFirst())
                .containsEntry("summary", "Vulnerabilidad de prueba");

        // Comprobar dependencia sin vulnerabilidades
        assertThat(output.get(3).vulnerabilities())
                .isEmpty();

        // Verificar que se realizaron todas las peticiones previstas
        server.verify();
    }

    @Test
    void noNetworkCallWhenAllVersionsAreUnresolved() {

        assertThat(service.scan(List.of(
                new DependencyInfo("g", "a", null),
                new DependencyInfo("g", "b", " ")
        )))
                .allSatisfy(r ->
                        assertThat(r.status())
                                .isEqualTo(
                                        AnalysisStatus.SKIPPED_UNRESOLVED_VERSION
                                )
                );

        assertThat(service.scan(List.of()))
                .isEmpty();

        server.verify();
    }

    @Test
    void rejectsMalformedIncompleteAndPaginatedResponses() {

        for (var response : List.of(
                "{}",
                "{\"results\":[]}",
                "{\"results\":[null]}",
                "{\"results\":[{\"next_page_token\":\"next\"}]}",
                "{\"results\":[{\"vulns\":\"invalid\"}]}",
                "{\"results\":[{\"vulns\":[{}]}]}"
        )) {

            server.reset();

            server.expect(requestTo(
                    "https://api.osv.dev/v1/querybatch"
            ))
                    .andRespond(withSuccess(
                            response,
                            MediaType.APPLICATION_JSON
                    ));

            assertThatThrownBy(() ->
                    service.scan(List.of(
                            new DependencyInfo("g", "a", "1")
                    ))
            )
                    .isInstanceOf(OsvException.class);

            server.verify();
        }
    }

    @Test
    void wrapsUpstreamHttpFailure() {

        server.expect(requestTo(
                "https://api.osv.dev/v1/querybatch"
        ))
                .andRespond(withServerError());

        assertThatThrownBy(() ->
                service.scan(List.of(
                        new DependencyInfo("g", "a", "1")
                ))
        )
                .isInstanceOf(OsvException.class);

        server.verify();
    }
}

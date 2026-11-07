package com.vulntrack.backend.dependency;

import java.util.List;

public final class OsvDtos {

    private OsvDtos() {}

    public record OsvPackage(
            String ecosystem,
            String name
    ) {}

    public record OsvQuery(
            OsvPackage pkg,
            String version
    ) {}

    public record OsvBatchRequest(
            List<OsvQuery> queries
    ) {}

    public record OsvVulnerability(
            String id
    ) {}

    public record OsvResult(
            List<OsvVulnerability> vulns,
            String next_page_token
    ) {}

    public record OsvBatchResponse(
            List<OsvResult> results
    ) {}
}
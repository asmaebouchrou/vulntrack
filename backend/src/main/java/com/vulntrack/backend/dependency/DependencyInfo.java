package com.vulntrack.backend.dependency;

public record DependencyInfo(
        String groupId,
        String artifactId,
        String version) {
    public boolean hasResolvedVersion() {
        return version != null && !version.isBlank() && !version.contains("${");
    }
}

package com.vulntrack.backend.dependency;

public record DependencyInfo(
        String groupId,
        String artifactId,
        String version) {

}

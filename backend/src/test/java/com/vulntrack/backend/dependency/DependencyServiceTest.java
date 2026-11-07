package com.vulntrack.backend.dependency;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DependencyServiceTest {

    private final DependencyService service = new DependencyService();

    @Test
    void shouldExtractMavenDependencies() {

        String xml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-core</artifactId>
                            <version>2.14.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pom.xml",
                "application/xml",
                xml.getBytes(StandardCharsets.UTF_8));

        var dependencies = service.extractDependencies(file);

        assertEquals(1, dependencies.size());
        assertEquals("org.apache.logging.log4j", dependencies.get(0).groupId());
        assertEquals("log4j-core", dependencies.get(0).artifactId());
        assertEquals("2.14.1", dependencies.get(0).version());
    }

    @Test
    void shouldRejectMalformedXml() {

        String xml = """
                <project>
                    <dependencies>
                        <dependency>
                </project>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pom.xml",
                "application/xml",
                xml.getBytes(StandardCharsets.UTF_8));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.extractDependencies(file));
    }

    @Test
    void shouldRejectXmlWithDoctype() {

        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE project [
                    <!ENTITY secret SYSTEM "file:///etc/passwd">
                ]>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <description>&secret;</description>
                </project>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pom.xml",
                "application/xml",
                xml.getBytes(StandardCharsets.UTF_8));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.extractDependencies(file));
    }
}
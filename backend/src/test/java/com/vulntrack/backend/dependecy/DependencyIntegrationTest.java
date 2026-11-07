package com.vulntrack.backend.dependecy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DependencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAnalyzeMavenDependencies() throws Exception {

        byte[] content = Files.readAllBytes(
                Path.of("../test-files/pom.xml")
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pom.xml",
                "application/xml",
                content
        );

        mockMvc.perform(
                multipart("/api/v1/dependencies/upload")
                        .file(file)
                        .with(user("test-user"))
                        .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath(
                "$.results[0].artifactId"
        ).value("log4j-core"))
        .andDo(result -> {
            System.out.println(
                    result.getResponse().getContentAsString()
            );
        });
    }
}
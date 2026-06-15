package com.nitrotech.api.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainBoundaryTest {

    @Test
    void domainCodeDoesNotImportInfrastructure() throws IOException {
        Path domainRoot = Path.of("src/main/java/com/nitrotech/api/domain");

        List<Path> offenders;
        try (var files = Files.walk(domainRoot)) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::importsInfrastructure)
                    .toList();
        }

        assertThat(offenders)
                .as("Domain classes must depend on domain ports, not infrastructure classes")
                .isEmpty();
    }

    private boolean importsInfrastructure(Path path) {
        try {
            return Files.readString(path).contains("import com.nitrotech.api.infrastructure");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}

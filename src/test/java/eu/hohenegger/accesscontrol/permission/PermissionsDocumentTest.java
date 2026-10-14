package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.web.PolicyRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps PERMISSIONS.md, the version of the policy that domain experts review, in sync with the code. A change to
 * the policy fails this test until the document is regenerated, so every change shows up as a readable diff.
 */
@DataJpaTest
@Import(PolicyRules.class)
class PermissionsDocumentTest {

    private static final Path DOCUMENT = Path.of("PERMISSIONS.md");

    @Autowired
    Policy policy;

    @Test
    void permissionsDocumentIsUpToDate() throws IOException {
        String rendered = PolicyDocument.render(policy);
        if (System.getProperty("permissions.update") != null || !Files.exists(DOCUMENT)) {
            Files.writeString(DOCUMENT, rendered);
        }
        assertThat(Files.readString(DOCUMENT))
                .as("PERMISSIONS.md is out of date. Review the policy change, then regenerate the document with "
                        + "./mvnw test -Dtest=PermissionsDocumentTest -Dpermissions.update")
                .isEqualTo(rendered);
    }
}

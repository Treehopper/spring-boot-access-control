package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.permission.PolicyConfiguration;
import eu.hohenegger.accesscontrol.permission.model.PolicyStore;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The policy with all rule classes, found the same way the application finds them, and stored in the database.
 * Needs the JPA auto-configuration, e.g. from {@code @DataJpaTest} or {@code @AutoConfigureDataJpa}.
 */
@Configuration
@Import({PolicyConfiguration.class, PolicyStore.class})
@ComponentScan("eu.hohenegger.accesscontrol.permission.rules")
public class PolicyRules {
}

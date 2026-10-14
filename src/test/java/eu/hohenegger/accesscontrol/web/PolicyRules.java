package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.permission.PolicyConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** The policy with all rule classes, found the same way the application finds them. */
@Configuration
@Import(PolicyConfiguration.class)
@ComponentScan("eu.hohenegger.accesscontrol.permission.rules")
public class PolicyRules {
}

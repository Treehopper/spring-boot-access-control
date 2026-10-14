package eu.hohenegger.accesscontrol.permission;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static eu.hohenegger.accesscontrol.permission.Audit.LOG;

/** Builds the {@link Policy} from all {@link PolicyCustomizer} beans. */
@Configuration
public class PolicyConfiguration {

    @Bean
    Policy policy(ObjectProvider<PolicyCustomizer> customizers) {
        Policy.Builder builder = Policy.define();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        Policy policy = builder.build();
        LOG.info("POLICY  {} permissions: {}", policy.permissions().size(), policy.describe());
        return policy;
    }
}

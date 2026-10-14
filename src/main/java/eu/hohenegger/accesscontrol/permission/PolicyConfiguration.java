package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.permission.model.PolicyStore;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.IdentityHashMap;
import java.util.Map;

import static eu.hohenegger.accesscontrol.permission.Audit.LOG;

/**
 * Builds the policy model from all {@link PolicyCustomizer} beans, stores it in the database and creates the
 * {@link Policy} from what was stored.
 */
@Configuration
public class PolicyConfiguration {

    @Bean
    Policy policy(ObjectProvider<PolicyCustomizer> customizers, ListableBeanFactory beans, PolicyStore store) {
        Map<PolicyCustomizer, String> beanNames = new IdentityHashMap<>();
        beans.getBeansOfType(PolicyCustomizer.class).forEach((name, customizer) -> beanNames.put(customizer, name));

        Policy.Builder builder = Policy.define();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder.definedBy(beanNames.get(customizer))));
        Policy policy = Policy.of(store.replaceWith(builder.build()));
        LOG.info("POLICY  {} permissions stored: {}", policy.permissions().size(), policy.describe());
        return policy;
    }
}

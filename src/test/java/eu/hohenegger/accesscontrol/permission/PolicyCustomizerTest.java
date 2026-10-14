package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.web.PolicyRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static eu.hohenegger.accesscontrol.permission.Action.VIEW_ASSOCIATION;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_POLLS;
import static eu.hohenegger.accesscontrol.permission.Relationships.DISTRICT_MANAGERS;
import static eu.hohenegger.accesscontrol.permission.Relationships.OWNERS;
import static eu.hohenegger.accesscontrol.permission.Relationships.TENANTS;
import static org.assertj.core.api.Assertions.assertThat;

/** A feature extends the policy just by declaring a {@link PolicyCustomizer} bean. */
@DataJpaTest
@Import({PolicyRules.class, PolicyCustomizerTest.TenantPolls.class})
class PolicyCustomizerTest {

    @TestConfiguration
    static class TenantPolls {

        @Bean
        PolicyCustomizer tenantsMayFollowPolls() {
            return policy -> policy.allow(TENANTS).to(VIEW_POLLS);
        }
    }

    @Autowired
    Policy policy;

    @Test
    void customizerBeansAddToTheRulesOfAllFeatures() {
        assertThat(policy.relationshipsAllowedTo(VIEW_POLLS)).containsExactlyInAnyOrder(OWNERS, DISTRICT_MANAGERS, TENANTS);
        assertThat(policy.relationshipsAllowedTo(VIEW_ASSOCIATION)).containsExactlyInAnyOrder(OWNERS, TENANTS, DISTRICT_MANAGERS);
    }
}

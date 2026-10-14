package eu.hohenegger.accesscontrol.permission.rules;

import eu.hohenegger.accesscontrol.permission.PolicyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static eu.hohenegger.accesscontrol.permission.Action.MANAGE_DELEGATES;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_DUES;
import static eu.hohenegger.accesscontrol.permission.Relationships.ACCOUNT_HOLDERS;
import static eu.hohenegger.accesscontrol.permission.Relationships.DELEGATES;
import static eu.hohenegger.accesscontrol.permission.Relationships.DISTRICT_MANAGERS;

/** Dues accounts: private to their holder, who may share them. */
@Configuration
public class DuesRules {

    @Bean
    PolicyCustomizer duesPermissions() {
        return policy -> policy
                .allow(ACCOUNT_HOLDERS).to(VIEW_DUES, MANAGE_DELEGATES)
                .allow(DELEGATES).to(VIEW_DUES)
                .allow(DISTRICT_MANAGERS).to(VIEW_DUES, MANAGE_DELEGATES);
    }
}

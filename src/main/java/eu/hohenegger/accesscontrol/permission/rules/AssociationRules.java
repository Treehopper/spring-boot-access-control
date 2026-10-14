package eu.hohenegger.accesscontrol.permission.rules;

import eu.hohenegger.accesscontrol.permission.PolicyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static eu.hohenegger.accesscontrol.permission.Action.COUNT_TENANTS;
import static eu.hohenegger.accesscontrol.permission.Action.MANAGE_RESIDENTS;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_ASSOCIATION;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_OWNERS;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_TENANT;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_VACANCIES;
import static eu.hohenegger.accesscontrol.permission.Relationships.DISTRICT_MANAGERS;
import static eu.hohenegger.accesscontrol.permission.Relationships.LANDLORDS;
import static eu.hohenegger.accesscontrol.permission.Relationships.OWNERS;
import static eu.hohenegger.accesscontrol.permission.Relationships.TENANTS;

/** Seeing an association and who lives there, and managing its residents. */
@Configuration
public class AssociationRules {

    @Bean
    PolicyCustomizer associationPermissions() {
        return policy -> policy
                .allow(OWNERS).to(VIEW_ASSOCIATION, VIEW_OWNERS, COUNT_TENANTS)
                .allow(TENANTS).to(VIEW_ASSOCIATION)
                .allow(LANDLORDS).to(VIEW_TENANT)
                .allow(DISTRICT_MANAGERS).to(VIEW_ASSOCIATION, VIEW_OWNERS, COUNT_TENANTS, VIEW_TENANT, VIEW_VACANCIES,
                        MANAGE_RESIDENTS);
    }
}

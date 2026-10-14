package eu.hohenegger.accesscontrol.permission.rules;

import eu.hohenegger.accesscontrol.permission.PolicyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static eu.hohenegger.accesscontrol.permission.Action.CREATE_POLL;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_POLLS;
import static eu.hohenegger.accesscontrol.permission.Action.VOTE;
import static eu.hohenegger.accesscontrol.permission.Relationships.DISTRICT_MANAGERS;
import static eu.hohenegger.accesscontrol.permission.Relationships.OWNERS;

/** Polls: owners decide, management asks. */
@Configuration
public class PollRules {

    @Bean
    PolicyCustomizer pollPermissions() {
        return policy -> policy
                .allow(OWNERS).to(VIEW_POLLS, VOTE)
                .allow(DISTRICT_MANAGERS).to(VIEW_POLLS, CREATE_POLL);
    }
}
